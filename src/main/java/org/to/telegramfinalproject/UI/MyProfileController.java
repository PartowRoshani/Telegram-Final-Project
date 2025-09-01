package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.Objects;

public class MyProfileController {

    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;
    @FXML private Button editButton;
    @FXML private ImageView profileImage;
    @FXML private Label profileName;
    @FXML private Label profileStatus;
    @FXML private Label userBio;
    @FXML private Label userId;
    @FXML private VBox profileCard;
    @FXML private VBox bioBlock;

    private final ThemeManager themeManager = ThemeManager.getInstance();

    @FXML
    private void initialize() {
        overlayBackground.setOnMouseClicked(e -> closeProfile());
        closeButton.setOnAction(e -> closeProfile());

        // Round profile picture
        profileImage.setClip(new Circle(40, 40, 40));

        // Default avatar
        profileImage.setImage(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream(
                        "/org/to/telegramfinalproject/Avatars/default_user_profile.png"))
        ));

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (profileCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(profileCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateEditIcon(newVal);
        });

        // Set initial state
        updateEditIcon(ThemeManager.getInstance().isDarkMode());

        // === Open edit profile ===
        editButton.setOnAction(e -> openEditProfile());
    }

    private void closeProfile() {
        MainController.getInstance().closeOverlay(profileCard.getParent());
    }

    // Populate user data from DB or active session
    public void setProfileData(String name, String status, String bio, String userId, String imageUrl) {
        profileName.setText(name);
        profileStatus.setText(status);

        if (bio == null || bio.isBlank()) {
            bioBlock.setVisible(false);
            bioBlock.setManaged(false);
        } else {
            userBio.setText(bio);
            userBio.setVisible(true);
            userBio.setManaged(true);
        }

        this.userId.setText(userId);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            profileImage.setImage(new Image(imageUrl));
        }
    }

    // === Theme-specific updates ===
    private void updateEditIcon(boolean darkMode) {
        String iconPath = darkMode
                ? "/org/to/telegramfinalproject/Icons/edit_light.png"
                : "/org/to/telegramfinalproject/Icons/edit_dark.png";

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        editButton.setGraphic(icon);
    }

    private void openEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/edit_profile.fxml"));
            Node editOverlay = loader.load();

            EditProfileController controller = loader.getController();
            // Pass current data (so fields are pre-filled)
            controller.setProfileData(
                    profileName.getText(),
                    profileStatus.getText(),
                    userBio.isVisible() ? userBio.getText() : null,
                    userId.getText(),
                    profileImage.getImage()
            );

            // Show new overlay
            MainController.getInstance().showOverlay(editOverlay);

            // Close the current My Profile overlay
            closeProfile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
