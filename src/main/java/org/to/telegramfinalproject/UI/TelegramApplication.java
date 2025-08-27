package org.to.telegramfinalproject.UI;//package org.to.telegramfinalproject.UI;
//
//
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Scene;
//import javafx.scene.image.Image;
//import javafx.stage.Stage;
//
//import java.io.IOException;
//
//public class TelegramApplication extends Application {
//    @Override
//    public void start(Stage stage) throws IOException {
//        FXMLLoader fxmlLoader = new FXMLLoader(TelegramApplication.class.getResource("/org/to/telegramfinalproject/Fxml/main.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), 1480, 820);
//
//        scene.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/light_theme.css").toExternalForm());
//
//        stage.setTitle("Telegram");
//        stage.setScene(scene);
//
//        // Add icon to the stage
//        Image icon = new Image(TelegramApplication.class.getResourceAsStream("/org/to/telegramfinalproject/Images/telegram_icon.png"));
//        stage.getIcons().add(icon);
//
//        stage.show();
//    }
//    public static void main(String[] args) {
//        launch();
//    }
//
//}


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.to.telegramfinalproject.UI.AppRouter;

import java.io.IOException;

public class TelegramApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // اول intro.fxml
        FXMLLoader fx = new FXMLLoader(
                TelegramApplication.class.getResource("/org/to/telegramfinalproject/Fxml/intro.fxml"));
        Scene scene = new Scene(fx.load(), 1480, 820);

        scene.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/light_theme.css").toExternalForm()
        );

        stage.setTitle("Telegram");
        stage.getIcons().add(new Image(
                TelegramApplication.class.getResourceAsStream("/org/to/telegramfinalproject/Images/telegram_icon.png")
        ));
        stage.setScene(scene);
        stage.show();

        AppRouter.init(stage, scene);
    }

    public static void main(String[] args) { launch(); }
}
