package com.csc.view;

import com.csc.model.EffectBlock;
import com.csc.model.EffectType;
import com.csc.model.TimelineModel;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.DoubleConsumer;

/**
 * Vista de la línea de tiempo: regla, carriles por tipo de efecto, zoom (escala)
 * y barras de desplazamiento horizontal y vertical. Observa un {@link TimelineModel}.
 */
public class TimelineView extends VBox {

    public static final String DRAG_PREFIX = "cinesphere-effect:";

    private static final double HEADER_WIDTH = 150;
    private static final double RULER_HEIGHT = 30;
    private static final double LANE_HEIGHT = 48;
    private static final double BLOCK_MARGIN = 6;
    private static final double MIN_PPS = 4;
    private static final double MAX_PPS = 400;
    private static final double DEFAULT_BLOCK_DURATION = TimelineModel.DEFAULT_BLOCK_DURATION;

    // Pasos de la regla (segundos) y subdivisiones de cada paso
    private static final double[] TICK_STEPS = {0.1, 0.2, 0.5, 1, 2, 5, 10, 15, 30, 60, 120, 300, 600};
    private static final int[] TICK_DIVISIONS = {2, 4, 5, 4, 4, 5, 5, 3, 3, 4, 4, 5, 4};
    private static final double MIN_MAJOR_SPACING_PX = 90;

    // Colores dibujados en Canvas (coinciden con la paleta del CSS)
    private static final Color RULER_BG = Color.web("#1a1d25");
    private static final Color RULER_TEXT = Color.web("#9aa3b8");
    private static final Color TICK_MAJOR = Color.web("#5b6478");
    private static final Color TICK_MINOR = Color.web("#363c4b");
    private static final Color LANE_A = Color.web("#1d2029");
    private static final Color LANE_B = Color.web("#20242e");
    private static final Color LANE_LINE = Color.web("#2b303d");
    private static final Color GRID_MAJOR = Color.web("#2c3240");
    private static final Color GRID_MINOR = Color.web("#232733");
    private static final Color OUT_OF_RANGE = Color.web("#13151b", 0.85);
    private static final Color PLAYHEAD = Color.web("#ff4d6d");

    private final DoubleProperty pixelsPerSecond = new SimpleDoubleProperty(40);
    private final BooleanProperty snapEnabled = new SimpleBooleanProperty(true);

    private final Canvas rulerCanvas = new Canvas();
    private final Pane rulerPane = new Pane(rulerCanvas);
    private final Canvas gridCanvas = new Canvas();
    private final Pane blocksLayer = new Pane();
    private final Region dropGhost = new Region();
    private final Line playheadLine = new Line();
    private final Pane content = new Pane();
    private final Pane viewport = new Pane();
    private final VBox laneHeaders = new VBox();
    private final Pane laneHeaderViewport = new Pane(laneHeaders);
    private final ScrollBar hBar = new ScrollBar();
    private final ScrollBar vBar = new ScrollBar();
    private final Slider zoomSlider = new Slider(Math.log(MIN_PPS), Math.log(MAX_PPS), Math.log(40));

    private final Label cornerLabel = new Label("", new FontIcon("bi-clock"));
    private final Map<EffectBlock, EffectBlockView> views = new IdentityHashMap<>();

    private TimelineModel model;
    private DoubleConsumer onSeek;
    private boolean syncingZoom;

    // Escuchas del modelo (se guardan para poder desconectarlas al cambiar de modelo)
    private final InvalidationListener durationListener = o -> refreshAll();
    private final InvalidationListener playheadListener = o -> {
        updatePlayhead();
        followPlayhead();
    };
    private final ListChangeListener<EffectBlock> blocksListener = change -> {
        while (change.next()) {
            change.getRemoved().forEach(this::removeView);
            change.getAddedSubList().forEach(this::createView);
        }
    };
    private final ChangeListener<EffectBlock> selectionListener = (o, oldBlock, newBlock) -> {
        if (oldBlock != null && views.containsKey(oldBlock)) views.get(oldBlock).setSelected(false);
        if (newBlock != null && views.containsKey(newBlock)) views.get(newBlock).setSelected(true);
    };

