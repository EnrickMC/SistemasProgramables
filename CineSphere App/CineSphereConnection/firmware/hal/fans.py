# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/fans.py
# Objetivo:  Controlar la potencia (0-100 %) de los dos ventiladores con el MOSFET AOD4184
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
import config
from hal.pwm_util import make_pwm, set_duty


class Fans:
    """Los dos ventiladores van en paralelo en un solo MOSFET: misma potencia para ambos."""

    def __init__(self):
        self.pwm = make_pwm(config.FANS_PWM_PIN, config.FANS_PWM_FREQ)
        self.power = 0

    def set_power(self, percent):
        percent = max(0, min(100, int(percent)))
        if percent == self.power:
            return
        self.power = percent
        if percent == 0:
            set_duty(self.pwm, 0)
            return
        min_p = config.FANS_MIN_PERCENT
        set_duty(self.pwm, (min_p + (100 - min_p) * percent / 100.0) / 100.0)

    def off(self):
        self.set_power(0)
