package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;

import java.util.*;
import java.util.stream.IntStream;

public class BlockedUsersController {

    @FXML private Pane overlayBackground;
    @FXML private Button backButton;
    @FXML private Button closeButton;
    @FXML private Label countLabel;
    @FXML private ListView<HBox> blockedList;
    @FXML private VBox blockedCard;

    private final Map<UUID, HBox> rowByUserId = new HashMap<>();

    @FXML
    public void initialize() {
        backButton.setGraphic(makeIcon("/org/to/telegramfinalproject/Icons/back_button_dark.png"));

        loadBlockedUsers();

        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(overlayBackground.getParent()));
        backButton.setOnAction(e -> MainController.getInstance().goBack((StackPane) overlayBackground.getParent()));

        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(blockedCard.getParent()));

        // Theme
        Platform.runLater(() -> {
            if (blockedCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(blockedCard.getScene());
            }
        });
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> updateBackIcon(newVal));
        updateBackIcon(ThemeManager.getInstance().isDarkMode());

        // Smooth scroll feel
        blockedList.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm()
        );
        blockedList.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                ScrollBar vBar = (ScrollBar) blockedList.lookup(".scroll-bar:vertical");
                if (vBar != null) {
                    blockedList.setOnScroll(event -> {
                        double deltaY = event.getDeltaY() * 0.003;
                        double newValue = vBar.getValue() - deltaY;
                        vBar.setValue(Math.max(0, Math.min(newValue, 1)));
                    });
                }
            }
        });
    }

    /* ===================== Networking ===================== */

    private void loadBlockedUsers() {
        new Thread(() -> {
            JSONObject req = new JSONObject().put("action", "get_blocked_users");
            JSONObject resp = ActionHandler.sendWithResponse(req);

            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                System.err.println("get_blocked_users failed: " + (resp != null ? resp.optString("message") : "no response"));
                Platform.runLater(() -> {
                    blockedList.getItems().clear();
                    updateCount();
                });
                return;
            }

            JSONObject data = resp.optJSONObject("data");
            JSONArray arr = data != null ? data.optJSONArray("blocked_users") : null;
            if (arr == null) arr = new JSONArray();

            final JSONArray finalArr = arr;
            Platform.runLater(() -> renderBlockedUsers(finalArr));
        }).start();
    }

    private void unblockUser(UUID targetId) {
        String me = Session.currentUser != null ? Session.currentUser.optString("internal_uuid", "") : "";
        if (me.isBlank() || targetId == null) return;

        new Thread(() -> {
            JSONObject req = new JSONObject()
                    .put("action", "toggle_block")
                    .put("user_id", me)              // UUID
                    .put("target_id", targetId.toString()); // UUID

            JSONObject resp = ActionHandler.sendWithResponse(req);
            boolean ok = resp != null && "success".equalsIgnoreCase(resp.optString("status"));

            Platform.runLater(() -> {
                if (ok) {
                    HBox row = rowByUserId.remove(targetId);
                    if (row != null) blockedList.getItems().remove(row);
                    updateCount();
                } else {
                    String msg = (resp != null ? resp.optString("message", "Failed to unblock.") : "Failed to unblock.");
                    showToast(msg);
                }
            });
        }).start();
    }

    /* ===================== UI build ===================== */

    private void renderBlockedUsers(JSONArray list) {
        blockedList.getItems().clear();
        rowByUserId.clear();

        IntStream.range(0, list.length()).forEach(i -> {
            JSONObject o = list.optJSONObject(i);
            if (o == null) return;

            UUID uid = parseUUID(optS(o, "internal_uuid"));
            if (uid == null) return;

            String name = nz(optS(o, "profile_name", optS(o, "name", "User")));
            String handle = firstNonEmpty(
                    optS(o, "username"),
                    optS(o, "display_id"),
                    optS(o, "user_name"),
                    ""
            );
            String avatarUrl = optS(o, "image_url");

            HBox row = buildRow(uid, name, handle, avatarUrl);
            rowByUserId.put(uid, row);
            blockedList.getItems().add(row);
        });

        updateCount();
    }

    private HBox buildRow(UUID uid, String name, String id, String avatarUrl) {
        // Avatar
        Image img = null;
        if (hasVal(avatarUrl)) {
            try { img = org.to.telegramfinalproject.Client.AvatarLocalResolver.load(avatarUrl); } catch (Exception ignore) {}
        }
        if (img == null) {
            img = new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Avatars/default_user_profile.png"));
        }

        ImageView avatar = new ImageView(img);
        avatar.setFitWidth(60);
        avatar.setFitHeight(60);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);
        avatar.setCache(true);
        avatar.setClip(new Circle(20, 20, 40));

        // Name + ID
        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("blocked-name");
        Label idLabel = new Label(id);
        idLabel.getStyleClass().add("blocked-id");
        info.getChildren().addAll(nameLabel, idLabel);

        // Unblock btn
        Button unblockBtn = new Button("Unblock");
        unblockBtn.getStyleClass().add("unblock-button");
        unblockBtn.setOnAction(e -> unblockUser(uid));

        HBox row = new HBox(12, avatar, info, unblockBtn);
        row.getStyleClass().add("blocked-row");
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
        return row;
    }

    private void updateCount() {
        countLabel.setText(blockedList.getItems().size() + " blocked users");
    }

    /* ===================== Helpers ===================== */

    private void updateBackIcon(boolean darkMode) {
        String iconPath = darkMode
                ? "/org/to/telegramfinalproject/Icons/back_button_light.png"
                : "/org/to/telegramfinalproject/Icons/back_button_dark.png";

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        backButton.setGraphic(icon);
    }

    private ImageView makeIcon(String path) {
        ImageView iv = new ImageView(new Image(getClass().getResourceAsStream(path)));
        iv.setFitWidth(20);
        iv.setFitHeight(20);
        return iv;
    }

    private static String nz(String s){ return s==null? "": s.trim(); }
    private static boolean hasVal(String s){ return s!=null && !s.trim().isEmpty() && !"null".equalsIgnoreCase(s); }
    private static String optS(JSONObject j, String k){ return j!=null ? nz(j.optString(k,"")) : ""; }
    private static String optS(JSONObject j, String k, String fallback){
        String v = optS(j,k);
        return hasVal(v) ? v : fallback;
    }
    private static String firstNonEmpty(String... vals){
        for (String v : vals) if (hasVal(v)) return v;
        return "";
    }
    private static UUID parseUUID(String s){
        try { return UUID.fromString(nz(s)); } catch (Exception e) { return null; }
    }

    private void showToast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        if (a.getDialogPane().getScene() != null) {
            ThemeManager.getInstance().registerScene(a.getDialogPane().getScene());
        }
        a.showAndWait();
    }
}
