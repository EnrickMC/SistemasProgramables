package com.csc.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Cliente HTTP para el servidor MicroPython del ESP32 (red WiFi propia, IP 192.168.4.1).
 * Todas las llamadas son asíncronas para no bloquear la interfaz.
 *
 * Endpoints:
 *   GET  /status     estado del ESP32
 *   POST /sequence   carga la secuencia completa (JSON de {@link SequenceSerializer})
 *   POST /play       {"time": t}  empieza a ejecutar la secuencia desde t segundos
 *   POST /pause      pausa y apaga las salidas
 *   POST /stop       detiene y apaga todo
 *   POST /test       ejecuta un único evento al instante
 */
public class Esp32Client {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private volatile String host = "192.168.4.1";

    public void setHost(String host) {
        this.host = host == null ? "" : host.trim();
    }

    public String getHost() { return host; }

    public CompletableFuture<String> status() {
        return send(HttpRequest.newBuilder(uri("/status")).timeout(TIMEOUT).GET().build());
    }

    public CompletableFuture<String> uploadSequence(String json) {
        return post("/sequence", json);
    }

    public CompletableFuture<String> play(double fromSeconds) {
        return post("/play", String.format(Locale.ROOT, "{\"time\":%.3f}", fromSeconds));
    }

    public CompletableFuture<String> pause() {
        return post("/pause", "{}");
    }

    public CompletableFuture<String> stop() {
        return post("/stop", "{}");
    }

    public CompletableFuture<String> test(String eventJson) {
        return post("/test", eventJson);
    }

    private CompletableFuture<String> post(String path, String json) {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    private CompletableFuture<String> send(HttpRequest request) {
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new CompletionException(new IOException(
                                "HTTP " + response.statusCode() + ": " + response.body()));
                    }
                    return response.body();
                });
    }

    private URI uri(String path) {
        String h = host.isEmpty() ? "192.168.4.1" : host;
        if (!h.startsWith("http://") && !h.startsWith("https://")) h = "http://" + h;
        return URI.create(h + path);
    }
}
