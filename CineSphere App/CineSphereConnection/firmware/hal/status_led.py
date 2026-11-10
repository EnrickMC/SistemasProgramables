# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/status_led.py
# Objetivo:  Indicar con el LED de la placa que el ESP32 está listo o atendiendo una petición
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
from machine import Pin

import config


class StatusLed:
    """Encendido = listo. Parpadea (se apaga) mientras atiende una petición."""

    def __init__(self):
        self.pin = Pin(config.LED_PIN, Pin.OUT, value=0) if config.LED_PIN is not None else None

    def on(self):
        if self.pin:
            self.pin.value(1)

    def off(self):
        if self.pin:
            self.pin.value(0)
