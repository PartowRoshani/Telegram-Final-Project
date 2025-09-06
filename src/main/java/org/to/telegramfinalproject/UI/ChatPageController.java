package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.AvatarLocalResolver;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.to.telegramfinalproject.Client.Session.currentChatId;

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
    private HBox chatHeader;            // Header

    @FXML
    private Button searchInChatButton;  // magnifier button
    @FXML
    private ImageView searchIcon;

    @FXML private VBox blockedPane;

    @FXML
    private Button chatMoreButton;          // 3-dots button
    @FXML
    private ImageView moreIcon;
    @FXML
    private ContextMenu chatMoreMenu;
    @FXML
    private MenuItem viewProfileItem;
    @FXML
    private MenuItem deleteChatItem;
    @FXML
    private MenuItem archiveItem;

    // ===== Group & Channel more button =====
    @FXML private MenuItem viewGroupItem;
    @FXML private MenuItem leaveGroupItem;

    @FXML private MenuItem viewChannelItem;
    @FXML private MenuItem leaveChannelItem;

    // ===== send icon =====
    @FXML
    private ImageView sendIcon;

    // ===== For Searching System =====
    @FXML private VBox composerPane;
    @FXML private VBox joinPane;
    @FXML private VBox addContactPane;

    // For search handling
    @FXML private Button joinButton;
    @FXML private Button addContactButton;

    // Handle View chat
    @FXML private Button unblockBtn;
    @FXML private VBox readOnlyPane;
    @FXML private Label readOnlyLabel;


    private ChatViewMode currentMode = ChatViewMode.NORMAL;



    // --- state for interactions ---
    private String pendingReplyToId = null;    // اگه کاربر ریپلای رو زده
    private String pendingEditMsgId = null;    // اگه کاربر ادیت رو شروع کرده
    private final Map<String, Node> messageNodes = new HashMap<>();
    private final Deque<HBox> pendingBubbles = new ArrayDeque<>();



    // جایی عمومی (مثلا بالای کلاس)
    private static final String UPLOADS_DIR = "C:/Users/User/Desktop/Project/uploads"; // با مسیر خودت یکی کن
    private static final String HTTP_BASE   = "http://localhost:8080";                 // اگر بعدا HTTP رو درست کردی



    private JSONObject lastHeaderData = null;


    // ===== Time formatter for messages =====
    private static final DateTimeFormatter FMT_HHMM       = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DATE_TIME  = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final String YESTERDAY_LABEL           = "Yesterday";

    private boolean blockedByMeFlag = false;
    private boolean blockedMeFlag   = false;
    private volatile boolean justJoinedThisChat = false;



    // ===== state =====
    private String chatName;
    private final ThemeManager themeManager = ThemeManager.getInstance();
    private final java.util.Set<String> pendingClientMsgIds =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    // Where your icons live
    private static final String ICON_BASE = "/org/to/telegramfinalproject/Icons/";
    private static ChatPageController instance;
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

    public ChatPageController() {
        instance = this;
    }

    public static ChatPageController getInstance() {
        return instance;
    }

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
        if (chatMoreButton != null) {
            chatMoreButton.setOnAction(e -> {
                if (!chatMoreMenu.isShowing()) {
                    chatMoreMenu.show(chatMoreButton, Side.BOTTOM, 0, 0);
                } else {
                    chatMoreMenu.hide();
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

        if (archiveItem != null) {
            archiveItem.setOnAction(e -> {
                ChatEntry entry = Session.currentChatEntry;
                if (entry == null) return;
                toggleArchive(entry);
            });
        }

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

//    private void initCurrentUserId() {
//        try {
//            String meStr = org.to.telegramfinalproject.Client.Session
//                    .currentUser.getString("internal_uuid");
//            me = UUID.fromString(meStr);
//        } catch (Exception ignore) {
//            me = null;
//        }
//    }
//
//    private String formatWhen(LocalDateTime ts) {
//        if (ts == null) return "";
//        LocalDate today = LocalDate.now();
//        LocalDate d = ts.toLocalDate();
//
//        if (d.isEqual(today)) {
//            return FMT_HHMM.format(ts);
//        } else if (d.isEqual(today.minusDays(1))) {
//            return YESTERDAY_LABEL + " " + FMT_HHMM.format(ts);
//        } else {
//            return FMT_DATE_TIME.format(ts);
//        }
//    }
//
//    /** ISO → LocalDateTime (با پشتیبانی از Offset/Z) */
//    private LocalDateTime parseWhen(String iso) {
//        if (iso == null || iso.isEmpty()) return null;
//        try {
//            return OffsetDateTime.parse(iso).toLocalDateTime();
//        } catch (Exception ignore) {
//            try {
//                return LocalDateTime.parse(iso);
//            } catch (Exception e) {
//                return null;
//            }
//        }
//    }

    @FXML
    private void openSearchPanel() {
        MainController.getInstance().showSearchPanel();
    }

//    /**
//     * Called by main controller when opening a chat.
//     */
//    public void setChat(String chatName, String avatarPath) {
//        this.chatName = chatName;
//
//        // Header text (if present)
//        if (chatTitle != null) chatTitle.setText(chatName);
//        if (chatStatus != null) chatStatus.setText("last seen recently"); // or live status
//
//        // Load avatar if provided
//        if (userAvatar != null && avatarPath != null) {
//            try {
//                Image avatarImg = new Image(getClass().getResourceAsStream(avatarPath));
//                userAvatar.setImage(avatarImg);
//                Circle clip = new Circle(18, 18, 18); // x, y, radius
//                userAvatar.setClip(clip);
//            } catch (Exception e) {
//                System.err.println("Could not load avatar: " + avatarPath);
//            }
//        }
//
//        addSystemMessage("Chat with " + chatName + " opened.");
//        Platform.runLater(() -> messageInput.requestFocus());
//    }

    // ----- actions -----

//    private void sendMessage() {
//        String text = messageInput.getText() == null ? "" : messageInput.getText().trim();
//        if (!text.isEmpty()) {
//            addMessage("You", text);
//            messageInput.clear();
//        }
//    }


//    private void sendMessage() {
//        // 0) Read and validate input
//        String raw = messageInput.getText();
//        String text = (raw == null) ? "" : raw.trim();
//        if (text.isEmpty()) return;
//
//        if (currentChat == null) {
//            addSystemMessage("No chat is selected.");
//            return;
//        }
//
//        // 1) Clear input immediately for good UX
//        messageInput.clear();
//
//        // 2) Snapshot chat info (must be final for lambdas)
//        final UUID targetChatId   = currentChat.getId();
//        final String targetType   = currentChat.getType();    // "private" | "group" | "channel"
//        final String contentToSend = text;                    // effectively final
//
//        // 3) Build the SAME JSON as your console method (for TEXT only)
//        org.json.JSONObject req = new org.json.JSONObject();
//        req.put("action", "send_message");
//        req.put("receiver_type", targetType);
//        req.put("receiver_id", targetChatId.toString());
//        req.put("content", contentToSend);
//        req.put("message_type", "TEXT");
//
//        // 4) Send on a background thread
//        new Thread(() -> {
//            org.json.JSONObject resp;
//            try {
//                resp = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                Platform.runLater(() -> addSystemMessage("Send failed: " + ex.getMessage()));
//                return;
//            }
//
//            // 5) Check status like your console method
//            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
//                String err = (resp != null) ? resp.optString("message", "No response") : "No response";
//                Platform.runLater(() -> addSystemMessage("Send failed: " + err));
//                return;
//            }
//
//            // 6) Extract fields (your console reads data.message_id; handle both shapes)
//            org.json.JSONObject data = resp.optJSONObject("data");
//            String messageId = null;
//            String sendAtIso = null;
//            if (data != null) {
//                // If server returns { data: { message_id, send_at, ... } }
//                messageId = data.optString("message_id", null);
//
//                // Some servers nest: { data: { message: {...} } }
//                if (messageId == null) {
//                    org.json.JSONObject msgObj = data.optJSONObject("message");
//                    if (msgObj != null) {
//                        messageId = msgObj.optString("message_id", null);
//                        sendAtIso = msgObj.optString("send_at", null);
//                    }
//                } else {
//                    sendAtIso = data.optString("send_at", null);
//                }
//            }
//            if (messageId == null) messageId = java.util.UUID.randomUUID().toString();
//
//            final java.time.LocalDateTime ts =
//                    (sendAtIso != null && !sendAtIso.isBlank()) ? parseWhen(sendAtIso)
//                            : java.time.LocalDateTime.now();
//
//            final String fMessageId = messageId;
//            final java.time.LocalDateTime fTs = ts;
//
//            // 7) Update UI on FX thread (render outgoing bubble + index for reply previews)
//            Platform.runLater(() -> {
//                // If user switched chats while sending, don’t render here
//                if (currentChat == null || !currentChat.getId().equals(targetChatId)) return;
//
//                addBubble(
//                        true,                 // outgoing
//                        "You",                // display name
//                        "TEXT",               // message type
//                        contentToSend,        // content
//                        fTs,                  // timestamp
//                        fMessageId,           // message_id
//                        "", "", "",           // forwarded_from, forwarded_by, reply_to_id
//                        false,                // edited
//                        null                  // reactions
//                );
//
//                //Real time
//                var mc = MainController.getInstance();
//                if (mc != null) {
//                    String preview = "You: " + (contentToSend.isBlank() ? "[Message]" : contentToSend);
//                    mc.onChatUpdated(targetChatId, targetType, fTs, /*isIncoming*/ false, preview);
//                }
//
//                // Keep it in msgIndex for reply previews
//                org.json.JSONObject idx = new org.json.JSONObject();
//                idx.put("message_id",   fMessageId);
//                idx.put("message_type", "TEXT");
//                idx.put("content",      contentToSend);
//                idx.put("sender_name",  "You");
//                idx.put("sender_id",    (me != null) ? me.toString() : "");
//                idx.put("send_at",      fTs.toString());
//                msgIndex.put(fMessageId, idx);
//            });
//        }).start();
//    }


    private void sendMessage() {
        // 1) متن ورودی
        String raw = messageInput.getText();
        String text = (raw == null) ? "" : raw.trim();
        if (text.isEmpty()) return;

        if (currentChat == null) {
            addSystemMessage("No chat is selected.");
            return;
        }

        // UX بهتر: اینپوت را سریع خالی کن
        messageInput.clear();

        final UUID chatId   = currentChat.getId();
        final String cType  = currentChat.getType();

        // =========================
        // A) حالت EDIT
        // =========================
        if (pendingEditMsgId != null) {
            final String msgIdForEdit = pendingEditMsgId;
            pendingEditMsgId = null;

            JSONObject req = new JSONObject()
                    .put("action", "edit_message")
                    .put("message_id", msgIdForEdit)
                    .put("new_content", text);

            new Thread(() -> {
                JSONObject resp = ActionHandler.sendWithResponse(req);
                Platform.runLater(() -> {
                    if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                        addSystemMessage("Edit failed: " + (resp == null ? "no response" : resp.optString("message","")));
                    } else {
                        // ساده‌ترین راه: پیام‌ها را دوباره بخوان
                        loadMessages(currentChat);
                    }
                });
            }).start();
            return;
        }

        // =========================
        // B) حالت REPLY
        // =========================
        if (pendingReplyToId != null) {
            final String replyTo = pendingReplyToId;
            pendingReplyToId = null;

            // اگر بالای کامپوزر پریویو ریپلای گذاشته‌ای، پاکش کن (اختیاری)
            if (!composerPane.getChildren().isEmpty()) {
                // اگر عنصر اول preview است، حذف کن
                composerPane.getChildren().remove(0);
            }

            JSONObject req = new JSONObject()
                    .put("action", "send_reply_message")
                    .put("receiver_type", cType)
                    .put("receiver_id", chatId.toString())
                    .put("content", text)
                    .put("reply_to_id", replyTo);

            new Thread(() -> {
                JSONObject resp = ActionHandler.sendWithResponse(req);
                Platform.runLater(() -> {
                    if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                        addSystemMessage("Reply failed: " + (resp == null ? "no response" : resp.optString("message","")));
                    } else {
                        // می‌توانی مثل حالت عادی حباب optimistic بسازی.
                        // ساده: رفرش لیست پیام‌ها
                        loadMessages(currentChat);
                    }
                });
            }).start();
            return;
        }

        // =========================
        // C) حالت عادی (send_message)
        // =========================
        final String contentToSend = text;

        JSONObject req = new JSONObject()
                .put("action", "send_message")
                .put("receiver_type", cType)
                .put("receiver_id", chatId.toString())
                .put("content", contentToSend)
                .put("message_type", "TEXT");

        new Thread(() -> {
            JSONObject resp;
            try {
                resp = ActionHandler.sendWithResponse(req);
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> addSystemMessage("Send failed: " + ex.getMessage()));
                return;
            }

            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                String err = (resp != null) ? resp.optString("message", "No response") : "No response";
                Platform.runLater(() -> addSystemMessage("Send failed: " + err));
                return;
            }

            // استخراج message_id و زمان
            JSONObject data = resp.optJSONObject("data");
            String messageId = null;
            String sendAtIso = null;
            if (data != null) {
                // { data: { message_id, send_at } }
                messageId = data.optString("message_id", null);
                sendAtIso = data.optString("send_at", null);

                // یا { data: { message: {...} } }
                if (messageId == null) {
                    JSONObject msgObj = data.optJSONObject("message");
                    if (msgObj != null) {
                        messageId = msgObj.optString("message_id", null);
                        sendAtIso = msgObj.optString("send_at", null);
                    }
                }
            }
            if (messageId == null) messageId = UUID.randomUUID().toString();

            final LocalDateTime ts =
                    (sendAtIso != null && !sendAtIso.isBlank()) ? parseWhen(sendAtIso)
                            : LocalDateTime.now();

            final String fMessageId = messageId;
            final LocalDateTime fTs = ts;

            Platform.runLater(() -> {
                // اگر کاربر چت را عوض کرده بود، چیزی رندر نکن
                if (currentChat == null || !currentChat.getId().equals(chatId)) return;

                // حباب outgoing
                addBubble(
                        true,                 // outgoing
                        "You",                // display name
                        "TEXT",               // message type
                        contentToSend,        // content
                        fTs,                  // timestamp
                        fMessageId,           // message_id
                        "", "", "",           // forwarded_from, forwarded_by, reply_to_id
                        false,                // edited
                        null,                  // reactions
                        null,
                        null

                );

                // آپدیت پیش‌نمایش لیست چت‌ها
                var mc = MainController.getInstance();
                if (mc != null) {
                    String preview = "You: " + (contentToSend.isBlank() ? "[Message]" : contentToSend);
                    mc.onChatUpdated(chatId, cType, fTs, /*isIncoming*/ false, preview);
                }

                // برای reply-preview بعدی، پیام را ایندکس کن
                JSONObject idx = new JSONObject();
                idx.put("message_id",   fMessageId);
                idx.put("message_type", "TEXT");
                idx.put("content",      contentToSend);
                idx.put("sender_name",  "You");
                idx.put("sender_id",    (me != null) ? me.toString() : "");
                idx.put("send_at",      fTs.toString());
                msgIndex.put(fMessageId, idx);
            });
        }).start();
    }


    private void openFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select image or audio");

        // فقط عکس/صدا
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
                new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.m4a", "*.ogg", "*.aac")
        );

        File file = fc.showOpenDialog(attachmentButton.getScene().getWindow());
        if (file == null) return;

        // نوع (IMAGE/AUDIO) را از اسم/محتوا حدس بزنیم
        String type = guessType(file);
        if (type == null) {
            toast("Only Image and Aduio");
            return;
        }
        currentChatId = String.valueOf(currentChat.getId());
        Session.currentChatType = currentChat.getType();

        // مقصد چت از Session
        if (Session.currentChatId == null || Session.currentChatType == null) {
            toast("Not available chat");
            return;
        }

        UUID receiverId = UUID.fromString(Session.currentChatId);
        String receiverType = Session.currentChatType; // "private" | "group" | "channel"

        String caption = (messageInput != null) ? messageInput.getText().trim() : "";
        if (messageInput != null) messageInput.clear();

        // 1) حباب Pending فوری در UI
        HBox pending = addPendingMediaBubble(file, type, caption);

        // 2) ارسال واقعی در بک‌گراند با ActionHandler
        new Thread(() -> {
            ActionHandler ah = ActionHandler.getInstance();
            ah.sendMediaMessage(receiverId, receiverType, type, file, caption);

            // نکته: پیام واقعی بعد از ذخیره روی سرور، از طریق Listener برمی‌گرده.
            // وقتی رسید، در Listener این متد را صدا بزنید تا یک حباب Pending حذف شود:
            // Platform.runLater(() -> removeOnePendingBubble());
        }, "Media-Uploader").start();
    }

    /** حدس نوع فایل: IMAGE یا AUDIO */
    private String guessType(File f) {
        String name = f.getName().toLowerCase();
        if (name.matches(".*\\.(png|jpg|jpeg|gif|bmp|webp)$")) return "IMAGE";
        if (name.matches(".*\\.(mp3|wav|m4a|ogg|aac)$"))       return "AUDIO";
        // اگر خواستی دقیق‌تر: با Files.probeContentType هم تست کن
        return null;
    }

    /** ساخت یک حباب «درحال ارسال…» */
    private HBox addPendingMediaBubble(File file, String type, String caption) {
        HBox root = new HBox(8);
        root.getStyleClass().add("bubble-outgoing"); // استایل دلخواهت
        root.setFillHeight(true);

        ImageView iv = null;
        if ("IMAGE".equalsIgnoreCase(type)) {
            iv = new ImageView(new Image(file.toURI().toString(), 360, 360, true, true, true));
            iv.setPreserveRatio(true);
            iv.setFitWidth(240);     // سایز معقول برای Pending
            iv.setFitHeight(240);
            root.getChildren().add(iv);
        } else if ("AUDIO".equalsIgnoreCase(type)) {
            // برای صدا یک آیکون ساده و نام فایل
            ImageView icon = new ImageView(); // اگر آیکون داری اینجا بگذار
            icon.setFitWidth(24); icon.setFitHeight(24);
            Label name = new Label(file.getName());
            HBox audioBox = new HBox(6, icon, name);
            root.getChildren().add(audioBox);
        }

        VBox right = new VBox(4);
        if (caption != null && !caption.isBlank()) {
            Label cap = new Label(caption);
            cap.getStyleClass().add("msg-caption");
            cap.setWrapText(true);
            right.getChildren().add(cap);
        }

        HBox statusRow = new HBox(6);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(16, 16);
        Label status = new Label("Sending...");
        status.getStyleClass().add("msg-status");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statusRow.getChildren().addAll(spinner, status, spacer);

        right.getChildren().add(statusRow);
        root.getChildren().add(right);

        messageContainer.getChildren().add(root);
        pendingBubbles.addLast(root);

        return root;
    }

    /** وقتی پیام واقعی (از خودِ کاربر) برای همین چت رسید، یکی از Pendingها را حذف کن. */
    public void removeOnePendingBubble() {
        HBox node = pendingBubbles.pollFirst();
        if (node != null) {
            messageContainer.getChildren().remove(node);
        }
    }

    private void toast(String msg) {
        // هر جور که خودت نوتیف/Toast داری
        System.out.println("ℹ️ " + msg);
        if (messageInput != null) {
            messageInput.setTooltip(new Tooltip(msg));
        }
    }

    // ----- UI helpers -----

