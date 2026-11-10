package com.csc.view;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

/**
 * Reproductor de video basado en JavaFX Media.
 *
 * Formatos soportados por JavaFX: MP4/M4V (H.264 + AAC), FLV (VP6), HLS (.m3u8),
 * audio MP3/WAV/AIFF. MKV, AVI, WebM, HEVC/H.265, etc. NO están soportados.
 */
public class VideoPlayerView extends BorderPane {

    public static final String SUPPORTED_FORMATS = "MP4 / M4V (H.264 + AAC), FLV, HLS (.m3u8)";

    private final StackPane surface = new StackPane();
    private final MediaView mediaView = new MediaView();
    private final VBox placeholder;
    private final FontIcon playIcon = new FontIcon("bi-play-fill");
    private final Slider progress = new Slider(0, 1, 0);
    private final Slider volume = new Slider(0, 1, 0.8);
    private final Label timeLabel = new Label("00:00.0 / 00:00.0");

    private final DoubleProperty currentTime = new SimpleDoubleProperty(0);
    private final DoubleProperty duration = new SimpleDoubleProperty(0);
    private final BooleanProperty hasMedia = new SimpleBooleanProperty(false);
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final StringProperty fileName = new SimpleStringProperty("Ningún video cargado");

    private MediaPlayer player;
    private boolean updatingProgress;

    public VideoPlayerView() {
        getStyleClass().add("video-player");

        placeholder = buildPlaceholder();
        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(surface.widthProperty());
        mediaView.fitHeightProperty().bind(surface.heightProperty());
        surface.getStyleClass().add("video-surface");
        surface.setMinSize(0, 0);
        surface.setPrefSize(640, 360);
        surface.getChildren().addAll(mediaView, placeholder);
        installFileDrop();

        setTop(buildHeader());
        setCenter(surface);
        setBottom(buildControls());
    }

    // ------------------------------------------------------------------ UI

    private HBox buildHeader() {
        FontIcon film = new FontIcon("bi-film");
        Label name = new Label();
        name.textProperty().bind(fileName);
        name.setGraphic(film);
        name.getStyleClass().add("player-file-name");
        name.setMinWidth(0);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label formats = new Label("Formatos: " + SUPPORTED_FORMATS, new FontIcon("bi-info-circle"));
        formats.getStyleClass().add("player-formats");
        formats.setMinWidth(0);

        HBox header = new HBox(10, name, spacer, formats);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("player-header");
        return header;
    }

    private VBox buildPlaceholder() {
        FontIcon icon = new FontIcon("bi-camera-reels");
        icon.getStyleClass().add("placeholder-icon");
        Label title = new Label("Arrastra un video aquí");
        title.getStyleClass().add("placeholder-title");
        Label subtitle = new Label("o usa «Abrir video» en la barra superior");
        subtitle.getStyleClass().add("placeholder-subtitle");
        VBox box = new VBox(8, icon, title, subtitle);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("video-placeholder");
        box.setMouseTransparent(true);
        return box;
    }

    private HBox buildControls() {
        Button toStart = controlButton("bi-skip-start-fill", "Ir al inicio");
        toStart.setOnAction(e -> seek(0));

        Button play = new Button(null, playIcon);
        play.getStyleClass().addAll("player-button", "play-button");
        play.setTooltip(new Tooltip("Reproducir / pausar"));
        play.setOnAction(e -> togglePlay());

        Button stop = controlButton("bi-stop-fill", "Detener");
        stop.setOnAction(e -> {
            if (player != null) {
                player.stop();
                player.seek(Duration.ZERO);
            }
        });

        Button toEnd = controlButton("bi-skip-end-fill", "Ir al final");
        toEnd.setOnAction(e -> seek(duration.get()));

        for (Button b : new Button[]{toStart, play, stop, toEnd}) {
            b.disableProperty().bind(hasMedia.not());
        }

        timeLabel.getStyleClass().add("player-time");
        timeLabel.setMinWidth(Region.USE_PREF_SIZE);

        progress.getStyleClass().add("player-progress");
        progress.disableProperty().bind(hasMedia.not());
        HBox.setHgrow(progress, Priority.ALWAYS);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.valueProperty().addListener((o, ov, nv) -> {
            if (updatingProgress || player == null) return;
            double t = nv.doubleValue();
            if (progress.isValueChanging() || Math.abs(t - player.getCurrentTime().toSeconds()) > 0.25) {
                player.seek(Duration.seconds(t));
            }
        });

        FontIcon volumeIcon = new FontIcon("bi-volume-up-fill");
        volumeIcon.getStyleClass().add("player-volume-icon");
        volume.getStyleClass().add("player-volume");
        volume.setPrefWidth(90);
        volume.valueProperty().addListener((o, ov, nv) ->
                volumeIcon.setIconLiteral(nv.doubleValue() == 0 ? "bi-volume-mute-fill" : "bi-volume-up-fill"));

        HBox controls = new HBox(8, toStart, play, stop, toEnd, timeLabel, progress, volumeIcon, volume);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("player-controls");
        return controls;
    }

