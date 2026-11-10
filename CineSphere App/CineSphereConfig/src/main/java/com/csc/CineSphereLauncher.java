package com.csc;

import javafx.application.Application;

/**
 * Punto de entrada para ejecutar desde el IDE (evita el error
 * "JavaFX runtime components are missing" al lanzar sin module-path).
 */
public class CineSphereLauncher {
    public static void main(String[] args) {
        Application.launch(CineSphereApplication.class, args);
    }
}