    public TimelineView() {
        getStyleClass().add("timeline");
        setFocusTraversable(true);

        GridPane grid = buildGrid();
        VBox.setVgrow(grid, Priority.ALWAYS);
        getChildren().addAll(buildToolbar(), grid);

        buildLaneHeaders();
        buildContent();
        installScrolling();
        installRulerSeeking();
        installDropTarget();
        installKeyboard();
        installZoomSync();

        viewport.widthProperty().addListener(o -> refreshAll());
        viewport.heightProperty().addListener(o -> refreshAll());
        pixelsPerSecond.addListener(o -> refreshAll());
        hBar.valueProperty().addListener(o -> redrawCanvases());
        vBar.valueProperty().addListener(o -> redrawCanvases());

        setModel(new TimelineModel());
    }

    // ------------------------------------------------------------------ modelo

    /** Conecta la vista a un modelo (la llama el controlador). */
    public void setModel(TimelineModel newModel) {
        if (model != null) {
            model.totalDurationProperty().removeListener(durationListener);
            model.playheadTimeProperty().removeListener(playheadListener);
            model.getBlocks().removeListener(blocksListener);
            model.selectedProperty().removeListener(selectionListener);
            model.getBlocks().forEach(this::removeView);
        }
        model = newModel;
        model.totalDurationProperty().addListener(durationListener);
        model.playheadTimeProperty().addListener(playheadListener);
        model.getBlocks().addListener(blocksListener);
        model.selectedProperty().addListener(selectionListener);
        model.getBlocks().forEach(this::createView);
        if (model.getSelected() != null) selectionListener.changed(null, null, model.getSelected());

        cornerLabel.textProperty().bind(Bindings.createStringBinding(
                () -> TimeFormat.format(model.getPlayheadTime()), model.playheadTimeProperty()));
        refreshAll();
    }

    public TimelineModel getModel() { return model; }

    private void createView(EffectBlock block) {
        EffectBlockView view = new EffectBlockView(this, block);
        views.put(block, view);
        blocksLayer.getChildren().add(view);
        layoutBlock(view);
    }

    private void removeView(EffectBlock block) {
        EffectBlockView view = views.remove(block);
        if (view != null) blocksLayer.getChildren().remove(view);
    }

    // ------------------------------------------------------------------ construcción de la UI