//    /**
//     * Add a normal message bubble (very simple for now).
//     */
//    public void addMessage(String sender, String content) {
//        Label msg = new Label(sender + ": " + content);
//        msg.setWrapText(true);
//
//        boolean dark = themeManager.isDarkMode();
//        String bubbleColor = dark ? "#20405a" : "#4fa8f0";
//        String textColor = dark ? "#e8f1f8" : "#0f141a";
//        msg.setStyle(
//                "-fx-background-color: " + bubbleColor + ";" +
//                        "-fx-text-fill: " + textColor + ";" +
//                        "-fx-padding: 6 10; -fx-background-radius: 10;"
//        );
//
//        messageContainer.getChildren().add(msg);
//    }

    public void addSystemMessage(String content) {
        Label sys = new Label(content);
        sys.setStyle("-fx-text-fill: gray; -fx-font-size: 11;");
        messageContainer.getChildren().add(sys);
        messageScrollPane.layout();
        messageScrollPane.setVvalue(1.0);
    }

//    /**
//     * Update all header/footer icons according to current theme.
//     */
//    private void syncIconsWithTheme() {
//        boolean dark = themeManager.isDarkMode();
//        // We use “_light” icons on dark backgrounds, and “_dark” on light backgrounds.
//        String suffix = dark ? "_light.png" : "_dark.png";
//
//        // attachment
//        if (attachmentIcon != null) {
//            attachmentIcon.setImage(loadIcon("attachment" + suffix));
//        }
//        // send
//        if (sendIcon != null) {
//            sendIcon.setImage(loadIcon("send_cyan2.png"));
//        }
//        // header icons
//        if (searchIcon != null) {
//            searchIcon.setImage(loadIcon("search" + suffix));
//        }
//        if (moreIcon != null) {
//            moreIcon.setImage(loadIcon("more" + suffix));
//        }
//
//        // header text tint (if you’re not fully relying on CSS)
//        if (chatTitle != null) chatTitle.setStyle(dark ? "-fx-text-fill:#e8f1f8;" : "-fx-text-fill:#0f141a;");
//        if (chatStatus != null) chatStatus.setStyle(dark ? "-fx-text-fill:#8ea1b2;" : "-fx-text-fill:#7e8a97;");
//
//        // View profile icon in more button
//        ((ImageView) viewProfileItem.getGraphic())
//                .setImage(loadIcon("view_profile" + suffix));
//    }

    private Image loadIcon(String filename) {
        var url = getClass().getResource(ICON_BASE + filename);
        if (url == null) {
            System.err.println("Icon not found: " + ICON_BASE + filename);
            return null;
        }
        return new Image(url.toExternalForm());
    }

//    public void showChat(ChatEntry entry) {
//        this.currentChat = entry;
//
//        chatTitle.setText(entry.getName());
//
//        // آواتار پیش‌فرض بر اساس نوع
//        // ChatPageController.showChat(...)
//        if (entry.getImageUrl() != null && !entry.getImageUrl().isEmpty()) {
//            Image img = AvatarLocalResolver.load(entry.getImageUrl());
//            if (img != null) {
//                userAvatar.setImage(img);
//            } else {
//                // ⬇️ فال‌بک بر اساس نوع
//                setDefaultHeaderAvatarByType(entry.getType());
//            }
//        } else {
//            setDefaultHeaderAvatarByType(entry.getType());
//        }
    ////        userAvatar.setClip(new Circle(20, 20, 20));
//        AvatarFX.circleClip(userAvatar, 36);
//
//
//
//        fetchAndRenderHeader(entry);
//
//        // پیام‌ها
//        messageContainer.getChildren().clear();
//        loadMessages(entry);
//        markAsRead(entry);
//
//        Platform.runLater(() -> messageInput.requestFocus());
//    }

