package com.csc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class CineSphereApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(CineSphereApplication.class.getResource("view/main-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1360, 840);
        scene.getStylesheets().add(Objects.requireNonNull(
                CineSphereApplication.class.getResource("styles/cinesphere.css")).toExternalForm());

        stage.setTitle("CineSphere - Configurador de efectos");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
