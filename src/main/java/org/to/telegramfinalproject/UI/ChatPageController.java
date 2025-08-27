package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

public class ChatPageController {

    // ===== messages area =====
    @FXML
    private VBox messageContainer;
    @FXML
    private ScrollPane messageScrollPane;

    // ===== input area =====
    @FXML
    private TextArea messageInput;
    @FXML
    private Button sendButton;

    @FXML
    private Button attachmentButton;
    @FXML
    private ImageView attachmentIcon;   // <ImageView> inside the attachment button

    // ===== header =====
    @FXML
    private ImageView userAvatar;       // 36x36 in the FXML
    @FXML
    private Label chatTitle;            // contact/group title
    @FXML
    private Label chatStatus;           // last seen / online

    @FXML
    private Button searchInChatButton;  // magnifier button
    @FXML
    private ImageView searchIcon;

    @FXML
    private Button moreButton;          // 3-dots button
    @FXML
    private ImageView moreIcon;
    @FXML
    private ContextMenu moreMenu;
    @FXML
    private MenuItem viewProfileItem;
    @FXML
    private MenuItem deleteChatItem;

    // ===== send icon =====
    @FXML
    private ImageView sendIcon;

    // ===== state =====
    private String chatName;
    private final ThemeManager themeManager = ThemeManager.getInstance();

    // Where your icons live
    private static final String ICON_BASE = "/org/to/telegramfinalproject/Icons/";
    private ChatEntry currentChat;
    private UUID me;

    private void initCurrentUserId() {
        try {
            // از سشنی که سمت کلاینت داری:
            String meStr = org.to.telegramfinalproject.Client.Session
                    .currentUser.getString("internal_uuid");
            me = UUID.fromString(meStr);
        } catch (Exception ignore) {
            me = null; // اگر به هر دلیلی نبود، خروجی‌ها رو ورودی فرض نکن
        }
    }


