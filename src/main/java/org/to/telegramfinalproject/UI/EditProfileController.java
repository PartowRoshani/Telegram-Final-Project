package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

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
        overlayBackground.setOnMouseClicked(e -> closeEdit());

        closeButton.setOnAction(e -> closeEdit());
        backButton.setOnAction(e -> goBackToProfile());

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

    @FXML
    private void goBackToProfile() {
        // Close edit overlay
        MainController.getInstance().closeOverlay(editCard.getParent());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/my_profile.fxml"));
            Node profileOverlay = loader.load();

            // Get the controller for the loaded MyProfile scene
            MyProfileController controller = loader.getController();

            // Pass updated data from edit fields
            controller.setProfileData(
                    nameField.getText(),
                    "online",
                    bioField.getText(),
                    usernameField.getText(),
                    profileImageView.getImage() != null ?
                            profileImageView.getImage().getUrl() :
                            "/org/to/telegramfinalproject/Avatars/default_profile.png"
            );

            // Show updated MyProfile overlay
            MainController.getInstance().showOverlay(profileOverlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setProfileData(String name, String status, String bio, String userId, Image profileImage) {
        profileStatus.setText(status);
        profileName.setText(name);
        nameField.setText(name);
        bioField.setText(bio != null ? bio : "");
        usernameField.setText(userId);
        profileImageView.setImage(profileImage);
    }
}
