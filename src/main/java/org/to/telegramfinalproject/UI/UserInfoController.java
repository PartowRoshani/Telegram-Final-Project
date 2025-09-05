package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.AvatarLocalResolver;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.net.URL;
import java.util.Optional;
import java.util.UUID;

public class UserInfoController {

    @FXML private VBox profileCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;

    @FXML private ImageView profileImage;
    @FXML private Label profileName;
    @FXML private Label profileStatus;
    @FXML private HBox bioBlock;
    @FXML private Label userBio;
    @FXML private HBox usernameBlock;
    @FXML private Label userId;

    @FXML private Button infoMoreButton;
    @FXML private ContextMenu infoMoreMenu;
    @FXML private MenuItem deleteChatItem;
    @FXML private MenuItem blockItem;
    @FXML private MenuItem unblockItem;
    @FXML private ImageView moreIcon;
    @FXML private ImageView bioIcon;
    @FXML private ImageView usernameIcon;
    @FXML private Button deleteChatButton;


    private String otherUserId; // internal_uuid of the other user

    private static final String ICON_PATH = "/org/to/telegramfinalproject/Icons/";

    @FXML
    private void initialize() {
        closeButton.setOnAction(e ->
                MainController.getInstance().closeOverlay(profileCard.getParent()));
        overlayBackground.setOnMouseClicked(e ->
                MainController.getInstance().closeOverlay(profileCard.getParent()));

        // Show menu manually
        infoMoreButton.setOnAction(e -> {
            if (infoMoreMenu != null) {
                infoMoreMenu.show(infoMoreButton, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });

        // Hide menu when clicking outside
        Platform.runLater(() -> {
            if (infoMoreButton.getScene() != null) {
                infoMoreButton.getScene().addEventFilter(
                        javafx.scene.input.MouseEvent.MOUSE_PRESSED, ev -> {
                            if (infoMoreMenu.isShowing()
                                    && !infoMoreButton.localToScreen(infoMoreButton.getBoundsInLocal())
                                    .contains(ev.getScreenX(), ev.getScreenY())) {
                                infoMoreMenu.hide();
                            }
                        }
                );
            }
        });

        deleteChatItem.setOnAction(e -> onDeleteChatClicked());
        blockItem.setOnAction(e -> handleBlock());
        unblockItem.setOnAction(e -> handleUnblock());

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (profileCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(profileCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateIcons(newVal);
        });

        // Set initial state
        updateIcons(ThemeManager.getInstance().isDarkMode());
    }

    /** Backend JSON → UI */
    public void setProfileDataFromJson(ChatEntry entry, JSONObject data) {
        // --- Profile name ---
        String name = data.optString("profile_name", entry.getName());
        profileName.setText(name);

        // --- Status ---
        String status = ChatPageController.getInstance().userStatusText(
                data.optBoolean("is_online", false),
                data.optString("last_seen", null)
        );
        profileStatus.setText(status);

        // --- Bio ---
        String bio = data.optString("bio", "");
        if (bio != null && !bio.isBlank()) {
            userBio.setText(bio);
            bioBlock.setVisible(true);
            bioBlock.setManaged(true);
        } else {
            bioBlock.setVisible(false);
            bioBlock.setManaged(false);
        }

        // --- Username ---
        String username = data.optString("user_id", "");
        if (username != null && !username.isBlank()) {
            userId.setText("@" + username);
            usernameBlock.setVisible(true);
            usernameBlock.setManaged(true);
        } else {
            usernameBlock.setVisible(false);
            usernameBlock.setManaged(false);
        }

        // --- Profile picture ---
        String imgUrl = data.optString("image_url", "");
        if (imgUrl != null && !imgUrl.isBlank()) {
            try {
                Image img = AvatarLocalResolver.load(imgUrl);
                if (img != null) profileImage.setImage(img);
            } catch (Exception ignore) {}
        } else {
            profileImage.setImage(
                    new Image(getClass().getResourceAsStream(
                            "/org/to/telegramfinalproject/Avatars/default_user_profile.png"))
            );
        }

        // --- Other user ID (needed for block/delete) ---
        this.otherUserId = data.optString("other_user_id", entry.getId().toString());

        // --- Block/Unblock ---
        boolean blocked = data.optBoolean("blocked", false)
                || data.optBoolean("is_blocked", false)
                || data.optBoolean("blocked_by_me", false);
        updateBlockMenu(blocked);
    }

    private void handleDeleteChat() {
        JSONObject req = new JSONObject()
                .put("action", "delete_chat")
                .put("receiver_id", otherUserId)
                .put("viewer_id", Session.getUserUUID());

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            MainController.getInstance().closeOverlay(profileCard.getParent());
        }
    }



    @FXML
    private void onDeleteChatClicked() {
//        if (!"private".equalsIgnoreCase(Session.currentChatType)) {
//            new Alert(Alert.AlertType.INFORMATION, "This action is available only for private chats.").showAndWait();
//            return;
//        }

        UUID chatId = Session.currentChatEntry != null
                ? Session.currentChatEntry.getId()
                : (Session.currentChatId != null ? UUID.fromString(Session.currentChatId) : null);

        if (chatId == null) {
            new Alert(Alert.AlertType.ERROR, "Invalid chat id.").showAndWait();
            return;
        }

        ButtonType oneSide  = new ButtonType("Delete one-sided", ButtonBar.ButtonData.LEFT);
        ButtonType bothSide = new ButtonType("Delete both-sided", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel   = ButtonType.CANCEL;

        Alert dlg = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Choose how you want to delete this private chat:",
                oneSide, bothSide, cancel
        );
        dlg.setHeaderText("Delete Private Chat");

        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isEmpty() || res.get() == cancel) return;

        boolean both = (res.get() == bothSide);
        performDeletePrivateChat(chatId, both);
    }


