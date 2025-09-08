package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.AvatarLocalResolver;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class ChannelInfoController {

    @FXML private VBox channelCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;

    @FXML private ImageView channelAvatar;
    @FXML private Label channelName;
    @FXML private Label subscriberCount;
    @FXML private Label channelDescription;

    @FXML private Button infoMoreButton;
    @FXML private ContextMenu infoMoreMenu;
    @FXML private MenuItem addMembersBtn;
    @FXML private MenuItem manageChannelBtn;
    @FXML private MenuItem deleteChannelBtn;
    @FXML private ImageView moreIcon;
    @FXML private ImageView manageChannelIcon;
    @FXML private ImageView addMemberIcon;

    @FXML private VBox subscribersList;
    @FXML private Label subscribersHeader;
    @FXML private Button addSubscriberButton;
    @FXML private ImageView subscribersIcon;
    @FXML private ScrollPane subscribersScroll;

    private UUID channelId;
    private String myRole;

    private static final String ICON_PATH = "/org/to/telegramfinalproject/Icons/";

    @FXML
    private void initialize() {
        closeButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(channelCard.getParent()));
        overlayBackground.setOnMouseClicked(e ->
                MainController.getInstance().closeOverlay(channelCard.getParent()));

        infoMoreButton.setOnAction(e -> {
            if (infoMoreMenu != null) infoMoreMenu.show(infoMoreButton, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        if (manageChannelBtn != null) {
            manageChannelBtn.setOnAction(e -> openManageChannel());
        }
        deleteChannelBtn.setOnAction(e -> handleDeleteChannel());

        Platform.runLater(() -> {
            if (channelCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(channelCard.getScene());
            }
        });

        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateIcons(newVal);
        });

        updateIcons(ThemeManager.getInstance().isDarkMode());

        // Smooth scroll feel
        subscribersScroll.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm()
        );
        subscribersScroll.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                ScrollBar vBar = (ScrollBar) subscribersScroll.lookup(".scroll-bar:vertical");
                if (vBar != null) {
                    subscribersScroll.setOnScroll(event -> {
                        double deltaY = event.getDeltaY() * 0.003;
                        double newValue = vBar.getValue() - deltaY;
                        vBar.setValue(Math.max(0, Math.min(newValue, 1)));
                    });
                }
            }
        });

        if (addSubscriberButton != null) {
            addSubscriberButton.setOnAction(e -> openAddSubscriberScene());
        }
        if (addMembersBtn != null) {
            addMembersBtn.setOnAction(e -> openAddSubscriberScene());
        }
    }

    public void setChannelDataFromJson(ChatEntry entry, JSONObject data) {
        this.channelId = entry.getId();

        channelName.setText(data.optString("channel_name", entry.getName()));
        subscriberCount.setText(data.optInt("subscriber_count", 0) + " subscribers");
        channelDescription.setText(data.optString("description", ""));

        // --- Role-based UI ---
        myRole = data.optString("my_role", "subscriber").toLowerCase();

        deleteChannelBtn.setVisible("owner".equals(myRole));

        boolean canAdd = "owner".equals(myRole) || "admin".equals(myRole);
        addSubscriberButton.setVisible(canAdd);
        addSubscriberButton.setManaged(canAdd);
        if (addMembersBtn != null) {
            addMembersBtn.setVisible(canAdd);
        }

        boolean showMore = "owner".equals(myRole) || "admin".equals(myRole);
        infoMoreButton.setVisible(showMore);
        infoMoreButton.setManaged(showMore);

        // --- Avatar ---
        String imgUrl = data.optString("image_url", "");
        if (!imgUrl.isBlank()) {
            try {
                Image img = AvatarLocalResolver.load(imgUrl);
                if (img != null) channelAvatar.setImage(img);
            } catch (Exception ignore) {}
        } else {
            channelAvatar.setImage(new Image(
                    getClass().getResourceAsStream("/org/to/telegramfinalproject/Avatars/default_channel_profile.png")
            ));
        }

        // --- Subscribers list ---
        subscribersList.getChildren().clear();
        var arr = data.optJSONArray("subscribers");
        if (arr != null) {
            subscribersHeader.setText(arr.length() + " SUBSCRIBERS");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                addSubscriberRow(s);
            }
        }
    }

    private void addSubscriberRow(JSONObject s) {
        HBox row = new HBox(10);
        row.getStyleClass().add("member-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);
        AvatarFX.circleClip(avatar, 36);

        String imgUrl = s.optString("image_url", "");
        if (!imgUrl.isBlank()) {
            Image img = AvatarLocalResolver.load(imgUrl);
            if (img != null) avatar.setImage(img);
        } else {
            avatar.setImage(new Image(getClass().getResourceAsStream(
                    "/org/to/telegramfinalproject/Avatars/default_user_profile.png"
            )));
        }

        // Name + status
        VBox nameBox = new VBox(2);
        Label name = new Label(s.optString("profile_name", "Unknown"));
        name.getStyleClass().add("member-name");

        String status;
        if (s.optBoolean("is_online", false)) {
            status = "online";
        } else {
            status = ChatPageController.getInstance().userStatusText(
                    false,
                    s.optString("last_seen", null)
            );
        }
        Label statusLbl = new Label(status);
        statusLbl.getStyleClass().add("member-status");

        nameBox.getChildren().addAll(name, statusLbl);

        // Role
        Label roleLbl = new Label();
        String roleStr = s.optString("role", "");
        if (!roleStr.isBlank()) {
            roleLbl.setText(roleStr.toLowerCase());
            roleLbl.getStyleClass().add("member-role");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(avatar, nameBox, spacer, roleLbl);
        subscribersList.getChildren().add(row);
    }

    private void openManageChannel() {
        new Thread(() -> {
            try {
                JSONObject req = new JSONObject()
                        .put("action", "view_channel")
                        .put("channel_id", channelId.toString()); // ✅ matches server case

                JSONObject resp = ActionHandler.sendWithResponse(req);

                if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                    Platform.runLater(() -> MainController.getInstance().showAlert(
                            "Error",
                            resp != null ? resp.optString("message") : "Server not responding.",
                            Alert.AlertType.ERROR
                    ));
                    return;
                }

                JSONObject data = resp.optJSONObject("data");
                if (data == null) {
                    Platform.runLater(() -> MainController.getInstance().showAlert(
                            "Error",
                            "Malformed server response.",
                            Alert.AlertType.ERROR
                    ));
                    return;
                }

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                                "/org/to/telegramfinalproject/Fxml/manage_channel.fxml"));
                        Node overlay = loader.load();

                        ManageChannelController controller = loader.getController();
                        controller.setChannelData(data); // ✅ pass JSON to controller

                        MainController.getInstance().showOverlay(overlay);
                    } catch (IOException e) {
                        e.printStackTrace();
                        MainController.getInstance().showAlert(
                                "Error",
                                "Could not load Manage Channel scene.",
                                Alert.AlertType.ERROR
                        );
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainController.getInstance().showAlert(
                        "Error",
                        "Error while fetching channel info: " + e.getMessage(),
                        Alert.AlertType.ERROR
                ));
            }
        }).start();
    }

    private void openAddSubscriberScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/add_subscriber.fxml")); // point to new fxml
            Node overlay = loader.load();

            AddSubscriberController controller = loader.getController();
            controller.setChannelInfo(channelId, channelName.getText(), "", null, "");
            // passing UUID + name, other fields optional

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error",
                    "Could not load Add Subscribers scene.",
                    Alert.AlertType.ERROR
            );
        }
    }
    private void handleDeleteChannel() {
        ChatEntry entry = Session.currentChatEntry;

        if (entry == null || !"channel".equalsIgnoreCase(entry.getType())) {
            alert(Alert.AlertType.INFORMATION, "This action is only available for channels.");
            return;
        }

        if (!confirm("Delete Channel",
                "Are you sure you want to DELETE this channel?\nThis action cannot be undone.")) {
            return;
        }

        JSONObject req = new JSONObject()
                .put("action", "delete_channel")
                .put("channel_id", entry.getId().toString());

        JSONObject res = ActionHandler.sendWithResponse(req);

        if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
            alert(Alert.AlertType.INFORMATION, "✅ Channel deleted successfully.");
            MainController.getInstance().refreshChatListUI();
            AppRouter.showMain();
        } else {
            String msg = (res != null) ? res.optString("message", "Failed to delete channel.") : "null response";
            alert(Alert.AlertType.ERROR, "❌ " + msg);
        }
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        return a.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent();
    }

    private void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).show();
    }

    private void updateIcons(boolean dark) {
        String suffix = dark ? "_light.png" : "_dark.png";

        moreIcon.setImage(loadImage(ICON_PATH + "more" + suffix));
        subscribersIcon.setImage(loadImage(ICON_PATH + "channel_subscriber" + suffix));
        addSubscriberButton.setGraphic(makeIcon(ICON_PATH + "add_member" + suffix));
        manageChannelIcon.setImage(loadImage(ICON_PATH + "manage" + suffix));
        addMemberIcon.setImage(loadImage(ICON_PATH + "add_member" + suffix));
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

    private Image loadImage(String path) {
        URL res = getClass().getResource(path);
        if (res == null) return null;
        return new Image(res.toExternalForm());
    }
}