    private Button controlButton(String icon, String tooltip) {
        Button b = new Button(null, new FontIcon(icon));
        b.getStyleClass().add("player-button");
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private void installFileDrop() {
        surface.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        surface.setOnDragDropped(e -> {
            boolean ok = e.getDragboard().hasFiles();
            if (ok) openFile(e.getDragboard().getFiles().get(0));
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    // ------------------------------------------------------------------ reproducción

    public void openFile(File file) {
        disposePlayer();
        try {
            Media media = new Media(file.toURI().toString());
            MediaPlayer p = new MediaPlayer(media);
            media.setOnError(() -> showError(file, media.getError()));
            p.setOnError(() -> showError(file, p.getError()));

            p.setOnReady(() -> {
                double d = media.getDuration().toSeconds();
                duration.set(Double.isFinite(d) ? d : 0);
                updatingProgress = true;
                progress.setMax(Math.max(0.1, duration.get()));
                progress.setValue(0);
                updatingProgress = false;
                hasMedia.set(true);
                updateTimeLabel();
            });
            p.currentTimeProperty().addListener((o, ov, nv) -> {
                currentTime.set(nv.toSeconds());
                if (!progress.isValueChanging()) {
                    updatingProgress = true;
                    progress.setValue(nv.toSeconds());
                    updatingProgress = false;
                }
                updateTimeLabel();
            });
            p.statusProperty().addListener((o, ov, nv) -> {
                playIcon.setIconLiteral(nv == MediaPlayer.Status.PLAYING ? "bi-pause-fill" : "bi-play-fill");
                playing.set(nv == MediaPlayer.Status.PLAYING);
            });
            p.setOnEndOfMedia(p::pause);
            p.volumeProperty().bind(volume.valueProperty());

            player = p;
            mediaView.setMediaPlayer(p);
            fileName.set(file.getName());
            placeholder.setVisible(false);
        } catch (MediaException | IllegalArgumentException ex) {
            showError(file, ex);
        }
    }

    public void togglePlay() {
        if (player == null) return;
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else {
            if (currentTime.get() >= duration.get() - 0.05) player.seek(Duration.ZERO);
            player.play();
        }
    }

    public void seek(double seconds) {
        if (player != null) player.seek(Duration.seconds(seconds));
    }

    private void disposePlayer() {
        if (player != null) {
            player.volumeProperty().unbind();
            player.dispose();
            player = null;
        }
        mediaView.setMediaPlayer(null);
        playing.set(false);
        hasMedia.set(false);
        duration.set(0);
        currentTime.set(0);
        placeholder.setVisible(true);
        fileName.set("Ningún video cargado");
        updateTimeLabel();
    }

    private void updateTimeLabel() {
        timeLabel.setText(TimeFormat.format(currentTime.get()) + " / " + TimeFormat.format(duration.get()));
    }

    private void showError(File file, Exception error) {
        Platform.runLater(() -> {
            disposePlayer();
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No se pudo abrir el video");
            alert.setHeaderText("JavaFX no puede reproducir «" + file.getName() + "»");
            alert.setContentText("JavaFX Media solo soporta: " + SUPPORTED_FORMATS + ".\n\n"
                    + "Convierte el archivo a MP4 (H.264 + AAC), por ejemplo con HandBrake o con:\n"
                    + "ffmpeg -i entrada.mkv -c:v libx264 -c:a aac salida.mp4\n\n"
                    + "Detalle: " + (error != null ? error.getMessage() : "desconocido"));
            alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
            alert.show();
        });
    }

    // ------------------------------------------------------------------ propiedades

    public ReadOnlyDoubleProperty currentTimeProperty() { return currentTime; }
    public ReadOnlyDoubleProperty durationProperty() { return duration; }
    public StringProperty fileNameProperty() { return fileName; }
    public BooleanProperty hasMediaProperty() { return hasMedia; }
    public BooleanProperty playingProperty() { return playing; }
    public boolean isPlaying() { return playing.get(); }
}
