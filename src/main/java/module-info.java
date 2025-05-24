module ulb {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.media;  // For audio/video functionality

    requires java.sql;  // For database operations
    requires org.xerial.sqlitejdbc;
    requires org.json;

    requires java.desktop;
    requires jaudiotagger;
    requires com.sun.jna;
    requires com.fasterxml.jackson.databind;
    requires transitive java.logging;
    requires javafx.graphics;

    opens ulb.controller to javafx.fxml;  // Allows JavaFX to access controllers
    opens ulb.view to javafx.fxml;        // Allows JavaFX to access views

    exports ulb;
    exports ulb.controller;
    exports ulb.view;
    exports ulb.model;
    exports ulb.model.handbleError;
    opens ulb to javafx.fxml;
    exports ulb.controller.handleError;
    opens ulb.controller.handleError to javafx.fxml;
}
