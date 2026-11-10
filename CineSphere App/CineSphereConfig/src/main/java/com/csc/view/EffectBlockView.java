package com.csc.view;

import com.csc.model.EffectBlock;
import com.csc.model.EffectType;
import com.csc.model.TimelineModel;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Locale;

/**
 * Vista de un {@link EffectBlock} dentro de la línea de tiempo.
 * - Arrastrar el cuerpo: mueve el bloque en el tiempo.
 * - Arrastrar los bordes izquierdo/derecho: cambia inicio/duración.
 */
public class EffectBlockView extends HBox {

    private static final double EDGE = 8;
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private enum DragMode { MOVE, RESIZE_LEFT, RESIZE_RIGHT }

    private final TimelineView timeline;
    private final EffectBlock block;

    private DragMode mode = DragMode.MOVE;
    private double pressSceneX;
    private double originalStart;
    private double originalDuration;

    public EffectBlockView(TimelineView timeline, EffectBlock block) {
        this.timeline = timeline;
        this.block = block;
        EffectType type = block.getType();

        getStyleClass().addAll("effect-block", type.getStyleClass());
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setMinWidth(0);
        setManaged(false); // posición y tamaño los controla TimelineView

        FontIcon icon = new FontIcon(type.getIconLiteral());
        icon.getStyleClass().add("effect-block-icon");

        Label name = new Label(type.getLabel());
        name.getStyleClass().add("effect-block-title");
        name.setMinWidth(0);

        Region grow = new Region();
        grow.setMinWidth(0);
        HBox.setHgrow(grow, Priority.ALWAYS);

        Label durationLabel = new Label();
        durationLabel.getStyleClass().add("effect-block-duration");
        durationLabel.setMinWidth(0);
        durationLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(Locale.ROOT, "%.1f s", block.getDuration()), block.durationProperty()));

        getChildren().addAll(icon, name, grow, durationLabel);

        // Potencia / ángulo siempre visible si el efecto tiene parámetro
        if (type.hasParam()) {
            Label valueBadge = new Label();
            valueBadge.getStyleClass().add("effect-block-value");
            valueBadge.setMinWidth(Region.USE_PREF_SIZE);
            valueBadge.textProperty().bind(Bindings.createStringBinding(
                    block::formatValue, block.valueProperty()));
            getChildren().add(2, valueBadge);
        }

        // En bloques estrechos se oculta la duración para priorizar el nombre
        widthProperty().addListener((o, ov, nv) -> {
            boolean wide = nv.doubleValue() > 150;
            durationLabel.setVisible(wide);
            durationLabel.setManaged(wide);
        });

        // Recorta el contenido cuando el bloque es muy estrecho
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        setClip(clip);

        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(Bindings.createStringBinding(
                () -> type.getDescription()
                        + (type.hasParam() ? "\n" + type.getParam().getLabel() + ": " + block.formatValue() : "")
                        + "\nInicio: " + TimeFormat.format(block.getStart())
                        + "\nFin: " + TimeFormat.format(block.getEnd())
                        + "\nDuración: " + String.format(Locale.ROOT, "%.2f s", block.getDuration()),
                block.startProperty(), block.durationProperty(), block.valueProperty()));
        Tooltip.install(this, tooltip);

        buildContextMenu();
        installMouseHandlers();

        block.startProperty().addListener(o -> timeline.layoutBlock(this));
        block.durationProperty().addListener(o -> timeline.layoutBlock(this));
    }

    private TimelineModel model() {
        return timeline.getModel();
    }

    private void buildContextMenu() {
        MenuItem duplicate = new MenuItem("Duplicar", new FontIcon("bi-files"));
        duplicate.setOnAction(e -> model().addBlock(block.getType(), block.getEnd(), block.getDuration())
                .setValue(block.getValue()));
        MenuItem delete = new MenuItem("Eliminar", new FontIcon("bi-trash"));
        delete.setOnAction(e -> model().removeBlock(block));
        ContextMenu menu = new ContextMenu(duplicate, new SeparatorMenuItem(), delete);

        setOnContextMenuRequested(e -> {
            model().select(block);
            menu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    private void installMouseHandlers() {
        setOnMouseMoved(e -> setCursor(modeFor(e) == DragMode.MOVE ? Cursor.OPEN_HAND : Cursor.H_RESIZE));

        setOnMousePressed(e -> {
            timeline.requestFocus();
            model().select(block);
            if (e.getButton() != MouseButton.PRIMARY) {
                e.consume();
                return;
            }
            mode = modeFor(e);
            pressSceneX = e.getSceneX();
            originalStart = block.getStart();
            originalDuration = block.getDuration();
            if (mode == DragMode.MOVE) setCursor(Cursor.CLOSED_HAND);
            e.consume();
        });

        setOnMouseDragged(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            double delta = (e.getSceneX() - pressSceneX) / timeline.getPixelsPerSecond();
            double total = model().getTotalDuration();
            double originalEnd = originalStart + originalDuration;
            double min = EffectBlock.MIN_DURATION;

            switch (mode) {
                case MOVE -> block.setStart(clamp(timeline.snap(originalStart + delta),
                        0, Math.max(0, total - originalDuration)));
                case RESIZE_RIGHT -> {
                    double end = clamp(timeline.snap(originalEnd + delta), originalStart + min, total);
                    block.setDuration(end - originalStart);
                }
                case RESIZE_LEFT -> {
                    double s = clamp(timeline.snap(originalStart + delta), 0, originalEnd - min);
                    block.setStart(s);
                    block.setDuration(originalEnd - s);
                }
            }
            e.consume();
        });

        setOnMouseReleased(e -> {
            setCursor(modeFor(e) == DragMode.MOVE ? Cursor.OPEN_HAND : Cursor.H_RESIZE);
            e.consume();
        });
    }

    private DragMode modeFor(MouseEvent e) {
        double edge = Math.min(EDGE, getWidth() / 3);
        if (e.getX() <= edge) return DragMode.RESIZE_LEFT;
        if (e.getX() >= getWidth() - edge) return DragMode.RESIZE_RIGHT;
        return DragMode.MOVE;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
        if (selected) toFront();
    }

    public EffectBlock getBlock() { return block; }
}
