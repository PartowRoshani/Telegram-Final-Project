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
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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

    // ===== Time formatter for messages =====
    private static final DateTimeFormatter FMT_HHMM       = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATE_TIME  = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final String YESTERDAY_LABEL           = "Yesterday";

    // ===== state =====
    private String chatName;
    private final ThemeManager themeManager = ThemeManager.getInstance();

    // Where your icons live
    private static final String ICON_BASE = "/org/to/telegramfinalproject/Icons/";
    private ChatEntry currentChat;
    private UUID me;

    // ----- helpers for safe strings -----
    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
    private static boolean hasVal(String s) {
        if (s == null) return false;
        String t = s.trim();
        return !t.isEmpty() && !"null".equalsIgnoreCase(t);
    }


    private void initCurrentUserId() {
        try {
            String meStr = org.to.telegramfinalproject.Client.Session
                    .currentUser.getString("internal_uuid");
            me = UUID.fromString(meStr);
        } catch (Exception ignore) {
            me = null;
        }
    }

    //Time formatter for messages

    private static final DateTimeFormatter FMT_HHMM       = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATE_TIME  = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final String YESTERDAY_LABEL           = "Yesterday";

    private static String str(org.json.JSONObject j, String k) {
        try { return (j.has(k) && !j.isNull(k)) ? j.getString(k) : ""; } catch (Exception e) { return ""; }
    }
    private static boolean bool(org.json.JSONObject j, String k) {
        try { return (j.has(k) && !j.isNull(k)) && j.getBoolean(k); } catch (Exception e) { return false; }
    }
    private static org.json.JSONArray arr(org.json.JSONObject j, String k) {
        try { return (j.has(k) && !j.isNull(k)) ? j.getJSONArray(k) : null; } catch (Exception e) { return null; }
    }


    private String formatWhen(LocalDateTime ts) {
        if (ts == null) return "";
        LocalDate today = LocalDate.now();
        LocalDate d = ts.toLocalDate();

        if (d.isEqual(today)) {
            return FMT_HHMM.format(ts);
        } else if (d.isEqual(today.minusDays(1))) {
            return YESTERDAY_LABEL + " " + FMT_HHMM.format(ts);
        } else {
            return FMT_DATE_TIME.format(ts);
        }
    }

    /** ISO → LocalDateTime (با پشتیبانی از Offset/Z) */
    private LocalDateTime parseWhen(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(iso);
            } catch (Exception e) {
                return null;
            }
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

    private void initCurrentUserId() {
        try {
            String meStr = org.to.telegramfinalproject.Client.Session
                    .currentUser.getString("internal_uuid");
            me = UUID.fromString(meStr);
        } catch (Exception ignore) {
            me = null;
        }
    }

    private String formatWhen(LocalDateTime ts) {
        if (ts == null) return "";
        LocalDate today = LocalDate.now();
        LocalDate d = ts.toLocalDate();

        if (d.isEqual(today)) {
            return FMT_HHMM.format(ts);
        } else if (d.isEqual(today.minusDays(1))) {
            return YESTERDAY_LABEL + " " + FMT_HHMM.format(ts);
        } else {
            return FMT_DATE_TIME.format(ts);
        }
    }

    /** ISO → LocalDateTime (با پشتیبانی از Offset/Z) */
    private LocalDateTime parseWhen(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(iso);
            } catch (Exception e) {
                return null;
            }
        }
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
        chatStatus.setText("");
        if (entry.getImageUrl() != null && !entry.getImageUrl().isEmpty()) {
            try {
                userAvatar.setImage(new Image(entry.getImageUrl()));
                userAvatar.setClip(new Circle(18, 18, 18));
            } catch (Exception ignored) {
            }
        }

        messageContainer.getChildren().clear();
        loadMessages(entry);

        markAsRead(entry);

        Platform.runLater(() -> messageInput.requestFocus());
    }

    private void loadMessages(ChatEntry entry) {
        JSONObject req = new JSONObject();
        req.put("action", "get_messages_UI");
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


//    private void renderMessages(org.json.JSONArray arr) {
//        messageContainer.getChildren().clear();
//
//        String myId = Session.currentUser != null
//                ? Session.currentUser.optString("internal_uuid", "")
//                : "";
//
//        for (int i = 0; i < arr.length(); i++) {
//            org.json.JSONObject m = arr.getJSONObject(i);
//
//            String senderId    = m.has("sender_id")    && !m.isNull("sender_id")    ? m.getString("sender_id")    : "";
//            String senderName  = m.has("sender_name")  && !m.isNull("sender_name")  ? m.getString("sender_name")  : "";
//            String type        = m.has("message_type") && !m.isNull("message_type") ? m.getString("message_type") : "TEXT";
//            String content     = m.has("content")      && !m.isNull("content")      ? m.getString("content")      : "";
//            String whenStr     = m.has("send_at")      && !m.isNull("send_at")      ? m.getString("send_at")      : null;
//
//            boolean outgoing = senderId.equalsIgnoreCase(myId);
//
//            String display = outgoing ? "You"
//                    : (!senderName.isEmpty() ? senderName
//                    : (senderId.isEmpty() ? "Unknown"
//                    : senderId.substring(0, Math.min(8, senderId.length()))));
//
//            String text;
//            switch (type.toUpperCase()) {
//                case "IMAGE": text = "[Image]"; break;
//                case "AUDIO": text = "[Audio]"; break;
//                case "VIDEO": text = "[Video]"; break;
//                case "FILE":  text = "[File]";  break;
//                default:      text = content;   break;
//            }
//
//            LocalDateTime ts = parseWhen(whenStr);
//            addBubble(outgoing, display, text, ts);
//        }
//
//        messageScrollPane.layout();
//        messageScrollPane.setVvalue(1.0);
//    }


    private final java.util.Map<String, org.json.JSONObject> msgIndex = new java.util.HashMap<>();

    private void renderMessages(org.json.JSONArray list) {
        messageContainer.getChildren().clear();

        // برای reply-preview: ایندکس کردن پیام‌ها با message_id
        msgIndex.clear();
        for (int i = 0; i < list.length(); i++) {
            org.json.JSONObject m = list.getJSONObject(i);
            String mid = str(m, "message_id");
            if (!mid.isEmpty()) msgIndex.put(mid, m);
        }

        String myId = (Session.currentUser != null && Session.currentUser.has("internal_uuid"))
                ? Session.currentUser.getString("internal_uuid") : "";

        for (int i = 0; i < list.length(); i++) {
            org.json.JSONObject m = list.getJSONObject(i);

            String senderId    = str(m, "sender_id");
            String senderName  = str(m, "sender_name");
            String type        = str(m, "message_type");
            String content     = str(m, "content");
            String whenStr     = str(m, "send_at");
            String msgId       = str(m, "message_id");

            // فوروارد / ریپلای / ادیت / ری‌اکشن
            String fwdFrom     = nz(str(m, "forwarded_from"));
            String fwdBy       = nz(str(m, "forwarded_by"));
            String replyToId   = nz(str(m, "reply_to_id"));
            boolean edited     = bool(m, "is_edited");
            org.json.JSONArray reactions = arr(m, "reactions");

            boolean outgoing = senderId.equalsIgnoreCase(myId);
            if (senderName == null || senderName.isBlank()) {
                senderName = outgoing ? "You"
                        : (senderId == null || senderId.isBlank()
                        ? "Unknown"
                        : senderId.substring(0, Math.min(8, senderId.length())));
            }

            java.time.LocalDateTime ts = parseWhen(whenStr);

            // نمایش
            addBubble(outgoing, senderName, type, content, ts, msgId,
                    fwdFrom, fwdBy, replyToId, edited, reactions);
        }

        // کمی فاصله بین پیام‌ها
        messageContainer.setSpacing(8);
        messageScrollPane.layout();
        messageScrollPane.setVvalue(1.0);
    }

    private String shortId(String id){
        return (id==null||id.isEmpty()) ? "Unknown" : id.substring(0, Math.min(8,id.length()));
    }



    private void markAsRead(ChatEntry entry) {
        JSONObject readReq = new JSONObject();
        readReq.put("action", "mark_as_read");
        readReq.put("receiver_id", entry.getId().toString()); // ⛳️ internal_id
        readReq.put("receiver_type", entry.getType());
        ActionHandler.sendWithResponse(readReq);
    }

//    private void addBubble(boolean outgoing, String displayName, String content, LocalDateTime sentAt) {
//
//        Label meta = new Label(displayName + " • " + formatWhen(sentAt));
//        meta.setStyle("-fx-font-size: 11; -fx-text-fill: #7e8a97;");
//        meta.setWrapText(true);
//
//        Label msg = new Label(content);
//        msg.setWrapText(true);
//
//        boolean dark = themeManager.isDarkMode();
//        String mine   = dark ? "#2b7cff" : "#d8ecff";
//        String theirs = dark ? "#2c333a" : "#ffffff";
//        String bg     = outgoing ? mine : theirs;
//
//        msg.setStyle(
//                "-fx-background-color:" + bg + ";" +
//                        "-fx-padding:8 12;" +
//                        "-fx-background-radius:12;" +
//                        "-fx-max-width: 520;"
//        );
//        msg.setMinHeight(Region.USE_PREF_SIZE);
//
//        VBox bubble = new VBox(4, meta, msg);
//
//        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(bubble);
//        row.setFillHeight(true);
//        row.setSpacing(6);
//        row.setAlignment(outgoing
//                ? javafx.geometry.Pos.CENTER_RIGHT
//                : javafx.geometry.Pos.CENTER_LEFT);
//
//        messageContainer.getChildren().add(row);
//    }





    private void addBubble(
            boolean outgoing,
            String displayName,
            String type,
            String content,
            java.time.LocalDateTime sentAt,
            String messageId,
            String forwardedFrom,
            String forwardedBy,
            String replyToId,
            boolean edited,
            org.json.JSONArray reactions
    ) {
        // meta (فرستنده + زمان [+ edited])
        String metaText = (displayName == null ? "" : displayName) + " • " + formatWhen(sentAt);
        if (edited) metaText += " (edited)";
        Label meta = new Label(metaText);
        meta.setStyle("-fx-font-size: 11; -fx-text-fill: #7e8a97;");
        meta.setWrapText(true);

        // نرمال‌سازی نوع پیام و متن
        String t = type == null ? "" : type.trim().toUpperCase();
        boolean isText = t.isEmpty() ? (content != null && !content.isBlank()) : "TEXT".equals(t);
        String bodyText = isText ? (content == null ? "" : content) : bracketLabel(t);

        Label msg = new Label(bodyText);
        msg.setWrapText(true);

        boolean dark = themeManager.isDarkMode();
        String mine   = dark ? "#2b7cff" : "#d8ecff";
        String theirs = dark ? "#2c333a" : "#ffffff";
        String bg     = outgoing ? mine : theirs;

        msg.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-padding:8 12;" +
                        "-fx-background-radius:12;" +
                        "-fx-max-width: 520;"
        );
        msg.setMinHeight(Region.USE_PREF_SIZE);

        // بدنه حباب
        VBox bubble = new VBox(4); // spacing عمودی داخل حباب
        bubble.getChildren().add(meta);

        // Forward header (اختیاری)
        if (hasVal(forwardedFrom) || hasVal(forwardedBy)) {
            bubble.getChildren().add(buildForwardHeader(forwardedFrom, forwardedBy));
        }

        // Reply preview (اختیاری)
        if (hasVal(replyToId)) {
            bubble.getChildren().add(buildReplyBoxFromIndex(replyToId));
        }

        // متن اصلی
        bubble.getChildren().add(msg);

        // Reactions (اختیاری)
        if (reactions != null && reactions.length() > 0) {
            bubble.getChildren().add(buildReactionsBarFromJson(reactions, dark));
        }

        // چیدمان راست/چپ
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(bubble);
        row.setFillHeight(true);
        row.setSpacing(4);
        row.setAlignment(outgoing ? javafx.geometry.Pos.CENTER_RIGHT
                : javafx.geometry.Pos.CENTER_LEFT);

        // کمی padding اطراف هر پیام برای کاهش فاصله‌های ناخوشایند
        row.setPadding(new javafx.geometry.Insets(2, 6, 2, 6));

        messageContainer.getChildren().add(row);
    }


    private String bracketLabel(String t) {
        String tt = (t == null) ? "" : t.trim().toUpperCase();
        switch (tt) {
            case "IMAGE": return "[Image]";
            case "AUDIO": return "[Audio]";
            case "VIDEO": return "[Video]";
            case "FILE":  return "[File]";
            default:      return "[Message]";
        }
    }


    private javafx.scene.Node buildForwardHeader(String forwardedFrom, String forwardedBy) {
        String from = hasVal(forwardedFrom) ? forwardedFrom.trim() : null;
        String by   = hasVal(forwardedBy)   ? forwardedBy.trim()   : null;

        String txt = (from != null && by != null) ? ("Forwarded from " + from + " by " + by)
                : (from != null) ? ("Forwarded from " + from)
                : (by   != null) ? ("Forwarded by "   + by)
                : "Forwarded";

        Label l = new Label(txt);
        l.setStyle("-fx-font-size: 11; -fx-text-fill: #5b8bb1;");
        l.setWrapText(true);
        return l;
    }

    private javafx.scene.Node buildReplyBoxFromIndex(String replyToId) {
        org.json.JSONObject r = msgIndex.get(replyToId);
        String from = "Unknown";
        String preview;

        if (r != null) {
            String rType    = r.optString("message_type", "TEXT");
            String rContent = r.optString("content", "");
            String rSender  = r.optString("sender_name", "");
            if (hasVal(rSender)) from = rSender;

            preview = "TEXT".equalsIgnoreCase(rType) ? rContent : bracketLabel(rType);
        } else {
            preview = "[Quoted message]";
        }

        Label t = new Label("Reply to " + from);
        t.setStyle("-fx-font-size: 11; -fx-text-fill: #5b8bb1; -fx-font-weight: bold;");

        Label p = new Label(ellipsize(preview == null ? "" : preview, 140));
        p.setWrapText(true);
        p.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7c8a;");

        VBox box = new VBox(2, t, p);
        box.setStyle(
                "-fx-background-color: rgba(125,150,175,0.10);" +
                        "-fx-padding:6 8; -fx-background-radius:8;" +
                        "-fx-border-color:#7da0b8; -fx-border-width:0 0 0 2;"
        );
        return box;
    }


    private javafx.scene.Node buildReactionsBarFromJson(org.json.JSONArray arr, boolean dark) {
        javafx.scene.layout.HBox bar = new javafx.scene.layout.HBox(6);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject ro = arr.optJSONObject(i);
            if (ro == null) continue;
            String emoji = ro.optString("emoji", "👍");
            int count    = ro.optInt("count", 1);
            boolean byMe = ro.optBoolean("by_me", false);

            String chipBg = byMe ? (dark ? "#215a9f" : "#d1e8ff")
                    : (dark ? "#2f3942" : "#eef3f7");
            String text   = emoji + (count > 0 ? (" " + count) : "");
            Label chip = new Label(text);
            chip.setStyle(
                    "-fx-font-size: 12;" +
                            "-fx-background-radius: 12;" +
                            "-fx-padding: 2 8;" +
                            "-fx-background-color: " + chipBg + ";"
            );
            bar.getChildren().add(chip);
        }
        return bar;
    }


    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static String ellipsize(String s, int max) { return s.length() > max ? s.substring(0, max) + "…" : s; }



}
