# ============================================
# Proyecto:  CineSphere
# Archivo:   net/mqtt_client.py
# Objetivo:  Cliente MQTT (umqtt.simple) para recibir órdenes y publicar estado; aún no se usa en main.py
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================
#
# Requiere una red con salida al broker (ver net/wifi_sta.py).
# Si el firmware no trae umqtt:  mpremote mip install umqtt.simple
#
# Uso futuro (ejemplo):
#     from net.mqtt_client import MqttLink
#     link = MqttLink(on_message=lambda topic, msg: print(topic, msg))
#     link.connect()
#     link.publish_status({"ok": True})
#     # dentro del bucle principal:  link.poll()
#
import json

import config

try:
    from umqtt.simple import MQTTClient
except ImportError:
    MQTTClient = None


class MqttLink:

    def __init__(self, on_message=None):
        if MQTTClient is None:
            raise ImportError("falta umqtt.simple: mpremote mip install umqtt.simple")
        self.on_message = on_message
        self.client = MQTTClient(
            config.MQTT_CLIENT_ID,
            config.MQTT_BROKER,
            port=config.MQTT_PORT,
            user=config.MQTT_USER,
            password=config.MQTT_PASSWORD,
            keepalive=config.MQTT_KEEPALIVE_S,
        )
        self.client.set_callback(self._callback)
        self.connected = False

    def connect(self):
        self.client.connect()
        self.client.subscribe(config.MQTT_TOPIC_CMD)
        self.connected = True
        print("MQTT conectado a %s, escuchando %s" % (config.MQTT_BROKER, config.MQTT_TOPIC_CMD))

    def _callback(self, topic, msg):
        if self.on_message:
            self.on_message(topic, msg)

    def poll(self):
        """Revisa si llegó un mensaje (no bloquea). Llamar seguido desde el bucle principal."""
        if self.connected:
            self.client.check_msg()

    def publish(self, topic, payload):
        if isinstance(payload, (dict, list)):
            payload = json.dumps(payload)
        self.client.publish(topic, payload)

    def publish_status(self, status):
        self.publish(config.MQTT_TOPIC_STATUS, status)

    def disconnect(self):
        if self.connected:
            self.client.disconnect()
            self.connected = False