//
//    public void showChat(ChatEntry entry) {
//        this.currentChat = entry;
//        this.chatName = entry.getName(); // برای لاگ/منو
//
//        chatTitle.setText(entry.getName());
//        if (entry.getImageUrl() != null && !entry.getImageUrl().isEmpty()) {
//            Image img = AvatarLocalResolver.load(entry.getImageUrl());
//            if (img != null) userAvatar.setImage(img); else setDefaultHeaderAvatarByType(entry.getType());
//        } else {
//            setDefaultHeaderAvatarByType(entry.getType());
//        }
//        AvatarFX.circleClip(userAvatar, 36);
//
//        fetchAndRenderHeader(entry);
//
//        messageContainer.getChildren().clear();
//        loadMessages(entry);
//        markAsRead(entry);
//
//        applyMode(ChatViewMode.NORMAL);
//
//        Platform.runLater(() -> messageInput.requestFocus());
//    }


    public void showChat(ChatEntry entry) {
        this.currentChat = entry;
        this.chatName = entry.getName();

        chatTitle.setText(entry.getName());
        if (entry.getImageUrl() != null && !entry.getImageUrl().isEmpty()) {
            Image img = AvatarLocalResolver.load(entry.getImageUrl());
            if (img != null) userAvatar.setImage(img); else setDefaultHeaderAvatarByType(entry.getType());
        } else {
            setDefaultHeaderAvatarByType(entry.getType());
        }
        AvatarFX.circleClip(userAvatar, 36);


        // حالت اولیه (بدون انتظار هدر)
        if ("channel".equalsIgnoreCase(entry.getType())) {
            boolean canPostLocal = entry.isOwner() || entry.isAdmin()
                    || (entry.getPermissions()!=null && entry.getPermissions().optBoolean("can_post", false));
            applyMode(canPostLocal ? ChatViewMode.NORMAL : ChatViewMode.READ_ONLY);
        } else {
            applyMode(ChatViewMode.NORMAL);
        }

        messageContainer.getChildren().clear();
        loadMessages(entry);
        markAsRead(entry);

        // حالا هدر بیاد، دوباره نهایی‌اش می‌کنیم
        fetchAndRenderHeader(entry);

    // === (3-dot menu + header click) ===
    configureHeaderActions(entry);

    requestBlockStatusByChat(entry);
}



    public void requestBlockStatusByChat(ChatEntry entry) {
        if (entry == null || !"private".equalsIgnoreCase(entry.getType())) return;

        String viewerId = Session.getUserUUID(); // internal_uuid کاربر فعلی
        if (viewerId == null || viewerId.isBlank()) return;

        JSONObject req = new JSONObject()
                .put("action", "check_block_status_by_chat")
                .put("viewer_id", viewerId)
                .put("chat_id", entry.getId().toString());

        new Thread(() -> {
            JSONObject res = ActionHandler.sendWithResponse(req);
            if (res == null || !"success".equalsIgnoreCase(res.optString("status"))) return;

            JSONObject data = res.optJSONObject("data");
            boolean blockedByMe = data != null && data.optBoolean("blocked_by_me", false);
            boolean blockedMe   = data != null && data.optBoolean("blocked_me", false);

            Platform.runLater(() -> applyBlockUi(blockedByMe, blockedMe));
        }).start();
    }

    public void applyBlockUi(boolean blockedByMe, boolean blockedMe) {
        this.blockedByMeFlag = blockedByMe;
        this.blockedMeFlag   = blockedMe;

        if (blockedByMe) {
            // من طرف مقابل را بلاک کرده‌ام → فقط دکمه UNBLOCK نمایش بده
            if (readOnlyLabel != null) readOnlyLabel.setText("");
            applyMode(ChatViewMode.BLOCKED);
            return;
        }

        if (blockedMe) {
            // طرف مقابل من را بلاک کرده → متن read-only مخصوص
            if (readOnlyLabel != null) readOnlyLabel.setText("YOU ARE BLOCKED");
            applyMode(ChatViewMode.READ_ONLY);
            return;
        }

        // هیچ‌کس بلاک نکرده
        applyMode(ChatViewMode.NORMAL);
    }


    public void showChat(ChatEntry entry, ChatViewMode mode) {
        this.currentChat = entry;

        // --- Header ---
        chatTitle.setText(entry.getName());
        if (entry.getImageUrl() != null && !entry.getImageUrl().isEmpty()) {
            Image img = AvatarLocalResolver.load(entry.getImageUrl());
            if (img != null) userAvatar.setImage(img);
            else setDefaultHeaderAvatarByType(entry.getType());
        } else {
            setDefaultHeaderAvatarByType(entry.getType());
        }
        AvatarFX.circleClip(userAvatar, 36);

        // Fetch live header info from server
        fetchAndRenderHeader(entry);

        // --- Messages ---
        messageContainer.getChildren().clear();
        loadMessages(entry);
        markAsRead(entry);

        // --- UI mode (Composer / Join / Add Contact) ---
        applyMode(mode);

        // Focus if normal mode
        if (mode == ChatViewMode.NORMAL) {
            Platform.runLater(() -> messageInput.requestFocus());
        }

        // Wire up menu + header click
        configureHeaderActions(entry);

        requestBlockStatusByChat(entry);
    }

    private void configureHeaderActions(ChatEntry entry) {
        chatMoreMenu.getItems().clear();

        switch (entry.getType().toLowerCase(Locale.ROOT)) {
            case "private" -> {
                archiveItem.setOnAction(e -> toggleArchive(entry));
                viewProfileItem.setOnAction(e -> openInfoScene(entry));
                deleteChatItem.setOnAction(e -> deleteChatButton(entry));
                chatMoreMenu.getItems().addAll(archiveItem, viewProfileItem, deleteChatItem);
            }
            case "group" -> {
                MenuItem viewGroup = new MenuItem("View group info");
                viewGroup.setOnAction(e -> openInfoScene(entry));
                MenuItem leaveGroup = new MenuItem("Leave group");
                leaveGroup.setOnAction(e -> leaveGroupButton(entry));
                chatMoreMenu.getItems().addAll(viewGroup, leaveGroup);
            }
            case "channel" -> {
                MenuItem viewChannel = new MenuItem("View channel info");
                viewChannel.setOnAction(e -> openInfoScene(entry));
                MenuItem leaveChannel = new MenuItem("Leave channel");
                leaveChannel.setOnAction(e -> leaveChannelButton(entry));
                chatMoreMenu.getItems().addAll(viewChannel, leaveChannel);
            }
        }

        // Header click opens info
        chatHeader.setOnMouseClicked(e -> {
            if (!chatMoreButton.equals(e.getTarget())) {
                openInfoScene(entry);
            }
        });
    }

    private void openInfoScene(ChatEntry entry) {
        JSONObject req = new JSONObject();
        String targetId = null; // 👈 capture it here for the private case

        switch (entry.getType().toLowerCase()) {
            case "private" -> {
                // First ask server for the target_id of this private chat
                JSONObject targetReq = new JSONObject()
                        .put("action", "get_private_chat_target")
                        .put("chat_id", entry.getId().toString());

                JSONObject targetResp = ActionHandler.sendWithResponse(targetReq);
                if (targetResp == null || !"success".equalsIgnoreCase(targetResp.optString("status"))) {
                    Platform.runLater(() ->
                            MainController.getInstance().showAlert(
                                    "Error",
                                    targetResp != null ? targetResp.optString("message") : "Failed to fetch chat target.",
                                    Alert.AlertType.ERROR
                            )
                    );
                    return;
                }

                targetId = targetResp.optJSONObject("data").optString("target_id", null);
                if (targetId == null || targetId.isBlank()) {
                    Platform.runLater(() ->
                            MainController.getInstance().showAlert(
                                    "Error",
                                    "No target user found for this chat.",
                                    Alert.AlertType.ERROR
                            )
                    );
                    return;
                }

                req.put("action", "view_profile")
                        .put("target_id", targetId);
            }
            case "group" -> {
                req.put("action", "view_group")
                        .put("group_id", entry.getId().toString());
            }
            case "channel" -> {
                req.put("action", "view_channel")
                        .put("channel_id", entry.getId().toString());
            }
            default -> {
                Platform.runLater(() ->
                        MainController.getInstance().showAlert(
                                "Error",
                                "Unsupported chat type: " + entry.getType(),
                                Alert.AlertType.ERROR
                        )
                );
                return;
            }
        }

        final String finalTargetId = targetId;

        new Thread(() -> {
            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                Platform.runLater(() ->
                        MainController.getInstance().showAlert(
                                "Error",
                                resp != null ? resp.optString("message") : "Server not responding.",
                                Alert.AlertType.ERROR
                        )
                );
                return;
            }

            JSONObject data = resp.optJSONObject("data");
            if (data == null) {
                Platform.runLater(() ->
                        MainController.getInstance().showAlert(
                                "Error",
                                "Malformed server response.",
                                Alert.AlertType.ERROR
                        )
                );
                return;
            }

            Platform.runLater(() -> {
                try {
                    FXMLLoader loader;
                    Node overlay = null;

                    switch (entry.getType().toLowerCase()) {
                        case "private" -> {
                            loader = new FXMLLoader(getClass().getResource(
                                    "/org/to/telegramfinalproject/Fxml/user_info.fxml"));
                            overlay = loader.load();
                            UserInfoController c = loader.getController();
                            // 👇 pass targetId so UserInfoController gets the real UUID
                            c.setProfileDataFromJson(entry, data, finalTargetId);
                        }
                        case "group" -> {
                            loader = new FXMLLoader(getClass().getResource(
                                    "/org/to/telegramfinalproject/Fxml/group_info.fxml"));
                            overlay = loader.load();
                            GroupInfoController c = loader.getController();
                            c.setGroupDataFromJson(entry, data);
                        }
                        case "channel" -> {
                            loader = new FXMLLoader(getClass().getResource(
                                    "/org/to/telegramfinalproject/Fxml/channel_info.fxml"));
                            overlay = loader.load();
                            ChannelInfoController c = loader.getController();
                            c.setChannelDataFromJson(entry, data);
                        }
                    }

                    if (overlay != null) {
                        MainController.getInstance().showOverlay(overlay);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    MainController.getInstance().showAlert(
                            "Error",
                            "Could not load info scene.",
                            Alert.AlertType.ERROR
                    );
                }
            });
        }).start();
    }

    private void deleteChatButton(ChatEntry entry) {
        JSONObject req = new JSONObject()
                .put("action", "delete_chat")
                .put("receiver_id", entry.getId().toString())
                .put("viewer_id", Session.getUserUUID());

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            // TODO: close chat + refresh sidebar
            MainController.getInstance().closeCurrentChat();
        }
    }

    private void leaveGroupButton(ChatEntry entry) {
        JSONObject req = new JSONObject()
                .put("action", "leave_group")
                .put("group_id", entry.getId().toString())
                .put("viewer_id", Session.getUserUUID());

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            MainController.getInstance().closeCurrentChat();
        }
    }

    private void leaveChannelButton(ChatEntry entry) {
        JSONObject req = new JSONObject()
                .put("action", "leave_channel")
                .put("channel_id", entry.getId().toString())
                .put("viewer_id", Session.getUserUUID());

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            MainController.getInstance().closeCurrentChat();
        }
    }

    private void toggleArchive(ChatEntry entry) {
        boolean currentlyArchived = Session.isArchived(entry.getId());

        // Disable UI source while processing
        if (archiveItem != null) archiveItem.setDisable(true);

        new Thread(() -> {
            JSONObject req = new JSONObject()
                    .put("chat_id", entry.getId().toString());

            if (!currentlyArchived) {
                // ARCHIVE
                req.put("action", "archive_chat")
                        .put("chat_type", entry.getType()); // سرور می‌خواهد
            } else {
                // UNARCHIVE
                req.put("action", "unarchive_chat");
            }

            JSONObject resp = ActionHandler.sendWithResponse(req);

            Platform.runLater(() -> {
                if (archiveItem != null) archiveItem.setDisable(false);

                if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
                    // 1) جابه‌جایی در Session
                    if (!currentlyArchived) {
                        Session.moveToArchived(entry);
                        if (archiveItem != null) archiveItem.setText("Unarchive chat");
                    } else {
                        Session.moveToActive(entry);
                        if (archiveItem != null) archiveItem.setText("Archive chat");
                    }
                    Session.sortListsByLastMessage();

                    // 2) تازه‌سازی UI لیست‌ها
                    try {
                        MainController.getInstance().refreshChatListUI(); // پیاده‌سازی در بخش 3
                    } catch (Exception ignore) {}

                    // 3) اگر وارد نمای آرشیو هستیم و آن‌آرشیو شد، بلافاصله از لیست آرشیو حذف شود
                    if (Session.inArchivedView && !Session.isArchived(entry.getId())) {
                        try { MainController.getInstance().refreshArchivedListUI(); } catch (Exception ignore) {}
                    }

                    // 4) اگر تازه برای اولین بار آرشیو داریم، ردیف "Archived Chats" در بالای لیست ظاهر شود
                    try { MainController.getInstance().ensureArchivedHeaderRow(); } catch (Exception ignore) {}

                } else {
                    String msg = (resp != null ? resp.optString("message", "Unknown error")
                            : "No response from server.");
                    new Alert(Alert.AlertType.ERROR, "Failed to toggle archive: " + msg).showAndWait();
                }
            });
        }).start();
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

//    private void renderMessages(org.json.JSONArray list) {
//        messageContainer.getChildren().clear();
//
//        // برای reply-preview: ایندکس کردن پیام‌ها با message_id
//        msgIndex.clear();
//        for (int i = 0; i < list.length(); i++) {
//            org.json.JSONObject m = list.getJSONObject(i);
//            String mid = str(m, "message_id");
//            if (!mid.isEmpty()) msgIndex.put(mid, m);
//        }
//
//        String myId = (Session.currentUser != null && Session.currentUser.has("internal_uuid"))
//                ? Session.currentUser.getString("internal_uuid") : "";
//
//        for (int i = 0; i < list.length(); i++) {
//            org.json.JSONObject m = list.getJSONObject(i);
//
//            String senderId    = str(m, "sender_id");
//            String senderName  = str(m, "sender_name");
//            String type        = str(m, "message_type");
//            String content     = str(m, "content");
//            String whenStr     = str(m, "send_at");
//            String msgId       = str(m, "message_id");
//
//            // فوروارد / ریپلای / ادیت / ری‌اکشن
//            String fwdFrom     = nz(str(m, "forwarded_from"));
//            String fwdBy       = nz(str(m, "forwarded_by"));
//            String replyToId   = nz(str(m, "reply_to_id"));
//            boolean edited     = bool(m, "is_edited");
//            org.json.JSONArray reactions = arr(m, "reactions");
//
//            boolean outgoing = senderId.equalsIgnoreCase(myId);
//            if (senderName == null || senderName.isBlank()) {
//                senderName = outgoing ? "You"
//                        : (senderId == null || senderId.isBlank()
//                        ? "Unknown"
//                        : senderId.substring(0, Math.min(8, senderId.length())));
//            }
//
//            java.time.LocalDateTime ts = parseWhen(whenStr);
//
//            // نمایش
//            addBubble(outgoing, senderName, type, content, ts, msgId,
//                    fwdFrom, fwdBy, replyToId, edited, reactions);
//        }
//
//        // کمی فاصله بین پیام‌ها
//        messageContainer.setSpacing(8);
//        messageScrollPane.layout();
//        messageScrollPane.setVvalue(1.0);
//    }

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
            String content     = str(m, "content");     // این همون کپشنه برای IMAGE
            String whenStr     = str(m, "send_at");
            String msgId       = str(m, "message_id");

            // 👇 جدید: URL ها
            String fileUrl     = str(m, "file_url");
            String thumbUrl    = str(m, "thumb_url");

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

            // 👇 امضای جدید addBubble با fileUrl/thumbUrl
            addBubble(outgoing, senderName, type, content, ts, msgId,
                    fwdFrom, fwdBy, replyToId, edited, reactions, fileUrl, thumbUrl);
        }

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
        readReq.put("receiver_id", entry.getId().toString()); // internal_id
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

