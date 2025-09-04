package org.to.telegramfinalproject.UI;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class EditProfileController {

    @FXML private Pane overlayBackground;
    @FXML private VBox editCard;

    @FXML private Button backButton;
    @FXML private Button closeButton;

    @FXML private ImageView profileImageView;
    @FXML private ImageView cameraIcon;
    @FXML private Button cameraButton;
    @FXML private Label profileName;
    @FXML private Label profileStatus;

    @FXML private TextField nameField;
    @FXML private TextField bioField;
    @FXML private TextField usernameField;

    @FXML
    private void initialize() {
        backButton.setGraphic(new ImageView(
                new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/back_button_dark.png"))
        ));

        // close on background click
        overlayBackground.setOnMouseClicked(e -> {
            saveChangesAndClose();
            closeEdit();
        });

        closeButton.setOnAction(e -> {
            saveChangesAndClose();
            closeEdit();
        });

        backButton.setOnAction(e -> {
            saveChangesAndClose();
            MainController.getInstance().goBack(overlayBackground);
        });

        // Load your camera icon image (white camera icon for visibility)
        cameraIcon.setImage(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/org/to/telegramfinalproject/Icons/camera.png"))
        ));

        // Handle click to open file chooser
        cameraButton.setOnMouseClicked(e -> openImageChooser());

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (editCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(editCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateBackIcon(newVal);
        });

        // Set initial state
        updateBackIcon(ThemeManager.getInstance().isDarkMode());
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

    private void openImageChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (file != null) {
            profileImageView.setImage(new Image(file.toURI().toString()));
            // TODO: save this new image to DB or server
        }
    }

    private void closeEdit() {
        MainController.getInstance().closeOverlay(editCard.getParent());
    }

    public void setProfileData(String name, String status, String bio, String userId, Image profileImage) {
        profileStatus.setText(status);
        profileName.setText(name);
        nameField.setText(name);
        bioField.setText(bio != null ? bio : "");
        usernameField.setText(userId);
        profileImageView.setImage(profileImage);
    }

    private void saveChangesAndClose() {
        JSONObject currentUser = Session.currentUser;

        String newBio = bioField.getText().trim();
        String newName = nameField.getText().trim();
        String newUserId = usernameField.getText().trim();

        boolean anyError = false;

        // --- Update bio ---
        String oldBio = currentUser.optString("bio", "");
        if (!newBio.equals(oldBio)) {
            JSONObject req = new JSONObject().put("action", "edit_bio").put("new_bio", newBio);
            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp == null || !"success".equals(resp.optString("status"))) {
                anyError = true;
                showAlert("Error", resp != null ? resp.optString("message", "Unknown error") : "Unknown error", Alert.AlertType.ERROR);
            } else {
                currentUser.put("bio", newBio);
            }
        }

        // --- Update profile name ---
        String oldName = currentUser.optString("profile_name", "");
        if (!newName.equals(oldName)) {
            JSONObject req = new JSONObject().put("action", "edit_profile_name").put("new_profile_name", newName);
            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp == null || !"success".equals(resp.optString("status"))) {
                anyError = true;
                showAlert("Error", resp != null ? resp.optString("message", "Unknown error") : "Unknown error", Alert.AlertType.ERROR);
            } else {
                currentUser.put("profile_name", newName);
            }
        }

        // --- Update user ID (username) ---
        String oldUserId = currentUser.optString("user_id", "");
        if (!newUserId.equals(oldUserId)) {
            JSONObject req = new JSONObject().put("action", "edit_user_id").put("new_user_id", newUserId);
            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp == null || !"success".equals(resp.optString("status"))) {
                anyError = true;
                showAlert("Error", resp != null ? resp.optString("message", "Unknown error") : "Unknown error", Alert.AlertType.ERROR);
            } else {
                currentUser.put("user_id", newUserId);
            }
        }

        if (!anyError) {
            MyProfileController.getInstance().setProfileData(
                    currentUser.optString("profile_name"),
                    "online",
                    currentUser.optString("bio"),
                    currentUser.optString("user_id"),
                    currentUser.optString("image_url", "/org/to/telegramfinalproject/Avatars/default_user_profile.png")
            );
            MainController.getInstance().closeOverlay(bioField.getParent().getParent());
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // no big header, just the message
        alert.setContentText(message);

        // optional: style it to fit your dark/light theme
        if (alert.getDialogPane().getScene() != null) {
            ThemeManager.getInstance().registerScene(alert.getDialogPane().getScene());
        }

        alert.showAndWait();
    }
}