    private void performDeletePrivateChat(UUID chatId, boolean both) {
        // نال‌سیف: هر کدوم هست disable کن
        if (deleteChatButton != null) deleteChatButton.setDisable(true);
        if (deleteChatItem   != null) deleteChatItem.setDisable(true);
        if (infoMoreMenu != null && infoMoreMenu.isShowing()) infoMoreMenu.hide();
        if (infoMoreButton != null) infoMoreButton.setDisable(true);

        new Thread(() -> {
            JSONObject req = new JSONObject()
                    .put("action", "delete_private_chat")
                    .put("chat_id", chatId.toString())
                    .put("both", both);

            JSONObject resp = ActionHandler.sendWithResponse(req);

            Platform.runLater(() -> {
                // re-enable نال‌سیف
                if (deleteChatButton != null) deleteChatButton.setDisable(false);
                if (deleteChatItem   != null) deleteChatItem.setDisable(false);
                if (infoMoreButton   != null) infoMoreButton.setDisable(false);

                if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
                    removeChatFromSessionAndGoBack(chatId);
                    MainController.getInstance().closeOverlay(profileCard.getParent());

//                    new Alert(Alert.AlertType.INFORMATION,
//                            "Chat deleted " + (both ? "for both sides." : "only for you.")
//                    ).showAndWait();
                } else {
                    String msg = (resp != null ? resp.optString("message", "Unknown error")
                            : "No response from server.");
                    new Alert(Alert.AlertType.ERROR, "Failed to delete chat: " + msg).showAndWait();
                }
            });
        }).start();
    }


    private void removeChatFromSessionAndGoBack(UUID chatId) {
//        if (Session.chatList != null)      Session.chatList.removeIf(e -> chatId.equals(e.getId()));
//        if (Session.activeChats != null)   Session.activeChats.removeIf(e -> chatId.equals(e.getId()));
//        if (Session.archivedChats != null) Session.archivedChats.removeIf(e -> chatId.equals(e.getId()));

        if (Session.currentChatId != null && Session.currentChatId.equals(chatId.toString())) {
            Session.currentChatId = null;
            Session.currentChatType = null;
            Session.currentChatEntry = null;
            Session.inChatMenu = false;

            MainController.getInstance().closeOverlay(profileCard.getParent());

            AppRouter.showMain();
            return;
        }

        try {
            MainController.getInstance().refreshChatListUI();
        } catch (Exception ignore) {
            AppRouter.showMain();
        }
    }


    private void handleBlock() {
        JSONObject req = new JSONObject()
                .put("action", "block_user")
                .put("target_id", otherUserId)
                .put("viewer_id", Session.getUserUUID());

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            updateBlockMenu(true);
        }
    }

    private void handleUnblock() {
        JSONObject req = new JSONObject()
                .put("action", "unblock_user")
                .put("target_id", otherUserId)
                .put("viewer_id", Session.getUserUUID());

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            updateBlockMenu(false);
        }
    }

    private void updateBlockMenu(boolean isBlocked) {
        blockItem.setVisible(!isBlocked);
        unblockItem.setVisible(isBlocked);
    }

    private void updateIcons(boolean darkMode) {
        String suffix = darkMode ? "_light.png" : "_dark.png";

        moreIcon.setImage(loadImage(ICON_PATH + "more" + suffix));
        bioIcon.setImage(loadImage(ICON_PATH + "bio" + suffix));
        usernameIcon.setImage(loadImage(ICON_PATH + "username" + suffix));
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
}
