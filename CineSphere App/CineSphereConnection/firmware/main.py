# ============================================
# Proyecto:  CineSphere
# Archivo:   main.py
# Objetivo:  Arrancar el ESP32: red propia, servidor HTTP, motor paso a paso y bucle de efectos
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
#
# Flujo actual:  red AP (net/wifi_ap.py) + HTTP (net/http_server.py)
# Para después:  net/wifi_sta.py, net/mqtt_client.py y cam/cam_link.py (no se importan aquí todavía)
#
try:
    import asyncio
except ImportError:
    import uasyncio as asyncio

import config
from hal.actuators import Actuators
from hal.dht_sensor import DhtSensor
from hal.status_led import StatusLed
from net.http_server import HttpServer
from net.wifi_ap import start_access_point
from sequencer import Sequencer


async def control_loop(sequencer):
    while True:
        try:
            sequencer.update()
        except Exception as e:
            print("Error en el bucle de control:", e)
        await asyncio.sleep_ms(config.CONTROL_PERIOD_MS)


async def main():
    led = StatusLed()
    actuators = Actuators()
    actuators.all_off()
    sequencer = Sequencer(actuators)

    try:
        sensor = DhtSensor()
    except Exception as e:
        print("DHT22 no disponible:", e)
        sensor = None

    ip = start_access_point()
    await HttpServer(sequencer, ip, led, sensor).start()
    print("Servidor listo: http://%s/status" % ip)
    led.on()

    asyncio.create_task(actuators.stepper.run())
    await control_loop(sequencer)


try:
    asyncio.run(main())
finally:
    try:
        asyncio.new_event_loop()
    except Exception:
        pass
