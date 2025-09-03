package org.to.telegramfinalproject.UI;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ClientConnection;
import org.to.telegramfinalproject.Database.userDatabase;

import java.io.IOException;

public class RegisterForm {
    @FXML
    private Button submitButton;
    @FXML
    private Button backButton;
    @FXML private TextField userIdField;
    @FXML private TextField usernameField;
    @FXML private TextField profileNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;



    private ClientConnection connection;

    @FXML
    public void initialize() {

        try {
            connection = new ClientConnection("localhost", 8000);
        } catch (Exception e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }

        submitButton.setOnAction(e -> {
            String userID = userIdField.getText();
            String username = usernameField.getText();
            String profile_name = profileNameField.getText();
            String password = passwordField.getText();
            String confirmPass =confirmPasswordField.getText();
            JSONObject request = new JSONObject();
            String passwordRegex = "\\b(?=[^\\s]*[A-Z])(?=[^\\s]*[a-z])(?=[^\\s]*\\d)(?=[^\\s]*[!@#$%^&*])[^\\s]{8,}\\b";

            userDatabase userDb = new userDatabase();
            if(password.equals(confirmPass) && password.matches(passwordRegex) && !userDb.existsByUserId(userID)&& !userDb.existsByUsername(username)){
                try {
                    request.put("action", "register");
                    request.put("user_id", userID);
                    request.put("username", username);
                    request.put("password", password);
                    request.put("profile_name", profile_name);
                    connection.send(request.toString());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Registration is successful");
                    alert.show();

                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error receiving response: " + ex.getMessage());
                    alert.show();
                }

            }
            else if(!password.equals(confirmPass) && password.matches(passwordRegex)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Password doesn't match");
                alert.show();
            }
            else if(userDb.existsByUserId(userID))
            {
                Alert alert = new Alert(Alert.AlertType.ERROR, "User ID is already exist");
                alert.show();
            }
            else if(userDb.existsByUsername(username)){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Username is already exist");
                alert.show();
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Password isn't Strong enough");
                alert.show();
            }
        });

        backButton.setOnAction(e -> {
            switchScene("login_view.fxml");
        });
    }

    private void switchScene(String fxmlFile) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/" + fxmlFile));
            Parent root = loader.load();


            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

