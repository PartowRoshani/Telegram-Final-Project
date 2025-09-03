module org.to.telegramfinalproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires org.json;
    requires java.sql;
    requires java.desktop;
    requires spark.core;
    requires javax.servlet.api;
    requires mp3agic;
    opens org.to.telegramfinalproject to javafx.fxml;
    exports org.to.telegramfinalproject;
    exports org.to.telegramfinalproject.Client;
    opens org.to.telegramfinalproject.Client to javafx.fxml;
}