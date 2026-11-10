# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/actuators.py
# Objetivo:  Agrupar todas las salidas (ventiladores, paso a paso, servo, difusores) en un solo objeto
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
from hal.fans import Fans
from hal.humidifiers import Humidifiers
from hal.servo import Servo
from hal.stepper import Stepper


class Actuators:

    def __init__(self):
        self.fans = Fans()
        self.stepper = Stepper()
        self.servo = Servo()
        self.humidifiers = Humidifiers()

    def set_fan_effect(self, direction, power):
        """Efecto "Mover ventilador": el paso a paso mueve los ventiladores y el MOSFET fija su potencia."""
        if direction is None or power <= 0:
            self.stepper.set_direction(None)
            self.fans.off()
        else:
            self.stepper.set_direction(direction)
            self.fans.set_power(power)

    def all_off(self, servo_rest=True):
        self.set_fan_effect(None, 0)
        self.humidifiers.off()
        if servo_rest:
            self.servo.rest()

    def snapshot(self):
        return {
            "fans_power": self.fans.power,
            "stepper": {"dir": self.stepper.direction, "steps": self.stepper.steps},
            "servo": self.servo.angle,
            "diffusers": list(self.humidifiers.states),
        }
