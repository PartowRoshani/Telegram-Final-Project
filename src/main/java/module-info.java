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
    requires jdk.internal.le;

    // FXML کنترلرها در این پکیج‌اند:
    opens org.to.telegramfinalproject.UI to javafx.fxml;
    // اگر FXML از کلاس‌های Client هم استفاده می‌کند:
    opens org.to.telegramfinalproject.Client to javafx.fxml;

    // فقط اگر لازم است از بیرون به این API‌ها کامپایل شود:
    exports org.to.telegramfinalproject.Client;
    // ⚠️ عمداً NOT exporting/opens ریشه‌ی پکیج، چون کلاس ندارد.
}
