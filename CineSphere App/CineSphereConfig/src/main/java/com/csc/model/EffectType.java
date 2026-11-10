package com.csc.model;

/**
 * Tipos de efecto que se pueden colocar en la línea de tiempo.
 * Cada tipo pertenece a un carril (lane) y puede tener un parámetro configurable
 * (potencia del ventilador, ángulo del servo...).
 */
public enum EffectType {

    FAN_LEFT_TO_RIGHT("Ventilador izq → der", "Mover ventilador de izquierda a derecha",
            "bi-arrow-right", "effect-fan", 0, "FAN", Param.FAN_POWER),
    FAN_RIGHT_TO_LEFT("Ventilador der → izq", "Mover ventilador de derecha a izquierda",
            "bi-arrow-left", "effect-fan", 0, "FAN", Param.FAN_POWER),
    SERVO_ROTATE("Girar servo", "Girar el servo de los difusores",
            "bi-arrow-repeat", "effect-servo", 1, "SERVO", Param.SERVO_ANGLE),
    DIFFUSER_1("Difusor 1", "Activar difusor 1", "bi-droplet-half", "effect-diffuser-1", 2, "DIFFUSER", null),
    DIFFUSER_2("Difusor 2", "Activar difusor 2", "bi-droplet-half", "effect-diffuser-2", 3, "DIFFUSER", null),
    DIFFUSER_3("Difusor 3", "Activar difusor 3", "bi-droplet-half", "effect-diffuser-3", 4, "DIFFUSER", null),
    DIFFUSER_4("Difusor 4", "Activar difusor 4", "bi-droplet-half", "effect-diffuser-4", 5, "DIFFUSER", null);

    /** Parámetro configurable de un efecto. */
    public enum Param {
        FAN_POWER("Potencia", "%", 0, 100, 100, "bi-speedometer2"),
        SERVO_ANGLE("Ángulo", "°", 0, 180, 90, "bi-compass");

        private final String label;
        private final String unit;
        private final double min;
        private final double max;
        private final double defaultValue;
        private final String iconLiteral;

        Param(String label, String unit, double min, double max, double defaultValue, String iconLiteral) {
            this.label = label;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.iconLiteral = iconLiteral;
        }

        public String getLabel() { return label; }
        public String getUnit() { return unit; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getDefaultValue() { return defaultValue; }
        public String getIconLiteral() { return iconLiteral; }

        public String format(double value) {
            return Math.round(value) + ("%".equals(unit) ? " %" : unit);
        }
    }

    /** Nombre, icono y estilo de cada carril de la línea de tiempo. */
    public static final String[] LANE_NAMES = {"Ventilador", "Servo difusores", "Difusor 1", "Difusor 2", "Difusor 3", "Difusor 4"};
    public static final String[] LANE_ICONS = {"bi-wind", "bi-arrow-repeat", "bi-droplet", "bi-droplet", "bi-droplet", "bi-droplet"};
    public static final String[] LANE_STYLES = {"effect-fan", "effect-servo", "effect-diffuser-1", "effect-diffuser-2", "effect-diffuser-3", "effect-diffuser-4"};

    private final String label;
    private final String description;
    private final String iconLiteral;
    private final String styleClass;
    private final int lane;
    private final String device;
    private final Param param;

    EffectType(String label, String description, String iconLiteral, String styleClass,
               int lane, String device, Param param) {
        this.label = label;
        this.description = description;
        this.iconLiteral = iconLiteral;
        this.styleClass = styleClass;
        this.lane = lane;
        this.device = device;
        this.param = param;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getIconLiteral() { return iconLiteral; }
    public String getStyleClass() { return styleClass; }
    public int getLane() { return lane; }
    /** Dispositivo del ESP32 al que corresponde: FAN, SERVO o DIFFUSER. */
    public String getDevice() { return device; }
    /** Parámetro configurable, o {@code null} si el efecto no tiene. */
    public Param getParam() { return param; }
    public boolean hasParam() { return param != null; }

    public static int laneCount() { return LANE_NAMES.length; }
}
