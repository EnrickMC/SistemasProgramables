# CineSphereConnection: firmware del ESP32-S2 (MicroPython)

El ESP32-S2 crea su propia red WiFi y recibe por HTTP las instrucciones de **CineSphereConfig**.
Los pines siguen el diagrama `diagramas/cinesphere4d-conexiones.html` del proyecto.

## Estructura

```
firmware/
├── main.py              Arranque: red AP + servidor HTTP + paso a paso + bucle de efectos
├── config.py            Red WiFi, MQTT y TODOS los pines
├── sequencer.py         Ejecuta la secuencia de efectos en el tiempo
├── hal/                 Sensores y actuadores
│   ├── actuators.py     Agrupa todas las salidas
│   ├── fans.py          2 ventiladores, potencia 0-100 % (MOSFET AOD4184, GPIO1)
│   ├── stepper.py       28BYJ-48 + ULN2003: mueve ventiladores izq↔der (GPIO6-9)
│   ├── servo.py         Servo de los difusores 0-180° (GPIO10)
│   ├── humidifiers.py   4 difusores / humidificadores (GPIO2-5)
│   ├── dht_sensor.py    Temperatura y humedad DHT22 (GPIO11)
│   ├── status_led.py    LED de la placa (GPIO15)
│   └── pwm_util.py      Ayuda para PWM compatible entre firmwares
├── net/                 Comunicaciones
│   ├── wifi_ap.py       Levanta la red propia "CineSphere"      ← EN USO
│   ├── http_server.py   Servidor HTTP/JSON                      ← EN USO
│   ├── wifi_sta.py      Conectarse a una red WiFi existente     ← para después
│   └── mqtt_client.py   Cliente MQTT (umqtt.simple)             ← para después
└── cam/
    └── cam_link.py      UART con la ESP32-CAM (GPIO12/13)       ← para después
```

`wifi_sta.py`, `mqtt_client.py` y `cam_link.py` están listos, pero `main.py` todavía no los importa.

## Cargar en la placa

Copia el **contenido** de `firmware/` (no la carpeta) a la raíz del ESP32. `main.py` tiene que quedar en `/`.
Las carpetas `hal/`, `net/` y `cam/` se copian completas, con su `__init__.py`.

```
mpremote cp -r firmware/. :
```

También funciona con MicroPython Tools de PyCharm o con Thonny. Se necesita MicroPython 1.19 o superior.

## Uso

1. Alimenta el ESP32-S2. Aparece la red **`CineSphere`** con la contraseña `cinesphere123`. El LED de la placa se enciende cuando está listo.
2. Conecta la PC a esa red.
3. En CineSphereConfig deja la IP `192.168.4.1` y pulsa **Conectar**.
4. **Enviar** sube la secuencia. Con **Sincronizar con video**, el ESP32 reproduce y pausa junto con el video.
   El botón ▶ ejecuta la secuencia desde el cursor y ■ apaga todo.

> Mientras la PC esté conectada a la red del ESP32 no tendrá internet.

## Comportamiento de cada efecto

| Bloque | Qué hace en el hardware |
|---|---|
| Ventilador izq → der / der → izq | El paso a paso mueve los ventiladores en ese sentido y el MOSFET los enciende a la potencia del bloque (0-100 %) |
| Girar servo | El servo va al ángulo del bloque (0-180°) y se queda ahí hasta el siguiente bloque |
| Difusor 1-4 | El humidificador correspondiente está encendido mientras dura el bloque |

En pausa se apagan los ventiladores, el paso a paso y los difusores, y el servo se queda donde está. Al detener, además, el servo vuelve a 90°.

## Protocolo HTTP (JSON)

| Método y ruta | Cuerpo | Acción |
|---|---|---|
| `GET /status` | — | Estado: red, IP, reproducción, salidas y lectura del DHT22 |
| `POST /sequence` | `{"duration": 60, "events": [...]}` | Carga la secuencia |
| `POST /play` | `{"time": 12.5}` | Ejecuta desde ese segundo |
| `POST /pause` | `{}` | Pausa |
| `POST /stop` | `{}` | Detiene y apaga todo |
| `POST /test` | un evento | Ejecuta ese evento al instante |

```json
{"device": "FAN", "dir": "LTR", "start": 1.0, "end": 3.0, "value": 75}
{"device": "SERVO", "start": 2.0, "end": 4.0, "value": 120}
{"device": "DIFFUSER", "id": 1, "start": 0.5, "end": 2.5}
```

Para una prueba rápida, abre `http://192.168.4.1/status` en el navegador de un dispositivo conectado a la red del ESP32.
