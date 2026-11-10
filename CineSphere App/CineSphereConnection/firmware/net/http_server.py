# ============================================
# Proyecto:  CineSphere
# Archivo:   net/http_server.py
# Objetivo:  Servidor HTTP (asyncio) que recibe en JSON las órdenes de CineSphereConfig
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
#
#  GET  /status     -> estado del ESP32 (incluye lectura del DHT22)
#  POST /sequence   -> carga la secuencia {"duration": s, "events": [...]}
#  POST /play       -> {"time": s} ejecuta la secuencia desde ese segundo
#  POST /pause      -> pausa y apaga ventiladores/difusores
#  POST /stop       -> detiene, apaga todo y servo a reposo
#  POST /test       -> ejecuta un evento suelto al instante
#
import json

try:
    import asyncio
except ImportError:
    import uasyncio as asyncio

import config

REASONS = {200: "OK", 400: "Bad Request", 404: "Not Found", 405: "Method Not Allowed", 500: "Internal Server Error"}
MAX_BODY = 32 * 1024


class HttpServer:

    def __init__(self, sequencer, ip, led=None, sensor=None):
        self.seq = sequencer
        self.ip = ip
        self.led = led
        self.sensor = sensor

    async def start(self):
        return await asyncio.start_server(self._handle, "0.0.0.0", config.HTTP_PORT)

    # ------------------------------------------------------------ conexión
    async def _handle(self, reader, writer):
        status, payload = 500, {"ok": False, "error": "error interno"}
        try:
            if self.led:
                self.led.off()
            request_line = await reader.readline()
            if not request_line:
                return
            parts = request_line.decode().split(" ")
            if len(parts) < 2:
                status, payload = 400, {"ok": False, "error": "petición inválida"}
            else:
                method, path = parts[0], parts[1].split("?")[0]
                length = 0
                while True:
                    header = await reader.readline()
                    if not header or header == b"\r\n":
                        break
                    name, _, value = header.decode().partition(":")
                    if name.strip().lower() == "content-length":
                        length = int(value.strip())
                if length > MAX_BODY:
                    status, payload = 400, {"ok": False, "error": "cuerpo demasiado grande"}
                else:
                    body = b""
                    while len(body) < length:
                        chunk = await reader.read(length - len(body))
                        if not chunk:
                            break
                        body += chunk
                    status, payload = self._route(method, path, body)
        except Exception as e:
            status, payload = 500, {"ok": False, "error": str(e)}
        finally:
            await self._respond(writer, status, payload)
            if self.led:
                self.led.on()

    async def _respond(self, writer, status, payload):
        try:
            data = json.dumps(payload).encode()
            head = ("HTTP/1.1 %d %s\r\n"
                    "Content-Type: application/json\r\n"
                    "Content-Length: %d\r\n"
                    "Access-Control-Allow-Origin: *\r\n"
                    "Connection: close\r\n\r\n") % (status, REASONS.get(status, "OK"), len(data))
            writer.write(head.encode())
            writer.write(data)
            await writer.drain()
        except Exception:
            pass
        try:
            writer.close()
            await writer.wait_closed()
        except Exception:
            pass

    # ------------------------------------------------------------ rutas
    def _route(self, method, path, body):
        if path in ("/", "/status"):
            return 200, self._status()

        if method != "POST":
            return 405, {"ok": False, "error": "usa POST para %s" % path}

        try:
            data = json.loads(body.decode()) if body else {}
        except ValueError:
            return 400, {"ok": False, "error": "JSON inválido"}

        try:
            if path == "/sequence":
                count = self.seq.load(data)
                print("Secuencia cargada: %d eventos" % count)
                return 200, {"ok": True, "events": count}
            if path == "/play":
                self.seq.play(data.get("time", 0))
                return 200, {"ok": True, "time": self.seq.current_time()}
            if path == "/pause":
                self.seq.pause()
                return 200, {"ok": True}
            if path == "/stop":
                self.seq.stop()
                return 200, {"ok": True}
            if path == "/test":
                self.seq.test(data)
                return 200, {"ok": True}
        except ValueError as e:
            return 400, {"ok": False, "error": str(e)}

        return 404, {"ok": False, "error": "ruta desconocida: %s" % path}

    def _status(self):
        status = {"ok": True, "device": "CineSphere ESP32-S2", "ssid": config.AP_SSID, "ip": self.ip}
        status.update(self.seq.status())
        status["outputs"] = self.seq.act.snapshot()
        if self.sensor:
            status["environment"] = self.sensor.read()
        return status