//    private void addBubble(
//            boolean outgoing,
//            String displayName,
//            String type,
//            String content,
//            java.time.LocalDateTime sentAt,
//            String messageId,
//            String forwardedFrom,
//            String forwardedBy,
//            String replyToId,
//            boolean edited,
//            org.json.JSONArray reactions
//    ) {
//        String metaText = (displayName == null ? "" : displayName) + " • " + formatWhen(sentAt);
//        if (edited) metaText += " (edited)";
//        Label meta = new Label(metaText);
//        meta.setStyle("-fx-font-size: 11; -fx-text-fill: #7e8a97;");
//        meta.setWrapText(true);
//
//        String t = type == null ? "" : type.trim().toUpperCase();
//        boolean isText = t.isEmpty() ? (content != null && !content.isBlank()) : "TEXT".equals(t);
//        String bodyText = isText ? (content == null ? "" : content) : bracketLabel(t);
//
//        Label msg = new Label(bodyText);
//        msg.setWrapText(true);
//
//        boolean dark = themeManager.isDarkMode();
//        String mine   = dark ? "#2b7cff" : "#d8ecff";
//        String theirs = dark ? "#2c333a" : "#f2f4f7";
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
//        // بدنه حباب
//        VBox bubble = new VBox(4); // spacing عمودی داخل حباب
//        bubble.getChildren().add(meta);
//
//        // Forward header (اختیاری)
//        if (hasVal(forwardedFrom) || hasVal(forwardedBy)) {
//            bubble.getChildren().add(buildForwardHeader(forwardedFrom, forwardedBy));
//        }
//
//        // Reply preview (اختیاری)
//        if (hasVal(replyToId)) {
//            bubble.getChildren().add(buildReplyBoxFromIndex(replyToId));
//        }
//
//        // متن اصلی
//        bubble.getChildren().add(msg);
//
//        // Reactions (اختیاری)
//        if (reactions != null && reactions.length() > 0) {
//            bubble.getChildren().add(buildReactionsBarFromJson(reactions, dark));
//        }
//
//        // چیدمان راست/چپ
//        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(bubble);
//        row.setFillHeight(true);
//        row.setSpacing(4);
//        row.setAlignment(outgoing ? javafx.geometry.Pos.CENTER_RIGHT
//                : javafx.geometry.Pos.CENTER_LEFT);
//
//        row.setPadding(new javafx.geometry.Insets(2, 6, 2, 6));
//
//        messageContainer.getChildren().add(row);
//
//        // ... inside addBubble(...) after creating 'row'
//        messageNodes.put(messageId, row);
//
//        boolean isMine = outgoing; // همون که قبلاً حساب کردی
//        ContextMenu menu = buildMessageMenu(isMine, messageId, type, content);
//        row.setOnContextMenuRequested(ev -> {
//            menu.show(row, ev.getScreenX(), ev.getScreenY());
//            ev.consume();
//        });
    //// با کلیک معمولی هم اگر دوست داری:
//        row.setOnMouseClicked(ev -> {
//            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY && ev.getClickCount() == 1) {
//                menu.show(row, ev.getScreenX(), ev.getScreenY());
//            }
//        });
//
//    }



