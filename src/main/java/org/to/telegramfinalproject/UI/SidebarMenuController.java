package org.to.telegramfinalproject.UI;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;

public class SidebarMenuController {

    private static final String CSS_PATH = "/org/to/telegramfinalproject/CSS/";
    private static final String ICON_PATH = "/org/to/telegramfinalproject/Icons/";

    @FXML private VBox sidebarRoot; // fx:id must match FXML
    @FXML private ImageView profileImage;
    @FXML private Label usernameLabel;

    @FXML private Button myProfileButton;
    @FXML private Button newGroupButton;
    @FXML private Button newChannelButton;
    @FXML private Button contactsButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button settingsButton;
    @FXML private Button telegramFeaturesButton;
    @FXML private Button telegramQnAButton;

    // Custom Telegram-style toggle
    @FXML private HBox nightModeToggle;
    @FXML private Region toggleThumb;
    @FXML private ImageView nightModeIcon;

    private boolean nightModeOn = false; // start light by default

    @FXML
    public void initialize() {
        // Load default profile picture (if present)
        Image profile = loadImage("/org/to/telegramfinalproject/Images/profile.png");
        if (profile != null) profileImage.setImage(profile);

        // Set up handlers and defer theme application until the scene is available
        setupButtonActions();
        setupToggleAction();

        // When the scene is set, apply the initial theme (light) and initialize icons/positions
        sidebarRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Apply light theme at startup
                updateTheme(false);

                // Position the thumb after layout pass (to use actual widths)
                Platform.runLater(() -> {
                    double initialX = nightModeOn
                            ? nightModeToggle.getWidth() - toggleThumb.getWidth() - 4
                            : 2;
                    toggleThumb.setTranslateX(initialX);
                });
            }
        });
    }

    private void setupButtonActions() {
        myProfileButton.setOnAction(e -> openMyProfile());
        newGroupButton.setOnAction(e -> createNewGroup());
        newChannelButton.setOnAction(e -> createNewChannel());
        contactsButton.setOnAction(e -> openContacts());
        savedMessagesButton.setOnAction(e -> openSavedMessages());
        settingsButton.setOnAction(e -> openSettings());
        telegramFeaturesButton.setOnAction(e -> openTelegramFeatures());
        telegramQnAButton.setOnAction(e -> openTelegramQnA());
    }

    private void setupToggleAction() {
        nightModeToggle.setOnMouseClicked(e -> {
            nightModeOn = !nightModeOn;
            updateTheme(nightModeOn);

            // animate thumb
            double targetX = nightModeOn
                    ? nightModeToggle.getWidth() - toggleThumb.getWidth() - 4
                    : 2;
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), toggleThumb);
            tt.setToX(targetX);
            tt.play();
        });
    }

    /**
     * Apply theme and swap icons. darkMode == true => apply dark theme (dark background),
     * and use light (white) icons. darkMode == false => apply light theme and use dark icons.
     */
    private void updateTheme(boolean darkMode) {
        Scene scene = sidebarRoot.getScene();
        if (scene == null) return; // safety

        // clear & apply the correct stylesheet
        scene.getStylesheets().clear();
        String cssFile = darkMode ? "dark_theme.css" : "light_theme.css";
        URL cssUrl = getClass().getResource(CSS_PATH + cssFile);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("CSS not found: " + CSS_PATH + cssFile);
        }

        // Keep toggle's "on" class in sync
        if (darkMode) {
            if (!nightModeToggle.getStyleClass().contains("on"))
                nightModeToggle.getStyleClass().add("on");
        } else {
            nightModeToggle.getStyleClass().remove("on");
        }

        // Icon file suffix mapping:
        // - For darkMode == true (dark background) we want white icons => use "_light.png"
        // - For darkMode == false (light background) we want dark icons => use "_dark.png"
        String iconSuffix = darkMode ? "_light.png" : "_dark.png";

        setIcons(iconSuffix);

        // night mode icon itself (moon)
        Image moon = loadImage(ICON_PATH + "night_mode" + iconSuffix);
        if (moon != null) nightModeIcon.setImage(moon);
    }

    private void setIcons(String suffix) {
        myProfileButton.setGraphic(makeIcon(ICON_PATH + "my_profile" + suffix));
        newGroupButton.setGraphic(makeIcon(ICON_PATH + "new_group" + suffix));
        newChannelButton.setGraphic(makeIcon(ICON_PATH + "new_channel" + suffix));
        contactsButton.setGraphic(makeIcon(ICON_PATH + "contacts" + suffix));
        savedMessagesButton.setGraphic(makeIcon(ICON_PATH + "saved_messages" + suffix));
        settingsButton.setGraphic(makeIcon(ICON_PATH + "settings" + suffix));
        telegramFeaturesButton.setGraphic(makeIcon(ICON_PATH + "telegram_features" + suffix));
        telegramQnAButton.setGraphic(makeIcon(ICON_PATH + "telegram_qna" + suffix)); // prefer safe filename
    }

    private Image loadImage(String path) {
        URL res = getClass().getResource(path);
        if (res == null) {
            System.err.println("Resource not found: " + path);
            return null;
        }
        return new Image(res.toExternalForm());
    }

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

    // Example button actions
    private void openMyProfile() { System.out.println("Opening My Profile..."); }
    private void createNewGroup() { System.out.println("Creating New Group..."); }
    private void createNewChannel() { System.out.println("Creating New Channel..."); }
    private void openContacts() { System.out.println("Opening Contacts..."); }
    private void openSavedMessages() { System.out.println("Opening Saved Messages..."); }
    private void openSettings() { System.out.println("Opening Settings..."); }
    private void openTelegramFeatures() { System.out.println("Opening Telegram Features..."); }
    private void openTelegramQnA() { System.out.println("Opening Telegram Q&A..."); }
}
