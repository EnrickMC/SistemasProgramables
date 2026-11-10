# ============================================
# Proyecto:  CineSphere
# Archivo:   net/wifi_ap.py
# Objetivo:  Levantar la red WiFi propia del ESP32 (modo punto de acceso) y devolver su IP
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
import time

import network

import config


def start_access_point(ssid=config.AP_SSID, password=config.AP_PASSWORD, channel=config.AP_CHANNEL):
    """Crea la red y devuelve la IP del ESP32 (normalmente 192.168.4.1)."""
    # Si la placa quedó en modo estación, se apaga para no interferir
    network.WLAN(network.STA_IF).active(False)

    ap = network.WLAN(network.AP_IF)
    ap.active(True)
    try:
        ap.config(ssid=ssid, key=password, channel=channel, security=network.AUTH_WPA_WPA2_PSK)
    except (TypeError, ValueError, AttributeError, OSError):
        # Firmwares anteriores a MicroPython 1.20 usan otros nombres de parámetros
        ap.config(essid=ssid, password=password, channel=channel, authmode=network.AUTH_WPA_WPA2_PSK)

    for _ in range(50):
        if ap.active():
            break
        time.sleep_ms(100)

    ip = ap.ifconfig()[0]
    print("Red WiFi '%s' activa (contraseña: %s)" % (ssid, password))
    print("IP del ESP32: %s" % ip)
    return ip


def stop_access_point():
    network.WLAN(network.AP_IF).active(False)
