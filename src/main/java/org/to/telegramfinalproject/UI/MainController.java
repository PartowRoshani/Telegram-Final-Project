package org.to.telegramfinalproject.UI;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Client.ActionHandler;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainController {

    // === LEFT PANE ===
    @FXML private VBox chatListContainer;     // inside the scrollPane
    @FXML private ScrollPane scrollPane;      // chat list scroll
    @FXML private VBox chatSearchPane;        // search results panel
    @FXML private ListView<String> chatSearchResults;
    @FXML private MenuButton scopeDropdown;

    // === TOP BAR ===
    @FXML private TextField searchBar;        // global search bar
    @FXML private Button menuButton;

    // === RIGHT PANE ===
    @FXML private VBox leftPane;
    @FXML private javafx.scene.layout.StackPane chatDisplayArea;
    @FXML private Label placeholderLabel;

    // === Main root ===
    @FXML private StackPane mainRoot;
    @FXML private SplitPane mainSplitPane;

    // === Sidebar ===
    @FXML private Pane overlay;
    @FXML private ImageView menuIcon;
    private SidebarMenuController sidebarController; //save sidebar controller

    // === Time formatter for messages ===
    private static final java.time.format.DateTimeFormatter FMT_HHMM =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter FMT_DATE_TIME =
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final String YESTERDAY_LABEL = "Yesterday";

    // === STATE ===
    private static MainController instance;
    private Node sidebarRoot;
    private boolean isSidebarOpen = false;

    // Add ThemeManager handle
    private final ThemeManager themeManager = ThemeManager.getInstance();

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }
    private final Map<UUID, ChatItemController> itemControllers = new HashMap<>();


    @FXML
    public void initialize() {
//        addSampleChats();
        populateChatListFromSession();


        // Register the scene for automatic CSS updates
        Platform.runLater(() -> {
            ThemeManager.getInstance().registerScene(mainRoot.getScene());
        });

        // Listen for theme changes to update icons & labels manually
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // Dark mode ON
                updateIconsForDarkMode();
                updateLabelsForDarkMode();
            } else {
                // Light mode ON
                updateIconsForLightMode();
                updateLabelsForLightMode();
            }
        });

        // Smooth scroll feel
        scrollPane.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003; // smaller = smoother
            scrollPane.setVvalue(scrollPane.getVvalue() - deltaY);
        });

        // ---------- SplitPane limits so panes can't fully collapse ----------
        leftPane.setMinWidth(72);           // avatars-only collapse
        chatDisplayArea.setMinWidth(360);   // chat area never fully collapses

        mainSplitPane.widthProperty().addListener((o, ov, nv) -> clampDivider());
        if (!mainSplitPane.getDividers().isEmpty()) {
            mainSplitPane.getDividers().get(0).positionProperty().addListener((o, ov, nv) -> clampDivider());
        }


        // Search in chat field listener
//        searchBar.textProperty().addListener((obs, oldV, newV) -> {
//            if (!chatSearchPane.isVisible()) return; // only react if in search mode
//
//            if (newV.trim().isEmpty()) {
//                chatSearchResults.setVisible(false);
//                chatSearchResults.setManaged(false);
//            } else {
//                chatSearchResults.setVisible(true);
//                chatSearchResults.setManaged(true);
//                chatSearchResults.getItems().setAll(
//                        "Result 1: " + newV,
//                        "Result 2: " + newV,
//                        "Result 3: " + newV
//                );
//            }
//        });

        // وقتی کاربر Enter زد روی سرچ‌بار، درخواست سرچ بفرست
        searchBar.setOnAction(e -> performGlobalSearch(searchBar.getText().trim()));

        // باز/بسته کردن پنل نتایج با تایپ (دلخواه)
        searchBar.textProperty().addListener((obs,o,n)->{
            if (n!=null && !n.isBlank()) showSearchPanel();
        });

        // کلیک روی نتیجه
        chatSearchResults.setOnMouseClicked(e -> {
            int idx = chatSearchResults.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < searchBacking.size()) {
                openSearchResult(searchBacking.get(idx));
            }
        });

    }

    // Called from ChatPageController when user clicks search button
    public void showSearchPanel() {
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);

        chatSearchPane.setVisible(true);
        chatSearchPane.setManaged(true);

        searchBar.requestFocus();
    }

    @FXML
    public void closeSearchPanel() {
        chatSearchPane.setVisible(false);
        chatSearchPane.setManaged(false);

        scrollPane.setVisible(true);
        scrollPane.setManaged(true);
    }

