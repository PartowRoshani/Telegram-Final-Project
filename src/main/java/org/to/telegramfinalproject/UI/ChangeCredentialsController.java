package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ChangeCredentialsController {

    @FXML private VBox credentialsCard;
    @FXML private Pane overlayBackground;
    @FXML private TextField usernameField;
    @FXML private Label usernameLabel;

    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordBtn;

    @FXML private Label confirmPasswordLabel;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField visibleConfirmPasswordField;
    @FXML private Button toggleConfirmBtn;

    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    @FXML private Button closeButton;

    private boolean passwordVisible = false;
    private boolean confirmVisible = false;

    @FXML
    private void initialize() {
        // Toggle password visibility
        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());
        toggleConfirmBtn.setOnAction(e -> toggleConfirmVisibility());

        saveButton.setOnAction(e -> validateAndSave());

        // Close the overlay
        closeButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(credentialsCard.getParent()));
        cancelButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(credentialsCard.getParent()));
        overlayBackground.setOnMouseClicked(e ->
                MainController.getInstance().closeOverlay(credentialsCard.getParent())
        );

        // Reset error when typing
        usernameField.textProperty().addListener((obs, o, n) -> removeError(usernameField, usernameLabel));

        passwordField.textProperty().addListener((obs, o, n) -> {
            visiblePasswordField.setText(n); // keep synced
            removeError(passwordField, passwordLabel);
            removeError(visiblePasswordField, passwordLabel);
        });
        visiblePasswordField.textProperty().addListener((obs, o, n) -> {
            passwordField.setText(n);
            removeError(passwordField, passwordLabel);
            removeError(visiblePasswordField, passwordLabel);
        });

        confirmPasswordField.textProperty().addListener((obs, o, n) -> {
            visibleConfirmPasswordField.setText(n);
            removeError(confirmPasswordField, confirmPasswordLabel);
            removeError(visibleConfirmPasswordField, confirmPasswordLabel);
        });
        visibleConfirmPasswordField.textProperty().addListener((obs, o, n) -> {
            confirmPasswordField.setText(n);
            removeError(confirmPasswordField, confirmPasswordLabel);
            removeError(visibleConfirmPasswordField, confirmPasswordLabel);
        });

        // Auto_focus password field when overlay opens
        Platform.runLater(() -> usernameField.requestFocus());
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        visiblePasswordField.setText(passwordField.getText());
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);

        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);

        togglePasswordBtn.setText(passwordVisible ? "👁" : "👁");
    }

    private void toggleConfirmVisibility() {
        confirmVisible = !confirmVisible;
        visibleConfirmPasswordField.setText(confirmPasswordField.getText());
        visibleConfirmPasswordField.setVisible(confirmVisible);
        visibleConfirmPasswordField.setManaged(confirmVisible);

        confirmPasswordField.setVisible(!confirmVisible);
        confirmPasswordField.setManaged(!confirmVisible);

        toggleConfirmBtn.setText(confirmVisible ? "👁" : "👁");
    }

    private void validateAndSave() {
        boolean hasError = false;

        // --- Username check ---
        if (usernameField.getText().trim().isEmpty()) {
            addError(usernameField, usernameLabel);
            hasError = true;
        } else {
            removeError(usernameField, usernameLabel);
        }

        // --- Password check ---
        String pwd = passwordVisible ? visiblePasswordField.getText().trim() : passwordField.getText().trim();
        if (pwd.isEmpty()) {
            addError(passwordField, passwordLabel);
            addError(visiblePasswordField, passwordLabel);
            hasError = true;
        } else {
            removeError(passwordField, passwordLabel);
            removeError(visiblePasswordField, passwordLabel);
        }

        // --- Confirm password check ---
        String confirm = confirmVisible ? visibleConfirmPasswordField.getText().trim() : confirmPasswordField.getText().trim();
        if (confirm.isEmpty()) {
            addError(confirmPasswordField, confirmPasswordLabel);
            addError(visibleConfirmPasswordField, confirmPasswordLabel);
            hasError = true;
        } else if (!confirm.equals(pwd)) {
            addError(confirmPasswordField, confirmPasswordLabel);
            addError(visibleConfirmPasswordField, confirmPasswordLabel);
            hasError = true;
        } else {
            removeError(confirmPasswordField, confirmPasswordLabel);
            removeError(visibleConfirmPasswordField, confirmPasswordLabel);
        }

        if (hasError) return;

        // TODO: send to server
        System.out.println("Saving new credentials...");
    }

    private void addError(TextField field, Label label) {
        if (!field.getStyleClass().contains("error-input")) {
            field.getStyleClass().add("error-input");
        }
        if (!label.getStyleClass().contains("error-label")) {
            label.getStyleClass().add("error-label");
        }
    }

    private void removeError(TextField field, Label label) {
        field.getStyleClass().remove("error-input");
        label.getStyleClass().remove("error-label");
    }
}