//    private void addBubble(
//            boolean outgoing,
//            String displayName,
//            String type,
//            String content,
//            java.time.LocalDateTime sentAt,
//            String messageId,
//            String forwardedFrom,
//            String forwardedBy,
//            String replyToId,
//            boolean edited,
//            org.json.JSONArray reactions
//    ) {
//        // === Meta (نام + زمان) ===
//        String metaText = (displayName == null ? "" : displayName) + " • " + formatWhen(sentAt);
//        if (edited) metaText += " (edited)";
//        Label meta = new Label(metaText);
//        meta.setStyle("-fx-font-size: 11; -fx-text-fill: #7e8a97;");
//        meta.setWrapText(true);
//        // برچسب برای آپدیت‌های بعدی (edit)
//        meta.getProperties().put("role", "metaLabel");
//
//        // === متن/نوع پیام ===
//        String t = type == null ? "" : type.trim().toUpperCase();
//        boolean isText = t.isEmpty() ? (content != null && !content.isBlank()) : "TEXT".equals(t);
//        String bodyText = isText ? (content == null ? "" : content) : bracketLabel(t);
//
//        Label msg = new Label(bodyText);
//        msg.setWrapText(true);
//        msg.setMinHeight(Region.USE_PREF_SIZE);
//        // برچسب برای آپدیت‌های بعدی (edit)
//        msg.getProperties().put("role", "msgLabel");
//
//        // === رنگ بابل‌ها
//        boolean dark = themeManager.isDarkMode();
//        String mine   = dark ? "#2b7cff" : "#d8ecff";  // outgoing (من)
//        String theirs = dark ? "#2c333a" : "#f2f4f7";  // incoming (خیلی روشن به‌جای سفید)
//        String bg     = outgoing ? mine : theirs;
//
//        msg.setStyle(
//                "-fx-background-color:" + bg + ";" +
//                        "-fx-padding:8 12;" +
//                        "-fx-background-radius:12;" +
//                        "-fx-max-width: 520;"
//        );
//
//        // === بدنه‌ی بابل ===
//        VBox bubble = new VBox(4);
//        bubble.getChildren().add(meta);
//
//        // برچسب‌گذاری بابل برای پیدا کردنش در آپدیت‌های realtime
//        if (messageId != null && !messageId.isBlank()) {
//            bubble.getProperties().put("messageId", messageId);
//        }
//
//        // Forward header (اختیاری)
//        if (hasVal(forwardedFrom) || hasVal(forwardedBy)) {
//            bubble.getChildren().add(buildForwardHeader(forwardedFrom, forwardedBy));
//        }
//
//        // Reply preview (اختیاری)
//        if (hasVal(replyToId)) {
//            bubble.getChildren().add(buildReplyBoxFromIndex(replyToId));
//        }
//
//        // متن اصلی
//        bubble.getChildren().add(msg);
//
//        // Reactions (اختیاری) + برچسب برای تعویض سریع در ریِل‌تایم
//        if (reactions != null && reactions.length() > 0) {
//            Node rxBar = buildReactionsBarFromJson(reactions, dark);
//            rxBar.getProperties().put("role", "reactionsBar");
//            bubble.getChildren().add(rxBar);
//        }
//
//        // === ردیف چیدمان راست/چپ ===
//        HBox row = new HBox(bubble);
//        row.setFillHeight(true);
//        row.setSpacing(4);
//        row.setAlignment(outgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//        row.setPadding(new Insets(2, 6, 2, 6));
//
//        // اضافه به کانتینر
//        messageContainer.getChildren().add(row);
//
//        // ایندکس نود برای آپدیت/حذف realtime
//        if (messageId != null && !messageId.isBlank()) {
//            messageNodes.put(messageId, row);
//        }
//
//        boolean isMine = outgoing;
//
//        // منوی راست‌کلیک/کلیک (بدون تغییر در ساختار کدت)
//        ContextMenu menu = buildMessageMenu(isMine, messageId, type, content);
//        row.setOnContextMenuRequested(ev -> {
//            menu.show(row, ev.getScreenX(), ev.getScreenY());
//            ev.consume();
//        });
//        row.setOnMouseClicked(ev -> {
//            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY && ev.getClickCount() == 1) {
//                menu.show(row, ev.getScreenX(), ev.getScreenY());
//            }
//        });
//    }

    private void addBubble(
            boolean outgoing,
            String displayName,
            String type,
            String content, // برای IMAGE = کپشن
            java.time.LocalDateTime sentAt,
            String messageId,
            String forwardedFrom,
            String forwardedBy,
            String replyToId,
            boolean edited,
            org.json.JSONArray reactions,
            String fileUrl,         // 👈 جدید
            String thumbUrl         // 👈 جدید
    ) {
        String metaText = (displayName == null ? "" : displayName) + " • " + formatWhen(sentAt);
        if (edited) metaText += " (edited)";
        Label meta = new Label(metaText);
        meta.setStyle("-fx-font-size: 11; -fx-text-fill: #7e8a97;");
        meta.setWrapText(true);
        meta.getProperties().put("role", "metaLabel");

        String t = type == null ? "" : type.trim().toUpperCase();
        boolean isText = t.isEmpty() ? (content != null && !content.isBlank()) : "TEXT".equals(t);

        // رنگ پس‌زمینه برای متن (برای عکس پس‌زمینه نمی‌ذاریم تا تمیز باشه)
        boolean dark = themeManager.isDarkMode();
        String mine   = dark ? "#2b7cff" : "#d8ecff";
        String theirs = dark ? "#2c333a" : "#f2f4f7";

        VBox bubble = new VBox(4);
        bubble.getChildren().add(meta);
        if (messageId != null && !messageId.isBlank()) {
            bubble.getProperties().put("messageId", messageId);
        }

        if (hasVal(forwardedFrom) || hasVal(forwardedBy)) {
            bubble.getChildren().add(buildForwardHeader(forwardedFrom, forwardedBy));
        }
        if (hasVal(replyToId)) {
            bubble.getChildren().add(buildReplyBoxFromIndex(replyToId));
        }

        if (isText) {
            // متن
            String bodyText = content == null ? "" : content;
            Label msg = new Label(bodyText);
            msg.setWrapText(true);
            msg.setMinHeight(Region.USE_PREF_SIZE);
            msg.getProperties().put("role", "msgLabel");
            String bg = outgoing ? mine : theirs;
            msg.setStyle("-fx-background-color:" + bg + ";" +
                    "-fx-padding:8 12;" +
                    "-fx-background-radius:12;" +
                    "-fx-max-width: 520;");
            bubble.getChildren().add(msg);

        } else if ("IMAGE".equals(t)) {
            // 👇 نمایش تصویر از روی URL سرور + کپشن اختیاری
            Node imageNode = buildImageNode(fileUrl, thumbUrl, content);
            bubble.getChildren().add(imageNode);

        } else if ("AUDIO".equals(t)) {
            // می‌تونی بعداً کاملش کنی
            Label ph = new Label("🎵 Audio");
            ph.setWrapText(true);
            String bg = outgoing ? mine : theirs;
            ph.setStyle("-fx-background-color:" + bg + ";" +
                    "-fx-padding:8 12;" +
                    "-fx-background-radius:12;" +
                    "-fx-max-width: 520;");
            bubble.getChildren().add(ph);

        } else {
            // ناشناخته
            Label ph = new Label("[" + t + "]");
            ph.setWrapText(true);
            String bg = outgoing ? mine : theirs;
            ph.setStyle("-fx-background-color:" + bg + ";" +
                    "-fx-padding:8 12;" +
                    "-fx-background-radius:12;" +
                    "-fx-max-width: 520;");
            bubble.getChildren().add(ph);
        }

        if (reactions != null && reactions.length() > 0) {
            Node rxBar = buildReactionsBarFromJson(reactions, dark);
            rxBar.getProperties().put("role", "reactionsBar");
            bubble.getChildren().add(rxBar);
        }

        HBox row = new HBox(bubble);
        row.setFillHeight(true);
        row.setSpacing(4);
        row.setAlignment(outgoing ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 6, 2, 6));

        messageContainer.getChildren().add(row);
        if (messageId != null && !messageId.isBlank()) {
            messageNodes.put(messageId, row);
        }

        boolean isMine = outgoing;
        ContextMenu menu = buildMessageMenu(isMine, messageId, type, content);
        row.setOnContextMenuRequested(ev -> { menu.show(row, ev.getScreenX(), ev.getScreenY()); ev.consume(); });
        row.setOnMouseClicked(ev -> {
            if (ev.getButton() == javafx.scene.input.MouseButton.PRIMARY && ev.getClickCount() == 1) {
                menu.show(row, ev.getScreenX(), ev.getScreenY());
            }
        });
    }

    private Node buildImageNode(String fileUrl, String thumbUrl, String caption) {
        String url = hasVal(fileUrl) ? absolute(fileUrl) : null;
        if (!hasVal(url)) return new Label("❌ Image not available");

        ImageView iv = new ImageView();
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setFitWidth(360);
        iv.setFitHeight(360);

        Image img = new Image(url, 360, 360, true, true, true);
        iv.setImage(img);

        img.errorProperty().addListener((obs, wasErr, isErr) -> {
            if (isErr) {
                System.err.println("⚠️ Image load failed: " + url + " | ex=" + img.getException());
                Label err = new Label("❌ Failed to load image");
                VBox fallback = new VBox(4, err);
                Platform.runLater(() -> {
                    if (iv.getParent() instanceof VBox v) {
                        int idx = v.getChildren().indexOf(iv);
                        if (idx >= 0) v.getChildren().set(idx, fallback);
                    }
                });
            }
        });

        iv.setOnMouseClicked(e -> openImagePreviewDialog(url));

        VBox box = new VBox(6, iv);
        if (hasVal(caption)) {
            Label cap = new Label(caption);
            cap.setWrapText(true);
            cap.setStyle("-fx-padding:6 8; -fx-background-radius:10; -fx-background-color: transparent; -fx-max-width: 520;");
            box.getChildren().add(cap);
        }
        return box;
    }




    private void openImagePreviewDialog(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) return;

        // IMPORTANT: fullUrl همین حالا absolute است؛ دوباره absolute(...) نزن
        String url = fullUrl;

        ImageView iv = new ImageView(new Image(url, true));
        iv.setPreserveRatio(true);

        ScrollPane sp = new ScrollPane(iv);
        sp.setPannable(true);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);

        Stage st = new Stage();
        st.setTitle("Preview");
        st.initOwner(attachmentButton.getScene().getWindow());
        st.setScene(new Scene(sp, 900, 700));
        st.addEventHandler(KeyEvent.KEY_PRESSED, e -> { if (e.getCode() == KeyCode.ESCAPE) st.close(); });
        st.show();
    }


    private String absolute(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) return null;
        if (pathOrUrl.startsWith("http")) return pathOrUrl;

        // اگر نسبی است مثل /images/2025-09-06/xxx.jpg یا /audios/...
        String rel = pathOrUrl.startsWith("/") ? pathOrUrl.substring(1) : pathOrUrl;

        // حالت لوکال: file://
        java.nio.file.Path p = java.nio.file.Paths.get(UPLOADS_DIR, rel.replace("/", java.io.File.separator));
        java.net.URI uri = p.toUri();               // می‌شود file:///C:/Users/.../uploads/images/...
        return uri.toString();

        // اگر خواستی از HTTP بخوانی، به‌جای return بالا این را برگردان:
        // return HTTP_BASE + (pathOrUrl.startsWith("/") ? pathOrUrl : "/" + pathOrUrl);
    }




    private ContextMenu buildMessageMenu(boolean isMine, String messageId, String type, String content) {
        ContextMenu menu = new ContextMenu();

        // --- 2.1 نوار ریکشن بالای منو (مثل تلگرام) ---
        HBox reactions = new HBox(8);
        String[] emojis = {"👍","👎","😂","😭","⚡"};
        for (String e : emojis) {
            Button b = new Button(e);
            b.getStyleClass().add("reaction-btn");
            b.setOnAction(ae -> {
                reactToMessage(messageId, e);
                menu.hide();
            });
            reactions.getChildren().add(b);
        }
        CustomMenuItem reactionsItem = new CustomMenuItem(reactions, false);
        reactionsItem.setHideOnClick(false);
        menu.getItems().add(reactionsItem);
        menu.getItems().add(new SeparatorMenuItem());

        // --- 2.2 گزینه‌های مشترک ---
        MenuItem reply = new MenuItem("Reply");
        reply.setOnAction(ae -> startReply(messageId));
        MenuItem forward = new MenuItem("Forward");
        forward.setOnAction(ae -> startForward(messageId));

        // (اختیاری) کپی متن فقط برای TEXT
        if ("TEXT".equalsIgnoreCase(nz(type)) && hasVal(content)) {
            MenuItem copy = new MenuItem("Copy");
            copy.setOnAction(ae -> {
                var cb = javafx.scene.input.Clipboard.getSystemClipboard();
                var contentCB = new javafx.scene.input.ClipboardContent();
                contentCB.putString(content);
                cb.setContent(contentCB);
            });
            menu.getItems().add(copy);
        }

        menu.getItems().addAll(reply, forward);
        // --- 2.3 فقط برای پیام‌های خودم: Edit/Delete ---
        if (isMine) {
            MenuItem edit = new MenuItem("Edit");
            edit.setOnAction(ae -> startEdit(messageId, content));

            MenuItem delete = new MenuItem("Delete");
            delete.getStyleClass().add("danger-item");

            delete.setOnAction(ae -> confirmDeleteDialog(messageId));

            menu.getItems().addAll(edit, delete);
        }


        return menu;
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


    private static String ellipsize(String s, int max) { return s.length() > max ? s.substring(0, max) + "…" : s; }

    public boolean isSameChat(UUID chatId, String type) {
        return currentChat != null
                && currentChat.getId().equals(chatId)
                && currentChat.getType().equalsIgnoreCase(type);
    }

//    public void onRealTimeNewMessage(JSONObject m) {
//        try {
//            String chatIdStr = str(m,"receiver_id");
//            String chatType  = str(m,"receiver_type");
//            if (chatIdStr.isEmpty() || chatType.isEmpty()) return;
//
//            UUID chatId = UUID.fromString(chatIdStr);
//            if (!isSameChat(chatId, chatType)) {
//                System.out.println("[UI] RT msg for another chat: " + chatId);
//                return;
//            }
//
//            // id → message_id fallback
//            if (!m.has("message_id") && m.has("id")) {
//                m.put("message_id", m.getString("id"));
//            }
//
//            String senderName = hasVal(str(m,"sender_name")) ? str(m,"sender_name")
//                    : (hasVal(str(m,"sender_id")) ? shortId(str(m,"sender_id")) : "Unknown");
//
//            String type    = hasVal(str(m,"message_type")) ? str(m,"message_type") : "TEXT";
//            String content = str(m,"content");
//            String whenIso = str(m,"send_at");
//            String msgId   = str(m,"message_id");
//
//            LocalDateTime ts = parseWhen(whenIso);
//            if (ts == null) ts = LocalDateTime.now();
//
//            addBubble(false, senderName, type, content, ts, msgId,
//                    str(m,"forwarded_from"), str(m,"forwarded_by"), str(m,"reply_to_id"),
//                    bool(m,"is_edited"), arr(m,"reactions"));
//
//            if (hasVal(msgId)) msgIndex.put(msgId, m);
//
//            if (currentChat != null) markAsRead(currentChat);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


//    public void onRealTimeNewMessage(JSONObject m) {
//        try {
//            String chatIdStr = str(m,"receiver_id");
//            String chatType  = str(m,"receiver_type");
//            if (chatIdStr.isEmpty() || chatType.isEmpty()) return;
//
//            UUID chatId = UUID.fromString(chatIdStr);
//            boolean isCurrent = isSameChat(chatId, chatType);
//
//            // id → message_id fallback
//            if (!m.has("message_id") && m.has("id")) {
//                m.put("message_id", m.getString("id"));
//            }
//            String msgId = str(m,"message_id");
//            if (!hasVal(msgId)) return;
//
//            // ✅ اگر قبلاً همین پیام داخل UI اضافه شده، دیگه دوباره نساز
//            if (messageNodes.containsKey(msgId)) return;
//
//            String senderName = hasVal(str(m,"sender_name")) ? str(m,"sender_name")
//                    : (hasVal(str(m,"sender_id")) ? shortId(str(m,"sender_id")) : "Unknown");
//
//            String type    = hasVal(str(m,"message_type")) ? str(m,"message_type") : "TEXT";
//            String content = str(m,"content");
//            String whenIso = str(m,"send_at");
//
//            String fwdFrom = str(m,"forwarded_from");
//            String fwdBy   = str(m,"forwarded_by");
//            String replyTo = str(m,"reply_to_id");
//            boolean edited = bool(m,"is_edited");
//            JSONArray reacts = arr(m,"reactions");
//
//            LocalDateTime ts = parseWhen(whenIso);
//            if (ts == null) ts = LocalDateTime.now();
//
//            // ایندکس برای ریپلای/ادیت/ری‌اکشن‌های بعدی
//            msgIndex.put(msgId, m);
//
//            // آپدیت لیست چت‌ها (پریویو)
//            boolean incoming = true; // از سرور آمده → ورودی
//            updateChatListPreview(chatId, chatType, incoming, content, type);
//
//            // اگر در چت فعلی نیستیم، فقط پریویو آپدیت شد؛ برگرد
//            if (!isCurrent) return;
//
//            // اضافه کردن حباب بدون رفرش
//            addBubble(false, senderName, type, content, ts, msgId,
//                    fwdFrom, fwdBy, replyTo, edited, reacts);
//
//            // خوانده شد (در صورت نیاز)
//            if (currentChat != null) markAsRead(currentChat);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


    public void onRealTimeNewMessage(JSONObject m) {
        try {
            String chatIdStr = str(m,"receiver_id");
            String chatType  = str(m,"receiver_type");
            if (chatIdStr.isEmpty() || chatType.isEmpty()) return;

            UUID chatId = UUID.fromString(chatIdStr);
            boolean isCurrent = isSameChat(chatId, chatType);

            // id → message_id fallback
            if (!m.has("message_id") && m.has("id")) {
                m.put("message_id", m.getString("id"));
            }
            String msgId = str(m,"message_id");
            if (!hasVal(msgId)) return;

            // اگر قبلاً تو UI هست، دوباره نساز
            if (messageNodes.containsKey(msgId)) return;

            String senderId   = str(m,"sender_id");
            String senderName = hasVal(str(m,"sender_name")) ? str(m,"sender_name")
                    : (hasVal(senderId) ? shortId(senderId) : "Unknown");

            String type    = hasVal(str(m,"message_type")) ? str(m,"message_type") : "TEXT";
            String content = str(m,"content");          // برای IMAGE = کپشن
            String whenIso = str(m,"send_at");

            // 👇 جدید: URL ها برای عکس/صدا
            String fileUrl  = str(m,"file_url");
            String thumbUrl = str(m,"thumb_url");

            String fwdFrom = str(m,"forwarded_from");
            String fwdBy   = str(m,"forwarded_by");
            String replyTo = str(m,"reply_to_id");
            boolean edited = bool(m,"is_edited");
            JSONArray reacts = arr(m,"reactions");

            LocalDateTime ts = parseWhen(whenIso);
            if (ts == null) ts = LocalDateTime.now();

            // اندیس پیام برای ریپلای/ادیت
            msgIndex.put(msgId, m);

            // تشخیص خروجی/ورودی
            String myId = (Session.currentUser != null && Session.currentUser.has("internal_uuid"))
                    ? Session.currentUser.getString("internal_uuid") : "";
            boolean outgoing = hasVal(senderId) && senderId.equalsIgnoreCase(myId);

            // آپدیت لیست چت‌ها (پریویوِ کوتاه)
            String previewText = switch (type.toUpperCase()) {
                case "IMAGE" -> (hasVal(content) ? "🖼️ Photo — " + content : "🖼️ Photo");
                case "AUDIO" -> "🎵 Audio";
                default      -> content;
            };
            updateChatListPreview(chatId, chatType, !outgoing, previewText, type);

            if (!isCurrent) return;

            // 👇 امضای جدیدِ addBubble (با fileUrl/thumbUrl)
            addBubble(outgoing, senderName, type, content, ts, msgId,
                    fwdFrom, fwdBy, replyTo, edited, reacts, fileUrl, thumbUrl);

            if (currentChat != null) markAsRead(currentChat);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateChatListPreview(UUID chatId, String type, boolean incoming, String content, String messageType) {
        var mc = MainController.getInstance();
        if (mc == null) return;
        String preview;
        switch ((messageType == null ? "" : messageType.toUpperCase())) {
            case "IMAGE" -> preview = (incoming ? "" : "You: ") + "[Image]";
            case "AUDIO" -> preview = (incoming ? "" : "You: ") + "[Audio]";
            case "VIDEO" -> preview = (incoming ? "" : "You: ") + "[Video]";
            case "FILE"  -> preview = (incoming ? "" : "You: ") + "[File]";
            default      -> preview = (incoming ? "" : "You: ") + (content == null || content.isBlank() ? "[Message]" : content);
        }
        mc.onChatUpdated(chatId, type, LocalDateTime.now(), incoming, preview);
    }

    public void onRealTimeReaction(JSONObject ev) {
        String msgId = str(ev, "message_id");
        if (!hasVal(msgId)) return;

        // 1) ایندکس را به‌روزرسانی کن
        JSONObject idx = msgIndex.getOrDefault(msgId, new JSONObject().put("message_id", msgId));

        // اگر «counts» اومد (map emoji→count)، به آرایه تبدیل کن
        JSONArray reactions = ev.optJSONArray("reactions");
        if (reactions == null) {
            JSONObject counts = ev.optJSONObject("counts");
            if (counts != null) {
                reactions = new JSONArray();
                for (String key : counts.keySet()) {
                    reactions.put(new JSONObject()
                            .put("emoji", key)
                            .put("count", counts.optInt(key, 0))
                    );
                }
            } else if (ev.has("emoji")) {
                reactions = new JSONArray().put(new JSONObject()
                        .put("emoji", ev.optString("emoji","👍"))
                        .put("count", ev.optInt("count", 1)));
            }
        }
        if (reactions != null) {
            idx.put("reactions", reactions);
            msgIndex.put(msgId, idx);
        }

        // 2) اگر حبابش روی صفحه هست، فقط نوار ری‌اکشن را عوض کن
        Node row = messageNodes.get(msgId);
        if (!(row instanceof HBox hbox)) return;

        for (Node child : hbox.getChildren()) {
            if (child instanceof VBox bubble && msgId.equals(bubble.getProperties().get("messageId"))) {
                Node oldBar = null;
                for (Node bch : bubble.getChildren()) {
                    if ("reactionsBar".equals(bch.getProperties().get("role"))) { oldBar = bch; break; }
                }
                if (oldBar != null) bubble.getChildren().remove(oldBar);

                JSONArray rx = reactions != null ? reactions : idx.optJSONArray("reactions");
                if (rx != null && rx.length() > 0) {
                    boolean dark = themeManager.isDarkMode();
                    Node newBar = buildReactionsBarFromJson(rx, dark);
                    newBar.getProperties().put("role", "reactionsBar");
                    bubble.getChildren().add(newBar);
                }
                break;
            }
        }
    }

    public void onRealTimeMessageEdited(JSONObject ev) {
        String msgId = str(ev, "message_id");
        if (!hasVal(msgId)) return;

        String newContent = str(ev, "new_content");

        // ایندکس
        JSONObject idx = msgIndex.getOrDefault(msgId, new JSONObject().put("message_id", msgId));
        if (hasVal(newContent)) idx.put("content", newContent);
        idx.put("is_edited", true);
        msgIndex.put(msgId, idx);

        // UI
        Node row = messageNodes.get(msgId);
        if (!(row instanceof HBox hbox)) return;

        for (Node child : hbox.getChildren()) {
            if (child instanceof VBox bubble && msgId.equals(bubble.getProperties().get("messageId"))) {
                for (Node bch : bubble.getChildren()) {
                    Object role = bch.getProperties().get("role");
                    if ("msgLabel".equals(role) && bch instanceof Label lbl && hasVal(newContent)) {
                        lbl.setText(newContent);
                    }
                    if ("metaLabel".equals(role) && bch instanceof Label meta) {
                        String t = meta.getText();
                        if (t != null && !t.contains("(edited)")) meta.setText(t + " (edited)");
                    }
                }
                break;
            }
        }
    }

    public void onRealTimeMessageDeleted(JSONObject ev) {
        String msgId = str(ev, "message_id");
        if (!hasVal(msgId)) return;

        Node n = messageNodes.remove(msgId);
        if (n != null) messageContainer.getChildren().remove(n);

        msgIndex.remove(msgId);
    }



    private void fetchAndRenderHeader(ChatEntry entry) {
        JSONObject req = new JSONObject();
        req.put("action", "get_header_info");
        req.put("receiver_id", entry.getId().toString());
        req.put("receiver_type", entry.getType()); // باید "private" باشه

        String viewer = Session.getUserUUID(); // internal_uuid کاربر فعلی
        if (viewer != null && !viewer.isBlank()) {
            req.put("viewer_id", viewer);
        }

//        // 👇 اضافه کن: آی‌دی کاربر فعلی (current user)
//        UUID viewerId = UUID.fromString(Session.getUserUUID()); // هر جایی که نگه می‌داری
//        if ("private".equalsIgnoreCase(entry.getType()) && viewerId != null) {
//            req.put("viewer_id", viewerId.toString());
//        }

        new Thread(() -> {
            JSONObject resp;
            try {
                resp = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);
            } catch (Exception ex) { ex.printStackTrace(); return; }

            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                System.err.println("get_header_info failed: " + (resp != null ? resp.optString("message") : "null resp"));
                return;
            }
            JSONObject data = resp.optJSONObject("data");
            if (data == null) return;

            Platform.runLater(() -> renderHeaderFromData(entry, data));
        }).start();
    }

    private void renderHeaderFromData(ChatEntry entry, JSONObject data) {
        String t = entry.getType() == null ? "" : entry.getType().toLowerCase();
        switch (t) {
            case "private" -> updatePrivateHeader(entry, data);
            case "group"   -> updateGroupHeader(entry, data);
            case "channel" -> updateChannelHeader(entry, data);
            default        -> chatStatus.setText("");
        }
    }

//    private void updatePrivateHeader(ChatEntry entry, JSONObject data) {
//        String name = nz(data.optString("profile_name", entry.getName()));
//        chatTitle.setText(name);
//
//        // other_user_id برای ریل‌تایم status
//        String other = data.optString("other_user_id", "");
//        if (!other.isBlank()) {
//            try { entry.setOtherUserId(java.util.UUID.fromString(other)); } catch (Exception ignore) {}
//        }
//
//        // تصویر
//        String img = data.optString("image_url", "");
//        if (hasVal(img)) {
//            try {
//                Image im = AvatarLocalResolver.load(img);  // ⬅️
//                if (im != null) userAvatar.setImage(im);
//                userAvatar.setClip(new Circle(20, 20, 20));
//            } catch (Exception ignore) {}
//        }
//
//        chatStatus.setText(userStatusText(
//                data.optBoolean("online", false),
//                data.optString("last_seen", null)
//        ));
//
//    }

    private void updatePrivateHeader(ChatEntry entry, JSONObject data) {
        String name = nz(data.optString("profile_name", entry.getName()));
        chatTitle.setText(name);

        String other = data.optString("other_user_id", "");
        if (!other.isBlank()) {
            try { entry.setOtherUserId(UUID.fromString(other)); } catch (Exception ignore) {}
        }

        String img = data.optString("image_url", "");
        if (hasVal(img)) {
            try {
                Image im = AvatarLocalResolver.load(img);
                if (im != null) userAvatar.setImage(im);
                userAvatar.setClip(new Circle(20, 20, 20));
            } catch (Exception ignore) {}
        }

        chatStatus.setText(userStatusText(
                data.optBoolean("online", false),
                data.optString("last_seen", null)
        ));

        // ❌ هیچ applyMode اینجا نزن!
        // حتی اگر blocked آمد، تصمیم مود از بیرون می‌آید.
    }



    private void updateGroupHeader(ChatEntry entry, JSONObject data) {
        chatTitle.setText(nz(data.optString("group_name", entry.getName())));

        String img = data.optString("image_url", "");
        if (hasVal(img)) {
            try {
                Image im = AvatarLocalResolver.load(img);
                if (im != null) userAvatar.setImage(im);
                userAvatar.setClip(new Circle(20, 20, 20));
            } catch (Exception ignore) {}
        }

        int members = data.optInt("member_count", 0);
        int online  = data.optInt("online_count", -1);
        chatStatus.setText(online >= 0 ? (members + " members, " + online + " online")
                : (members + " members"));

        // ❌ هیچ applyMode اینجا نزن!
    }

//    private void updateChannelHeader(ChatEntry entry, JSONObject data) {
//        chatTitle.setText(nz(data.optString("channel_name", entry.getName())));
//
//        String img = data.optString("image_url", "");
//        if (hasVal(img)) {
//            try {
//                Image im = AvatarLocalResolver.load(img);  // ⬅️
//                if (im != null) userAvatar.setImage(im);
//                userAvatar.setClip(new Circle(20, 20, 20));
//            } catch (Exception ignore) {}
//        }
//
//
//        int subs = data.optInt("member_count", 0);
//        chatStatus.setText(subs + " subscribers");
//    }


    private void updateChannelHeader(ChatEntry entry, JSONObject data) {
        chatTitle.setText(nz(data.optString("channel_name", entry.getName())));

        String img = data.optString("image_url", "");
        if (hasVal(img)) {
            try {
                Image im = AvatarLocalResolver.load(img);
                if (im != null) userAvatar.setImage(im);
                userAvatar.setClip(new Circle(20, 20, 20));
            } catch (Exception ignore) {}
        }

        int subs = data.optInt("member_count", 0);
        chatStatus.setText(subs + " subscribers");

        // ❌ هیچ applyMode اینجا نزن!
        // حتی اگر can_post را بده، به مود دست نزن.
    }



    public void onUserStatusChanged(String userUuid, String status, String lastSeenIso) {
        if (currentChat == null || !"private".equalsIgnoreCase(currentChat.getType())) return;
        var other = currentChat.getOtherUserId();
        if (other == null || !other.toString().equalsIgnoreCase(userUuid)) return;

        if ("online".equalsIgnoreCase(status)) {
            chatStatus.setText("online");
        } else {
            if (hasVal(lastSeenIso)) {
                var ts = parseWhen(lastSeenIso);
                chatStatus.setText(ts != null ? ("last seen " + formatWhen(ts)) : "last seen recently");
            } else {
                chatStatus.setText("last seen recently");
            }
        }
    }



    public String userStatusText(boolean online, String lastSeenIso) {
        if (online) return "online";

        LocalDateTime ts = parseWhen(lastSeenIso);
        if (ts == null) return "last seen recently";

        LocalDate today = LocalDate.now();
        LocalDate d = ts.toLocalDate();

        if (d.isEqual(today)) {
            return FMT_HHMM.format(ts);
        }

        long days = ChronoUnit.DAYS.between(d, today);
        if (days > 30) {
            return "Last seen long time ago";
        }
        return "last seen recently";
    }

    private void setDefaultHeaderAvatarByType(String type){
        String path = switch (type == null ? "" : type.toLowerCase()) {
            case "channel" -> "/org/to/telegramfinalproject/Avatars/default_channel_profile.png";
            case "group"   -> "/org/to/telegramfinalproject/Avatars/default_group_profile.png";
            default        -> "/org/to/telegramfinalproject/Avatars/default_user_profile.png";
        };
        userAvatar.setImage(new Image(
                java.util.Objects.requireNonNull(getClass().getResourceAsStream(path))
        ));
    }

//    @FXML
//    private void onJoinClicked() {
//        if (currentChat == null) return;
//
//        // 1) internal_uuid کاربر فعلی (UUID)
//        String myInternalUuid = Session.currentUser != null
//                ? Session.currentUser.optString("internal_uuid", "")
//                : "";
//        if (myInternalUuid.isBlank()) {
//            addSystemMessage("Join failed: missing current user internal_uuid.");
//            return;
//        }
//
//        // 2) internal_uuid مقصد (گروه/کانال)
//        String targetId = currentChat.getId().toString();
//
//        // 3) نوع و نام اکشن
//        String t = currentChat.getType();
//        String action = "group".equalsIgnoreCase(t) ? "join_group" : "join_channel";
//
//        // 4) درخواست طبق قرارداد سرور (کلیدها: user_id = UUID کاربر، id = UUID مقصد)
//        JSONObject req = new JSONObject()
//                .put("action", action)
//                .put("user_id", myInternalUuid) // ← UUID
//                .put("id",       targetId);     // ← UUID گروه/کانال
//
//        JSONObject res = ActionHandler.sendWithResponse(req);
//        if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
//            MainController.getInstance().onJoinedOrAdded(currentChat);
//            applyMode(ChatViewMode.NORMAL);
//            Platform.runLater(() -> messageInput.requestFocus());
//        } else {
//            addSystemMessage("Join failed: " + (res != null ? res.optString("message","") : "no response"));
//        }
//    }

    @FXML
    private void onJoinClicked() {
        if (currentChat == null) return;

        String myInternalUuid = Session.currentUser != null
                ? Session.currentUser.optString("internal_uuid", "")
                : "";
        if (myInternalUuid.isBlank()) {
            addSystemMessage("Join failed: missing current user internal_uuid.");
            return;
        }

        final String targetId = currentChat.getId().toString();
        final String t = currentChat.getType();
        final String action = "group".equalsIgnoreCase(t) ? "join_group" : "join_channel";

        JSONObject req = new JSONObject()
                .put("action", action)
                .put("user_id", myInternalUuid) // UUID من
                .put("id", targetId);           // UUID مقصد

        new Thread(() -> {
            JSONObject res = ActionHandler.sendWithResponse(req);
            boolean ok = (res != null && "success".equalsIgnoreCase(res.optString("status")));

            Platform.runLater(() -> {
                if (!ok) {
                    addSystemMessage("Join failed: " + (res != null ? res.optString("message","") : "no response"));
                    return;
                }

                // برای جلوگیری از داون‌گِرید مود توسط هِدرِ بعدی:
                justJoinedThisChat = true;

                // اگر MainController نال نبود، لیست چت‌ها را آپدیت کن
                var mc = MainController.getInstance();
                if (mc != null) {
                    mc.onJoinedOrAdded(currentChat);
                }

                // ⚠️ منطق: گروه → همیشه Composer باز شود.
                //        کانال → اگر اجازه‌ی پست داری، Composer؛ وگرنه READ_ONLY با پیام.
//                if ("group".equalsIgnoreCase(t)) {
//                    applyMode(ChatViewMode.NORMAL);
//                    messageInput.requestFocus();
//                } else if ("channel".equalsIgnoreCase(t)) {
//                    // اگر می‌خواهی «همان‌جا» Composer فعال شود، باید اجازه‌ی پست را
//                    // یا از سرور بگیری یا لوکال ست کنی (طبق بیزینس‌لاک‌ت).
//                    // این‌جا منطقی‌تر: فقط اگر واقعاً اجازه داری.
//                    boolean canPost =
//                            (currentChat.isOwner() || currentChat.isAdmin()) ||
//                                    (currentChat.getPermissions()!=null && currentChat.getPermissions().optBoolean("can_post", false));
//                    if (canPost) {
//                        applyMode(ChatViewMode.NORMAL);
//                        messageInput.requestFocus();
//                    } else {
//                        if (readOnlyLabel != null)
//                            readOnlyLabel.setText("YOU CAN’T SEND MESSAGES IN THIS CHANNEL");
//                        applyMode(ChatViewMode.READ_ONLY);
//                    }
//                } else {
//                    applyMode(ChatViewMode.NORMAL);
//                    messageInput.requestFocus();
//                }

                if ("group".equalsIgnoreCase(t)) {
                    applyMode(ChatViewMode.NORMAL);
                } else if ("channel".equalsIgnoreCase(t)) {
                    // تصمیم بیزینسی: ثبت کن.
                    applyMode(ChatViewMode.READ_ONLY);  // یا NORMAL اگر همین را می‌خواهی
                }

                // هدر را دوباره بگیر (ولی نگذار مود را خراب کند)
                fetchAndRenderHeader(currentChat);
            });
        }).start();
    }

//
//    @FXML
//    private void onAddContactClicked() {
//        if (currentChat == null) return;
//
//        String myUserId = Session.currentUser != null
//                ? Session.currentUser.optString("user_id", "")
//                : "";
//
//        // 2) internal_uuid طرف مقابل
//        UUID other = currentChat.getOtherUserId();
//        if (other == null) {
//            // اگر otherUserId هنوز نگرفته‌ای، بهتره قبلش از هدر/پروفایل بیاری.
//            addSystemMessage("Cannot add: other user UUID is missing.");
//            return;
//        }
//
//        // 3) درخواست طبق قرارداد سرور
//        JSONObject req = new JSONObject()
//                .put("action", "add_contact")
//                .put("user_id", myUserId)            // ← stringِ user_id (غیر UUID)
//                .put("contact_id", other.toString()); // ← UUID طرف مقابل
//
//        // 4) ارسال
//        JSONObject res = ActionHandler.sendWithResponse(req);
//        if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
//            // به لیست چت‌ها اضافه و سوییچ به حالت نرمال
//            MainController.getInstance().onJoinedOrAdded(currentChat);
//            applyMode(ChatViewMode.NORMAL);
//            Platform.runLater(() -> messageInput.requestFocus());
//        } else {
//            addSystemMessage("Add contact failed: " + (res != null ? res.optString("message","") : "no response"));
//        }
//    }


    @FXML
    private void onAddContactClicked() {
        if (currentChat == null) return;

        // user_id من (همان string که سرور انتظار دارد)
        String myUserId = Session.currentUser != null
                ? Session.currentUser.optString("user_id", "")
                : "";
        if (myUserId.isBlank()) {
            addSystemMessage("Add contact failed: missing user_id.");
            return;
        }

        // UUID طرف مقابل (از هدر آمده)
        UUID other = currentChat.getOtherUserId();
        if (other == null) {
            addSystemMessage("Cannot add: other user UUID is missing.");
            return;
        }

        // درخواست به سرور
        JSONObject req = new JSONObject()
                .put("action", "add_contact")
                .put("user_id", myUserId)
                .put("contact_id", other.toString());

        new Thread(() -> {
            JSONObject res = ActionHandler.sendWithResponse(req);
            boolean ok = (res != null && "success".equalsIgnoreCase(res.optString("status")));

            Platform.runLater(() -> {
                if (!ok) {
                    addSystemMessage("Add contact failed: " + (res != null ? res.optString("message","") : "no response"));
                    return;
                }

                // ✅ فقط به کانتکت‌های سشن اضافه کن (لوکالی)
                try {
                    // اگر مدل ContactEntry داری از همان استفاده کن
                    // این یک نمونه‌ی امن برای پر کردن حداقل فیلدهاست
                    org.to.telegramfinalproject.Models.ContactEntry ce =
                            new org.to.telegramfinalproject.Models.ContactEntry(
                                    other,                                 // contact_id (UUID)
                                    currentChat.getDisplayId(),            // contact_display_id / user_id دیدنی
                                    currentChat.getDisplayId(),            // هر دو اگر یکی داری
                                    nz(chatTitle.getText()),               // نام نمایشی
                                    currentChat.getImageUrl(),             // آواتار (اگر هست)
                                    false,                                 // is_blocked
                                    null                                   // last_seen
                            );

                    if (Session.contactEntries == null)
                        Session.contactEntries = new java.util.ArrayList<>();

                    boolean exists = Session.contactEntries.stream()
                            .anyMatch(c -> other.equals(c.getContactId()));
                    if (!exists) Session.contactEntries.add(ce);
                } catch (Exception ignore) {}


                showOpenFromContactsHint();
            });
        }).start();
    }


//    private void applyMode(ChatViewMode mode) {
//        currentMode = mode;
//
//        boolean normal    = (mode == ChatViewMode.NORMAL);
//        boolean needsJoin = (mode == ChatViewMode.NEEDS_JOIN);
//        boolean needsAdd  = (mode == ChatViewMode.NEEDS_ADD_CONTACT);
//
//        composerPane.setVisible(normal);
//        composerPane.setManaged(normal);
//
//        joinPane.setVisible(needsJoin);
//        joinPane.setManaged(needsJoin);
//
//        addContactPane.setVisible(needsAdd);
//        addContactPane.setManaged(needsAdd);
//
//        if (needsJoin && joinButton != null && currentChat != null) {
//            String what = "channel".equalsIgnoreCase(currentChat.getType()) ? "CHANNEL" : "GROUP";
//            joinButton.setText(("Join " + what).toUpperCase());   // => JOIN CHANNEL / JOIN GROUP
//        }
//        if (needsAdd && addContactButton != null) {
//            addContactButton.setText("ADD CONTACT");
//        }
//    }



//    private void applyMode(ChatViewMode mode) {
//        currentMode = mode;
//
//        boolean normal    = (mode == ChatViewMode.NORMAL);
//        boolean needsJoin = (mode == ChatViewMode.NEEDS_JOIN);
//        boolean needsAdd  = (mode == ChatViewMode.NEEDS_ADD_CONTACT);
//        boolean readOnly  = (mode == ChatViewMode.READ_ONLY);
//        boolean blocked   = (mode == ChatViewMode.BLOCKED);
//
//        // Composer فقط در حالت نرمال
//        composerPane.setVisible(normal);
//        composerPane.setManaged(normal);
//
//        // Join / Add
//        joinPane.setVisible(needsJoin);
//        joinPane.setManaged(needsJoin);
//        addContactPane.setVisible(needsAdd);
//        addContactPane.setManaged(needsAdd);
//
//        // پنل پایین برای READ_ONLY/BLOCKED
//        boolean showRO = readOnly || blocked;
//        if (readOnlyPane != null) {
//            readOnlyPane.setVisible(showRO);
//            readOnlyPane.setManaged(showRO);
//        }
//
//        // متن آبی برای READ_ONLY
//        if (readOnlyLabel != null) {
//            readOnlyLabel.setVisible(readOnly);
//            readOnlyLabel.setManaged(readOnly);
//        }
//
//        // دکمهٔ قرمز UNBLOCK فقط در BLOCKED
//        if (unblockBtn != null) {
//            unblockBtn.setVisible(blocked);
//            unblockBtn.setManaged(blocked);
//        }
//
//        // متن دکمه‌های Join/Add
//        if (needsJoin && joinButton != null && currentChat != null) {
//            String what = "channel".equalsIgnoreCase(currentChat.getType()) ? "CHANNEL" : "GROUP";
//            joinButton.setText(("Join " + what).toUpperCase());
//        }
//        if (needsAdd && addContactButton != null) {
//            addContactButton.setText("ADD CONTACT");
//        }
//    }


    private void applyMode(ChatViewMode mode) {
        currentMode = mode;

        boolean normal    = (mode == ChatViewMode.NORMAL);
        boolean needsJoin = (mode == ChatViewMode.NEEDS_JOIN);
        boolean needsAdd  = (mode == ChatViewMode.NEEDS_ADD_CONTACT);
        boolean readOnly  = (mode == ChatViewMode.READ_ONLY);
        boolean blocked   = (mode == ChatViewMode.BLOCKED);

        // فقط در حالت نرمال: کامپوزر
        composerPane.setVisible(normal);
        composerPane.setManaged(normal);

        // پنل‌های Join / Add
        joinPane.setVisible(needsJoin);
        joinPane.setManaged(needsJoin);
        addContactPane.setVisible(needsAdd);
        addContactPane.setManaged(needsAdd);

        // پنل‌های پایین
        if (readOnlyPane != null) {
            readOnlyPane.setVisible(readOnly);
            readOnlyPane.setManaged(readOnly);
        }
        if (blockedPane != null) {
            blockedPane.setVisible(blocked);
            blockedPane.setManaged(blocked);
        }

        // متن دکمه‌ها
        if (needsJoin && joinButton != null && currentChat != null) {
            String what = "channel".equalsIgnoreCase(currentChat.getType()) ? "CHANNEL" : "GROUP";
            joinButton.setText(("Join " + what).toUpperCase());
        }
        if (needsAdd && addContactButton != null) {
            addContactButton.setText("ADD CONTACT");
        }
    }


    @FXML
    private void onUnblockClicked() {
        if (currentChat == null) return;

        final String viewerUuid = org.to.telegramfinalproject.Client.Session.getUserUUID(); // internal_uuid
        if (viewerUuid == null || viewerUuid.isBlank()) {
            addSystemMessage("Missing viewer UUID");
            return;
        }

        // شبکه روی بک‌گراند
        new Thread(() -> {
            // 1) اگر otherUserId نداشتیم، از سرور بگیر
            java.util.UUID other = currentChat.getOtherUserId();
            if (other == null) {
                other = resolvePeerUuidFromServer(currentChat);
            }
            if (other == null) {
                Platform.runLater(() -> addSystemMessage("Could not resolve peer UUID."));
                return;
            }

            // 2) حالا درخواست آن‌بلاک
            org.json.JSONObject req = new org.json.JSONObject()
                    .put("action", "toggle_block")
                    .put("user_id", viewerUuid)        // internal_uuid خودت
                    .put("target_id", other.toString()); // internal_uuid طرف مقابل

            org.json.JSONObject res = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);
            boolean ok = res != null && "success".equalsIgnoreCase(res.optString("status"));

            Platform.runLater(() -> {
                if (ok) {
                    applyMode(ChatViewMode.NORMAL);
                    messageInput.requestFocus();
                } else {
                    addSystemMessage("Unblock failed: " + (res != null ? res.optString("message","") : ""));
                }
            });
        }).start();
    }



    private boolean canPostToChannel(ChatEntry entry, JSONObject headerData) {
        // 1) اگر سرور صراحتاً can_post داد، همان را بگیر
        if (headerData != null && headerData.has("can_post")) {
            return headerData.optBoolean("can_post", false);
        }
        // 2) یا اگر is_owner / is_admin را داد
        if (headerData != null && (headerData.has("is_owner") || headerData.has("is_admin"))) {
            return headerData.optBoolean("is_owner", false) || headerData.optBoolean("is_admin", false);
        }
        // 3) فال‌بک به اطلاعات لوکال: ChatEntry + permissions محلی
        if (entry != null) {
            if (entry.isOwner() || entry.isAdmin()) return true;
            if (entry.getPermissions() != null && entry.getPermissions().optBoolean("can_post", false)) {
                return true;
            }
        }
        return false;
    }



    private void reactToMessage(String msgId, String emoji) {
        JSONObject req = new JSONObject()
                .put("action", "react_to_message")
                .put("message_id", msgId)
                .put("reaction", emoji);

        new Thread(() -> {
            JSONObject res = ActionHandler.sendWithResponse(req);
            Platform.runLater(() -> {
                if (res == null || !"success".equalsIgnoreCase(res.optString("status"))) {
                    addSystemMessage("Failed to react.");
                } else {
                    // ساده‌ترین کار: رفرش
                    loadMessages(currentChat);
                }
            });
        }).start();
    }

    private void startReply(String msgId) {
        pendingReplyToId = msgId;
        // یک پریویو کوچیک بالای TextArea نشان بده (می‌تونی از buildReplyBoxFromIndex استفاده کنی)
        var preview = buildReplyBoxFromIndex(msgId);
        if (!composerPane.getChildren().contains(preview)) {
            composerPane.getChildren().add(0, preview);
        }
        messageInput.requestFocus();
    }

    private void startForward(String originalMsgId) {
        openForwardPickerFromSession(originalMsgId);
    }

    private void openForwardPickerFromSession(String originalMsgId) {
        java.util.List<ForwardTarget> targets = fetchForwardTargetsFromSession();

        // اگر نخواستی به همین چت فعلی هم اجازه بدی، حذفش کن:
        if (currentChat != null) {
            targets.removeIf(t ->
                    t.id.equals(currentChat.getId()) &&
                            t.type.equalsIgnoreCase(currentChat.getType())
            );
        }

        Dialog<ForwardTarget> dialog = new Dialog<>();
        dialog.setTitle("Forward message");
        if (messageContainer != null && messageContainer.getScene() != null) {
            dialog.initOwner(messageContainer.getScene().getWindow());
        }

        ButtonType btnSend = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(btnSend, btnCancel);

        // آیکن/گرافیک (اگر آیکن forward داری)
        try {
            var iv = new ImageView(new Image(
                    getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/ic_forward.png")
            ));
            iv.setFitWidth(18); iv.setFitHeight(18);
            dialog.getDialogPane().setGraphic(iv);
        } catch (Exception ignore) {}

        TextField search = new TextField();
        search.setPromptText("Search chats…");

        var base = javafx.collections.FXCollections.observableArrayList(targets);
        var filtered = new javafx.collections.transformation.FilteredList<>(base, _x -> true);

        search.textProperty().addListener((obs, ov, nv) -> {
            String q = nv == null ? "" : nv.trim().toLowerCase();
            filtered.setPredicate(t -> {
                if (q.isEmpty()) return true;
                return t.name.toLowerCase().contains(q) || t.type.toLowerCase().contains(q);
            });
        });

        ListView<ForwardTarget> listView = new ListView<>(filtered);
        listView.setPrefHeight(360);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final HBox root = new HBox(10);
            private final ImageView avatar = new ImageView();
            private final VBox texts = new VBox(2);
            private final Label title = new Label();
            private final Label subtitle = new Label();

            {
                avatar.setFitWidth(28);
                avatar.setFitHeight(28);
                root.setAlignment(Pos.CENTER_LEFT);
                subtitle.setStyle("-fx-font-size: 11; -fx-text-fill: #7e8a97;");
                texts.getChildren().addAll(title, subtitle);
                root.getChildren().addAll(avatar, texts);
            }

            @Override protected void updateItem(ForwardTarget item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Image img = null;
                    if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                        img = org.to.telegramfinalproject.Client.AvatarLocalResolver.load(item.imageUrl);
                    }
                    if (img == null) {
                        String fallback = switch (item.type.toLowerCase()) {
                            case "channel" -> "/org/to/telegramfinalproject/Avatars/default_channel_profile.png";
                            case "group"   -> "/org/to/telegramfinalproject/Avatars/default_group_profile.png";
                            default        -> "/org/to/telegramfinalproject/Avatars/default_user_profile.png";
                        };
                        img = new Image(getClass().getResourceAsStream(fallback));
                    }
                    avatar.setImage(img);

                    title.setText(item.name.isBlank() ? item.id.toString() : item.name);
                    subtitle.setText(item.type.toUpperCase());

                    setGraphic(root);
                }
            }
        });

        dialog.setOnShown(ev -> {
            Button sendBtn = (Button) dialog.getDialogPane().lookupButton(btnSend);
            sendBtn.setDisable(true);
            listView.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
                sendBtn.setDisable(nv == null);
            });
            listView.setOnMouseClicked(me -> {
                if (me.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                    sendBtn.fire();
                }
            });
        });

        VBox content = new VBox(10, search, listView);
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(bt -> {
            if (bt == btnSend) return listView.getSelectionModel().getSelectedItem();
            return null;
        });

        var result = dialog.showAndWait();
        result.ifPresent(target -> forwardToTarget(originalMsgId, target));
    }


    private void forwardToTarget(String originalMsgId, ForwardTarget target) {
        if (target == null) return;

        // (اختیاری) قبل از ارسال، محدودیت‌ها را چک کن
        // مثلا کانال‌هایی که اجازه‌ی پست نداری:
        // if ("channel".equalsIgnoreCase(target.type) && !/*canPost*/ false) { addSystemMessage("You can’t post to this channel."); return; }

        JSONObject req = new JSONObject()
                .put("action", "forward_message")
                .put("original_message_id", originalMsgId)
                .put("target_chat_id", target.id.toString())
                .put("target_chat_type", target.type);

        new Thread(() -> {
            JSONObject res = ActionHandler.sendWithResponse(req);
            Platform.runLater(() -> {
                if (res == null || !"success".equalsIgnoreCase(res.optString("status"))) {
                    addSystemMessage("Forward failed: " + (res == null ? "" : res.optString("message","")));
                } else {
                    // اگر مقصد همین چت بود، لیست پیام‌ها را رفرش کن
                    if (currentChat != null &&
                            currentChat.getId().equals(target.id) &&
                            currentChat.getType().equalsIgnoreCase(target.type)) {
                        loadMessages(currentChat);
                    } else {
//                        addSystemMessage("Forwarded to " + (target.name.isBlank() ? target.id : target.name));
                    }
                }
            });
        }).start();
    }


    private void startEdit(String msgId, String currentText) {
        pendingEditMsgId = msgId;
        messageInput.setText(currentText == null ? "" : currentText);
        messageInput.requestFocus();
        messageInput.positionCaret(messageInput.getText().length());
    }
    private void confirmDelete(String msgId) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setHeaderText("Delete message?");
        ButtonType onlyMe = new ButtonType("Delete for me");
        ButtonType everyone = new ButtonType("Delete for everyone");
        ButtonType cancel = ButtonType.CANCEL;

        // نمایش «Delete for everyone» فقط اگر منطقی به‌نظر می‌رسد
        boolean showGlobal = true; // ساده: بذار سرور رد کند اگر مجاز نیست
        if (showGlobal) a.getButtonTypes().setAll(onlyMe, everyone, cancel);
        else            a.getButtonTypes().setAll(onlyMe, cancel);

        a.showAndWait().ifPresent(btn -> {
            if (btn == onlyMe) deleteMessage(msgId, "one-sided");
            else if (btn == everyone) deleteMessage(msgId, "global");
        });
    }

    private void deleteMessage(String msgId, String deleteType) {
        JSONObject req = new JSONObject()
                .put("action", "delete_message")
                .put("message_id", msgId)
                .put("delete_type", deleteType);

        new Thread(() -> {
            JSONObject res = ActionHandler.sendWithResponse(req);
            Platform.runLater(() -> {
                if (res == null || !"success".equalsIgnoreCase(res.optString("status"))) {
                    addSystemMessage("Delete failed: " + (res==null?"":res.optString("message")));
                    return;
                }
                // one-sided: سریعاً از UI حذف کن
                if ("one-sided".equals(deleteType)) {
                    Node n = messageNodes.remove(msgId);
                    if (n != null) messageContainer.getChildren().remove(n);
                } else {
                    // global: سرور RT می‌فرستد، اما برای UX می‌توانی رفرش کنی
                    loadMessages(currentChat);
                }
            });
        }).start();
    }


    // پیام مالِ من است؟
    private boolean isOutgoingMessage(String messageId) {
        if (Session.currentUser == null || !Session.currentUser.has("internal_uuid")) return false;
        String meId = Session.currentUser.optString("internal_uuid", "");
        JSONObject m = msgIndex.get(messageId);
        if (m == null) return false;
        return meId.equalsIgnoreCase(m.optString("sender_id", ""));
    }

    // در کانال می‌تونم global حذف کنم؟
    private boolean canDeleteInChannel() {
        if (currentChat == null) return false;
        if (currentChat.isOwner() || currentChat.isAdmin()) return true;
        return currentChat.getPermissions()!=null &&
                currentChat.getPermissions().optBoolean("can_delete", false);
    }




    private void confirmDeleteDialog(String messageId) {
        boolean outgoing = isOutgoingMessage(messageId);
        String t = currentChat != null ? currentChat.getType() : "";
        boolean canGlobal =
                "private".equalsIgnoreCase(t) || "group".equalsIgnoreCase(t) ? outgoing
                        : "channel".equalsIgnoreCase(t) ? canDeleteInChannel()
                        : false;

        String peerName = (currentChat != null && currentChat.getName()!=null)
                ? currentChat.getName()
                : "everyone";

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Delete message");

        // مالک دیالوگ (اختیاری ولی بهتر)
        if (messageContainer != null && messageContainer.getScene() != null) {
            dialog.initOwner(messageContainer.getScene().getWindow());
        }

        // دکمه‌ها
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType btnDelete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(btnDelete, btnCancel); // Delete اول بیاید

        // عنوان و چک‌باکس
        Label title = new Label("Do you want to delete this message?");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: -fx-text-base-color;");

        CheckBox alsoDelete = new CheckBox("Also delete for " + peerName);
        alsoDelete.setSelected(false);
        alsoDelete.setVisible(canGlobal);
        alsoDelete.setManaged(canGlobal);

        VBox box = new VBox(10, title, alsoDelete);
        box.setPadding(new Insets(12, 12, 6, 12));
        dialog.getDialogPane().setContent(box);

        Image trashImg = new Image(getClass().getResourceAsStream(
                "/org/to/telegramfinalproject/Icons/ic_delete_danger.png"
        ));
        ImageView trashIv = new ImageView(trashImg);
        trashIv.setFitWidth(18);
        trashIv.setFitHeight(18);
        dialog.getDialogPane().setGraphic(trashIv);

        // آیکن خود پنجره (بالا-چپ فریم)
        dialog.getDialogPane().sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                stage.getIcons().setAll(trashImg);
            }
        });

        // کمی استایل
        dialog.getDialogPane().setStyle("""
        -fx-background-radius: 12;
        -fx-background-insets: 0;
        -fx-padding: 8;
    """);


        dialog.setOnShown(ev -> {
            Button btnDel = (Button) dialog.getDialogPane().lookupButton(btnDelete);
            if (btnDel != null) {
                btnDel.getStyleClass().add("tg-btn-danger");
            }
            Button btnCan = (Button) dialog.getDialogPane().lookupButton(btnCancel);
            if (btnCan != null) {
                btnCan.getStyleClass().add("tg-btn-secondary");
            }
        });

        // نمایش و تصمیم
        var res = dialog.showAndWait();
        if (res.isPresent() && res.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String deleteType = (alsoDelete.isSelected() && canGlobal) ? "global" : "one-sided";
            deleteMessage(messageId, deleteType);
        }
    }

    // ChatPageController.java

    public void onChatAvatarUpdated(UUID chatId, String newUrl) {
        // اگه چت فعلی چیز دیگریه، کاری نکن
        if (currentChat == null || chatId == null || newUrl == null || newUrl.isBlank()) return;
        if (!currentChat.getId().equals(chatId)) return;

        // اگر از نخ غیر JavaFX صدا زده شد، امنش کنیم
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> onChatAvatarUpdated(chatId, newUrl));
            return;
        }

        // State داخلی entry را هم آپدیت کن
        currentChat.setImageUrl(newUrl);

        // آواتار هدر را ست کن (با resolver خودت)
        try {
            Image im = org.to.telegramfinalproject.Client.AvatarLocalResolver.load(newUrl);
            if (im != null) {
                userAvatar.setImage(im);
            } else {
                // fallback اگر لود نشد
                setDefaultHeaderAvatarByType(currentChat.getType());
            }
        } catch (Exception ignore) {
            setDefaultHeaderAvatarByType(currentChat.getType());
        }

        // مطمئن شو همچنان دایره‌ایه
        try { AvatarFX.circleClip(userAvatar, 36); } catch (Throwable ignored) {}
    }



    private static final class ForwardTarget {
        final UUID id;            // internal_id
        final String type;        // private | group | channel
        final String name;        // title
        final String imageUrl;    // optional

        ForwardTarget(UUID id, String type, String name, String imageUrl) {
            this.id = id;
            this.type = type == null ? "" : type;
            this.name = name == null ? "" : name;
            this.imageUrl = imageUrl == null ? "" : imageUrl;
        }

        @Override public String toString() {
            return name + " (" + type + ")";
        }
    }


    private java.util.List<ForwardTarget> fetchForwardTargetsFromSession() {
        java.util.LinkedHashMap<String, ForwardTarget> map = new java.util.LinkedHashMap<>();

        org.json.JSONObject cu = org.to.telegramfinalproject.Client.Session.currentUser;
        if (cu == null) return new java.util.ArrayList<>();

        // دو منبع معمول در Session: chat_list و active_chat_list
        org.json.JSONArray[] sources = new org.json.JSONArray[]{
                cu.optJSONArray("chat_list"),
                cu.optJSONArray("active_chat_list")
        };

        for (org.json.JSONArray arr : sources) {
            if (arr == null) continue;
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String internalId = o.optString("internal_id", "");
                String type       = o.optString("type", "");
                String name       = o.optString("name", "");
                String imageUrl   = o.optString("image_url", "");

                if (internalId.isBlank() || type.isBlank()) continue;

                UUID id;
                try { id = java.util.UUID.fromString(internalId); }
                catch (Exception ignore) { continue; }

                ForwardTarget ft = new ForwardTarget(id, type, name, imageUrl);
                // کلید یکتا: id + type
                map.put(id.toString() + "|" + type.toLowerCase(), ft);
            }
        }

        // (اختیاری) Saved Messages اگر داری می‌خوای اضافه کنی، اینجا اضافه کن.

        return new java.util.ArrayList<>(map.values());
    }


    private void showOpenFromContactsHint() {
        if (addContactPane == null) return;

        // پنل را نگه دار، فقط محتوا را عوض کن
        addContactPane.getChildren().clear();

        Label hint = new Label("you should open chat from contact list for first time");
        hint.getStyleClass().add("footer-link-btn"); // همان کلاس CSS دکمه‌ی پایین
        // اگر می‌خواهی شبیه لینک آبی شود و کلیک‌پذیر نباشد:
        hint.setUnderline(true);

        addContactPane.getChildren().add(hint);

        // مطمئن شو فقط همین پانل دیده شود (کامپوزر/بقیه بسته بمانند)
        composerPane.setVisible(false);
        composerPane.setManaged(false);
        joinPane.setVisible(false);
        joinPane.setManaged(false);
        addContactPane.setVisible(true);
        addContactPane.setManaged(true);
    }


    private void syncIconsWithTheme() {
        boolean dark = themeManager.isDarkMode();
        String suffix = dark ? "_light.png" : "_dark.png";

        if (attachmentIcon != null) attachmentIcon.setImage(loadIcon("attachment" + suffix));
        if (sendIcon != null)       sendIcon.setImage(loadIcon("send_cyan2.png")); // always cyan
        if (searchIcon != null)     searchIcon.setImage(loadIcon("search" + suffix));
        if (moreIcon != null)       moreIcon.setImage(loadIcon("more" + suffix));

        if (chatTitle != null)  chatTitle.setStyle(dark ? "-fx-text-fill:#e8f1f8;" : "-fx-text-fill:#0f141a;");
        if (chatStatus != null) chatStatus.setStyle(dark ? "-fx-text-fill:#8ea1b2;" : "-fx-text-fill:#7e8a97;");

        // === Context menu icons ===
        if (archiveItem != null && archiveItem.getGraphic() instanceof ImageView iv) {
            iv.setImage(loadIcon("archived_chats" + suffix));
        }
        if (viewProfileItem != null && viewProfileItem.getGraphic() instanceof ImageView iv) {
            iv.setImage(loadIcon("view_profile" + suffix));
        }
        if (deleteChatItem != null && deleteChatItem.getGraphic() instanceof ImageView iv) {
            iv.setImage(loadIcon("delete_red.png")); // stays red in both themes
        }
    }

    // ChatPageController

    private UUID resolvePeerUuidFromServer(ChatEntry chat) {
        if (chat == null || !"private".equalsIgnoreCase(chat.getType())) return null;

        // اگر از قبل ست شده بود از همون استفاده کن
        try {
            UUID cached = chat.getOtherUserId();
            if (cached != null) return cached;
        } catch (Exception ignore) {}

        // درخواست به سرور برای گرفتن target_id
        org.json.JSONObject req = new org.json.JSONObject()
                .put("action", "get_private_chat_target")
                .put("chat_id", chat.getId().toString());
        // اگر سمت سرور لازم دارد، می‌توانی viewer را هم بفرستی:
        // .put("viewer_id", Session.getUserUUID());

        org.json.JSONObject res = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);
        if (res == null || !"success".equalsIgnoreCase(res.optString("status"))) return null;

        org.json.JSONObject data = res.optJSONObject("data");
        if (data == null) return null;

        String tid = data.optString("target_id", "");
        if (tid == null || tid.isBlank()) return null;

        try {
            java.util.UUID target = java.util.UUID.fromString(tid);
            chat.setOtherUserId(target); // کش محلی کن که دفعات بعد لازم نشه
            return target;
        } catch (Exception ignore) {
            return null;
        }
    }

}