//    private void addSampleChats() {
//        addChat("Archived Chats", "Your archived chats", "10:45", 0);
//        addChat("Saved Messages", "Keep messages for later", "Yesterday", 0);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//        addChat("Alice", "Hey, how are you?", "14:12", 3);
//        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
//
//
//    }

    // ...
    private String formatChatTime(java.time.LocalDateTime ts) {
        if (ts == null) return "";
        var today = java.time.LocalDate.now();
        var d = ts.toLocalDate();

        if (d.isEqual(today)) {
            //today
            return FMT_HHMM.format(ts);
            //yesterday
        } else if (d.isEqual(today.minusDays(1))) {
            return YESTERDAY_LABEL;
        } else {
            return FMT_DATE_TIME.format(ts);
        }
    }

    private void populateChatListFromSession() {
        chatListContainer.getChildren().clear();

        var list = (org.to.telegramfinalproject.Client.Session.activeChats != null
                && !org.to.telegramfinalproject.Client.Session.activeChats.isEmpty())
                ? org.to.telegramfinalproject.Client.Session.activeChats
                : org.to.telegramfinalproject.Client.Session.chatList;

        if (list == null || list.isEmpty()) return;

        // جدا کردن Saved از بقیه
        ChatEntry saved = null;
        java.util.List<ChatEntry> others = new java.util.ArrayList<>();
        for (ChatEntry c : list) {
            if (saved == null && isSavedMessages(c)) {
                saved = c;
            } else {
                others.add(c);
            }
        }

        // اگر “Archived Chats” یا هدر دیگری داری، قبلش اضافه کن (اختیاری)
        // addArchivedHeaderIfYouHaveOne();

        // 1) همیشه Saved اول بیاد (اگر وجود داشت)
        if (saved != null) {
            addChatNode(saved);
        }

        for (ChatEntry c : others) {
            addChatNode(c);
        }
    }


//    private void addChatNode(ChatEntry chat ) {
//        try {
//            FXMLLoader fx = new FXMLLoader(getClass().getResource(
//                    "/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
//            Node item = fx.load();
//            ChatItemController cc = fx.getController();
//
//            String lastPreview = "";
//            String time = (chat.getLastMessageTime() != null)
//                    ? timeFmt.format(chat.getLastMessageTime()) : "";
//
//            cc.setChatData(chat.getName(), lastPreview, time, 0);
//
//            item.setOnMouseClicked(e -> openChat(String.valueOf(chat)));
//            chatListContainer.getChildren().add(item);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void addChatNode(ChatEntry chat) {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
            Node item = fx.load();
            ChatItemController cc = fx.getController();

            String preview = chat.getLastMessagePreview() == null ? "" : chat.getLastMessagePreview();
            String timeText = formatChatTime(chat.getLastMessageTime());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
            Node chatItem = loader.load();
            ChatItemController controller = loader.getController();

            cc.setChatData(chat.getName(), preview, timeText, chat.getUnreadCount(), "/org/to/telegramfinalproject/Avatars/default_profile.png");
            item.setOnMouseClicked(e -> openChat(chat));
            chatListContainer.getChildren().add(item);

            itemControllers.put(chat.getId(), cc);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String mapTypeToLabel(String t) {
        switch (t.toUpperCase()) {
            case "IMAGE": return "[Image]";
            case "AUDIO": return "[Audio]";
            case "VIDEO": return "[Video]";
            case "FILE":  return "[File]";
            default:      return "[Message]";
        }
    }

