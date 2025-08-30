package org.to.telegramfinalproject.UI;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ClientConnection;
import org.to.telegramfinalproject.Database.userDatabase;

import java.io.IOException;

import static org.to.telegramfinalproject.UI.AppRouter.showLogin;

public class RegisterController {

    @FXML private TextField userIdField;
    @FXML private TextField usernameField;
    @FXML private TextField profileNameField;

    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visibleConfirmPasswordField;

    @FXML private Button togglePasswordBtn;
    @FXML private Button toggleConfirmBtn;

    @FXML private Label errorLabel;

    private ClientConnection connection;

    private boolean passwordVisible = false;
    private boolean confirmVisible = false;

    @FXML
    public void initialize() {

        try {
            org.to.telegramfinalproject.Client.TelegramClient.getOrInitForUI();
        } catch (Exception ignored) {
        }
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visibleConfirmPasswordField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
    }

        @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        togglePasswordBtn.setText(passwordVisible ? "👁" : "👁");
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        confirmVisible = !confirmVisible;
        visibleConfirmPasswordField.setVisible(confirmVisible);
        visibleConfirmPasswordField.setManaged(confirmVisible);
        confirmPasswordField.setVisible(!confirmVisible);
        confirmPasswordField.setManaged(!confirmVisible);
        toggleConfirmBtn.setText(confirmVisible ? "👁" : "👁");
    }

//    @FXML
//    private void handleRegister() {
//        String userID = userIdField.getText().trim();
//        String username = usernameField.getText().trim();
//        String profileName = profileNameField.getText().trim();
//        String password = passwordField.getText();
//        String confirmPass = confirmPasswordField.getText();
//
//        userDatabase userDb = new userDatabase();
//        String passwordRegex = "\\b(?=[^\\s]*[A-Z])(?=[^\\s]*[a-z])(?=[^\\s]*\\d)(?=[^\\s]*[!@#$%^&*])[^\\s]{8,}\\b";
//
//        // 1. Empty fields
//        if (userID.isEmpty() || username.isEmpty() || profileName.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
//            showError("Please fill in all required fields.");
//            return;
//        }
//
//        // 2. Username exists
//        if (userDb.existsByUsername(username)) {
//            showError("This username is already taken.");
//            return;
//        }
//
//        // 3. User ID exists
//        if (userDb.existsByUserId(userID)) {
//            showError("This user ID is already taken.");
//            return;
//        }
//
//        // 4. Password mismatch
//        if (!password.equals(confirmPass)) {
//            showError("Passwords do not match.");
//            return;
//        }
//
//        // 5. Weak password
//        if (!password.matches(passwordRegex)) {
//            showError("Password must be at least 8 characters, include a capital letter, a number, and a special character.");
//            return;
//        }
//
//        // 6. Attempt registration
//        try {
//            JSONObject request = new JSONObject();
//            request.put("action", "register");
//            request.put("user_id", userID);
//            request.put("username", username);
//            request.put("password", password);
//            request.put("profile_name", profileName);
//            connection.send(request.toString());
//
//            // Simulate successful registration (since main.fxml isn’t ready)
//            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Registration successful!");
//            alert.show();
//
//        } catch (Exception ex) {
//            showError("Failed to register. Please try again later.");
//        }
//    }



    @FXML
    private void handleRegister() {
        String userID      = userIdField.getText().trim();
        String username    = usernameField.getText().trim();
        String profileName = profileNameField.getText().trim();
        String password    = passwordField.getText();
        String confirmPass = confirmPasswordField.getText();

        userDatabase userDb = new userDatabase();
        String passwordRegex = "\\b(?=[^\\s]*[A-Z])(?=[^\\s]*[a-z])(?=[^\\s]*\\d)(?=[^\\s]*[!@#$%^&*])[^\\s]{8,}\\b";

        // اعتبارسنجی‌ها
        if (userID.isEmpty() || username.isEmpty() || profileName.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
            showError("Please fill in all required fields."); return;
        }
        if (userDb.existsByUsername(username)) { showError("This username is already taken."); return; }
        if (userDb.existsByUserId(userID))     { showError("This user ID is already taken."); return; }
        if (!password.equals(confirmPass))     { showError("Passwords do not match."); return; }
        if (!password.matches(passwordRegex))  { showError("Password must be at least 8 characters, include a capital letter, a number, and a special character."); return; }

        // UI را موقتاً disable کن (اختیاری)
        setBusy(true);

        new Thread(() -> {
            try {
                // 1) مطمئن شو کانکشن/لیسنر روشن است
                var cli     = org.to.telegramfinalproject.Client.TelegramClient.getOrInitForUI();
                var handler = cli.getHandler();

                // 2) Register
                var regReq = new org.json.JSONObject()
                        .put("action", "register")
                        .put("user_id", userID)
                        .put("username", username)
                        .put("password", password)
                        .put("profile_name", profileName);

                var regResp = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(regReq);
                if (regResp == null || !"success".equalsIgnoreCase(regResp.optString("status"))) {
                    javafx.application.Platform.runLater(() -> {
                        setBusy(false);
                        showError(regResp != null ? regResp.optString("message","Register failed.") : "Register failed.");
                    });
                    return;
                }

                // 3) Auto-Login با همان کانکشن
                handler.login(username, password);

                javafx.application.Platform.runLater(() -> {
                    setBusy(false);
                    if (org.to.telegramfinalproject.Client.Session.currentUser == null || !handler.wasSuccess()) {
                        showError(handler.getLastMessage().isEmpty() ? "Login failed." : handler.getLastMessage());
                        return;
                    }
                    org.to.telegramfinalproject.UI.AppRouter.showMain();
                });

            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    setBusy(false);
                    showError("Failed to register/login: " + ex.getMessage());
                });
            }
        }, "register-thread").start();
    }

    private void setBusy(boolean b){
        userIdField.setDisable(b);
        usernameField.setDisable(b);
        profileNameField.setDisable(b);
        passwordField.setDisable(b);
        visiblePasswordField.setDisable(b);
        confirmPasswordField.setDisable(b);
        visibleConfirmPasswordField.setDisable(b);
        togglePasswordBtn.setDisable(b);
        toggleConfirmBtn.setDisable(b);
    }



    @FXML
    private void switchToLogin() throws IOException {
        showLogin();
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
            errorLabel.setStyle("-fx-text-fill: red;"); // 🔴 force red
            errorLabel.setVisible(true);
        });
        pause.play();
    }
}
