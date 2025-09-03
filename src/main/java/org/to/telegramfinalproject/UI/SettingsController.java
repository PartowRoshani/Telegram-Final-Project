package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;

public class SettingsController {

    @FXML private VBox settingsCard;
    @FXML private ImageView profileImage;
    @FXML private Label profileName;
    @FXML private Label profileId;
    @FXML private MenuButton menuButton;
    @FXML private MenuItem editProfileItem;
    @FXML private MenuItem logoutItem;
    @FXML private Button myAccountButton;
    @FXML private Button privacyButton;
    @FXML private Button faqButton;
    @FXML private Button featuresButton;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;

    private static final String ICON_PATH = "/org/to/telegramfinalproject/Icons/";

    @FXML
    public void initialize() {
        editProfileItem.setOnAction(e -> openEditProfile());
        logoutItem.setOnAction(e -> System.out.println("Log Out clicked"));

        // Example data – load from DB or user session later
        profileName.setText("Asal");
        profileId.setText("@Whales_suicide");
        profileImage.setImage(new Image(
                getClass().getResourceAsStream("/org/to/telegramfinalproject/Avatars/default_user_profile.png")
        ));

        // Make it circular
        Circle clip = new Circle(48, 28, 38); // centerX, centerY, radius
        profileImage.setClip(clip);

        // Buttons
        myAccountButton.setOnAction(e -> openEditProfile());

        privacyButton.setOnAction(e -> System.out.println("Privacy and Security"));
        faqButton.setOnAction(e -> System.out.println("Telegram Q&A"));
        featuresButton.setOnAction(e -> System.out.println("Telegram Features"));

        // Close overlay on background click
        overlayBackground.setOnMouseClicked(e ->
                MainController.getInstance().closeOverlay(settingsCard.getParent()));

        closeButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(settingsCard.getParent())
        );

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (settingsCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(settingsCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateEditIcon(newVal);
            updateMoreIcon(newVal);
            updateButtonsIcons(newVal);
        });

        // Set initial state
        updateEditIcon(ThemeManager.getInstance().isDarkMode());
        updateMoreIcon(ThemeManager.getInstance().isDarkMode());
        updateButtonsIcons(ThemeManager.getInstance().isDarkMode());
    }

    private void openEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/edit_profile.fxml"));
            Node editOverlay = loader.load();

            EditProfileController controller = loader.getController();
            controller.setProfileData(
                    profileName.getText(),
                    "online",
                    "Pass me that lovely little gun♡.",
                    "@Whales_suicide",
                    profileImage.getImage()
            );

            MainController.getInstance().showOverlay(editOverlay);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void updateEditIcon(boolean darkMode) {
        String editPath = darkMode
                ? "/org/to/telegramfinalproject/Icons/edit_light.png"
                : "/org/to/telegramfinalproject/Icons/edit_dark.png";

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(editPath)));
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        editProfileItem.setGraphic(icon);
    }

    private void updateMoreIcon(boolean darkMode) {
        String morePath = darkMode
                ? "/org/to/telegramfinalproject/Icons/more_light.png"
                : "/org/to/telegramfinalproject/Icons/more_dark.png";

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(morePath)));
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        menuButton.setGraphic(icon);
    }

    private void updateButtonsIcons(boolean darkMode) {
        String suffix = darkMode ? "_light.png" : "_dark.png";
        myAccountButton.setGraphic(makeIcon(ICON_PATH + "my_profile" + suffix));
        privacyButton.setGraphic(makeIcon(ICON_PATH + "lock" + suffix));
        faqButton.setGraphic(makeIcon(ICON_PATH + "telegram_features" + suffix));
        featuresButton.setGraphic(makeIcon(ICON_PATH + "telegram_qna" + suffix));
    }

    // --- helpers -------------------------------------------------------------

    private ImageView makeIcon(String path) {
        ImageView iv = new ImageView();
        Image img = loadImage(path);
        if (img != null) {
            iv.setImage(img);
            iv.setFitWidth(22);
            iv.setFitHeight(22);
            iv.setPreserveRatio(true);
        }
        return iv;
    }

    private Image loadImage(String path) {
        URL res = getClass().getResource(path);
        if (res == null) {
            System.err.println("Resource not found: " + path);
            return null;
        }
        return new Image(res.toExternalForm());
    }
}
