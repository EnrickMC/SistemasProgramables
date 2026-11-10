package com.csc.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Estado de la conexión con el ESP32 (dirección, estado y último mensaje). */
public class ConnectionModel {

    /** IP por defecto del ESP32 en modo punto de acceso. */
    public static final String DEFAULT_HOST = "192.168.4.1";

    private final StringProperty host = new SimpleStringProperty(DEFAULT_HOST);
    private final ObjectProperty<ConnectionState> state = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
    private final StringProperty message = new SimpleStringProperty(
            "Conéctate a la red WiFi \"CineSphere\" y pulsa Conectar");
    private final BooleanProperty syncWithVideo = new SimpleBooleanProperty(true);

    public String getHost() { return host.get(); }
    public void setHost(String value) { host.set(value); }
    public StringProperty hostProperty() { return host; }

    public ConnectionState getState() { return state.get(); }
    public void setState(ConnectionState value) { state.set(value); }
    public ObjectProperty<ConnectionState> stateProperty() { return state; }

    public String getMessage() { return message.get(); }
    public void setMessage(String value) { message.set(value); }
    public StringProperty messageProperty() { return message; }

    public boolean isConnected() { return getState() == ConnectionState.CONNECTED; }

    public boolean isSyncWithVideo() { return syncWithVideo.get(); }
    public BooleanProperty syncWithVideoProperty() { return syncWithVideo; }
}