    private HBox buildToolbar() {
        FontIcon titleIcon = new FontIcon("bi-sliders");
        titleIcon.getStyleClass().add("toolbar-title-icon");
        Label title = new Label("Línea de tiempo", titleIcon);
        title.getStyleClass().add("toolbar-title");
        title.setMinWidth(Region.USE_PREF_SIZE);

        Label hint = new Label("Ctrl + rueda: zoom   ·   Shift + rueda: desplazar   ·   Supr: eliminar bloque");
        hint.getStyleClass().add("toolbar-hint");
        hint.setMinWidth(0);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleButton snap = new ToggleButton("Ajustar a rejilla", new FontIcon("bi-grid-3x3-gap"));
        snap.getStyleClass().add("toolbar-toggle");
        snap.setMinWidth(Region.USE_PREF_SIZE);
        snap.selectedProperty().bindBidirectional(snapEnabled);
        snap.setTooltip(new Tooltip("Ajusta inicio y duración de los bloques a las divisiones de la regla"));

        Button fit = iconButton("bi-arrows-angle-contract", "Ver toda la duración");
        fit.setOnAction(e -> fitToView());
        Button zoomOut = iconButton("bi-zoom-out", "Alejar");
        zoomOut.setOnAction(e -> zoomAt(1 / 1.25, viewport.getWidth() / 2));
        Button zoomIn = iconButton("bi-zoom-in", "Acercar");
        zoomIn.setOnAction(e -> zoomAt(1.25, viewport.getWidth() / 2));

        zoomSlider.getStyleClass().add("zoom-slider");
        zoomSlider.setPrefWidth(150);
        zoomSlider.setMinWidth(80);
        zoomSlider.setTooltip(new Tooltip("Escala de la línea de tiempo"));

        Label scaleLabel = new Label();
        scaleLabel.getStyleClass().add("toolbar-scale");
        scaleLabel.setMinWidth(82);
        scaleLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "1 s = " + Math.round(pixelsPerSecond.get()) + " px", pixelsPerSecond));

        HBox bar = new HBox(8, title, new Separator(Orientation.VERTICAL), hint, spacer,
                snap, new Separator(Orientation.VERTICAL), fit, zoomOut, zoomSlider, zoomIn, scaleLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("timeline-toolbar");
        return bar;
    }

    private GridPane buildGrid() {
        Label corner = cornerLabel;
        corner.getStyleClass().add("timeline-corner");
        corner.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        rulerPane.getStyleClass().add("timeline-ruler");
        rulerCanvas.widthProperty().bind(rulerPane.widthProperty());
        rulerCanvas.heightProperty().bind(rulerPane.heightProperty());
        rulerCanvas.setManaged(false);
        clipToSize(rulerPane);

        laneHeaderViewport.getStyleClass().add("lane-header-viewport");
        clipToSize(laneHeaderViewport);

        viewport.getStyleClass().add("timeline-viewport");
        clipToSize(viewport);

        hBar.setOrientation(Orientation.HORIZONTAL);
        vBar.setOrientation(Orientation.VERTICAL);
        hBar.setUnitIncrement(30);
        vBar.setUnitIncrement(20);

        Region bottomLeft = new Region();
        bottomLeft.getStyleClass().add("timeline-corner-bottom");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("timeline-grid");
        ColumnConstraints c0 = new ColumnConstraints(HEADER_WIDTH, HEADER_WIDTH, HEADER_WIDTH);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        RowConstraints r0 = new RowConstraints(RULER_HEIGHT, RULER_HEIGHT, RULER_HEIGHT);
        RowConstraints r1 = new RowConstraints();
        r1.setVgrow(Priority.ALWAYS);
        r1.setMinHeight(LANE_HEIGHT);
        RowConstraints r2 = new RowConstraints();
        grid.getColumnConstraints().addAll(c0, c1, c2);
        grid.getRowConstraints().addAll(r0, r1, r2);

        grid.add(corner, 0, 0);
        grid.add(rulerPane, 1, 0);
        grid.add(laneHeaderViewport, 0, 1);
        grid.add(viewport, 1, 1);
        grid.add(vBar, 2, 1);
        grid.add(bottomLeft, 0, 2);
        grid.add(hBar, 1, 2);
        return grid;
    }

    private void buildLaneHeaders() {
        laneHeaders.setPrefWidth(HEADER_WIDTH);
        for (int i = 0; i < EffectType.laneCount(); i++) {
            FontIcon icon = new FontIcon(EffectType.LANE_ICONS[i]);
            icon.getStyleClass().add("lane-header-icon");
            Label label = new Label(EffectType.LANE_NAMES[i], icon);
            label.getStyleClass().addAll("lane-header", EffectType.LANE_STYLES[i]);
            label.setPrefSize(HEADER_WIDTH, LANE_HEIGHT);
            label.setMinSize(HEADER_WIDTH, LANE_HEIGHT);
            label.setMaxSize(HEADER_WIDTH, LANE_HEIGHT);
            laneHeaders.getChildren().add(label);
        }
        laneHeaders.translateYProperty().bind(vBar.valueProperty().negate());
    }

    private void buildContent() {
        gridCanvas.widthProperty().bind(viewport.widthProperty());
        gridCanvas.heightProperty().bind(viewport.heightProperty());
        gridCanvas.setManaged(false);

        dropGhost.getStyleClass().add("drop-ghost");
        dropGhost.setVisible(false);
        dropGhost.setMouseTransparent(true);
        dropGhost.setManaged(false);

        playheadLine.getStyleClass().add("playhead-line");
        playheadLine.setMouseTransparent(true);
        playheadLine.setManaged(false);

        blocksLayer.setPickOnBounds(false);
        blocksLayer.setManaged(false);
        content.setPickOnBounds(false);
        content.getChildren().addAll(blocksLayer, dropGhost, playheadLine);
        content.translateXProperty().bind(hBar.valueProperty().negate());
        content.translateYProperty().bind(vBar.valueProperty().negate());
        content.setManaged(false);

        viewport.getChildren().addAll(gridCanvas, content);

        // Clic en zona vacía: deselecciona y mueve el cursor de reproducción
        viewport.setOnMousePressed(e -> {
            requestFocus();
            if (e.getButton() == MouseButton.PRIMARY) {
                model.select(null);
                seekToX(e.getX());
            }
        });
        viewport.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) seekToX(e.getX());
        });
    }

    private Button iconButton(String icon, String tooltip) {
        Button b = new Button(null, new FontIcon(icon));
        b.getStyleClass().add("icon-button");
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private static void clipToSize(Region region) {
        region.setMinSize(0, 0);
        region.setPrefSize(10, 10);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    // ------------------------------------------------------------------ interacción

    private void installScrolling() {
        EventHandler<ScrollEvent> handler = e -> {
            double anchorX = e.getSource() == laneHeaderViewport ? 0 : e.getX();
            if (e.isControlDown()) {
                double delta = e.getDeltaY() != 0 ? e.getDeltaY() : e.getDeltaX();
                if (delta != 0) zoomAt(delta > 0 ? 1.15 : 1 / 1.15, anchorX);
            } else {
                boolean horizontal = e.isShiftDown()
                        || Math.abs(e.getDeltaX()) > Math.abs(e.getDeltaY())
                        || vBar.isDisabled();
                if (horizontal) {
                    double d = e.getDeltaX() != 0 ? e.getDeltaX() : e.getDeltaY();
                    hBar.setValue(clamp(hBar.getValue() - d, 0, hBar.getMax()));
                } else {
                    vBar.setValue(clamp(vBar.getValue() - e.getDeltaY(), 0, vBar.getMax()));
                }
            }
            e.consume();
        };
        viewport.addEventFilter(ScrollEvent.SCROLL, handler);
        rulerPane.addEventFilter(ScrollEvent.SCROLL, handler);
        laneHeaderViewport.addEventFilter(ScrollEvent.SCROLL, handler);
    }

    private void installRulerSeeking() {
        EventHandler<MouseEvent> seek = e -> {
            if (e.getButton() == MouseButton.PRIMARY) seekToX(e.getX());
        };
        rulerPane.setOnMousePressed(seek);
        rulerPane.setOnMouseDragged(seek);
    }

    private void installDropTarget() {
        viewport.setOnDragOver(e -> {
            EffectType type = typeFrom(e.getDragboard());
            if (type != null) {
                e.acceptTransferModes(TransferMode.COPY);
                showGhost(type, e.getX());
            }
            e.consume();
        });
        viewport.setOnDragExited(e -> dropGhost.setVisible(false));
        viewport.setOnDragDropped(e -> {
            EffectType type = typeFrom(e.getDragboard());
            boolean ok = false;
            if (type != null) {
                model.addBlock(type, snap(xToTime(e.getX())), DEFAULT_BLOCK_DURATION);
                requestFocus();
                ok = true;
            }
            dropGhost.setVisible(false);
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    private void installKeyboard() {
        setOnKeyPressed(e -> {
            if ((e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) && model.getSelected() != null) {
                model.removeSelected();
                e.consume();
            }
        });
    }

    private void installZoomSync() {
        zoomSlider.valueProperty().addListener((o, ov, nv) -> {
            if (syncingZoom) return;
            zoomAt(Math.exp(nv.doubleValue()) / getPixelsPerSecond(), viewport.getWidth() / 2);
        });
        pixelsPerSecond.addListener((o, ov, nv) -> {
            syncingZoom = true;
            zoomSlider.setValue(Math.log(nv.doubleValue()));
            syncingZoom = false;
        });
    }

    private EffectType typeFrom(Dragboard db) {
        if (db == null || !db.hasString() || !db.getString().startsWith(DRAG_PREFIX)) return null;
        try {
            return EffectType.valueOf(db.getString().substring(DRAG_PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void showGhost(EffectType type, double viewportX) {
        double pps = getPixelsPerSecond();
        double start = clamp(snap(xToTime(viewportX)), 0, Math.max(0, getTotalDuration() - EffectBlock.MIN_DURATION));
        double dur = Math.min(DEFAULT_BLOCK_DURATION, getTotalDuration() - start);
        dropGhost.resizeRelocate(start * pps, type.getLane() * LANE_HEIGHT + BLOCK_MARGIN,
                Math.max(4, dur * pps), LANE_HEIGHT - 2 * BLOCK_MARGIN);
        dropGhost.setVisible(true);
    }

    // ------------------------------------------------------------------ zoom y desplazamiento

    /** Cambia la escala manteniendo fijo el instante bajo {@code anchorX} (coordenada del viewport). */
    public void zoomAt(double factor, double anchorX) {
        double oldPps = getPixelsPerSecond();
        double anchorTime = (anchorX + hBar.getValue()) / oldPps;
        double newPps = clamp(oldPps * factor, MIN_PPS, MAX_PPS);
        if (newPps == oldPps) return;
        pixelsPerSecond.set(newPps);          // refreshAll() recalcula las barras
        hBar.setValue(clamp(anchorTime * newPps - anchorX, 0, hBar.getMax()));
    }

    /** Ajusta la escala para ver toda la duración. */
    public void fitToView() {
        double w = viewport.getWidth();
        if (w <= 0) return;
        pixelsPerSecond.set(clamp((w - 30) / getTotalDuration(), MIN_PPS, MAX_PPS));
        hBar.setValue(0);
    }

    private void followPlayhead() {
        double x = getPlayheadTime() * getPixelsPerSecond();
        double w = viewport.getWidth();
        double h = hBar.getValue();
        if (w <= 0) return;
        if (x < h || x > h + w) {
            hBar.setValue(clamp(x - w * 0.1, 0, hBar.getMax()));
        }
    }

    private double contentWidth() {
        return getTotalDuration() * getPixelsPerSecond() + 40;
    }

    private double contentHeight() {
        return EffectType.laneCount() * LANE_HEIGHT;
    }

    private void updateScrollBars() {
        configureBar(hBar, contentWidth(), viewport.getWidth());
        configureBar(vBar, contentHeight(), viewport.getHeight());
    }

    private static void configureBar(ScrollBar bar, double contentSize, double viewSize) {
        double max = contentSize - viewSize;
        if (max <= 0 || viewSize <= 0) {
            bar.setMax(1);
            bar.setVisibleAmount(1);
            bar.setValue(0);
            bar.setDisable(true);
        } else {
            bar.setDisable(false);
            bar.setMax(max);
            bar.setVisibleAmount(max * viewSize / contentSize);
            bar.setBlockIncrement(viewSize * 0.8);
            if (bar.getValue() > max) bar.setValue(max);
        }
    }

    // ------------------------------------------------------------------ dibujo

    private void refreshAll() {
        updateScrollBars();
        content.resize(contentWidth(), contentHeight());
        blocksLayer.resize(contentWidth(), contentHeight());
        views.values().forEach(this::layoutBlock);
        updatePlayhead();
        redrawCanvases();
    }

    private void redrawCanvases() {
        drawRuler();
        drawGrid();
    }

    private void updatePlayhead() {
        double x = Math.round(getPlayheadTime() * getPixelsPerSecond()) + 0.5;
        playheadLine.setStartX(x);
        playheadLine.setEndX(x);
        playheadLine.setStartY(0);
        playheadLine.setEndY(contentHeight());
        drawRuler();
    }

    private int stepIndex() {
        double pps = getPixelsPerSecond();
        for (int i = 0; i < TICK_STEPS.length; i++) {
            if (TICK_STEPS[i] * pps >= MIN_MAJOR_SPACING_PX) return i;
        }
        return TICK_STEPS.length - 1;
    }

    private void drawRuler() {
        GraphicsContext g = rulerCanvas.getGraphicsContext2D();
        double w = rulerCanvas.getWidth();
        double h = rulerCanvas.getHeight();
        g.setFill(RULER_BG);
        g.fillRect(0, 0, w, h);

        double pps = getPixelsPerSecond();
        double offset = hBar.getValue();
        int idx = stepIndex();
        double major = TICK_STEPS[idx];
        double minor = major / TICK_DIVISIONS[idx];
        double startT = offset / pps;
        double endT = Math.min(getTotalDuration(), (offset + w) / pps);

        g.setFont(Font.font("System", 11));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.setLineWidth(1);

        for (long i = (long) Math.floor(startT / minor); i * minor <= endT + 1e-9; i++) {
            double t = i * minor;
            if (t < 0) continue;
            double x = Math.round(t * pps - offset) + 0.5;
            boolean isMajor = Math.abs(t / major - Math.round(t / major)) < 1e-6;
            g.setStroke(isMajor ? TICK_MAJOR : TICK_MINOR);
            g.strokeLine(x, isMajor ? h * 0.35 : h * 0.68, x, h);
            if (isMajor) {
                g.setFill(RULER_TEXT);
                g.fillText(TimeFormat.ruler(t, major < 1), x + 4, 3);
            }
        }

        g.setStroke(LANE_LINE);
        g.strokeLine(0, h - 0.5, w, h - 0.5);

        // Marcador del cursor de reproducción
        double px = Math.round(getPlayheadTime() * pps - offset) + 0.5;
        if (px >= -8 && px <= w + 8) {
            g.setFill(PLAYHEAD);
            g.fillPolygon(new double[]{px - 6, px + 6, px + 6, px, px - 6},
                    new double[]{h - 16, h - 16, h - 8, h, h - 8}, 5);
        }
    }

    private void drawGrid() {
        GraphicsContext g = gridCanvas.getGraphicsContext2D();
        double w = gridCanvas.getWidth();
        double h = gridCanvas.getHeight();
        g.setFill(LANE_A);
        g.fillRect(0, 0, w, h);

        double vOffset = vBar.getValue();
        for (int i = 0; i < EffectType.laneCount(); i++) {
            double y = i * LANE_HEIGHT - vOffset;
            if (y > h || y + LANE_HEIGHT < 0) continue;
            g.setFill(i % 2 == 0 ? LANE_A : LANE_B);
            g.fillRect(0, y, w, LANE_HEIGHT);
            g.setStroke(LANE_LINE);
            g.strokeLine(0, Math.round(y + LANE_HEIGHT) - 0.5, w, Math.round(y + LANE_HEIGHT) - 0.5);
        }

        double pps = getPixelsPerSecond();
        double offset = hBar.getValue();
        int idx = stepIndex();
        double major = TICK_STEPS[idx];
        double minor = major / TICK_DIVISIONS[idx];
        double lanesBottom = Math.min(h, contentHeight() - vOffset);
        double endT = Math.min(getTotalDuration(), (offset + w) / pps);
        g.setLineWidth(1);
        for (long i = (long) Math.floor((offset / pps) / minor); i * minor <= endT + 1e-9; i++) {
            double t = i * minor;
            if (t < 0) continue;
            double x = Math.round(t * pps - offset) + 0.5;
            boolean isMajor = Math.abs(t / major - Math.round(t / major)) < 1e-6;
            g.setStroke(isMajor ? GRID_MAJOR : GRID_MINOR);
            g.strokeLine(x, 0, x, lanesBottom);
        }

        // Zona posterior al final del video
        double endX = getTotalDuration() * pps - offset;
        if (endX < w) {
            g.setFill(OUT_OF_RANGE);
            g.fillRect(Math.max(0, endX), 0, w - Math.max(0, endX), h);
        }
    }

    // ------------------------------------------------------------------ bloques

    void layoutBlock(EffectBlockView view) {
        double pps = getPixelsPerSecond();
        EffectBlock block = view.getBlock();
        view.resizeRelocate(block.getStart() * pps,
                block.getType().getLane() * LANE_HEIGHT + BLOCK_MARGIN,
                Math.max(4, block.getDuration() * pps),
                LANE_HEIGHT - 2 * BLOCK_MARGIN);
    }

    /** Redondea un tiempo a la subdivisión visible de la regla (si el ajuste está activo). */
    public double snap(double seconds) {
        if (!snapEnabled.get()) return Math.round(seconds * 100) / 100.0;
        int idx = stepIndex();
        double minor = TICK_STEPS[idx] / TICK_DIVISIONS[idx];
        return Math.round(seconds / minor) * minor;
    }

    private void seekToX(double viewportX) {
        double t = clamp(xToTime(viewportX), 0, getTotalDuration());
        model.setPlayheadTime(t);
        if (onSeek != null) onSeek.accept(t);
    }

    private double xToTime(double viewportX) {
        return (viewportX + hBar.getValue()) / getPixelsPerSecond();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ------------------------------------------------------------------ propiedades

    public double getPixelsPerSecond() { return pixelsPerSecond.get(); }
    public DoubleProperty pixelsPerSecondProperty() { return pixelsPerSecond; }

    private double getTotalDuration() { return model.getTotalDuration(); }

    private double getPlayheadTime() { return model.getPlayheadTime(); }

    /** Se llama cuando el usuario mueve el cursor de reproducción desde la línea de tiempo. */
    public void setOnSeek(DoubleConsumer onSeek) { this.onSeek = onSeek; }
}
