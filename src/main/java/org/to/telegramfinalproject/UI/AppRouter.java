// org.to.telegramfinalproject.UI.AppRouter
package org.to.telegramfinalproject.UI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class AppRouter {
    private static Stage stage;
    private static Scene scene;

    private AppRouter() {}

    public static void init(Stage st, Scene sc) {
        stage = st;
        scene = sc;
    }

    public static void showIntro()  { setRoot("/org/to/telegramfinalproject/Fxml/intro.fxml"); }
    public static void showLogin()  { setRoot("/org/to/telegramfinalproject/Fxml/login_view.fxml"); }
    public static void showRegister(){ setRoot("/org/to/telegramfinalproject/Fxml/register_view.fxml"); }
    public static void showMain()   { setRoot("/org/to/telegramfinalproject/Fxml/main.fxml"); }

    private static void setRoot(String fxmlPath) {
        try {
            System.out.println("Router: setRoot -> " + fxmlPath);
            FXMLLoader fx = new FXMLLoader(AppRouter.class.getResource(fxmlPath));
            Parent root = fx.load();
            if (scene != null) scene.setRoot(root);
            else if (stage != null) stage.setScene(new Scene(root, 1480, 820));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
