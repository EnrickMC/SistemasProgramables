# ============================================
# Proyecto:  CineSphere
# Archivo:   sequencer.py
# Objetivo:  Ejecutar en el tiempo la secuencia de efectos recibida y aplicarla a los actuadores
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
#
#  Formato de un evento (lo envía CineSphereConfig):
#    {"device": "FAN",      "dir": "LTR"|"RTL", "start": 1.0, "end": 3.0, "value": 0-100}
#    {"device": "SERVO",                        "start": 2.0, "end": 4.0, "value": 0-180}
#    {"device": "DIFFUSER", "id": 1-4,          "start": 0.5, "end": 2.5}
#
#  Reglas:
#   - FAN: el paso a paso mueve los ventiladores en esa dirección y el MOSFET les da esa potencia.
#   - SERVO: va al ángulo del bloque y se queda ahí hasta el siguiente bloque.
#   - DIFFUSER: encendido solo mientras dura su bloque.
#
import time

DEVICES = ("FAN", "SERVO", "DIFFUSER")


def validate_event(ev):
    """Normaliza un evento; lanza ValueError si no es válido."""
    device = ev.get("device")
    if device not in DEVICES:
        raise ValueError("device inválido: %s" % device)
    start = float(ev.get("start", 0))
    end = float(ev.get("end", start))
    if end < start:
        raise ValueError("end < start")
    out = {"device": device, "start": start, "end": end}
    if device == "FAN":
        direction = ev.get("dir", "LTR")
        if direction not in ("LTR", "RTL"):
            raise ValueError("dir inválido: %s" % direction)
        out["dir"] = direction
        out["value"] = max(0, min(100, int(ev.get("value", 100))))
    elif device == "SERVO":
        out["value"] = max(0, min(180, int(ev.get("value", 90))))
    else:
        idx = int(ev.get("id", 1))
        if idx < 1 or idx > 4:
            raise ValueError("id de difusor inválido: %s" % idx)
        out["id"] = idx
    return out


class Sequencer:

    def __init__(self, actuators):
        self.act = actuators
        self.events = []
        self.duration = 0.0
        self.playing = False
        self.offset = 0.0        # segundo de la secuencia en el que se pulsó play
        self.t0 = 0              # ticks_ms del play
        self.tests = []          # [(ticks_ms_inicio, evento)]

    # ------------------------------------------------------------ comandos
    def load(self, data):
        events = [validate_event(e) for e in data.get("events", [])]
        events.sort(key=lambda e: e["start"])
        self.events = events
        self.duration = float(data.get("duration", max([e["end"] for e in events] or [0])))
        return len(events)

    def play(self, from_seconds=0.0):
        self.offset = max(0.0, float(from_seconds))
        self.t0 = time.ticks_ms()
        self.playing = True

    def pause(self):
        if self.playing:
            self.offset = self.current_time()
        self.playing = False
        self.act.all_off(servo_rest=False)

    def stop(self):
        self.playing = False
        self.offset = 0.0
        self.tests = []
        self.act.all_off(servo_rest=True)

    def test(self, ev):
        ev = validate_event(ev)
        ev["end"] = ev["end"] - ev["start"]   # duración
        ev["start"] = 0.0
        self.tests.append((time.ticks_ms(), ev))

    # ------------------------------------------------------------ tiempo
    def current_time(self):
        if not self.playing:
            return self.offset
        return self.offset + time.ticks_diff(time.ticks_ms(), self.t0) / 1000.0

    # ------------------------------------------------------------ bucle de control
    def update(self):
        fan = None          # (dir, potencia)
        servo = None        # ángulo
        diffusers = [False, False, False, False]

        if self.playing:
            t = self.current_time()
            if self.duration > 0 and t > self.duration:
                self.playing = False
                self.offset = 0.0
                self.act.all_off(servo_rest=False)
            else:
                for ev in self.events:
                    if ev["start"] > t:
                        break
                    if t < ev["end"]:
                        fan, servo = self._apply(ev, fan, servo, diffusers)
                if servo is None:
                    servo = self._last_servo_angle(t)

        if self.tests:
            now = time.ticks_ms()
            alive = []
            for started, ev in self.tests:
                if time.ticks_diff(now, started) / 1000.0 < ev["end"]:
                    alive.append((started, ev))
                    fan, servo = self._apply(ev, fan, servo, diffusers)
            self.tests = alive

        # Los actuadores guardan su estado y solo escriben en los pines si cambia
        self.act.set_fan_effect(fan[0] if fan else None, fan[1] if fan else 0)
        self.act.humidifiers.set_all(diffusers)
        if servo is not None:
            self.act.servo.set_angle(servo)

    @staticmethod
    def _apply(ev, fan, servo, diffusers):
        device = ev["device"]
        if device == "FAN":
            fan = (ev["dir"], ev["value"])
        elif device == "SERVO":
            servo = ev["value"]
        else:
            diffusers[ev["id"] - 1] = True
        return fan, servo

    def _last_servo_angle(self, t):
        angle = None
        for ev in self.events:
            if ev["start"] > t:
                break
            if ev["device"] == "SERVO":
                angle = ev["value"]
        return angle

    def status(self):
        return {
            "playing": self.playing,
            "time": round(self.current_time(), 2),
            "duration": self.duration,
            "events": len(self.events),
            "tests": len(self.tests),
        }
