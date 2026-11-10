package com.csc.controller;

import com.csc.model.ConnectionModel;
import com.csc.model.ConnectionState;
import com.csc.model.TimelineModel;
import com.csc.view.EffectInspector;
import com.csc.view.EffectPalette;
import com.csc.view.TimelineView;
import com.csc.view.VideoPlayerView;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

/**
 * Controlador de la ventana principal (main-view.fxml).
 * Conecta los modelos (línea de tiempo y conexión) con las vistas
 * (reproductor, línea de tiempo, paleta, propiedades) y con el ESP32.
 */
public class MainController {

    @FXML private EffectPalette palette;
    @FXML private VideoPlayerView player;
    @FXML private TimelineView timeline;
    @FXML private EffectInspector inspector;
    @FXML private Label statusLabel;
    @FXML private Label blockCountLabel;

    @FXML private Label connectionStateLabel;
    @FXML private FontIcon connectionIcon;
    @FXML private Label connectionMessageLabel;
    @FXML private TextField hostField;
    @FXML private Button connectButton;
    @FXML private Button uploadButton;
    @FXML private Button runButton;
    @FXML private Button stopButton;
    @FXML private CheckBox syncCheck;

    private final TimelineModel timelineModel = new TimelineModel();
    private final ConnectionModel connectionModel = new ConnectionModel();
    private ConnectionController connection;

    @FXML
    private void initialize() {
        connection = new ConnectionController(connectionModel, timelineModel);
        timeline.setModel(timelineModel);

        initTimelineAndPlayer();
        initInspector();
        initConnectionBar();

        statusLabel.textProperty().bind(Bindings.concat("Video: ", player.fileNameProperty()));
        blockCountLabel.textProperty().bind(Bindings.size(timelineModel.getBlocks()).asString("%d bloques"));
    }

    private void initTimelineAndPlayer() {
        // Doble clic en la paleta: añade el bloque en la posición del cursor
        palette.setOnQuickAdd(type -> timelineModel.addBlock(type,
                timeline.snap(timelineModel.getPlayheadTime()), TimelineModel.DEFAULT_BLOCK_DURATION));

        // Reproductor -> modelo
        player.durationProperty().addListener((o, ov, nv) -> {
            if (nv.doubleValue() > 0) {
                timelineModel.setTotalDuration(nv.doubleValue());
                timeline.fitToView();
            }
        });
        player.currentTimeProperty().addListener((o, ov, nv) -> {
            timelineModel.setPlayheadTime(nv.doubleValue());
            // Un salto mientras se reproduce = el usuario buscó otro punto: re-sincroniza el ESP32
            if (player.isPlaying() && Math.abs(nv.doubleValue() - ov.doubleValue()) > 0.75 && syncEnabled()) {
                connection.play(nv.doubleValue());
            }
        });

        // Reproducción del video -> ESP32
        player.playingProperty().addListener((o, wasPlaying, isPlaying) -> {
            if (!syncEnabled()) return;
            if (isPlaying) connection.play(player.currentTimeProperty().get());
            else connection.pause();
        });

        // Línea de tiempo -> reproductor
        timeline.setOnSeek(player::seek);
    }

    private void initInspector() {
        timelineModel.selectedProperty().addListener((o, ov, block) ->
                inspector.setBlock(block, timelineModel.getTotalDuration()));
        timelineModel.totalDurationProperty().addListener((o, ov, nv) ->
                inspector.setBlock(timelineModel.getSelected(), nv.doubleValue()));
        inspector.setOnDelete(timelineModel::removeBlock);
        inspector.setOnTest(connection::test);
    }

    private void initConnectionBar() {
        hostField.textProperty().bindBidirectional(connectionModel.hostProperty());
        syncCheck.selectedProperty().bindBidirectional(connectionModel.syncWithVideoProperty());
        connectionMessageLabel.textProperty().bind(connectionModel.messageProperty());

        connectionModel.stateProperty().addListener((o, ov, state) -> showConnectionState(state));
        showConnectionState(connectionModel.getState());

        var notConnected = Bindings.createBooleanBinding(
                () -> connectionModel.getState() != ConnectionState.CONNECTED, connectionModel.stateProperty());
        uploadButton.disableProperty().bind(notConnected);
        runButton.disableProperty().bind(notConnected);
        stopButton.disableProperty().bind(notConnected);
        hostField.disableProperty().bind(Bindings.createBooleanBinding(
                () -> connectionModel.getState() == ConnectionState.CONNECTED
                        || connectionModel.getState() == ConnectionState.CONNECTING,
                connectionModel.stateProperty()));
    }

    private void showConnectionState(ConnectionState state) {
        connectionStateLabel.setText(state.getLabel());
        connectionIcon.setIconLiteral(state.getIconLiteral());
        for (ConnectionState s : ConnectionState.values()) {
            connectionStateLabel.getStyleClass().remove(s.getStyleClass());
        }
        connectionStateLabel.getStyleClass().add(state.getStyleClass());
        boolean active = state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING;
        connectButton.setText(active ? "Desconectar" : "Conectar");
        ((FontIcon) connectButton.getGraphic()).setIconLiteral(active ? "bi-x" : "bi-plug");
    }

    private boolean syncEnabled() {
        return connectionModel.isSyncWithVideo() && connectionModel.isConnected();
    }

    // ------------------------------------------------------------------ acciones del FXML

    @FXML
    private void onToggleConnection() {
        connection.toggleConnection();
    }

    @FXML
    private void onUploadSequence() {
        connection.uploadSequence();
    }

    @FXML
    private void onRunFromPlayhead() {
        connection.play(timelineModel.getPlayheadTime());
    }

    @FXML
    private void onStopAll() {
        connection.stopAll();
    }

    @FXML
    private void onOpenVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Abrir video");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video compatible (MP4, M4V, FLV, HLS)",
                        "*.mp4", "*.m4v", "*.flv", "*.fxm", "*.m3u8"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        File file = chooser.showOpenDialog(player.getScene().getWindow());
        if (file != null) player.openFile(file);
    }

    @FXML
    private void onSave() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Guardar");
        alert.setHeaderText("Próximamente");
        alert.setContentText("El guardado de la configuración de efectos aún no está implementado.");
        alert.getDialogPane().getStylesheets().addAll(player.getScene().getStylesheets());
        alert.show();
    }
}
