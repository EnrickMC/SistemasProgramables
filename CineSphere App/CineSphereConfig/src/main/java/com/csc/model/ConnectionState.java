package com.csc.model;

/** Estado de la conexión con el ESP32. */
public enum ConnectionState {
    DISCONNECTED("Desconectado", "bi-wifi-off"),
    CONNECTING("Conectando...", "bi-hourglass-split"),
    CONNECTED("Conectado", "bi-wifi"),
    ERROR("Sin respuesta", "bi-exclamation-circle");

    private final String label;
    private final String iconLiteral;

    ConnectionState(String label, String iconLiteral) {
        this.label = label;
        this.iconLiteral = iconLiteral;
    }

    public String getLabel() { return label; }
    public String getIconLiteral() { return iconLiteral; }
    public String getStyleClass() { return "conn-" + name().toLowerCase(); }
}
