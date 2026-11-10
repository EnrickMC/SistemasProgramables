# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/humidifiers.py
# Objetivo:  Encender y apagar los 4 difusores (humidificadores ACEBOTT)
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
from machine import Pin

import config


class Humidifiers:

    def __init__(self):
        off = 0 if config.HUMIDIFIER_ACTIVE_HIGH else 1
        self.pins = [Pin(p, Pin.OUT, value=off) for p in config.HUMIDIFIER_PINS]
        self.states = [False] * len(self.pins)

    def set(self, index, on):
        """index: 0-3 (difusor 1-4)."""
        if index < 0 or index >= len(self.pins) or self.states[index] == on:
            return
        self.states[index] = on
        level = on if config.HUMIDIFIER_ACTIVE_HIGH else not on
        self.pins[index].value(1 if level else 0)

    def set_all(self, states):
        for i, on in enumerate(states):
            self.set(i, on)

    def off(self):
        self.set_all([False] * len(self.pins))
