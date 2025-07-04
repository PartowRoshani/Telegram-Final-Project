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
    opens org.to.telegramfinalproject to javafx.fxml;
    exports org.to.telegramfinalproject;
    exports org.to.telegramfinalproject.Client;
    opens org.to.telegramfinalproject.Client to javafx.fxml;
}