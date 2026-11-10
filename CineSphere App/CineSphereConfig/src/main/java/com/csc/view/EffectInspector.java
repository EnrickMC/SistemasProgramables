package com.csc.view;

import com.csc.model.EffectBlock;
import com.csc.model.EffectType;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Panel derecho "Propiedades": edita el bloque seleccionado
 * (inicio, duración y su parámetro: potencia 0-100 % o ángulo 0-180°).
 */
public class EffectInspector extends VBox {

    private final VBox form = new VBox(14);
    private final VBox emptyState;
    private final List<Runnable> unbinders = new ArrayList<>();
    // Referencias fuertes a los adaptadores Double <-> Object (las bindings bidireccionales son débiles)
    private final List<Object> keepAlive = new ArrayList<>();

    private EffectBlock block;
    private double totalDuration = 60;
    private Consumer<EffectBlock> onTest;
    private Consumer<EffectBlock> onDelete;

    public EffectInspector() {
        getStyleClass().add("effect-inspector");
        setSpacing(12);

        Label title = new Label("Propiedades");
        title.getStyleClass().add("panel-title");

        FontIcon emptyIcon = new FontIcon("bi-cursor");
        emptyIcon.getStyleClass().add("inspector-empty-icon");
        Label emptyText = new Label("Selecciona un bloque de la línea de tiempo para configurarlo");
        emptyText.getStyleClass().add("panel-subtitle");
        emptyText.setWrapText(true);
        emptyText.setAlignment(Pos.CENTER);
        emptyState = new VBox(10, emptyIcon, emptyText);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.getStyleClass().add("inspector-empty");

        getChildren().addAll(title, emptyState, form);
        showEmpty(true);
    }

    /** Muestra un bloque (o nada si es {@code null}). La duración total limita los campos de tiempo. */
    public void setBlock(EffectBlock newBlock, double totalDuration) {
        unbinders.forEach(Runnable::run);
        unbinders.clear();
        keepAlive.clear();
        form.getChildren().clear();
        this.block = newBlock;
        this.totalDuration = totalDuration;
        if (newBlock == null) {
            showEmpty(true);
            return;
        }
        showEmpty(false);
        buildForm(newBlock);
    }

    public EffectBlock getBlock() { return block; }

    private void showEmpty(boolean empty) {
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
        form.setVisible(!empty);
        form.setManaged(!empty);
    }

    private void buildForm(EffectBlock b) {
        EffectType type = b.getType();

        // Cabecera con el tipo de efecto
        FontIcon icon = new FontIcon(type.getIconLiteral());
        StackPane iconBox = new StackPane(icon);
        iconBox.getStyleClass().add("palette-item-icon");
        Label name = new Label(type.getLabel());
        name.getStyleClass().add("palette-item-title");
        Label desc = new Label(type.getDescription());
        desc.getStyleClass().add("palette-item-desc");
        desc.setWrapText(true);
        VBox texts = new VBox(2, name, desc);
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox header = new HBox(10, iconBox, texts);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().addAll("inspector-header", type.getStyleClass());
        form.getChildren().add(header);

        // Tiempo
        form.getChildren().add(field("Inicio (s)", "bi-stopwatch",
                doubleSpinner(b.startProperty().asObject(), 0, totalDuration, 0.1)));
        form.getChildren().add(field("Duración (s)", "bi-hourglass-split",
                doubleSpinner(b.durationProperty().asObject(), EffectBlock.MIN_DURATION, totalDuration, 0.1)));

        // Parámetro (potencia / ángulo)
        if (type.hasParam()) {
            form.getChildren().add(paramEditor(b, type.getParam()));
        }

        // Acciones
        Button test = new Button("Probar en el ESP32", new FontIcon("bi-lightning-charge-fill"));
        test.getStyleClass().add("accent-button");
        test.setMaxWidth(Double.MAX_VALUE);
        test.setOnAction(e -> { if (onTest != null) onTest.accept(b); });

        Button delete = new Button("Eliminar bloque", new FontIcon("bi-trash"));
        delete.getStyleClass().add("danger-button");
        delete.setMaxWidth(Double.MAX_VALUE);
        delete.setOnAction(e -> { if (onDelete != null) onDelete.accept(b); });

        Region spacer = new Region();
        spacer.setMinHeight(6);
        form.getChildren().addAll(spacer, test, delete);
    }

    private VBox paramEditor(EffectBlock b, EffectType.Param param) {
        Slider slider = new Slider(param.getMin(), param.getMax(), b.getValue());
        slider.getStyleClass().add("param-slider");
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(param == EffectType.Param.SERVO_ANGLE ? 45 : 25);
        slider.setMinorTickCount(param == EffectType.Param.SERVO_ANGLE ? 2 : 4);
        slider.setBlockIncrement(1);
        HBox.setHgrow(slider, Priority.ALWAYS);

        // El slider redondea a enteros
        slider.valueProperty().addListener((o, ov, nv) -> {
            double rounded = Math.round(nv.doubleValue());
            if (rounded != nv.doubleValue()) slider.setValue(rounded);
        });
        slider.valueProperty().bindBidirectional(b.valueProperty());
        unbinders.add(() -> slider.valueProperty().unbindBidirectional(b.valueProperty()));

        Spinner<Double> spinner = doubleSpinner(b.valueProperty().asObject(), param.getMin(), param.getMax(), 1);
        spinner.setPrefWidth(84);

        HBox row = new HBox(10, slider, spinner);
        row.setAlignment(Pos.CENTER_LEFT);

        Label scale = new Label(param.format(param.getMin()) + "  ·  " + param.format(param.getMax()));
        scale.getStyleClass().add("inspector-scale");

        FontIcon icon = new FontIcon(param.getIconLiteral());
        Label label = new Label(param.getLabel() + " (" + param.getUnit() + ")", icon);
        label.getStyleClass().add("inspector-label");

        VBox box = new VBox(6, label, row, scale);
        box.getStyleClass().add("inspector-param");
        return box;
    }

    private Spinner<Double> doubleSpinner(ObjectProperty<Double> target, double min, double max, double step) {
        keepAlive.add(target);
        SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, target.get(), step);
        boolean integer = step >= 1;
        factory.setConverter(new StringConverter<>() {
            @Override public String toString(Double v) {
                if (v == null) return "";
                return integer ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.2f", v);
            }
            @Override public Double fromString(String s) {
                try {
                    return Double.parseDouble(s.trim().replace(',', '.'));
                } catch (NumberFormatException ex) {
                    return factory.getValue();
                }
            }
        });
        factory.valueProperty().bindBidirectional(target);
        unbinders.add(() -> factory.valueProperty().unbindBidirectional(target));

        Spinner<Double> spinner = new Spinner<>(factory);
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
        // Aplica el texto escrito al perder el foco
        spinner.focusedProperty().addListener((o, was, is) -> {
            if (!is) factory.setValue(factory.getConverter().fromString(spinner.getEditor().getText()));
        });
        return spinner;
    }

    private VBox field(String text, String icon, Region editor) {
        Label label = new Label(text, new FontIcon(icon));
        label.getStyleClass().add("inspector-label");
        return new VBox(6, label, editor);
    }

    public void setOnTest(Consumer<EffectBlock> onTest) { this.onTest = onTest; }
    public void setOnDelete(Consumer<EffectBlock> onDelete) { this.onDelete = onDelete; }
}