    @FXML
    public void initialize() {

        initCurrentUserId();

        // Send button
        if (sendButton != null) {
            sendButton.setOnAction(e -> sendMessage());
        }

        // ENTER = send, SHIFT+ENTER = newline
        messageInput.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (e.isShiftDown()) {
                    // let newline happen
                } else {
                    e.consume(); // block newline
                    sendMessage();
                }
            }
        });

        // Enable/disable send button by content (ignore spaces/newlines)
        messageInput.textProperty().addListener((obs, oldV, newV) -> {
            boolean hasRealText = newV != null && !newV.trim().isEmpty();
            sendButton.setDisable(!hasRealText);

            // toggle style class for color state
            var sc = sendButton.getStyleClass();
            sc.removeAll("send-empty", "send-ready");
            sc.add(hasRealText ? "send-ready" : "send-empty");
        });

        // Auto-resize input box like Telegram
        messageInput.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                var textNode = messageInput.lookup(".text");
                if (textNode != null) {
                    double textHeight = textNode.getBoundsInLocal().getHeight();
                    double padding = 20; // top + bottom padding
                    double newHeight = textHeight + padding;

                    if (newHeight < 40) newHeight = 40;   // min (1 row)
                    if (newHeight > 120) newHeight = 120; // max (~5 rows)

                    messageInput.setPrefHeight(newHeight);
                }
            });
        });

        // Start in "empty" state
        sendButton.getStyleClass().add("send-empty");

        // attach file
        if (attachmentButton != null) {
            attachmentButton.setOnAction(e -> openFileChooser());
        }

        // Hook "More" button → show menu under it
        if (moreButton != null) {
            moreButton.setOnAction(e -> {
                if (!moreMenu.isShowing()) {
                    moreMenu.show(moreButton, Side.BOTTOM, 0, 0);
                } else {
                    moreMenu.hide();
                }
            });
        }

        // Menu item actions
        viewProfileItem.setOnAction(e -> {
            System.out.println("Viewing profile of " + chatName);
            // TODO: open profile UI
        });

        deleteChatItem.setOnAction(e -> {
            System.out.println("Deleting chat with " + chatName);
            // TODO: delete logic
        });

        // Initial icon sync once the Scene is ready (stylesheet applied)
        Platform.runLater(this::syncIconsWithTheme);

        // Auto-Scroll
        messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                var timeline = new javafx.animation.Timeline();
                var kv = new javafx.animation.KeyValue(
                        messageScrollPane.vvalueProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH
                );
                var kf = new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), kv);
                timeline.getKeyFrames().add(kf);
                timeline.play();
            });
        });

        // Smooth scroll feel
        messageScrollPane.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        messageScrollPane.setPannable(true);
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setFitToHeight(false);
        messageScrollPane.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.00003; // smaller = smoother
            messageScrollPane.setVvalue(messageScrollPane.getVvalue() - deltaY);
        });

        // React to theme changes everywhere
        themeManager.darkModeProperty().addListener((o, oldVal, isDark) -> syncIconsWithTheme());
    }

    @FXML
    private void openSearchPanel() {
        MainController.getInstance().showSearchPanel();
    }

    /**
     * Called by main controller when opening a chat.
     */
    public void setChat(String chatName, String avatarPath) {
        this.chatName = chatName;

        // Header text (if present)
        if (chatTitle != null) chatTitle.setText(chatName);
        if (chatStatus != null) chatStatus.setText("last seen recently"); // or live status

        // Load avatar if provided
        if (userAvatar != null && avatarPath != null) {
            try {
                Image avatarImg = new Image(getClass().getResourceAsStream(avatarPath));
                userAvatar.setImage(avatarImg);
                Circle clip = new Circle(18, 18, 18); // x, y, radius
                userAvatar.setClip(clip);
            } catch (Exception e) {
                System.err.println("Could not load avatar: " + avatarPath);
            }
        }

        addSystemMessage("Chat with " + chatName + " opened.");
        Platform.runLater(() -> messageInput.requestFocus());
    }

    // ----- actions -----

    private void sendMessage() {
        String text = messageInput.getText() == null ? "" : messageInput.getText().trim();
        if (!text.isEmpty()) {
            addMessage("You", text);
            messageInput.clear();
            // TODO: send to server
        }
    }

    private void openFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select a file to send");
        File file = fc.showOpenDialog(attachmentButton.getScene().getWindow());
        if (file != null) {
            System.out.println("Selected file: " + file.getAbsolutePath());
            // TODO: actually send file
            addSystemMessage("Attached file: " + file.getName());
        }
    }

    // ----- UI helpers -----

    /**
     * Add a normal message bubble (very simple for now).
     */
    public void addMessage(String sender, String content) {
        Label msg = new Label(sender + ": " + content);
        msg.setWrapText(true);

        boolean dark = themeManager.isDarkMode();
        String bubbleColor = dark ? "#20405a" : "#4fa8f0";
        String textColor = dark ? "#e8f1f8" : "#0f141a";
        msg.setStyle(
                "-fx-background-color: " + bubbleColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-padding: 6 10; -fx-background-radius: 10;"
        );

        messageContainer.getChildren().add(msg);
    }

    private void addSystemMessage(String content) {
        Label sys = new Label(content);
        sys.setStyle("-fx-text-fill: gray; -fx-font-size: 11;");
        messageContainer.getChildren().add(sys);
        messageScrollPane.layout();
        messageScrollPane.setVvalue(1.0);
    }

    /**
     * Update all header/footer icons according to current theme.
     */
    private void syncIconsWithTheme() {
        boolean dark = themeManager.isDarkMode();
        // We use “_light” icons on dark backgrounds, and “_dark” on light backgrounds.
        String suffix = dark ? "_light.png" : "_dark.png";

        // attachment
        if (attachmentIcon != null) {
            attachmentIcon.setImage(loadIcon("attachment" + suffix));
        }
        // send
        if (sendIcon != null) {
            sendIcon.setImage(loadIcon("send_cyan2.png"));
        }
        // header icons
        if (searchIcon != null) {
            searchIcon.setImage(loadIcon("search" + suffix));
        }
        if (moreIcon != null) {
            moreIcon.setImage(loadIcon("more" + suffix));
        }

        // header text tint (if you’re not fully relying on CSS)
        if (chatTitle != null) chatTitle.setStyle(dark ? "-fx-text-fill:#e8f1f8;" : "-fx-text-fill:#0f141a;");
        if (chatStatus != null) chatStatus.setStyle(dark ? "-fx-text-fill:#8ea1b2;" : "-fx-text-fill:#7e8a97;");

        // View profile icon in more button
        ((ImageView) viewProfileItem.getGraphic())
                .setImage(loadIcon("view_profile" + suffix));
    }

    private Image loadIcon(String filename) {
        var url = getClass().getResource(ICON_BASE + filename);
        if (url == null) {
            System.err.println("Icon not found: " + ICON_BASE + filename);
            return null;
        }
        return new Image(url.toExternalForm());
    }

    public void showChat(ChatEntry entry) {
        this.currentChat = entry;

        // Header
        chatTitle.setText(entry.getName());
        chatStatus.setText(""); // اگر last seen داری اینجا بگذار
        if (entry.getImageUrl() != null && !entry.getImageUrl().isEmpty()) {
            try {
                userAvatar.setImage(new Image(entry.getImageUrl())); // یا لود از ریسورس خودت
                userAvatar.setClip(new Circle(18, 18, 18));
            } catch (Exception ignored) {
            }
        }

        messageContainer.getChildren().clear();
        loadMessages(entry);

        // مارک به‌عنوان خوانده
        markAsRead(entry);

        // فوکوس روی ورودی
        Platform.runLater(() -> messageInput.requestFocus());
    }

    private void loadMessages(ChatEntry entry) {
        JSONObject req = new JSONObject();
        req.put("action", "get_messages");
        req.put("receiver_id", String.valueOf(entry.getId()));
        req.put("receiver_type", entry.getType());
        req.put("limit", 50);

        new Thread(() -> {
            JSONObject resp;
            try {
                resp = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            if (resp == null) return;

            // بدون opt* :
            String status = "";
            try {
                status = resp.getString("status");
            } catch (Exception ignore) {
            }
            if (!"success".equals(status)) return;

            JSONObject data = null;
            try {
                data = resp.getJSONObject("data");
            } catch (Exception ignore) {
            }
            if (data == null) return;

            JSONArray arr = null;
            try {
                arr = data.getJSONArray("messages");
            } catch (Exception ignore) {
            }
            if (arr == null) return;

            JSONArray finalArr = arr;
            Platform.runLater(() -> renderMessages(finalArr));
        }).start();
    }


    private void renderMessages(JSONArray arr) {
        messageContainer.getChildren().clear();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject m = arr.getJSONObject(i);
            String senderId = m.optString("sender_id", "");
            String type = m.optString("message_type", "TEXT");
            String content = m.optString("content", "");

            boolean outgoing = false;
            if (me != null && senderId != null && !senderId.isEmpty()) {
                try {
                    outgoing = me.equals(UUID.fromString(senderId));
                } catch (Exception ignore) {
                }
            }

            String text;
            switch (type.toUpperCase()) {
                case "TEXT":
                    text = content;
                    break;
                case "IMAGE":
                    text = "[Image]";
                    break;
                case "AUDIO":
                    text = "[Audio]";
                    break;
                case "VIDEO":
                    text = "[Video]";
                    break;
                case "FILE":
                    text = "[File]";
                    break;
                default:
                    text = "[Message]";
            }
            addBubble(outgoing, text);
        }
        messageScrollPane.layout();
        messageScrollPane.setVvalue(1.0);
    }


    private void markAsRead(ChatEntry entry) {
        JSONObject readReq = new JSONObject();
        readReq.put("action", "mark_as_read");
        readReq.put("receiver_id", entry.getId().toString()); // ⛳️ internal_id
        readReq.put("receiver_type", entry.getType());
        ActionHandler.sendWithResponse(readReq);
    }

    private void addBubble(boolean outgoing, String content) {
        Label msg = new Label(content);
        msg.setWrapText(true);

        boolean dark = themeManager.isDarkMode();
        String mine = dark ? "#2b7cff" : "#d8ecff";
        String theirs = dark ? "#2c333a" : "#ffffff";
        String bg = outgoing ? mine : theirs;

        msg.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-padding:8 12;" +
                        "-fx-background-radius:12;" +
                        "-fx-max-width: 520;"
        );
        msg.setMinHeight(Region.USE_PREF_SIZE);

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(msg);
        row.setFillHeight(true);
        row.setSpacing(6);
        row.setAlignment(outgoing
                ? javafx.geometry.Pos.CENTER_RIGHT
                : javafx.geometry.Pos.CENTER_LEFT);

        messageContainer.getChildren().add(row);
    }


}
