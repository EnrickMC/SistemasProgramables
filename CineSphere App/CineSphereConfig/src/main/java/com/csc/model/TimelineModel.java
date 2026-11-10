package com.csc.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Estado de la línea de tiempo: bloques, duración total, cursor de
 * reproducción y bloque seleccionado. La vista observa este modelo.
 */
public class TimelineModel {

    public static final double DEFAULT_DURATION = 60;
    public static final double DEFAULT_BLOCK_DURATION = 2.0;

    private final ObservableList<EffectBlock> blocks = FXCollections.observableArrayList();
    private final DoubleProperty totalDuration = new SimpleDoubleProperty(DEFAULT_DURATION);
    private final DoubleProperty playheadTime = new SimpleDoubleProperty(0);
    private final ObjectProperty<EffectBlock> selected = new SimpleObjectProperty<>();

    /** Crea un bloque dentro del rango válido [0, duración total] y lo selecciona. */
    public EffectBlock addBlock(EffectType type, double start, double duration) {
        double total = getTotalDuration();
        double s = clamp(start, 0, Math.max(0, total - EffectBlock.MIN_DURATION));
        double d = clamp(duration, EffectBlock.MIN_DURATION, Math.max(EffectBlock.MIN_DURATION, total - s));
        EffectBlock block = new EffectBlock(type, s, d);
        blocks.add(block);
        selected.set(block);
        return block;
    }

    public void removeBlock(EffectBlock block) {
        if (block == null) return;
        blocks.remove(block);
        if (selected.get() == block) selected.set(null);
    }

    public void removeSelected() {
        removeBlock(selected.get());
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public ObservableList<EffectBlock> getBlocks() { return blocks; }

    public double getTotalDuration() { return totalDuration.get(); }
    public void setTotalDuration(double seconds) { totalDuration.set(Math.max(1, seconds)); }
    public DoubleProperty totalDurationProperty() { return totalDuration; }

    public double getPlayheadTime() { return playheadTime.get(); }
    public void setPlayheadTime(double seconds) { playheadTime.set(seconds); }
    public DoubleProperty playheadTimeProperty() { return playheadTime; }

    public EffectBlock getSelected() { return selected.get(); }
    public void select(EffectBlock block) { selected.set(block); }
    public ObjectProperty<EffectBlock> selectedProperty() { return selected; }
}
