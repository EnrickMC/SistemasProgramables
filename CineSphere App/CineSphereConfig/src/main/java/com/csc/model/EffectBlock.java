package com.csc.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Modelo de un bloque de efecto colocado en la línea de tiempo.
 * Datos: tipo, inicio, duración (segundos) y valor del parámetro
 * (potencia 0-100 % en ventiladores, ángulo 0-180° en el servo).
 */
public class EffectBlock {

    public static final double MIN_DURATION = 0.2;

    private final EffectType type;
    private final DoubleProperty start = new SimpleDoubleProperty();
    private final DoubleProperty duration = new SimpleDoubleProperty();
    private final DoubleProperty value = new SimpleDoubleProperty();

    public EffectBlock(EffectType type, double start, double duration) {
        this.type = type;
        this.start.set(start);
        this.duration.set(duration);
        if (type.hasParam()) value.set(type.getParam().getDefaultValue());
    }

    public EffectType getType() { return type; }

    public double getStart() { return start.get(); }
    public void setStart(double v) { start.set(v); }
    public DoubleProperty startProperty() { return start; }

    public double getDuration() { return duration.get(); }
    public void setDuration(double v) { duration.set(v); }
    public DoubleProperty durationProperty() { return duration; }

    public double getEnd() { return getStart() + getDuration(); }

    /** Valor del parámetro (solo tiene sentido si {@code type.hasParam()}). */
    public double getValue() { return value.get(); }
    public void setValue(double v) {
        if (type.hasParam()) {
            v = Math.max(type.getParam().getMin(), Math.min(type.getParam().getMax(), v));
        }
        value.set(v);
    }
    public DoubleProperty valueProperty() { return value; }

    /** Texto corto del parámetro, p. ej. "75 %" o "120°". Vacío si no tiene. */
    public String formatValue() {
        return type.hasParam() ? type.getParam().format(getValue()) : "";
    }
}
