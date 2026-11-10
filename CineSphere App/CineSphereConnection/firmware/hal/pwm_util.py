# ============================================
# Proyecto:  CineSphere
# Archivo:   hal/pwm_util.py
# Objetivo:  Crear salidas PWM y fijar su ciclo de trabajo de forma compatible entre firmwares
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
from machine import Pin, PWM


def make_pwm(pin, freq):
    """Crea una salida PWM apagada."""
    pwm = PWM(Pin(pin), freq=freq)
    set_duty(pwm, 0)
    return pwm


def set_duty(pwm, fraction):
    """fraction: 0.0 - 1.0. Usa duty_u16 si existe o duty (0-1023) en firmwares antiguos."""
    fraction = max(0.0, min(1.0, fraction))
    if hasattr(pwm, "duty_u16"):
        pwm.duty_u16(int(fraction * 65535))
    else:
        pwm.duty(int(fraction * 1023))