//    private void addChat(String name, String lastMsg, String time, int unread) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
//            Node chatItem = loader.load();
//            ChatItemController controller = loader.getController();
//            controller.setChatData(name, lastMsg, time, unread);
//
//            chatItem.setOnMouseClicked(e -> openChat(name));
//            chatListContainer.getChildren().add(chatItem);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void openChat(ChatEntry chat) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_page.fxml"));
            Node chatPage = loader.load();

            ChatPageController controller = loader.getController();
            controller.showChat(chat);

            chatDisplayArea.getChildren().setAll(chatPage);

            ChatItemController item = itemControllers.get(chat.getId());
            if (item != null) item.setUnread(0);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    @FXML
    private void toggleSidebar() {
        if (isSidebarOpen) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }

//    @FXML
//    private void openSidebar() {
//        try {
//            if (sidebarRoot == null) {
//                sidebarRoot = FXMLLoader.load(getClass().getResource("/org/to/telegramfinalproject/Fxml/Sidebar_menu.fxml"));
//                StackPane.setAlignment(sidebarRoot, Pos.CENTER_LEFT);
//                sidebarRoot.setTranslateX(-getSidebarWidth());
//                mainRoot.getChildren().add(sidebarRoot);
//            }
//
//            TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), sidebarRoot);
//            slideIn.setToX(0);
//            slideIn.play();
//
//            overlay.setVisible(true);
//            overlay.setMouseTransparent(false);
//
//            isSidebarOpen = true;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @FXML
    private void openSidebar() {
        try {
            if (sidebarRoot == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/org/to/telegramfinalproject/Fxml/Sidebar_menu.fxml"));
                sidebarRoot = loader.load();
                sidebarController = loader.getController();

                StackPane.setAlignment(sidebarRoot, Pos.CENTER_LEFT);
                sidebarRoot.setTranslateX(-getSidebarWidth());
                mainRoot.getChildren().add(sidebarRoot);
            }

            if (sidebarController != null && org.to.telegramfinalproject.Client.Session.currentUser != null) {
                sidebarController.setUserFromSession(
                        org.to.telegramfinalproject.Client.Session.currentUser);
            }

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), sidebarRoot);
            slideIn.setToX(0);
            slideIn.play();

            overlay.setVisible(true);
            overlay.setMouseTransparent(false);
            isSidebarOpen = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void closeSidebar() {
        if (sidebarRoot != null && isSidebarOpen) {
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(250), sidebarRoot);
            slideOut.setToX(-getSidebarWidth());
            slideOut.setOnFinished(e -> {
                overlay.setVisible(false);
                overlay.setMouseTransparent(true);
                isSidebarOpen = false;
            });
            slideOut.play();
        }
    }

    private double getSidebarWidth() {
        return 250; // Sidebar width in px
    }

    private void clampDivider() {
        if (mainSplitPane.getDividers().isEmpty()) return;

        double totalW = mainSplitPane.getWidth();
        if (totalW <= 0) return;

        double leftMinRatio  = leftPane.getMinWidth() / totalW;
        double rightMinRatio = chatDisplayArea.getMinWidth() / totalW;
        double maxLeftRatio  = 1.0 - rightMinRatio;

        SplitPane.Divider d = mainSplitPane.getDividers().get(0);
        double p = d.getPosition();
        if (p < leftMinRatio) {
            d.setPosition(leftMinRatio);
        } else if (p > maxLeftRatio) {
            d.setPosition(maxLeftRatio);
        }
    }

    private void updateIconsForDarkMode() {
        // Example: switch images to white versions
        menuIcon.setImage(new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/menu_light.png")));
    }

    private void updateIconsForLightMode() {
        // Example: switch images to black versions
        menuIcon.setImage(new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/menu_dark.png")));
    }

    private void updateLabelsForDarkMode() {
        //myLabel.setStyle("-fx-text-fill: white;");
    }

    private void updateLabelsForLightMode() {
        //myLabel.setStyle("-fx-text-fill: black;");
    }





    // ===== Search UI state =====
    private final java.util.List<SearchResult> searchBacking = new java.util.ArrayList<>();

    private enum SRType { USER, GROUP, CHANNEL, MESSAGE }

    private static class SearchResult {
        final SRType type;
        final String title;         // name / sender_name / ...
        final String subtitle;      // @"id" / context string / time
        final String receiverType;  // برای MESSAGE/GROUP/CHANNEL: private|group|channel, برای USER: null
        final java.util.UUID uuid;  // uuid آن موجودیت (user/group/channel/chat holder) یا receiver_id پیام
        final String displayId;     // id قابل نمایش (مثل username یا group_id/channel_id)
        final String messageId;     // فقط برای MESSAGE
        final String time;          // نمایش

        SearchResult(SRType t, String title, String subtitle, String receiverType,
                     java.util.UUID uuid, String displayId, String messageId, String time) {
            this.type = t; this.title = title; this.subtitle = subtitle;
            this.receiverType = receiverType; this.uuid = uuid;
            this.displayId = displayId; this.messageId = messageId; this.time = time;
        }

        String toDisplay() {
            // رشته‌ای که داخل ListView نشان می‌دهیم
            switch (type) {
                case MESSAGE:
                    String left = (title == null || title.isBlank()) ? "Message" : title;
                    String right = (time == null ? "" : (" • " + time));
                    return "🗨 " + left + right + (subtitle==null?"":(" — " + subtitle));
                case USER:    return "👤 " + title + (subtitle==null?"":(" — " + subtitle));
                case GROUP:   return "👥 " + title + (subtitle==null?"":(" — " + subtitle));
                case CHANNEL: return "📣 " + title + (subtitle==null?"":(" — " + subtitle));
            }
            return title;
        }
    }


    public void performGlobalSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            chatSearchResults.getItems().clear();
            searchBacking.clear();
            return;
        }
        if (org.to.telegramfinalproject.Client.Session.currentUser == null ||
                !org.to.telegramfinalproject.Client.Session.currentUser.has("user_id")) {
            System.out.println("You must be logged in to search.");
            return;
        }

        showSearchPanel();

        // درخواست به سرور (مثل کنسول)
        org.json.JSONObject req = new org.json.JSONObject();
        req.put("action", "search");
        req.put("keyword", keyword);
        req.put("user_id", org.to.telegramfinalproject.Client.Session.currentUser.getString("user_id"));

        new Thread(() -> {
            org.json.JSONObject resp;
            try {
                resp = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            if (resp == null || !"success".equals(resp.optString("status"))) return;

            org.json.JSONArray arr = resp.optJSONObject("data").optJSONArray("results");
            if (arr == null) arr = new org.json.JSONArray();

            final java.util.List<SearchResult> tmp = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject it = arr.getJSONObject(i);
                String type = it.optString("type","");

                switch (type) {
                    case "user": {
                        java.util.UUID uuid = java.util.UUID.fromString(it.getString("uuid"));
                        String name = it.optString("name","Unknown");
                        String id   = it.optString("id", "");
                        tmp.add(new SearchResult(
                                SRType.USER, name,
                                id.isBlank()? null : "@"+id,
                                null, uuid, id, null, null
                        ));
                        break;
                    }
                    case "group": {
                        java.util.UUID uuid = java.util.UUID.fromString(it.getString("uuid"));
                        String name = it.optString("name","Unknown group");
                        String id   = it.optString("id", "");
                        tmp.add(new SearchResult(
                                SRType.GROUP, name,
                                id.isBlank()? null : id,
                                "group", uuid, id, null, null
                        ));
                        break;
                    }
                    case "channel": {
                        java.util.UUID uuid = java.util.UUID.fromString(it.getString("uuid"));
                        String name = it.optString("name","Unknown channel");
                        String id   = it.optString("id", "");
                        tmp.add(new SearchResult(
                                SRType.CHANNEL, name,
                                id.isBlank()? null : id,
                                "channel", uuid, id, null, null
                        ));
                        break;
                    }
                    case "message": {
                        String senderName = it.optString("sender_name", it.optString("sender","Unknown"));
                        String time = it.optString("time", "");
                        String content = it.optString("content","[No content]");
                        String rType = it.optString("receiver_type","");        // private|group|channel
                        java.util.UUID rUuid = java.util.UUID.fromString(it.getString("receiver_id"));
                        String ctx = "";
                        if ("group".equals(rType))   ctx = it.optString("group_name","");
                        if ("channel".equals(rType)) ctx = it.optString("channel_name","");
                        String subtitle = (ctx.isBlank()? "" : ctx) + (content.isBlank()? "" : (subtitleSep(ctx)+content));
                        tmp.add(new SearchResult(
                                SRType.MESSAGE, senderName, subtitle, rType, rUuid,
                                it.optString("receiver_display_id", ""),    // اگر داشتی
                                it.optString("message_id", null),
                                time
                        ));
                        break;
                    }
                }
            }

            Platform.runLater(() -> renderSearchResults(tmp));
        }).start();
    }

    private String subtitleSep(String s){ return s==null || s.isBlank()? "" : " — "; }

    private void renderSearchResults(java.util.List<SearchResult> results) {
        searchBacking.clear();
        searchBacking.addAll(results);

        javafx.collections.ObservableList<String> view = javafx.collections.FXCollections.observableArrayList();
        for (SearchResult r : results) view.add(r.toDisplay());
        chatSearchResults.setItems(view);
        chatSearchResults.setVisible(true);
        chatSearchResults.setManaged(true);
    }


    private void openSearchResult(SearchResult r) {
        switch (r.type) {
            case USER: {
                // مثل کنسول: اگر چت private با این یوزر داریم بازش کن؛
                // وگرنه chat_id را از سرور بگیر/بساز، بعد باز کن.
                java.util.UUID chatId = findExistingPrivateChatId(r.uuid);
                if (chatId == null) {
                    chatId = fetchOrCreatePrivateChat(r.uuid); // نیاز به API سمت سرور
                    if (chatId == null) {
                        System.out.println("❌ Failed to create/find private chat.");
                        return;
                    }
                }

                org.to.telegramfinalproject.Models.ChatEntry ce = new org.to.telegramfinalproject.Models.ChatEntry();
                ce.setId(chatId.toString());                  // ⬅️ internal chat_id
                ce.setDisplayId(r.displayId);                 // username
                ce.setName(r.title);                          // profile_name
                ce.setType("private");

                openChat(ce); // همون متد فعلی MainController
                break;
            }

            case GROUP:
            case CHANNEL: {
                // اگر تو لیست هست بازش کن، وگرنه با همون uuid باز کن
                org.to.telegramfinalproject.Models.ChatEntry existing =
                        findExistingChat(r.uuid, r.receiverType);
                if (existing != null) {
                    openChat(existing);
                } else {
                    org.to.telegramfinalproject.Models.ChatEntry ce = new org.to.telegramfinalproject.Models.ChatEntry();
                    ce.setId(r.uuid.toString());          // internal_uuid group/channel
                    ce.setDisplayId(r.displayId);         // group_id/channel_id
                    ce.setName(r.title);
                    ce.setType(r.receiverType);
                    openChat(ce);
                }
                break;
            }

            case MESSAGE: {
                // مثل کنسول: چتِ پیام را باز کن (اسکرول به پیام را بعداً اضافه کن)
                org.to.telegramfinalproject.Models.ChatEntry existing =
                        findExistingChat(r.uuid, r.receiverType);
                if (existing != null) {
                    openChat(existing);
                } else {
                    org.to.telegramfinalproject.Models.ChatEntry ce = new org.to.telegramfinalproject.Models.ChatEntry();
                    ce.setId(r.uuid.toString());         // receiver internal_uuid
                    ce.setType(r.receiverType);
                    ce.setName(guessNameForReceiver(r)); // اگر خواستی از subtitle استفاده کن
                    ce.setDisplayId(r.displayId);
                    openChat(ce);
                }
                break;
            }
        }
    }

    private org.to.telegramfinalproject.Models.ChatEntry findExistingChat(java.util.UUID internalId, String type) {
        var list = (org.to.telegramfinalproject.Client.Session.chatList==null)
                ? java.util.Collections.<org.to.telegramfinalproject.Models.ChatEntry>emptyList()
                : org.to.telegramfinalproject.Client.Session.chatList;
        for (var c : list) {
            if (internalId.toString().equals(c.getId().toString())
                    && type.equalsIgnoreCase(c.getType())) {
                return c;
            }
        }
        return null;
    }

    private java.util.UUID findExistingPrivateChatId(java.util.UUID otherUserUuid) {
        // اگر در لیست چت‌ها private با همین طرف داری و internal_id همان chat_id است، برش گردان
        var list = (org.to.telegramfinalproject.Client.Session.chatList==null)
                ? java.util.Collections.<org.to.telegramfinalproject.Models.ChatEntry>emptyList()
                : org.to.telegramfinalproject.Client.Session.chatList;

        for (var c : list) {
            if ("private".equalsIgnoreCase(c.getType())) {
                // اگر مدل‌ات otherUserId در ChatEntry دارد، از آن استفاده کن
                if (otherUserUuid.equals(c.getOtherUserId())) {
                    try { return java.util.UUID.fromString(c.getId().toString()); } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private java.util.UUID fetchOrCreatePrivateChat(java.util.UUID otherUserUuid) {
        try {
            org.json.JSONObject req = new org.json.JSONObject();
            req.put("action", "get_or_create_private_chat");
            req.put("user1", org.to.telegramfinalproject.Client.Session.currentUser.getString("internal_uuid"));
            req.put("user2", otherUserUuid.toString());
            org.json.JSONObject resp = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);
            if (resp != null && "success".equals(resp.optString("status"))) {
                String chatId = resp.getJSONObject("data").getString("chat_id");
                return java.util.UUID.fromString(chatId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String guessNameForReceiver(SearchResult r) {
        // فقط برای زمانی که موجودیت در لیست نبود
        if (r.title != null && !r.title.isBlank()) return r.title;
        if (r.receiverType != null) {
            switch (r.receiverType) {
                case "group": return "Group";
                case "channel": return "Channel";
                case "private": return "Private Chat";
            }
        }
        return "Chat";
    }

    private boolean isSavedMessages(ChatEntry c) {
        if (c == null) return false;

        // اگر تایپ اختصاصی داری
        if ("saved".equalsIgnoreCase(c.getType())) return true;

        // اگر با نام مشخص ذخیره می‌کنی
        String n = c.getName();
        if (n != null && n.equalsIgnoreCase("Saved Messages")) return true;

        // حالت پرایوت با خودِ کاربر
        String me = (org.to.telegramfinalproject.Client.Session.currentUser != null)
                ? org.to.telegramfinalproject.Client.Session.currentUser.optString("internal_uuid", "")
                : "";
        try {
            UUID other = c.getOtherUserId(); // اگر این فیلد را داری
            if ("private".equalsIgnoreCase(c.getType()) &&
                    other != null && other.toString().equalsIgnoreCase(me)) {
                return true;
            }
        } catch (Exception ignore) {}

        return false;
    }




}