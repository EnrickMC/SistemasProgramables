# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/servo.py
# Objetivo:  Girar el servo de los difusores entre 0 y 180 grados
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
import config
from hal.pwm_util import make_pwm, set_duty


class Servo:

    def __init__(self):
        self.pwm = make_pwm(config.SERVO_PIN, config.SERVO_FREQ)
        self.angle = None
        self.rest()

    def set_angle(self, angle):
        angle = max(0, min(180, int(angle)))
        if angle == self.angle:
            return
        self.angle = angle
        pulse_us = config.SERVO_MIN_US + (config.SERVO_MAX_US - config.SERVO_MIN_US) * angle / 180.0
        period_us = 1000000.0 / config.SERVO_FREQ
        set_duty(self.pwm, pulse_us / period_us)

    def rest(self):
        self.set_angle(config.SERVO_REST_ANGLE)
