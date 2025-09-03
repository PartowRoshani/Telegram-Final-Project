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

import java.io.IOException;
import java.net.URL;

public class PrivacySecurityController {

    @FXML private VBox privacyCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;
    @FXML private Button backButton;
    @FXML private Button blockedUsersButton;
    @FXML private Button changeCredentialsButton;
    @FXML private ImageView blockIcon;
    @FXML private ImageView usernameIcon;

    private static final String ICON_PATH = "/org/to/telegramfinalproject/Icons/";

    @FXML
    public void initialize() {
        // Set back button icon
        ImageView backIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/back_button_dark.png"))
        );
        backIcon.setFitWidth(18);
        backIcon.setFitHeight(18);
        backButton.setGraphic(backIcon);

        changeCredentialsButton.setOnAction(e -> openPasswordCheckOverlay());

        // Blocked users management
        blockedUsersButton.setOnAction(e -> {
            // TODO: open Blocked Users scene
            System.out.println("Open blocked users management");
        });

        // Close
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(privacyCard.getParent()));

        // Back
        backButton.setOnAction(e -> MainController.getInstance().goBack(privacyCard));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(privacyCard.getParent()));

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (privacyCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(privacyCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateIcons(newVal);
        });

        // Set initial state
        updateIcons(ThemeManager.getInstance().isDarkMode());
    }

    private void openPasswordCheckOverlay() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/check_password.fxml"));
            Node overlay = loader.load();

            // Show new overlay
            MainController.getInstance().showOverlay(overlay);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void updateIcons(boolean darkMode) {
        String suffix = darkMode ? "_light.png" : "_dark.png";

        backButton.setGraphic(makeIcon(ICON_PATH + "back_button" + suffix));
        blockIcon.setImage(loadImage(ICON_PATH + "hand" + suffix));
        usernameIcon.setImage(loadImage(ICON_PATH + "username" + suffix));
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
