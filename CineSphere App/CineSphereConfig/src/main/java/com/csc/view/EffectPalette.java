package com.csc.view;

import com.csc.model.EffectType;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Panel izquierdo con los bloques de efecto que se arrastran a la línea de tiempo.
 */
public class EffectPalette extends VBox {

    private Consumer<EffectType> onQuickAdd;

    public EffectPalette() {
        getStyleClass().add("effect-palette");
        setSpacing(10);

        Label title = new Label("Efectos");
        title.getStyleClass().add("panel-title");
        Label subtitle = new Label("Arrastra un bloque a la línea de tiempo");
        subtitle.getStyleClass().add("panel-subtitle");
        subtitle.setWrapText(true);

        getChildren().addAll(title, subtitle,
                sectionLabel("Ventiladores", "bi-wind"),
                createItem(EffectType.FAN_LEFT_TO_RIGHT),
                createItem(EffectType.FAN_RIGHT_TO_LEFT),
                sectionLabel("Difusores", "bi-droplet"),
                createItem(EffectType.SERVO_ROTATE),
                createItem(EffectType.DIFFUSER_1),
                createItem(EffectType.DIFFUSER_2),
                createItem(EffectType.DIFFUSER_3),
                createItem(EffectType.DIFFUSER_4));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Doble clic en un bloque para añadirlo en la posición del cursor de reproducción.",
                new FontIcon("bi-info-circle"));
        hint.getStyleClass().add("palette-hint");
        hint.setWrapText(true);

        getChildren().addAll(spacer, hint);
    }

    private Label sectionLabel(String text, String icon) {
        Label label = new Label(text.toUpperCase(), new FontIcon(icon));
        label.getStyleClass().add("palette-section");
        return label;
    }

    private HBox createItem(EffectType type) {
        FontIcon icon = new FontIcon(type.getIconLiteral());
        StackPane iconBox = new StackPane(icon);
        iconBox.getStyleClass().add("palette-item-icon");

        Label name = new Label(type.getLabel());
        name.getStyleClass().add("palette-item-title");
        Label description = new Label(type.getDescription());
        description.getStyleClass().add("palette-item-desc");
        description.setWrapText(true);
        VBox texts = new VBox(2, name, description);
        texts.setMinWidth(0);
        HBox.setHgrow(texts, Priority.ALWAYS);

        FontIcon grip = new FontIcon("bi-grip-vertical");
        grip.getStyleClass().add("palette-item-grip");

        HBox item = new HBox(10, iconBox, texts, grip);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().addAll("palette-item", type.getStyleClass());
        Tooltip.install(item, new Tooltip(type.getDescription() + "\nArrastra a la línea de tiempo"));

        item.setOnDragDetected(e -> {
            Dragboard db = item.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString(TimelineView.DRAG_PREFIX + type.name());
            db.setContent(content);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            db.setDragView(item.snapshot(params, null), e.getX(), e.getY());
            e.consume();
        });
        item.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onQuickAdd != null) onQuickAdd.accept(type);
        });
        return item;
    }

    /** Acción al hacer doble clic sobre un bloque (la asigna el controlador). */
    public void setOnQuickAdd(Consumer<EffectType> onQuickAdd) {
        this.onQuickAdd = onQuickAdd;
    }
}
