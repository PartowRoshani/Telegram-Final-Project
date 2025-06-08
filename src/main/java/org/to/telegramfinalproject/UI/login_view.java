package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class login_view {

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    public void initialize() {

        loginButton.setOnAction(e -> {
            switchScene("LoginForm.fxml");
        });

        registerButton.setOnAction(e -> {
            switchScene("RegisterForm.fxml");
        });
    }

    private void switchScene(String fxmlFile) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/" + fxmlFile));
            Parent root = loader.load();


            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

