module org.to.telegramfinalproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;   // بهتره اضافه باشه
    requires javafx.media;
    requires javafx.web;

    requires org.json;
    requires java.sql;
    requires java.desktop;

    // لایبرری‌های جانبی — اگر واقعاً در مسیر ماژول هستند
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires mp3agic;
    requires spark.core;          // فقط اگر واقعاً استفاده می‌کنی
    requires javax.servlet.api;   // فقط اگر واقعاً استفاده می‌کنی

    // کنترلرها/کلاس‌هایی که FXML به آن‌ها دسترسی بازتابی دارد
    opens org.to.telegramfinalproject.UI to javafx.fxml;
    opens org.to.telegramfinalproject.Client to javafx.fxml;
    opens org.to.telegramfinalproject.Models to javafx.fxml; // اگر مدل‌ها داخل FXML مصرف می‌شوند

    // اگر پکیج‌هایی را می‌خواهی خارج از ماژول در دسترس قرار دهی
    exports org.to.telegramfinalproject.UI;
    exports org.to.telegramfinalproject.Client;
    exports org.to.telegramfinalproject.Models;
}
