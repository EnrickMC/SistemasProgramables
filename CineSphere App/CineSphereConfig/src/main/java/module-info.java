module com.csc {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;
    requires java.net.http;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.bootstrapicons;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    // MVC
    opens com.csc to javafx.fxml;
    opens com.csc.controller to javafx.fxml;
    opens com.csc.view to javafx.fxml;

    exports com.csc;
    exports com.csc.model;
    exports com.csc.view;
    exports com.csc.controller;
    exports com.csc.service;
}
