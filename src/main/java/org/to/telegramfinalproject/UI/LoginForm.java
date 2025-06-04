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
import org.to.telegramfinalproject.Models.User;
import org.to.telegramfinalproject.Security.PasswordHashing;

import java.io.IOException;

public class LoginForm {

    @FXML
    private Button loginButton;
    @FXML
    private Button backButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private ClientConnection connection;

    @FXML
    public void initialize() {

        try {
            connection = new ClientConnection("localhost", 12345);
        } catch (Exception e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            JSONObject request = new JSONObject();
            request.put("action", "login");
            request.put("user_id", JSONObject.NULL);
            request.put("username", username);
            request.put("password", password);
            request.put("profile_name", JSONObject.NULL);
            if (connection!=null) {
                connection.send(request.toString());
            }
            userDatabase userDb = new userDatabase();
            User user = userDb.findByUsername(username);

            if(!userDb.existsByUsername(username)){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid username");
                alert.show();
            }
            else if(!PasswordHashing.verify(password,user.getPassword())){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid password");
                alert.show();
            }
            else if(!PasswordHashing.verify(password,user.getPassword()) && !userDb.existsByUsername(username)){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid password and username");
                alert.show();
            }
            else{
                try {
                    String responseStr = connection.receive();
                    JSONObject response = new JSONObject(responseStr);
                    System.out.println("Status: " + response.getString("status"));
                    System.out.println("Message: " + response.getString("message"));
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, " Message: " + response.getString("message"));
                    alert.show();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error receiving response: " + ex.getMessage());
                    alert.show();
                }
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

