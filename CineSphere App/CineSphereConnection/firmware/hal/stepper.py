# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/stepper.py
# Objetivo:  Mover los ventiladores izq→der / der→izq con el paso a paso 28BYJ-48 y el ULN2003
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
from machine import Pin

try:
    import asyncio
except ImportError:
    import uasyncio as asyncio

import config

# Secuencia de medio paso (8 fases) para el 28BYJ-48
HALF_STEP = (
    (1, 0, 0, 0), (1, 1, 0, 0), (0, 1, 0, 0), (0, 1, 1, 0),
    (0, 0, 1, 0), (0, 0, 1, 1), (0, 0, 0, 1), (1, 0, 0, 1),
)


class Stepper:
    """Gira de forma continua mientras tenga una dirección asignada ('LTR', 'RTL' o None)."""

    def __init__(self):
        self.coils = [Pin(p, Pin.OUT, value=0) for p in config.STEPPER_PINS]
        self.direction = None
        self.phase = 0
        self.steps = 0          # contador de medios pasos (positivo = izq→der)

    def set_direction(self, direction):
        if direction not in ("LTR", "RTL"):
            direction = None
        self.direction = direction

    def stop(self):
        self.direction = None
        self._release()

    def _release(self):
        """Apaga las bobinas para que el motor no se caliente en reposo."""
        for c in self.coils:
            c.value(0)

    def _step(self, forward):
        self.phase = (self.phase + (1 if forward else -1)) % len(HALF_STEP)
        for coil, level in zip(self.coils, HALF_STEP[self.phase]):
            coil.value(level)

    async def run(self):
        """Tarea asíncrona: da un medio paso cada STEPPER_STEP_MS mientras haya dirección."""
        energized = False
        while True:
            if self.direction is None:
                if energized:
                    self._release()
                    energized = False
                await asyncio.sleep_ms(20)
                continue
            ltr = self.direction == "LTR"
            self._step(ltr == config.STEPPER_LTR_CLOCKWISE)
            self.steps += 1 if ltr else -1
            energized = True
            await asyncio.sleep_ms(config.STEPPER_STEP_MS)
