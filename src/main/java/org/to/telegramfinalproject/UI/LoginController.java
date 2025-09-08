package org.to.telegramfinalproject.UI;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.ClientConnection;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Client.TelegramClient;
import org.to.telegramfinalproject.Database.userDatabase;
import org.to.telegramfinalproject.Models.User;
import org.to.telegramfinalproject.Security.PasswordHashing;

import java.io.IOException;

import static org.to.telegramfinalproject.UI.AppRouter.showRegister;

public class LoginController {

    @FXML private TextField usernameField;

    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;

    @FXML private Button toggleVisibilityBtn;
    @FXML private Label errorLabel;

    private ClientConnection connection;
    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        // Sync hidden and visible password fields
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        try {
            connection = new ClientConnection("localhost", 8080);
        } catch (Exception e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        toggleVisibilityBtn.setText(passwordVisible ? "👁" : "👁");
    }

//    @FXML
//    private void handleLogin() {
//        String username = usernameField.getText();
//        String password = passwordField.getText();
//
//        // 1. Check for empty fields
//        if (username.isEmpty() || password.isEmpty()) {
//            showError("Please fill in all required fields.");
//            return;
//        }
//
//        // 2. Check if username exists
//        userDatabase userDb = new userDatabase();
//        if (!userDb.existsByUsername(username)) {
//            showError("This username doesn’t exist.");
//            return;
//        }
//
//        // 3. Check if password is correct
//        User user = userDb.findByUsername(username);
//        if (!PasswordHashing.verify(password, user.getPassword())) {
//            showError("Incorrect password.");
//            return;
//        }
//
//        // 4. Attempt to send to server
//        try {
//            JSONObject request = new JSONObject();
//            request.put("action", "login");
//            request.put("user_id", JSONObject.NULL);
//            request.put("username", username);
//            request.put("password", password);
//            request.put("profile_name", JSONObject.NULL);
//
//            if (connection != null) {
//                connection.send(request.toString());
//                String responseStr = connection.receive();
//                JSONObject response = new JSONObject(responseStr);
//                System.out.println("Status: " + response.getString("status"));
//                System.out.println("Message: " + response.getString("message"));
//
//                // Simulate successful login (since main.fxml isn’t ready)
//                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Login successful!");
//                alert.show();
//            }
//
//        } catch (Exception ex) {
//            showError("Unable to connect to server. Please try again later.");
//        }
//    }



    @FXML
    private void handleLogin() {
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        // 1. Check for empty fields
        if (u.isEmpty() || p.isEmpty()) {
            showError("Please fill in all required fields.");
            return;
        }

        // 2. Check if username exists
        userDatabase userDb = new userDatabase();
        if (!userDb.existsByUsername(u)) {
            showError("This username doesn’t exist.");
            return;
        }

        // 3. Check if password is correct
        User user = userDb.findByUsername(u);
        if (!PasswordHashing.verify(p, user.getPassword())) {
            showError("Incorrect password.");
            return;
        }

        setUiBusy(true);

        new Thread(() -> {
            try {
                var cli     = org.to.telegramfinalproject.Client.TelegramClient.getOrInitForUI();
                var handler = cli.getHandler();


                handler.login(u,p);

                javafx.application.Platform.runLater(() -> {
                    setUiBusy(false);

                    if (!handler.wasSuccess() || Session.currentUser == null) {
                        showError(handler.getLastMessage().isEmpty() ? "Login failed." : handler.getLastMessage());
                        return;
                    }


                    goMain();
                });

            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    setUiBusy(false);
                    showError("Connection error.");
                });
            }
        }, "login-thread").start();
    }

    private void goMain() {
        AppRouter.showMain();
    }


    private void setUiBusy(boolean b) {
        usernameField.setDisable(b);
        passwordField.setDisable(b);
    }


    @FXML
    private void switchToRegister() throws IOException {
        showRegister();
    }

    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/" + fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 500, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load scene: " + fxmlFile);
        }
    }

    private void showError(String message) {
        errorLabel.setVisible(false);
        errorLabel.setText("");  // Clear the label text

        PauseTransition pause = new PauseTransition(Duration.millis(50));
        pause.setOnFinished(event -> {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setVisible(true);
        });
        pause.play();
    }
}