package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.layout.Pane;

import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NewChannelController {

    @FXML private VBox newChannelCard;
    @FXML private Pane overlayBackground;
    @FXML private TextField channelNameField;
    @FXML private Label channelNameLabel;
    @FXML private TextArea channelDescField;
    @FXML private Label channelDescLabel;
    @FXML private Button cameraButton;
    @FXML private ImageView cameraIcon;
    @FXML private Button cancelButton;
    @FXML private Button createButton;
    @FXML private StackPane overlayRoot;
    @FXML private Label descCounter;
    @FXML private Label channelIdLabel;
    @FXML private TextField channelIdField;

    private File channelImageFile;

    @FXML
    public void initialize() {
        cameraIcon.setImage(new Image(
                getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/camera.png")
        ));

        cameraButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Channel Picture");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            File file = chooser.showOpenDialog(cameraButton.getScene().getWindow());
            if (file != null) {
                channelImageFile = file;
                cameraIcon.setImage(new Image(file.toURI().toString()));
            }
        });

        cancelButton.setOnAction(e -> MainController.getInstance().closeOverlay(overlayRoot));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(overlayRoot));

        createButton.setOnAction(e -> onCreateChannel());

        // محدودیت توضیح
        final int MAX_LENGTH = 255;
        channelDescField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> {
            if (channelDescField.getText().length() >= MAX_LENGTH) e.consume();
        });
        channelDescField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > MAX_LENGTH) {
                channelDescField.setText(newText.substring(0, MAX_LENGTH));
                channelDescField.positionCaret(MAX_LENGTH);
            }
            int current = channelDescField.getText().length();
            descCounter.setText(current + " / " + MAX_LENGTH);
            descCounter.setStyle(current == MAX_LENGTH ? "-fx-text-fill: red;" : "");
        });
        descCounter.setText("0 / 255");

        // پاک کردن استایل خطا هنگام تایپ
        channelNameField.textProperty().addListener((obs, ov, nv) -> {
            if (!nv.trim().isEmpty()) {
                channelNameField.getStyleClass().remove("error");
                channelNameLabel.getStyleClass().remove("error");
            }
        });
        channelIdField.textProperty().addListener((obs, ov, nv) -> {
            if (!nv.trim().isEmpty()) {
                channelIdField.getStyleClass().remove("error");
                channelIdLabel.getStyleClass().remove("error");
            }
        });

        Platform.runLater(() -> channelNameField.requestFocus());

        Platform.runLater(() -> {
            if (newChannelCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(newChannelCard.getScene());
            }
        });
    }

    private void onCreateChannel() {
        String name        = nz(channelNameField.getText()).trim();
        String dispId      = nz(channelIdField.getText()).trim();
        String description = nz(channelDescField.getText()).trim();

        boolean ok = true;
        if (name.isEmpty())   { channelNameField.getStyleClass().add("error"); channelNameLabel.getStyleClass().add("error"); ok = false; }
        if (dispId.isEmpty()) { channelIdField.getStyleClass().add("error"); channelIdLabel.getStyleClass().add("error"); ok = false; }
        if (!ok) return;

        final String me = Session.getUserUUID();
        if (me == null || me.isBlank()) {
            showToast("Cannot create channel: current user UUID missing.");
            return;
        }

        // در مرحله ساخت، image_url را خالی بفرست؛ بعداً آپلود می‌کنیم
        JSONObject req = new JSONObject()
                .put("action", "create_channel")
                .put("channel_id", dispId)
                .put("channel_name", name)
                .put("user_id", me)
                .put("image_url", (Object) null)
                .put("description", description);

        createButton.setDisable(true);

        new Thread(() -> {
            try {
                JSONObject resp = ActionHandler.sendWithResponse(req);
                if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                    Platform.runLater(() -> {
                        createButton.setDisable(false);
                        showToast("Create failed: " + (resp == null ? "no response" : resp.optString("message","")));
                    });
                    return;
                }

                JSONObject data = resp.optJSONObject("data");
                if (data == null) {
                    Platform.runLater(() -> {
                        createButton.setDisable(false);
                        showToast("Create failed: empty data.");
                    });
                    return;
                }

                UUID internalId     = UUID.fromString(data.optString("internal_id"));
                String returnedName = data.optString("name", name);
                String returnedDisp = data.optString("id", dispId);
                String returnedImg  = data.optString("image_url", "");

                // ✅ اگر کاربر عکس انتخاب کرده بود: الان آپلود کن (target_type=channel)
                if (channelImageFile != null) {
                    ActionHandler.instance.uploadAvatarFor("channel", internalId, channelImageFile);
                    if (ActionHandler.instance.wasSuccess()) {
                        String url = ActionHandler.instance.getLastMessage(); // display_url
                        if (url != null && !url.isBlank()) {
                            returnedImg = url;
                        }
                    } else {
                        // اختیاری: پیام خطا را نشان بده
                        System.out.println("Channel avatar upload failed: " + ActionHandler.instance.getLastMessage());
                    }
                }

                String finalImg = returnedImg;
                Platform.runLater(() -> {
                    // 1) کانال را به سایدبار اضافه و انتخاب کن
                    ChatEntry entry = ChatEntry.fromServer(
                            internalId, "channel", returnedName, returnedDisp, finalImg,
                            /*isOwner*/ true, /*isAdmin*/ true
                    );
                    MainController.getInstance().addChatAndSelect(entry);

                    // 2) باز کردن Overlay افزودن سابسکرایبر و بستن این Overlay
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                                "/org/to/telegramfinalproject/Fxml/add_subscriber.fxml"));
                        StackPane addSubsOverlay = loader.load();
                        AddSubscriberController controller = loader.getController();
                        controller.setChannelInfo(internalId, returnedName, returnedDisp, channelImageFile, description);

                        MainController.getInstance().showOverlay(addSubsOverlay);
                        MainController.getInstance().closeOverlay(overlayRoot);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        showToast("Failed to open Add Subscribers.");
                    }

                    createButton.setDisable(false);
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    createButton.setDisable(false);
                    showToast("Create failed: " + ex.getMessage());
                });
            }
        }).start();
    }


    private void showToast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.initOwner(overlayRoot.getScene().getWindow());
        a.show();
    }

    private static String nz(String s){ return s==null? "": s; }
}
