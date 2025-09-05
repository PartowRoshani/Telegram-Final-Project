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
import javafx.scene.shape.Circle;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;

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

    private Image cachedAvatar;
    private String cachedName = "";
    private String cachedUsername = "";
    private String cachedStatus = "";
    private String cachedBio = "";

    private static String nz(String s){ return s==null? "": s.trim(); }
    private static boolean hasVal(String s){ return s!=null && !s.trim().isEmpty() && !"null".equalsIgnoreCase(s); }

    private static SettingsController instance;
    public static SettingsController getInstance() { return instance; }
    @FXML
    public void initialize() {
        instance = this;

        editProfileItem.setOnAction(e -> openEditProfile());
        logoutItem.setOnAction(e -> onLogoutClicked());

        populateFromSession();

        // Make it circular
        Circle clip = new Circle(48, 28, 38); // centerX, centerY, radius
        profileImage.setClip(clip);

        // Buttons
        myAccountButton.setOnAction(e -> openEditProfile());

        privacyButton.setOnAction(e ->
        {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/org/to/telegramfinalproject/Fxml/privacy_security.fxml"));
                Node privacyOverlay = loader.load();

                MainController.getInstance().showOverlay(privacyOverlay);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

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
                    cachedName,
                    cachedStatus,
                    cachedBio,
                    cachedUsername,
                    cachedAvatar
            );
            controller.setParentSettings(this);
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




    void populateFromSession() {
        var u = org.to.telegramfinalproject.Client.Session.currentUser;
        if (u == null) return;

        // نام
        String name = nz(u.optString("profile_name",
                u.optString("name",
                        u.optString("first_name",""))));

        // یوزرنیم/آی‌دی نمایشی
        String handle = nz(u.optString("username",
                u.optString("display_id",
                        u.optString("user_name",""))));
        // وضعیت
        String status = u.optBoolean("online", false) ? "online" : "online";
        String lastSeen = nz(u.optString("last_seen", ""));
        if (!u.optBoolean("online", false) && hasVal(lastSeen)) status = "online"; // ساده

        // بیو (اگر داری)
        String bio = nz(u.optString("bio",""));

        // آواتار
        Image avatar = null;
        String imageUrl = nz(u.optString("image_url",""));
        if (hasVal(imageUrl)) {
            try { avatar = org.to.telegramfinalproject.Client.AvatarLocalResolver.load(imageUrl); }
            catch (Exception ignore) {}
        }
        if (avatar == null) {
            avatar = new Image(getClass().getResourceAsStream(
                    "/org/to/telegramfinalproject/Avatars/default_user_profile.png"));
        }

        // کش محلی برای استفاده در صفحه‌ی ویرایش
        cachedName = hasVal(name) ? name : "User";
        cachedUsername = hasVal(handle) ? handle : "";
        cachedStatus = status;
        cachedBio = bio;
        cachedAvatar = avatar;

        // ست کردن در UI
        profileName.setText(cachedName);
        profileId.setText(cachedUsername);
        profileImage.setImage(cachedAvatar);

        // گرد کردن تصویر (با توجه به اندازه‌ی واقعی)
        Circle clip = new Circle(38, 38, 38); // centerX, centerY, radius
        profileImage.setClip(clip);
    }

    // جایی مثل SidebarMenuController یا MainController
    private void onLogoutClicked() {
        new Thread(() -> {
            JSONObject req = new JSONObject().put("action","logout"); // user_id لازم نیست
             req.put("user_id",Session.getUserUUID()); // user_id لازم نیست

            JSONObject res = ActionHandler.sendWithResponse(req);

            Platform.runLater(() -> {
                if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
                    try {
                        // قطع ارتباط/لیسنر (اگر متد داری)
                        // TelegramClient.disconnect();
                    } catch (Exception ignore) {}

                    // پاک‌سازی امن سشن (ترجیحاً clear به‌جای null)
                    Session.currentUser = null;
                    Session.chatList = null;
                    AppRouter.showIntro();   // intro.fxml
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            "Logout not successful: " + (res != null ? res.optString("message") : "No response")
                    ).showAndWait();
                }
            });
        }).start();
    }

}
