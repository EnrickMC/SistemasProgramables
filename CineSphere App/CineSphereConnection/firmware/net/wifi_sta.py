# ============================================
# Proyecto:  CineSphere
# Archivo:   net/wifi_sta.py
# Objetivo:  Conectar el ESP32 a una red WiFi existente (modo estación); aún no se usa en main.py
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
#
# Uso futuro (ejemplo):
#     from net.wifi_sta import connect
#     ip = connect()            # usa STA_SSID / STA_PASSWORD de config.py
#
import time

import network

import config


def connect(ssid=config.STA_SSID, password=config.STA_PASSWORD, timeout_s=config.STA_TIMEOUT_S):
    """Se conecta a la red y devuelve la IP asignada. Lanza OSError si no lo logra a tiempo."""
    sta = network.WLAN(network.STA_IF)
    sta.active(True)
    if sta.isconnected():
        return sta.ifconfig()[0]

    print("Conectando a la red '%s'..." % ssid)
    sta.connect(ssid, password)
    start = time.ticks_ms()
    while not sta.isconnected():
        if time.ticks_diff(time.ticks_ms(), start) > timeout_s * 1000:
            sta.active(False)
            raise OSError("no se pudo conectar a '%s' en %d s" % (ssid, timeout_s))
        time.sleep_ms(250)

    ip = sta.ifconfig()[0]
    print("Conectado a '%s'. IP: %s" % (ssid, ip))
    return ip


def is_connected():
    return network.WLAN(network.STA_IF).isconnected()


def disconnect():
    sta = network.WLAN(network.STA_IF)
    if sta.isconnected():
        sta.disconnect()
    sta.active(False)
