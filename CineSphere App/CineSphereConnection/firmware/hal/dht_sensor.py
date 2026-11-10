# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/dht_sensor.py
# Objetivo:  Leer temperatura y humedad del DHT22 respetando el intervalo mínimo de 2 s
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
import time

import dht
from machine import Pin

import config


class DhtSensor:

    def __init__(self):
        self.sensor = dht.DHT22(Pin(config.DHT_PIN))
        self.last_ms = None
        self.temperature = None
        self.humidity = None
        self.error = None

    def read(self):
        """Devuelve la última lectura; solo mide de nuevo si pasaron DHT_MIN_INTERVAL_MS."""
        now = time.ticks_ms()
        if self.last_ms is None or time.ticks_diff(now, self.last_ms) >= config.DHT_MIN_INTERVAL_MS:
            self.last_ms = now
            try:
                self.sensor.measure()
                self.temperature = self.sensor.temperature()
                self.humidity = self.sensor.humidity()
                self.error = None
            except OSError as e:
                self.error = "sin lectura (%s)" % e
        return {"temperature": self.temperature, "humidity": self.humidity, "error": self.error}
