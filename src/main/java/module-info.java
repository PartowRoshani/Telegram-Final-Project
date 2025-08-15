module org.to.telegramfinalproject {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // UI libraries
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    // Database (JDBC)
    requires java.sql;

    // JSON Handling
    requires org.json;

    opens org.to.telegramfinalproject to javafx.fxml;
    opens org.to.telegramfinalproject.Models to javafx.base;
    opens org.to.telegramfinalproject.Database to javafx.base;

    exports org.to.telegramfinalproject;
    exports org.to.telegramfinalproject.Models;
    exports org.to.telegramfinalproject.Database;
}
