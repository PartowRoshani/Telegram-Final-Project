package org.to.telegramfinalproject.UI;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.AvatarLocalResolver;
import org.to.telegramfinalproject.Models.ChatEntry;


import java.io.IOException;
import java.net.URL;
import java.util.UUID;

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
            // 1. Ask server for profile info
            JSONObject request = new JSONObject().put("action", "get_user_profile");
            JSONObject response = ActionHandler.sendWithResponse(request);

            if (response == null || !"success".equals(response.optString("status"))) {
                showAlert("Error", "Failed to load profile", Alert.AlertType.ERROR);
                return;
            }

            JSONObject profile = response.optJSONObject("data");
            if (profile == null) {
                showAlert("Error", "Malformed profile data", Alert.AlertType.ERROR);
                return;
            }

            // Extract fields
            String profileName = profile.optString("profile_name", "Unknown");
            String bio = profile.optString("bio", "");
            String userId = profile.optString("user_id", "");
            String imageUrl = profile.optString("profile_picture_url", null);

            // 2. Load overlay FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/my_profile.fxml"));
            Node profileOverlay = loader.load();

            // 3. Pass real data to controller
            MyProfileController controller = loader.getController();
            controller.setProfileData(profileName, "online", bio, userId, imageUrl);

            // 4. Show overlay
            MainController.getInstance().showOverlay(profileOverlay);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error",  "Error opening profile", Alert.AlertType.ERROR);
        }
    }

    private void createNewGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/new_group.fxml"));
            Node groupOverlay = loader.load();

            // Show overlay (like MyProfile, Contacts, etc.)
            MainController.getInstance().showOverlay(groupOverlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNewChannel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/new_channel.fxml"));
            Node channelOverlay = loader.load();

            // Show overlay (like MyProfile, Contacts, etc.)
            MainController.getInstance().showOverlay(channelOverlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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



    private void openSavedMessages() {
        try {
            // 1) درخواست به سرور
            JSONObject req = new JSONObject().put("action", "get_or_create_saved_messages");
            JSONObject res = ActionHandler.sendWithResponse(req);

            if (res == null || !"success".equals(res.optString("status"))) {
                String msg = (res != null) ? res.optString("message", "Unknown error") : "No response";
                showAlert("Saved Messages", "Failed: " + msg, Alert.AlertType.ERROR);
                return;
            }

            JSONObject data = res.optJSONObject("data");
            if (data == null) {
                showAlert("Saved Messages", "Malformed response.", Alert.AlertType.ERROR);
                return;
            }

            // 2) داده‌ها
            String chatIdStr = data.optString("chat_id", null);
            if (chatIdStr == null || chatIdStr.isBlank()) {
                showAlert("Saved Messages", "Missing chat_id.", Alert.AlertType.ERROR);
                return;
            }
            UUID chatId = UUID.fromString(chatIdStr);
            String name = data.optString("name", "Saved Messages");
            String chatType = data.optString("chat_type", "private");
            boolean isSaved = data.optBoolean("is_saved_messages", true);

            // اگر آیکن اختصاصی برای Saved داری، این مسیر رو بده؛ وگرنه null بذار:
            String savedIcon = ICON_PATH + (themeManager.isDarkMode() ? "saved_messages_light.png" : "saved_messages_dark.png");
            // اگر چنین آیکنی نداری، می‌تونی null بدی تا همان image_url خالی بماند:
            // String savedIcon = null;

            // 3) درج/به‌روزرسانی در Session و بیاور اول لیست
            ChatEntry entry = org.to.telegramfinalproject.Client.Session
                    .upsertSavedMessages(chatId, name, chatType, savedIcon);

            // 4) وضعیت‌های فعلی سشن برای ناوبری
            org.to.telegramfinalproject.Client.Session.currentChatId = chatId.toString();
            org.to.telegramfinalproject.Client.Session.currentChatType = chatType;
            org.to.telegramfinalproject.Client.Session.currentChatEntry = entry;
            org.to.telegramfinalproject.Client.Session.backToChatList = false;

            // 5) ریفِرش ظاهری لیست چت‌ها (اگر متدی برای این داری، صدا بزن)
            try {
                MainController.getInstance().refreshChatListUI(); // اگر متد دیگری داری عوضش کن
            } catch (Throwable ignore) { }

            // 6) باز کردن چت
            // --- مسیر اول: اگر متدی داری که با ChatEntry باز می‌کند:
            boolean opened = false;
            try {
                MainController.getInstance().openChat(entry);
                opened = true;
            } catch (Throwable t) {
                // مسیر دوم: اگر با id/type باز می‌کنی، یا اول info می‌گیری:
                try {
                    ActionHandler.requestChatInfo(String.valueOf(chatId), chatType);
                    // اگر متد آشکار برای باز کردن با id داری، اینجا صدا بزن:
                    // MainController.getInstance().openChatById(chatId, chatType);
                    opened = true;
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }

            if (!opened) {
                // اگر هیچ‌کدام نداشت، حداقل پیغام بده که چت ساخته و آماده است:
                System.out.println("Saved Messages ready. Open manually with currentChatId/currentChatType.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Saved Messages", "Error: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }


    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/settings.fxml"));
            Node settingsOverlay = loader.load();

            // Show overlay on top of everything
            MainController.getInstance().showOverlay(settingsOverlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
