# ============================================
# Proyecto:  CineSphere
# Archivo:   config.py
# Objetivo:  Centralizar red WiFi, MQTT y pines del ESP32-S2 según el diagrama de conexiones
# Equipo:    Equipo 4
#            - Aguirre García Gabriel Enrique — 23240414
#            - Becerra Godinez Cristofer Haret — 23240370
#            - Ramos López Héctor Daniel — 23240423
# Materia:   Sistemas Programables — ISC TecNM León
# Semestre:  ago-dic-2026
# ============================================

# ---------------------------------------------------------------- Red propia (modo punto de acceso) — EN USO
# La PC se conecta a esta red y CineSphereConfig habla con http://192.168.4.1
AP_SSID = "CineSphere"
AP_PASSWORD = "cinesphere123"   # mínimo 8 caracteres
AP_CHANNEL = 6
HTTP_PORT = 80

# ---------------------------------------------------------------- Red WiFi existente (modo estación) — PARA DESPUÉS
STA_SSID = "MiRedWiFi"
STA_PASSWORD = "MiContrasena"
STA_TIMEOUT_S = 15

# ---------------------------------------------------------------- MQTT — PARA DESPUÉS
MQTT_BROKER = "192.168.1.100"
MQTT_PORT = 1883
MQTT_CLIENT_ID = "cinesphere-esp32"
MQTT_USER = None
MQTT_PASSWORD = None
MQTT_KEEPALIVE_S = 60
MQTT_TOPIC_CMD = b"cinesphere/cmd"        # órdenes que llegan al ESP32
MQTT_TOPIC_STATUS = b"cinesphere/status"  # estado que publica el ESP32

# ---------------------------------------------------------------- Ventiladores (2 en paralelo, un MOSFET AOD4184)
FANS_PWM_PIN = 1          # GPIO1 -> PWM+ del AOD4184
FANS_PWM_FREQ = 1000      # Hz
FANS_MIN_PERCENT = 0      # súbelo (p. ej. 25) si los ventiladores no arrancan con potencias bajas

# ---------------------------------------------------------------- Motor paso a paso 28BYJ-48 + ULN2003 (mueve los ventiladores)
STEPPER_PINS = [6, 7, 8, 9]   # GPIO6..9 -> IN1..IN4
STEPPER_STEP_MS = 2           # ms entre medios pasos (menor = más rápido; < 2 puede perder pasos)
STEPPER_LTR_CLOCKWISE = True  # invierte si "izquierda -> derecha" gira al revés

# ---------------------------------------------------------------- Servo de los difusores
SERVO_PIN = 10            # GPIO10, cable naranja
SERVO_FREQ = 50           # Hz
SERVO_MIN_US = 500        # pulso para 0°
SERVO_MAX_US = 2500       # pulso para 180°
SERVO_REST_ANGLE = 90     # posición al arrancar y al detener

# ---------------------------------------------------------------- Difusores (4 humidificadores ACEBOTT)
HUMIDIFIER_PINS = [2, 3, 4, 5]   # GPIO2..5 -> S de cada humidificador
HUMIDIFIER_ACTIVE_HIGH = True

# ---------------------------------------------------------------- Sensor DHT22
DHT_PIN = 11              # GPIO11 (10 kΩ a 3.3 V)
DHT_MIN_INTERVAL_MS = 2000

# ---------------------------------------------------------------- ESP32-CAM por UART (opcional) — PARA DESPUÉS
CAM_UART_ID = 1
CAM_TX_PIN = 12           # GPIO12 -> IO14 de la ESP32-CAM
CAM_RX_PIN = 13           # GPIO13 <- IO13 de la ESP32-CAM
CAM_BAUDRATE = 115200

# ---------------------------------------------------------------- LED de estado
LED_PIN = 15              # LED de la placa S2 Mini; None para desactivarlo

# ---------------------------------------------------------------- Bucle de control
CONTROL_PERIOD_MS = 20
