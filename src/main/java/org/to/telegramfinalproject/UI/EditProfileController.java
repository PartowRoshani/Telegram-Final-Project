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
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;
import org.json.JSONObject;

import java.io.File;

public class EditProfileController {

    private File pickedAvatarFile;

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

    // EditProfileController.java
    private SettingsController parentSettings;
    public void setParentSettings(SettingsController sc) {
        this.parentSettings = sc;
    }


    @FXML
    private void initialize() {
        backButton.setGraphic(new ImageView(
                new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/back_button_dark.png"))
        ));

        // close on background click
//        overlayBackground.setOnMouseClicked(e -> {
//            saveChangesAndClose();
//            closeEdit();
//        });
//
//        closeButton.setOnAction(e -> {
//            saveChangesAndClose();
//            closeEdit();
//        });
//
//        backButton.setOnAction(e -> {
//            saveChangesAndClose();
//            MainController.getInstance().goBack(overlayBackground);
//        });

        overlayBackground.setOnMouseClicked(e -> {
            saveChangesAndClose();
            closeEdit(); // ← همینه
        });

        closeButton.setOnAction(e -> {
            saveChangesAndClose();
            closeEdit(); // ← همینه
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
            pickedAvatarFile = file; // ← ذخیره کن برای ارسال هنگام خروج
            profileImageView.setImage(new Image(file.toURI().toString())); // پیش‌نمایش
        }
    }

    private void closeEdit() {
//        MainController.getInstance().goBack(overlayBackground);
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

//        if (pickedAvatarFile != null) {
//            ActionHandler.instance.uploadAvatarFor("user", null, pickedAvatarFile);
//
//            if (ActionHandler.instance.wasSuccess()) {
//                String url = ActionHandler.instance.getLastMessage();
//                if (url != null && !url.isBlank()) {
//                    currentUser.put("image_url", url);
//                }
//            } else {
//                anyError = true;
//                showAlert(
//                        "Avatar upload failed",
//                        ActionHandler.instance.getLastMessage() == null ? "Unknown error" : ActionHandler.instance.getLastMessage(),
//                        Alert.AlertType.ERROR
//                );
//            }
//        }


        if (pickedAvatarFile != null) {
            ActionHandler.instance.uploadAvatarFor("user", null, pickedAvatarFile);

            if (ActionHandler.instance.wasSuccess()) {
                String url = ActionHandler.instance.getLastMessage();
                if (url != null && !url.isBlank()) {
                    // قبلاً اینجا مستقیم bust می‌زدیم → مشکل ایجاد می‌کرد
                    // currentUser.put("image_url", url);

                    // ✔️ نسخه‌ی امن:
                    String extracted = extractImageUrl(url); // از helper پایین
                    if (isLikelyImageUrl(extracted)) {
                        String finalUrl = addCacheBusterIfHttp(extracted); // فقط برای http/https
                        Session.currentUser.put("image_url", finalUrl);
                    } else {
                        // اگر سرور چیز عجیبی مثل "Welcome Farid" برگردوند، نادیده بگیر
                        System.out.println("Upload avatar returned a non-image value: " + url);
                    }
                }
            } else {
                anyError = true;
                showAlert(
                        "Avatar upload failed",
                        ActionHandler.instance.getLastMessage() == null ? "Unknown error" : ActionHandler.instance.getLastMessage(),
                        Alert.AlertType.ERROR
                );
            }
        }


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
            // 1) اگر صفحه‌ی MyProfile باز است
            var mp = MyProfileController.getInstance();
            if (mp != null) {
                mp.setProfileData(
                        currentUser.optString("profile_name"),
                        "online",
                        currentUser.optString("bio"),
                        currentUser.optString("user_id"),
                        currentUser.optString("image_url", "/org/to/telegramfinalproject/Avatars/default_user_profile.png")
                );
            }

            // 2) خودِ کارت Settings را فوراً از Session ریفرش کن
            if (parentSettings != null) {
                parentSettings.populateFromSession();
            }

            // 3) سایدبار بالایی (اسم/آواتار خود کاربر) اگر داری
            MainController.getInstance().refreshSidebarUserFromSession();

            // 4) بستن اورلی ادیت
//            MainController.getInstance().goBack(overlayBackground);
            MainController.getInstance().closeOverlay(editCard.getParent());

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



    private boolean isLikelyImageUrl(String s) {
        if (s == null || s.isBlank()) return false;
        // URL کامل، مسیر نسبی سرور، یا file: و پسوند تصویر
        return s.startsWith("http://") || s.startsWith("https://")
                || s.startsWith("file:")
                || s.startsWith("/")
                || s.matches("(?i).+\\.(png|jpe?g|gif|webp)$");
    }

    private String addCacheBusterIfHttp(String url) {
        if (url == null) return null;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url + (url.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis();
        }
        // برای مسیر لوکال/نسبی اصلاً ?v اضافه نکن (روی ویندوز مشکل می‌دهد)
        return url;
    }

    /** اگر سرور به‌جای URL، JSON یا متن دیگری داد، اینجا URL را دربیار. */
    private String extractImageUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // اگر برگشتی JSON است (بعضی سرویس‌ها data.display_url برمی‌گردانند)
        if (s.startsWith("{") && s.endsWith("}")) {
            try {
                org.json.JSONObject j = new org.json.JSONObject(s);
                // هر کلیدی که سرویس‌ات استفاده می‌کند را امتحان کن
                if (j.has("display_url")) return j.optString("display_url", null);
                if (j.has("url"))         return j.optString("url", null);
                if (j.has("data")) {
                    var d = j.optJSONObject("data");
                    if (d != null) {
                        if (d.has("display_url")) return d.optString("display_url", null);
                        if (d.has("url"))         return d.optString("url", null);
                    }
                }
            } catch (Exception ignore) {}
        }
        return s; // در غیر این صورت همان raw
    }

}
