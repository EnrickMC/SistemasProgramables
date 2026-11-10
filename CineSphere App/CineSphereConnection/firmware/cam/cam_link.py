# ============================================
# Proyecto:  CineSphere
# Archivo:   cam/cam_link.py
# Objetivo:  Enlace UART opcional ESP32-S2 <-> ESP32-CAM (GPIO12/13); aún no se usa en main.py
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
#
# Protocolo propuesto: una línea JSON por mensaje terminada en "\n".
# Uso futuro (ejemplo):
#     from cam.cam_link import CamLink
#     cam = CamLink()
#     cam.send({"cmd": "snapshot"})
#     msg = cam.receive()       # dict o None
#
import json

from machine import UART

import config


class CamLink:

    def __init__(self):
        self.uart = UART(config.CAM_UART_ID, baudrate=config.CAM_BAUDRATE,
                         tx=config.CAM_TX_PIN, rx=config.CAM_RX_PIN)
        self.buffer = b""

    def send(self, message):
        self.uart.write(json.dumps(message).encode() + b"\n")

    def receive(self):
        """Devuelve el siguiente mensaje completo recibido o None (no bloquea)."""
        data = self.uart.read()
        if data:
            self.buffer += data
        if b"\n" not in self.buffer:
            return None
        line, self.buffer = self.buffer.split(b"\n", 1)
        try:
            return json.loads(line.decode())
        except ValueError:
            return None
