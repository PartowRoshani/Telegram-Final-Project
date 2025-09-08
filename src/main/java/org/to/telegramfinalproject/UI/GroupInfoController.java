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

public class GroupInfoController {

    @FXML private VBox groupCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;

    @FXML private ImageView groupImage;
    @FXML private Label groupName;
    @FXML private Label memberCount;

    @FXML private Button infoMoreButton;
    @FXML private ContextMenu infoMoreMenu;
    @FXML private MenuItem addMemberItem;
    @FXML private MenuItem manageGroupItem;
    @FXML private MenuItem deleteGroupItem;
    @FXML private ImageView moreIcon;

    @FXML private VBox membersList;
    @FXML private Label membersHeader;
    @FXML private Button addMemberButton;
    @FXML private ImageView membersIcon;
    @FXML private ScrollPane membersScroll;

    private String groupId; // the UUID of this group

    private static final String ICON_PATH = "/org/to/telegramfinalproject/Icons/";

    @FXML
    private void initialize() {
        closeButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(groupCard.getParent()));
        overlayBackground.setOnMouseClicked(e ->
                MainController.getInstance().closeOverlay(groupCard.getParent()));

        infoMoreButton.setOnAction(e -> {
            if (infoMoreMenu != null) infoMoreMenu.show(infoMoreButton, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        if (manageGroupItem != null) {
            manageGroupItem.setOnAction(e -> openManageGroupScene());
        }

        deleteGroupItem.setOnAction(e -> handleDeleteGroup());

        Platform.runLater(() -> {
            if (groupCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(groupCard.getScene());
            }
        });

        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateIcons(newVal);
        });

        updateIcons(ThemeManager.getInstance().isDarkMode());

        // Smooth scroll feel
        membersScroll.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm()
        );
        membersScroll.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                ScrollBar vBar = (ScrollBar) membersScroll.lookup(".scroll-bar:vertical");
                if (vBar != null) {
                    membersScroll.setOnScroll(event -> {
                        double deltaY = event.getDeltaY() * 0.003;
                        double newValue = vBar.getValue() - deltaY;
                        vBar.setValue(Math.max(0, Math.min(newValue, 1)));
                    });
                }
            }
        });

        if (addMemberButton != null) {
            addMemberButton.setOnAction(e -> openAddMemberScene());
        }
        if (addMemberItem != null) {
            addMemberItem.setOnAction(e -> openAddMemberScene());
        }
    }

    public void setGroupDataFromJson(ChatEntry entry, JSONObject data) {
        this.groupId = data.optString("internal_uuid", entry.getId().toString());

        groupName.setText(data.optString("group_name", entry.getName()));
        memberCount.setText(data.optInt("member_count", 0) + " members");

        // --- Role-based UI ---
        String myRole = data.optString("my_role", "member").toLowerCase();

        // Delete group → only owner
        deleteGroupItem.setVisible("owner".equals(myRole));

        // Add member (button + menu item) → owner or admin
        boolean canAdd = "owner".equals(myRole) || "admin".equals(myRole);
        addMemberButton.setVisible(canAdd);
        addMemberButton.setManaged(canAdd);
        if (addMemberItem != null) {
            addMemberItem.setVisible(canAdd);
        }

        // More menu (3-dot) → hide entirely for plain members
        boolean showMore = "owner".equals(myRole) || "admin".equals(myRole);
        infoMoreButton.setVisible(showMore);
        infoMoreButton.setManaged(showMore);

        // --- Group picture ---
        String imgUrl = data.optString("image_url", "");
        if (!imgUrl.isBlank()) {
            try {
                Image img = AvatarLocalResolver.load(imgUrl);
                if (img != null) groupImage.setImage(img);
            } catch (Exception ignore) {}
        } else {
            groupImage.setImage(
                    new Image(getClass().getResourceAsStream(
                            "/org/to/telegramfinalproject/Avatars/default_group_profile.png"))
            );
        }

        // --- Members list ---
        membersList.getChildren().clear();
        var arr = data.optJSONArray("members");
        if (arr != null) {
            membersHeader.setText(arr.length() + " MEMBERS");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject m = arr.getJSONObject(i);
                addMemberRow(m);
            }
        }
    }

    private void addMemberRow(JSONObject m) {
        HBox row = new HBox(10);
        row.getStyleClass().add("member-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // === Avatar ===
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);
        AvatarFX.circleClip(avatar, 36);

        String imgUrl = m.optString("image_url", "");
        if (!imgUrl.isBlank()) {
            Image img = AvatarLocalResolver.load(imgUrl);
            if (img != null) avatar.setImage(img);
        } else {
            avatar.setImage(new Image(getClass().getResourceAsStream(
                    "/org/to/telegramfinalproject/Avatars/default_user_profile.png"
            )));
        }

        // === Name + Status ===
        VBox nameBox = new VBox(2);
        Label name = new Label(m.optString("profile_name", "Unknown"));
        name.getStyleClass().add("member-name");

        // Status = online / last seen recently
        String status;
        if (m.optBoolean("is_online", false)) {
            status = "online";
        } else {
            status = ChatPageController.getInstance().userStatusText(
                    false,
                    m.optString("last_seen", null)
            );
        }
        Label statusLbl = new Label(status);
        statusLbl.getStyleClass().add("member-status");

        nameBox.getChildren().addAll(name, statusLbl);

        // === Role (owner/admin/member) ===
        Label role = new Label();
        String roleStr = m.optString("role", "");
        if (!roleStr.isBlank()) {
            role.setText(roleStr.toLowerCase());
            role.getStyleClass().add("member-role");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(avatar, nameBox, spacer, role);
        membersList.getChildren().add(row);
    }

    private void openManageGroupScene() {
        new Thread(() -> {
            try {
                JSONObject req = new JSONObject()
                        .put("action", "view_group")
                        .put("group_id", groupId)
                        .put("viewer_id", Session.getUserUUID());

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
                                "/org/to/telegramfinalproject/Fxml/manage_group.fxml"));
                        Node overlay = loader.load();

                        ManageGroupController controller = loader.getController();
                        controller.setGroupData(data); // pass the full JSON

                        MainController.getInstance().showOverlay(overlay);
                    } catch (IOException e) {
                        e.printStackTrace();
                        MainController.getInstance().showAlert(
                                "Error",
                                "Could not load Manage Group scene.",
                                Alert.AlertType.ERROR
                        );
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainController.getInstance().showAlert(
                        "Error",
                        "Error while fetching group info: " + e.getMessage(),
                        Alert.AlertType.ERROR
                ));
            }
        }).start();
    }

    private void openAddMemberScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/add_member.fxml"));
            Node overlay = loader.load();

            AddMembersController controller = loader.getController();
            controller.setGroupForAdd(UUID.fromString(groupId), String.valueOf(groupName));

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error",
                    "Could not load Add Member scene.",
                    Alert.AlertType.ERROR
            );
        }
    }

    private void handleDeleteGroup() {
        ChatEntry entry = Session.currentChatEntry;

//        if (entry == null || !"group".equalsIgnoreCase(entry.getType())) {
//            alert(Alert.AlertType.INFORMATION, "This action is only available for groups.");
//            return;
//        }

        if (!confirm("Delete Group",
                "Are you sure you want to DELETE this group?\nThis action cannot be undone.")) {
            return;
        }

        JSONObject req = new JSONObject()
                .put("action", "delete_group")
                .put("group_id", entry.getId().toString());

        JSONObject res = ActionHandler.sendWithResponse(req);

        if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
            alert(Alert.AlertType.INFORMATION, "✅ Group deleted successfully.");
            MainController.getInstance().refreshChatListUI();
            AppRouter.showMain();
        } else {
            String msg = (res != null) ? res.optString("message", "Failed to delete group.") : "null response";
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
        membersIcon.setImage(loadImage(ICON_PATH + "group" + suffix));
        addMemberButton.setGraphic(makeIcon(ICON_PATH + "add_member" + suffix));
        membersIcon.setImage(loadImage(ICON_PATH + "group_member" + suffix));

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
        if (res == null) return null;
        return new Image(res.toExternalForm());
    }
}
