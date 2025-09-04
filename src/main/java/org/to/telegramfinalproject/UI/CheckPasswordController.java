package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class CheckPasswordController {

    @FXML private VBox checkPasswordCard;
    @FXML private Pane overlayBackground;
    @FXML private Button backButton;
    @FXML private Button closeButton;
    @FXML private Button cancelButton;
    @FXML private Button confirmButton;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button toggleVisibilityBtn;
    @FXML private Label passwordLabel;

    private boolean passwordVisible = false;


    @FXML
    public void initialize() {
        // Set back button icon
        ImageView backIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/back_button_dark.png"))
        );
        backIcon.setFitWidth(18);
        backIcon.setFitHeight(18);
        backButton.setGraphic(backIcon);

        cancelButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(checkPasswordCard.getParent())
        );

        closeButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(checkPasswordCard.getParent())
        );

        backButton.setOnAction(e ->
                MainController.getInstance().goBack(checkPasswordCard)
        );

        overlayBackground.setOnMouseClicked(e ->
                MainController.getInstance().closeOverlay(checkPasswordCard.getParent())
        );

        confirmButton.setOnAction(e -> validatePassword());

        // Reset error state when user starts typing again
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                passwordField.getStyleClass().remove("error");
                passwordLabel.getStyleClass().remove("error");
            }
        });

        // Reset error state when user starts typing again
        visiblePasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                visiblePasswordField.getStyleClass().remove("error");
                passwordLabel.getStyleClass().remove("error");
            }
        });

        // Auto_focus password field when overlay opens
        Platform.runLater(() -> passwordField.requestFocus());

        // Keep fields in sync
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Toggle button action
        toggleVisibilityBtn.setOnAction(e -> togglePasswordVisibility());

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (checkPasswordCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(checkPasswordCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateBackIcon(newVal);
        });

        // Set initial state
        updateBackIcon(ThemeManager.getInstance().isDarkMode());
    }

    private void validatePassword() {
        String entered = passwordField.getText().trim();
        String correct = "12345"; // demo only

        if (entered.equals(correct)) {
            // Success → reset error styles
            passwordField.getStyleClass().remove("error");
            visiblePasswordField.getStyleClass().remove("error");
            passwordLabel.getStyleClass().remove("error");

            // Go to the next scene
            openChangeCredentials();

        } else {
            // Error → add error styles
            if (!passwordField.getStyleClass().contains("error"))
                passwordField.getStyleClass().add("error");

            if (!visiblePasswordField.getStyleClass().contains("error"))
                visiblePasswordField.getStyleClass().add("error");

            if (!passwordLabel.getStyleClass().contains("error"))
                passwordLabel.getStyleClass().add("error");
        }
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);

        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);

        toggleVisibilityBtn.setText(passwordVisible ? "👁" : "👁");
    }

    private void updateBackIcon(boolean darkMode) {
        String iconPath = darkMode
                ? "/org/to/telegramfinalproject/Icons/back_button_light.png"
                : "/org/to/telegramfinalproject/Icons/back_button_dark.png";

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        backButton.setGraphic(icon);
    }

    private void openChangeCredentials() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/change_credentials.fxml"));
            Node overlay = loader.load();

            // Show new overlay
            MainController.getInstance().showOverlay(overlay);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
