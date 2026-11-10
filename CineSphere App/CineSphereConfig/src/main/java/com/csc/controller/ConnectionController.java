package com.csc.controller;

import com.csc.model.ConnectionModel;
import com.csc.model.ConnectionState;
import com.csc.model.EffectBlock;
import com.csc.model.TimelineModel;
import com.csc.service.Esp32Client;
import com.csc.service.SequenceSerializer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controla la comunicación con el ESP32: conectar, enviar la secuencia,
 * sincronizar reproducción/pausa con el video y probar bloques sueltos.
 */
public class ConnectionController {

    private static final Pattern SSID = Pattern.compile("\"ssid\"\\s*:\\s*\"([^\"]*)\"");

    private final ConnectionModel connection;
    private final TimelineModel timeline;
    private final Esp32Client client = new Esp32Client();
    private final Timeline heartbeat;

    public ConnectionController(ConnectionModel connection, TimelineModel timeline) {
        this.connection = connection;
        this.timeline = timeline;
        // Comprueba cada 5 s que el ESP32 sigue respondiendo
        heartbeat = new Timeline(new KeyFrame(Duration.seconds(5), e -> checkAlive()));
        heartbeat.setCycleCount(Timeline.INDEFINITE);
    }

    // ------------------------------------------------------------------ conexión

    public void toggleConnection() {
        if (connection.getState() == ConnectionState.CONNECTED
                || connection.getState() == ConnectionState.CONNECTING) {
            disconnect();
        } else {
            connect();
        }
    }

    public void connect() {
        client.setHost(connection.getHost());
        connection.setState(ConnectionState.CONNECTING);
        connection.setMessage("Conectando con " + client.getHost() + "...");
        client.status().whenComplete((body, error) -> Platform.runLater(() -> {
            if (connection.getState() != ConnectionState.CONNECTING) return; // cancelado
            if (error != null) {
                fail(error);
            } else {
                connection.setState(ConnectionState.CONNECTED);
                connection.setMessage("ESP32 conectado" + ssidOf(body) + " - " + client.getHost());
                heartbeat.play();
            }
        }));
    }

    public void disconnect() {
        heartbeat.stop();
        if (connection.isConnected()) client.stop();
        connection.setState(ConnectionState.DISCONNECTED);
        connection.setMessage("Desconectado del ESP32");
    }

    private void checkAlive() {
        if (!connection.isConnected()) return;
        client.status().whenComplete((body, error) -> {
            if (error != null) Platform.runLater(() -> fail(error));
        });
    }

    // ------------------------------------------------------------------ comandos

    /** Envía la secuencia actual al ESP32. */
    public CompletableFuture<String> uploadSequence() {
        if (!requireConnection()) return CompletableFuture.completedFuture("");
        int count = timeline.getBlocks().size();
        return track(client.uploadSequence(SequenceSerializer.toJson(timeline)),
                "Secuencia enviada (" + count + " bloques)");
    }

    /** Envía la secuencia y la ejecuta desde {@code fromSeconds}. */
    public void play(double fromSeconds) {
        if (!requireConnection()) return;
        String json = SequenceSerializer.toJson(timeline);
        track(client.uploadSequence(json).thenCompose(r -> client.play(fromSeconds)),
                String.format("Ejecutando en el ESP32 desde %.1f s", fromSeconds));
    }

    public void pause() {
        if (!connection.isConnected()) return;
        track(client.pause(), "ESP32 en pausa");
    }

    public void stopAll() {
        if (!requireConnection()) return;
        track(client.stop(), "ESP32 detenido: salidas apagadas");
    }

    /** Ejecuta un solo bloque al instante, para probar el actuador. */
    public void test(EffectBlock block) {
        if (block == null || !requireConnection()) return;
        track(client.test(SequenceSerializer.testEventJson(block)),
                "Probando \"" + block.getType().getLabel() + "\" durante "
                        + String.format("%.1f s", block.getDuration()));
    }

    // ------------------------------------------------------------------ utilidades

    private boolean requireConnection() {
        if (connection.isConnected()) return true;
        connection.setMessage("Primero conéctate al ESP32");
        return false;
    }

    private CompletableFuture<String> track(CompletableFuture<String> future, String okMessage) {
        return future.whenComplete((body, error) -> Platform.runLater(() -> {
            if (error != null) fail(error);
            else connection.setMessage(okMessage);
        }));
    }

    private void fail(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        heartbeat.stop();
        connection.setState(ConnectionState.ERROR);
        String reason;
        if (cause instanceof HttpConnectTimeoutException || cause instanceof ConnectException) {
            reason = "no se encontró el ESP32 en " + client.getHost()
                    + ". ¿Estás conectado a la red WiFi \"CineSphere\"?";
        } else if (cause instanceof HttpTimeoutException) {
            reason = "el ESP32 no respondió a tiempo";
        } else {
            reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        }
        connection.setMessage("Error de conexión: " + reason);
    }

    private static String ssidOf(String body) {
        if (body == null) return "";
        Matcher m = SSID.matcher(body);
        return m.find() ? " (red " + m.group(1) + ")" : "";
    }
}
