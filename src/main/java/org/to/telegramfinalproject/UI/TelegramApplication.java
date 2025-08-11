package org.to.telegramfinalproject.UI;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.to.telegramfinalproject.HelloApplication;

import java.io.IOException;

public class TelegramApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(TelegramApplication.class.getResource("/org/to/telegramfinalproject/sidebar_menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1480, 820);

        scene.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/sidebar_menu.css").toExternalForm());

        stage.setTitle("Telegram");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }

}