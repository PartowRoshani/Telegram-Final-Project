package org.to.telegramfinalproject.UI;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.AvatarLocalResolver;


import java.io.IOException;
import java.net.URL;

public class SidebarMenuController {

    private static final String ICON_PATH = "/org/to/telegramfinalproject/Icons/";




    @FXML private VBox sidebarRoot;
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

    // convenience handle
    private final ThemeManager themeManager = ThemeManager.getInstance();

    @FXML
    public void initialize() {
        // Load default profile image
        Image profile = loadImage("/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        if (profile != null) profileImage.setImage(profile);
        AvatarFX.circleClip(profileImage, 56);


        setupButtonActions();
        setupToggleAction();

        // When the Scene is ready:
        sidebarRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // 1) Register with ThemeManager so this scene auto-updates on theme changes
                themeManager.registerScene(newScene);

                // 2) Make sure we start in LIGHT mode
                themeManager.setDarkMode(false);

                // 3) Sync icons & toggle with current mode
                boolean dark = themeManager.isDarkMode();
                updateIcons(dark);
                syncToggleVisual(dark, /*animate=*/true);

                // Ensure the thumb is correctly positioned after layout pass
                Platform.runLater(() -> syncToggleVisual(themeManager.isDarkMode(), false));
            }
        });

        // If theme is changed from somewhere else (another screen), keep sidebar in sync
        themeManager.darkModeProperty().addListener((o, wasDark, isDark) -> {
            updateIcons(isDark);
            syncToggleVisual(isDark, /*animate=*/true);
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
            boolean newDark = !themeManager.isDarkMode();

            // Update the global theme via ThemeManager
            themeManager.setDarkMode(newDark);
            themeManager.applyThemeToAll();

            // Animate the thumb to its new position (listener will handle icons & final position sync)
            syncToggleVisual(newDark, /*animate=*/true);
        });
    }

    /** Update all button icons to match theme.
     *  darkMode == true  => white icons => use *_light.png
     *  darkMode == false => dark icons  => use *_dark.png
     */
    private void updateIcons(boolean darkMode) {
        String suffix = darkMode ? "_light.png" : "_dark.png";
        myProfileButton.setGraphic(makeIcon(ICON_PATH + "my_profile" + suffix));
        newGroupButton.setGraphic(makeIcon(ICON_PATH + "new_group" + suffix));
        newChannelButton.setGraphic(makeIcon(ICON_PATH + "new_channel" + suffix));
        contactsButton.setGraphic(makeIcon(ICON_PATH + "contacts" + suffix));
        savedMessagesButton.setGraphic(makeIcon(ICON_PATH + "saved_messages" + suffix));
        settingsButton.setGraphic(makeIcon(ICON_PATH + "settings" + suffix));
        telegramFeaturesButton.setGraphic(makeIcon(ICON_PATH + "telegram_features" + suffix));
        telegramQnAButton.setGraphic(makeIcon(ICON_PATH + "telegram_qna" + suffix));

        // Night-mode moon icon
        Image moon = loadImage(ICON_PATH + "night_mode" + suffix);
        if (moon != null) nightModeIcon.setImage(moon);
    }

    /** Keep the toggle’s CSS class and thumb position in sync with the current mode. */
    private void syncToggleVisual(boolean darkMode, boolean animate) {
        // CSS class "on" on the track
        if (darkMode) {
            if (!nightModeToggle.getStyleClass().contains("on")) {
                nightModeToggle.getStyleClass().add("on");
            }
        } else {
            nightModeToggle.getStyleClass().remove("on");
        }

        // Compute target X for the thumb
        double offX = 2;
        double onX  = Math.max(2, nightModeToggle.getWidth() - toggleThumb.getWidth() - 4);
        double targetX = darkMode ? onX : offX;

        if (animate) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), toggleThumb);
            tt.setToX(targetX);
            tt.play();
        } else {
            toggleThumb.setTranslateX(targetX);
        }
    }


    // --- helpers -------------------------------------------------------------

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

    // Sidebar actions
    private void openMyProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/my_profile.fxml"));
            Node profileOverlay = loader.load();

            MyProfileController controller = loader.getController();
            controller.setProfileData("Asal", "online", "Pass me that lovely little gun♡.", "@Whales_suicide", null);

            // Show the overlay on top of mainRoot
            MainController.getInstance().showOverlay(profileOverlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Example button actions
    private void createNewGroup() { System.out.println("Creating New Group..."); }
    private void createNewChannel() { System.out.println("Creating New Channel..."); }

    private void openContacts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/contacts.fxml"));
            Node contactsOverlay = loader.load();

            // Show overlay on top of everything
            MainController.getInstance().showOverlay(contactsOverlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openSavedMessages() { System.out.println("Opening Saved Messages..."); }
    private void openSettings() { System.out.println("Opening Settings..."); }
    private void openTelegramFeatures() { System.out.println("Opening Telegram Features..."); }
    private void openTelegramQnA() { System.out.println("Opening Telegram Q&A..."); }



    public void setUserFromSession(JSONObject user) {
        if (user == null) return;

        // نام نمایشی (اول profile_name بعد username)
        String displayName = user.optString("profile_name",
                user.optString("username", ""));
        usernameLabel.setText(displayName);

        // عکس پروفایل: هم URL اینترنتی هم مسیر ریسورس را پشتیبانی کن
        String img = user.optString("image_url", "");
        Image pic = tryLoadImage(img);
        if (pic == null) {
            pic = loadImage("/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        }
        if (pic != null) profileImage.setImage(pic);
    }

    // کمک‌کننده: هم URL و هم ریسورس کلاس‌پث را امتحان می‌کند
//    private Image tryLoadImage(String src) {
//        if (src == null || src.isBlank()) return null;
//        try {
//            // اگر ریسورس داخل پروژه است (با / شروع شود یا در resources موجود باشد)
//            var res = getClass().getResource(src);
//            if (res != null) return new Image(res.toExternalForm(), true);
//            // در غیر این صورت فرض کن URL است (http/https/file)
//            return new Image(src, true);
//        } catch (Exception ignored) {
//            return null;
//        }
//    }


    private Image tryLoadImage(String src) {
        if (src == null || src.isBlank()) return null;
        try {
            // اگر مسیر داخل resources است
            var res = getClass().getResource(src);
            if (res != null) return new Image(res.toExternalForm(), true);

            // اگر مسیر نسبی سرور (مثل /avatars/...) است
            String fileUri = AvatarLocalResolver.resolve(src);
            if (fileUri != null) return new Image(fileUri, true);

            // در غیر این صورت، فرض URL کامل
            return new Image(src, true);
        } catch (Exception ignored) {
            return null;
        }
    }


}
