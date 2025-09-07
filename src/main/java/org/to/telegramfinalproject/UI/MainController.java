package org.to.telegramfinalproject.UI;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Client.ActionHandler;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class MainController {

    // === LEFT PANE ===
    @FXML private VBox chatListContainer;     // inside the scrollPane
    @FXML private ScrollPane scrollPane;      // chat list scroll

    // === Global Search ===
    @FXML private TextField searchBar;
    @FXML private VBox globalSearchPane;
    @FXML private VBox noResultsBox;
    @FXML private ImageView noResultIcon;
    @FXML private ScrollPane globalSearchScroll;
    @FXML private VBox globalSearchResultsContainer;

    //Archives
    @FXML private VBox chatListVBox;
    @FXML private Label chatListHeader;

    private boolean blockedByMeFlag = false;
    private boolean blockedMeFlag   = false;

    private enum SearchMode {
        GLOBAL,
        CHAT
    }
    private ChatViewMode currentMode = ChatViewMode.NORMAL; // حالت فعلی: NORMAL/NEEDS_JOIN/NEEDS_ADD_CONTACT
    private SearchMode currentSearchMode = SearchMode.GLOBAL;
    private UUID currentChatId; // if in CHAT mode, which chat to search in

    // === Search In Chat ===
    @FXML private VBox chatSearchPane;        // search results panel
    @FXML private ListView<String> chatSearchResults;

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

    // === Overlay ===
    @FXML private StackPane overlayLayer;

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

    // Keep track of the scene user comes from
    private final Deque<Node> navigationStack = new ArrayDeque<>();
    private final Map<UUID, ChatItemController> itemControllers = new HashMap<>();
    private final java.util.Set<UUID> enrichInFlight = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());


    private static final String SAVED_TITLE  = "Saved Messages";
    private static final String SAVED_AVATAR = "/org/to/telegramfinalproject/Avatars/saved_messages.png";

    //For realtime handling
    public void onChatUpdated(UUID chatId, String chatType, LocalDateTime lastTs,
                              boolean isIncoming, String lastPreview) {

        boolean exists =
                (Session.chatList != null && Session.chatList.stream().anyMatch(c -> chatId.equals(c.getId()))) ||
                        (Session.activeChats != null && Session.activeChats.stream().anyMatch(c -> chatId.equals(c.getId()))) ||
                        (Session.archivedChats != null && Session.archivedChats.stream().anyMatch(c -> chatId.equals(c.getId())));
        if (!exists) return;

        boolean isOpen = Session.currentChatId != null &&
                Session.currentChatId.equals(chatId.toString());

        forEachChat(chatId, chat -> {
            chat.setLastMessageTime(lastTs);
            if (lastPreview != null) chat.setLastMessagePreview(lastPreview);
            if (isIncoming && !isOpen) chat.setUnreadCount(chat.getUnreadCount() + 1); // ✅ یکدست با unreadCount
        });

        java.util.Comparator<ChatEntry> byTimeDesc = (c1, c2) -> {
            var t1 = c1.getLastMessageTime();
            var t2 = c2.getLastMessageTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        };
        if (Session.chatList != null) Session.chatList.sort(byTimeDesc);
        if (Session.activeChats != null) Session.activeChats.sort(byTimeDesc);
        if (Session.archivedChats != null) Session.archivedChats.sort(byTimeDesc);

        refreshChatListUI();
        refreshBadgesIfAny();
    }




    // MainController
    private void forEachChat(UUID chatId, java.util.function.Consumer<ChatEntry> fn) {
        java.util.List<java.util.List<ChatEntry>> lists = java.util.List.of(
                Session.chatList != null ? Session.chatList : java.util.List.of(),
                Session.activeChats != null ? Session.activeChats : java.util.List.of(),
                Session.archivedChats != null ? Session.archivedChats : java.util.List.of()
        );
        for (var lst : lists) {
            for (var c : lst) {
                if (chatId.equals(c.getId())) {
                    fn.accept(c);
                }
            }
        }
    }


//    public void refreshChatListUI() {
//        Platform.runLater(() -> {
//            chatListContainer.getChildren().clear();
//            itemControllers.clear();
//            populateChatListFromSession(); // همون متدی که نودها رو می‌سازه و می‌چسبونه
//        });
//    }


    public void refreshChatListUI() {
        Platform.runLater(() -> {
            chatListContainer.getChildren().clear();
            itemControllers.clear();

            if (Session.inArchivedView) {
                refreshArchivedListUI(); // ⬅️ همین ظرف chatListContainer را پر می‌کند
            } else {
                ensureArchivedHeaderRow();   // ⬅️ اول هدر را در chatListContainer بگذار
                populateChatListFromSession();
            }
        });
    }


    private void refreshBadgesIfAny() {
        // آپدیت نشان‌ها اگر لازم است
    }


    @FXML
    public void initialize() {
        // Populate chat list
//        populateChatListFromSession();
        refreshChatListUI();

        // Register the scene for automatic CSS updates
        Platform.runLater(() -> {
            ThemeManager.getInstance().registerScene(mainRoot.getScene());
        });

        // Listen for theme changes to update icons & labels manually
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                updateIconsForDarkMode();
                updateLabelsForDarkMode();
            } else {
                updateIconsForLightMode();
                updateLabelsForLightMode();
            }
        });

        // Smooth scroll feel for chat list
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

        // Search bar enter action
        searchBar.setOnAction(e -> {
            String keyword = searchBar.getText().trim();
            if (keyword.isEmpty()) return;

            if (currentSearchMode == SearchMode.GLOBAL) {
                performGlobalSearch(keyword);
            } else if (currentSearchMode == SearchMode.CHAT && currentChatId != null) {
                performChatSearch(keyword, currentChatId);
            }
        });

        // Typing in search bar shows global search panel
        searchBar.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) {
                if (currentSearchMode == SearchMode.GLOBAL) {
                    showGlobalSearchPanel();
                } else if (currentSearchMode == SearchMode.CHAT) {
                    showSearchPanel();
                }
            }
        });

        chatSearchResults.setOnMouseClicked(e -> {
            int idx = chatSearchResults.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < searchBacking.size()) {
                openSearchResult(searchBacking.get(idx));
            }
        });

        // Smooth scroll feel for global search results
        globalSearchScroll.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm()
        );
        globalSearchScroll.setPannable(true);
        globalSearchScroll.setFitToWidth(true);
        globalSearchScroll.setFitToHeight(false);
        globalSearchScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003; // match chat list smoothness
            globalSearchScroll.setVvalue(globalSearchScroll.getVvalue() - deltaY);
        });
    }

    // Called from ChatPageController when user clicks search button
    public void showSearchPanel() {
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);

        currentSearchMode = SearchMode.CHAT;

        chatSearchPane.setVisible(true);
        chatSearchPane.setManaged(true);

        searchBar.requestFocus();
    }

    @FXML
    public void closeSearchPanel() {
        chatSearchPane.setVisible(false);
        chatSearchPane.setManaged(false);

        currentSearchMode = SearchMode.GLOBAL;
        currentChatId = null;

        scrollPane.setVisible(true);
        scrollPane.setManaged(true);

        searchBar.clear();
    }

    public void showGlobalSearchPanel() {
        scrollPane.setVisible(false);
        scrollPane.setManaged(false);

        chatSearchPane.setVisible(false);
        chatSearchPane.setManaged(false);

        globalSearchPane.setVisible(true);
        globalSearchPane.setManaged(true);
    }

    @FXML
    public void closeGlobalSearch() {
        globalSearchPane.setVisible(false);
        globalSearchPane.setManaged(false);

        scrollPane.setVisible(true);
        scrollPane.setManaged(true);

        searchBar.clear();
    }

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
//        chatListContainer.getChildren().clear();

        var list = (org.to.telegramfinalproject.Client.Session.activeChats != null
                && !org.to.telegramfinalproject.Client.Session.activeChats.isEmpty())
                ? org.to.telegramfinalproject.Client.Session.activeChats
                : org.to.telegramfinalproject.Client.Session.chatList;

        if (list == null || list.isEmpty()) return;

        ChatEntry saved = null;
        java.util.List<ChatEntry> others = new java.util.ArrayList<>();
        for (ChatEntry c : list) {
            if (saved == null && isSavedMessages(c)) {
                saved = c;
            } else {
                others.add(c);
            }
        }

//         addArchivedHeaderIfYouHaveOne();

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

//    private void addChatNode(ChatEntry chat) {
//        try {
//            FXMLLoader fx = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
//            Node item = fx.load();
//            ChatItemController cc = fx.getController();
//
//            String preview = chat.getLastMessagePreview() == null ? "" : chat.getLastMessagePreview();
//            String timeText = formatChatTime(chat.getLastMessageTime());
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
//            Node chatItem = loader.load();
//            ChatItemController controller = loader.getController();
//
//            cc.setChatData(chat.getName(), preview, timeText, chat.getUnreadCount(), "/org/to/telegramfinalproject/Avatars/default_profile.png");
//            item.setOnMouseClicked(e -> openChat(chat));
//            chatListContainer.getChildren().add(item);
//
//            itemControllers.put(chat.getId(), cc);
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }

    private void addChatNode(ChatEntry chat) {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
            Node item = fx.load();
            ChatItemController cc = fx.getController();

            // --- saved detection
            boolean saved = isSaved(chat);

            String title    = safeTitle(chat);
            String preview  = chat.getLastMessagePreview() == null ? "" : chat.getLastMessagePreview();
            String timeText = chat.getLastMessageTime() == null ? "" : formatChatTime(chat.getLastMessageTime());
            String imageUrl = safeImage(chat.getImageUrl());

            // --- force Saved Messages title & avatar
            if (saved) {
                title    = SAVED_TITLE;
                imageUrl = SAVED_AVATAR;
            }

            cc.setChatData(title, preview, timeText, chat.getUnreadCount(), imageUrl, chat.getType());
            item.setOnMouseClicked(e -> openChat(chat));
            chatListContainer.getChildren().add(item);

            itemControllers.put(chat.getId(), cc);

            // --- no enrich for Saved
            boolean needName  = (chat.getName() == null || chat.getName().isBlank());
            boolean needImage = (chat.getImageUrl() == null || chat.getImageUrl().isBlank());
            if (!saved && (needName || needImage)) {
                enrichChatEntryAsync(chat);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private boolean isSaved(ChatEntry e) {
        if (e == null) return false;
        if (e.isSavedMessages()) return true;
        return "saved".equalsIgnoreCase(e.getType()); // اگر type اختصاصی داری
    }


    public void updateSingleChatCell(ChatEntry entry) {
        ChatItemController cc = itemControllers.get(entry.getId());
        if (cc != null) {
            Platform.runLater(() -> cc.setChatData(
                    safeTitle(entry),
                    entry.getLastMessagePreview() == null ? "" : entry.getLastMessagePreview(),
                    entry.getLastMessageTime() == null ? "" : formatChatTime(entry.getLastMessageTime()),
                    entry.getUnreadCount(),
                    safeImage(entry.getImageUrl()),
                    entry.getType()
            ));
        } else {
            refreshChatListUI(); // اگر پیدا نشد، کل لیست را رفرش کن
        }
    }

    public void enrichChatEntryAsync(ChatEntry entry) {
        if (entry == null) return;
        // اگر در حال دریافت هستیم، تکراری نفرست
        if (!enrichInFlight.add(entry.getId())) return;

        new Thread(() -> {
            try {
                JSONObject req = new JSONObject()
                        .put("action", "get_header_info")
                        .put("receiver_id", entry.getId().toString())
                        .put("receiver_type", entry.getType())
                        .put("viewer_id", Session.getUserUUID());

                JSONObject res = ActionHandler.sendWithResponse(req);
                if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
                    JSONObject d = res.optJSONObject("data");
                    if (d != null) {
                        String name  = d.optString("name", "");
                        String image = d.optString("image_url", "");
                        Platform.runLater(() -> {
                            if (!name.isBlank())  entry.setName(name);
                            if (!image.isBlank()) entry.setImageUrl(image);
                            updateSingleChatCell(entry); // فقط همان آیتم را نوسازی کن
                        });
                    }
                }
            } catch (Exception ignored) {
            } finally {
                enrichInFlight.remove(entry.getId());
            }
        }).start();
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

//    void openChat(ChatEntry chat) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_page.fxml"));
//            Node chatPage = loader.load();
//
//
//
//
//
//
//
//            ChatPageController controller = loader.getController();
//            controller.showChat(chat);
//
//            this.chatPageController = controller;
//            Session.currentChatId = chat.getId().toString();
//
//            chatDisplayArea.getChildren().setAll(chatPage);
//            chat.setUnreadCount(0);
//            ChatItemController item = itemControllers.get(chat.getId());
//            if (item != null) item.setUnread(0);
//
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }

    void openChat(ChatEntry chat) {
        // فقط برای پرایوت: اول بلاک/بلاک‌شدن را چک کن، بعد مود را تعیین کن
        if ("private".equalsIgnoreCase(chat.getType())) {
            final String viewerId = org.to.telegramfinalproject.Client.Session.getUserUUID(); // internal_uuid کاربر فعلی
            if (viewerId != null && !viewerId.isBlank()) {
                new Thread(() -> {
                    org.json.JSONObject req = new org.json.JSONObject()
                            .put("action", "check_block_status_by_chat")
                            .put("viewer_id", viewerId)
                            .put("chat_id", chat.getId().toString());

                    org.json.JSONObject res = org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse(req);

                    boolean blockedByMe = false;
                    boolean blockedMe   = false;
                    if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
                        org.json.JSONObject data = res.optJSONObject("data");
                        if (data != null) {
                            // اسم فیلدها را با سرور خودت یکی کن
                            blockedByMe = data.optBoolean("blocked_by_me", false) || data.optBoolean("is_blocked", false);
                            blockedMe   = data.optBoolean("blocked_me", false);
                        }
                    }

                    final ChatViewMode mode = blockedByMe
                            ? ChatViewMode.BLOCKED
                            : (blockedMe ? ChatViewMode.READ_ONLY : ChatViewMode.NORMAL);

                    Platform.runLater(() -> openChatWithMode(chat, mode));
                }).start();
                return; // نذار پایین دوباره باز شود
            }
        }

        if ("channel".equalsIgnoreCase(chat.getType())) {
            boolean canPost =
                    chat.isOwner() || chat.isAdmin() ||
                            (chat.getPermissions() != null &&
                                    chat.getPermissions().optBoolean("can_post", false));
            // اگر فقط owner/admin ملاک توست، خط بالا را به این تغییر بده:
            // boolean canPost = chat.isOwner() || chat.isAdmin();

            openChatWithMode(chat, canPost ? ChatViewMode.NORMAL : ChatViewMode.READ_ONLY);
            return;
        }

        // غیرپرایوت یا اگر viewerId نبود → نرمال
        openChatWithMode(chat, ChatViewMode.NORMAL);
    }


    private void openChatWithMode(ChatEntry chat, ChatViewMode mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_page.fxml"));
            Node chatPage = loader.load();

            ChatPageController controller = loader.getController();
            controller.showChat(chat, mode); // ⬅️ متد جدید در ChatPageController

            this.chatPageController = controller;
            Session.currentChatId = chat.getId().toString();

            chatDisplayArea.getChildren().setAll(chatPage);

            chat.setUnreadCount(0);
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
    public void closeSidebar() {
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
        noResultIcon.setImage(new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/no_result_light.png")));
    }

    private void updateIconsForLightMode() {
        // Example: switch images to black versions
        menuIcon.setImage(new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/menu_dark.png")));
        noResultIcon.setImage(new Image(getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/no_result_dark.png")));
    }

    private void updateLabelsForDarkMode() {
        //myLabel.setStyle("-fx-text-fill: white;");
    }

    private void updateLabelsForLightMode() {
        //myLabel.setStyle("-fx-text-fill: black;");
    }

    // === Overlay Handling ===
    public void showOverlay(Node overlayNode) {
        if (!overlayLayer.getChildren().isEmpty()) {
            Node current = overlayLayer.getChildren().get(overlayLayer.getChildren().size() - 1);
            navigationStack.push(current);
            overlayLayer.getChildren().remove(current);
        }
        overlayLayer.getChildren().add(overlayNode);

        // Automatically close sidebar
        closeSidebar();
    }

    public void closeOverlay(Node overlayNode) {
        overlayLayer.getChildren().remove(overlayNode);
    }

    public void goBack(Pane currentOverlay) {
        // Remove the current overlay
        closeOverlay(currentOverlay);

        if (!navigationStack.isEmpty()) {
            Node previous = navigationStack.pop();

            // Clear existing overlays before showing previous
            overlayLayer.getChildren().clear();
            overlayLayer.getChildren().add(previous);
        }
    }

    public void closeCurrentChat() {
        // 1. Clear the chat area
        chatDisplayArea.getChildren().clear();

        // 2. Re-add the placeholder label
        placeholderLabel.setVisible(true);
        placeholderLabel.setManaged(true);
        placeholderLabel.setText("Select a chat to start messaging");

        chatDisplayArea.getChildren().add(placeholderLabel);

        // 3. Ask the chat list to refresh
        MainController.getInstance().refreshChatListUI();
    }

    public void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            // Tie it to the main window so it centers nicely
            if (mainRoot.getScene() != null && mainRoot.getScene().getWindow() != null) {
                alert.initOwner(mainRoot.getScene().getWindow());
            }

            alert.showAndWait();
        });
    }

    // ===== Search UI state =====
    private final java.util.List<SearchResult> searchBacking = new java.util.ArrayList<>();

    @FXML private AnchorPane chatHost; // کانتینر مناسب در مین برای قرار دادن ChatPage

    private ChatPageController chatPageController;



    public ChatPageController getChatPageController() {
        return chatPageController;
    }


    private enum SRType { USER, GROUP, CHANNEL, MESSAGE }

    // Inner class
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

    private void performChatSearch(String keyword, UUID chatId) {
        if (keyword == null || keyword.isBlank()) {
            chatSearchResults.getItems().clear();
            return;
        }

        showSearchPanel();

        org.json.JSONObject req = new org.json.JSONObject();
        req.put("action", "search_in_chat");
        req.put("keyword", keyword);
        req.put("chat_id", chatId.toString());
        req.put("user_id", org.to.telegramfinalproject.Client.Session.currentUser.getString("user_id"));

        new Thread(() -> {
            org.json.JSONObject resp;
            try {
                resp = ActionHandler.sendWithResponse(req);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            if (resp == null || !"success".equals(resp.optString("status"))) return;
            org.json.JSONArray arr = resp.optJSONObject("data").optJSONArray("results");
            if (arr == null) arr = new org.json.JSONArray();

            java.util.List<String> tmp = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject it = arr.getJSONObject(i);
                String time = it.optString("time", "");
                String content = it.optString("content", "[No content]");
                tmp.add("🗨 " + content + (time.isEmpty() ? "" : " • " + time));
            }

            Platform.runLater(() -> {
                chatSearchResults.getItems().setAll(tmp);
                chatSearchResults.setVisible(true);
                chatSearchResults.setManaged(true);
            });
        }).start();
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

        showGlobalSearchPanel();

        // Request to server
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
                                it.optString("receiver_display_id", ""),
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

//    private void renderSearchResults(java.util.List<SearchResult> results) {
//        searchBacking.clear();
//        searchBacking.addAll(results);
//
//        javafx.collections.ObservableList<SearchResult> view =
//                javafx.collections.FXCollections.observableArrayList(results);
//        globalSearchResults.setItems(view);
//        globalSearchResults.setVisible(true);
//        globalSearchResults.setManaged(true);
//    }

    private void renderSearchResults(List<SearchResult> results) {
        globalSearchResultsContainer.getChildren().clear();

        if (results.isEmpty()) {
            noResultsBox.setVisible(true);
            noResultsBox.setManaged(true);
            globalSearchScroll.setVisible(false);
            globalSearchScroll.setManaged(false);
            return;
        }

        noResultsBox.setVisible(false);
        noResultsBox.setManaged(false);
        globalSearchScroll.setVisible(true);
        globalSearchScroll.setManaged(true);

        for (SearchResult r : results) {
            HBox container = new HBox(10);
            ImageView avatar = new ImageView();
            avatar.setFitWidth(40);
            avatar.setFitHeight(40);
            avatar.setSmooth(true);
            avatar.setPreserveRatio(true);

            // 🔑 Clip it into a circle
            Circle clip = new Circle(20, 20, 20); // x,y = center, radius = 20
            avatar.setClip(clip);

            Label title = new Label(r.title);
            title.getStyleClass().add("global-search-title");

            Label subtitle = new Label(
                    r.type == SRType.MESSAGE
                            ? (r.subtitle != null ? r.subtitle : "")
                            : "Press to see messages"
            );
            subtitle.getStyleClass().add("global-search-subtitle");

            VBox texts = new VBox(2, title, subtitle);
            container.getChildren().addAll(avatar, texts);

            // Default profile per type
            switch (r.type) {
                case USER -> avatar.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                        "/org/to/telegramfinalproject/Avatars/default_user_profile.png"))));
                case GROUP -> avatar.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                        "/org/to/telegramfinalproject/Avatars/default_group_profile.png"))));
                case CHANNEL -> avatar.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                        "/org/to/telegramfinalproject/Avatars/default_channel_profile.png"))));
                case MESSAGE -> avatar.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                        "/org/to/telegramfinalproject/Avatars/default_user_profile.png"))));
            }

            container.setOnMouseClicked(e -> openSearchResult(r));
            globalSearchResultsContainer.getChildren().add(container);
        }
    }

    private void openSearchResult(SearchResult r) {
        switch (r.type) {
            case USER: {
                // 1) اگه قبلاً چت پرایوت با این یوزر داری، از همون استفاده کن
                UUID existingChatId = findExistingPrivateChatId(r.uuid);

                ChatEntry ce = new ChatEntry();
                ce.setType("private");
                ce.setName(r.title);
                ce.setDisplayId(r.displayId);
                try { ce.setOtherUserId(r.uuid); } catch (Exception ignore) {}

                ChatViewMode mode;
                if (existingChatId != null) {
                    ce.setId(existingChatId.toString());
                    mode = ChatViewMode.NORMAL;
                } else {
                    // هنوز چتی وجود ندارد → Preview (بدون ساخت چت)
                    // برای Preview از uuid خودِ طرف مقابل به‌عنوان id موقت استفاده می‌کنیم
                    ce.setId(r.uuid.toString());
                    mode = isContact(r.uuid) ? ChatViewMode.NORMAL : ChatViewMode.NEEDS_ADD_CONTACT;
                }

                openChatWithMode(ce, mode);
                break;
            }


            case GROUP:
            case CHANNEL: {
                org.to.telegramfinalproject.Models.ChatEntry existing =
                        findExistingChat(r.uuid, r.receiverType);
                if (existing != null) {
                    openChatWithMode(existing, ChatViewMode.NORMAL);
                } else {
                    org.to.telegramfinalproject.Models.ChatEntry ce = new org.to.telegramfinalproject.Models.ChatEntry();
                    ce.setId(r.uuid.toString());          // internal_uuid group/channel
                    ce.setDisplayId(r.displayId);         // group_id/channel_id
                    ce.setName(r.title);
                    ce.setType(r.receiverType);
                    //openChat(ce);
                    ChatViewMode mode;
                    try {
                        mode = isInAnyChatList(r.uuid) ? ChatViewMode.NORMAL : ChatViewMode.NEEDS_JOIN;
                    } catch (Exception e) {
                        mode = ChatViewMode.NEEDS_JOIN;
                    }
                    openChatWithMode(ce, mode);

                }
                break;
            }

            case MESSAGE: {
                org.to.telegramfinalproject.Models.ChatEntry existing =
                        findExistingChat(r.uuid, r.receiverType);
                if (existing != null) {
                    openChat(existing);
                } else {
                    org.to.telegramfinalproject.Models.ChatEntry ce = new org.to.telegramfinalproject.Models.ChatEntry();
                    ce.setId(r.uuid.toString());         // receiver internal_uuid
                    ce.setType(r.receiverType);
                    ce.setName(guessNameForReceiver(r));
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
        var list = (org.to.telegramfinalproject.Client.Session.chatList==null)
                ? java.util.Collections.<org.to.telegramfinalproject.Models.ChatEntry>emptyList()
                : org.to.telegramfinalproject.Client.Session.chatList;

        for (var c : list) {
            if ("private".equalsIgnoreCase(c.getType())) {
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


    //Search
    private boolean isInAnyChatList(UUID chatId) {
        var lists = List.of(
                Session.chatList != null ? Session.chatList : List.<ChatEntry>of(),
                Session.activeChats != null ? Session.activeChats : List.<ChatEntry>of(),
                Session.archivedChats != null ? Session.archivedChats : List.<ChatEntry>of()
        );
        for (var lst : lists) {
            for (var c : lst) {
                try {
                    if (chatId.equals(UUID.fromString(c.getId().toString()))) return true;
                } catch (Exception ignore) {}
            }
        }
        return false;
    }


    private boolean isContact(UUID userUuid) {
        if (Session.contactEntries == null) return false;
        try {
            for (var c : Session.contactEntries) {
                UUID id = c.getContactId();
                if (userUuid.equals(id)) return true;
            }
        } catch (Exception ignore) {}
        return false;
    }



    private ChatViewMode computeMode(ChatEntry ce) {
        try {
            UUID id = UUID.fromString(ce.getId().toString());
            if (isInAnyChatList(id)) return ChatViewMode.NORMAL;
        } catch (Exception ignore) {}

        String t = ce.getType();
        if ("group".equalsIgnoreCase(t) || "channel".equalsIgnoreCase(t)) {
            return ChatViewMode.NEEDS_JOIN;
        }
        if ("private".equalsIgnoreCase(t)) {
            UUID other = null;
            try { other = ce.getOtherUserId(); } catch (Exception ignore) {}
            return (other != null && isContact(other))
                    ? ChatViewMode.NORMAL
                    : ChatViewMode.NEEDS_ADD_CONTACT;
        }
        return ChatViewMode.NORMAL;
    }

    public void onJoinedOrAdded(ChatEntry ce) {
        try {
            UUID id = UUID.fromString(ce.getId().toString());
            if (!isInAnyChatList(id)) {
                if (Session.chatList == null) Session.chatList = new ArrayList<>();
                Session.chatList.add(ce);
            }
            if (Session.activeChats != null && Session.activeChats.stream().noneMatch(c -> id.equals(c.getId()))) {
                Session.activeChats.add(ce);
            }
        } catch (Exception ignore) {}

        Comparator<ChatEntry> byTimeDesc = (a,b) -> {
            LocalDateTime t1 = a.getLastMessageTime(), t2 = b.getLastMessageTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        };
        if (Session.chatList != null) Session.chatList.sort(byTimeDesc);
        if (Session.activeChats != null) Session.activeChats.sort(byTimeDesc);

        refreshChatListUI();
    }


    public void addChatAndSelect(org.to.telegramfinalproject.Models.ChatEntry entry) {
        // در Session نگه‌داری
        if (org.to.telegramfinalproject.Client.Session.chatList.stream()
                .noneMatch(c -> c.getId().equals(entry.getId()))) {
            org.to.telegramfinalproject.Client.Session.chatList.add(0, entry);
            org.to.telegramfinalproject.Client.Session.activeChats.add(0, entry);
        }
        // سایدبارت اگر متدی برای رفرش دارد صداش بزن (اسمش را با کلاس خودت هماهنگ کن)
        try {
            this.refreshChatListUI(); // اگر نداری، این خط را حذف کن
        } catch (Exception ignore) {}

        // نمایش پیج چت
        if (getChatPageController() != null) {
            getChatPageController().showChat(entry);
        }
    }


    public void refreshSidebarUserFromSession() {
        if (sidebarController != null && Session.currentUser != null) {
            Platform.runLater(() -> sidebarController.setUserFromSession(Session.currentUser));
        }
    }




    private static final String ARCHIVED_ROW_KEY = "ARCHIVED_HEADER_ROW";

    public void ensureArchivedHeaderRow() {
        boolean hasArchived = Session.archivedChats != null && !Session.archivedChats.isEmpty();

        // اگر آرشیو نداریم، هدر قبلی را حذف کن و برگرد
        if (!hasArchived) {
            chatListContainer.getChildren().removeIf(n -> ARCHIVED_ROW_KEY.equals(n.getUserData()));
            return;
        }

        // اگر قبلاً هست، دوباره نساز
        boolean exists = chatListContainer.getChildren().stream()
                .anyMatch(n -> ARCHIVED_ROW_KEY.equals(n.getUserData()));
        if (exists) return;

        HBox headerRow = new HBox(8);
        headerRow.setUserData(ARCHIVED_ROW_KEY);
        headerRow.getStyleClass().add("archived-header-row");

        Label title = new Label("Archived Chats");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> showArchivedListUI());

        headerRow.getChildren().addAll(title, spacer, openBtn);
        headerRow.setMinHeight(36); headerRow.setPrefHeight(36);

        // در ابتدای همان لیست اصلیِ چت‌ها
        chatListContainer.getChildren().add(0, headerRow);
    }

    public void showArchivedListUI() {
        Session.inArchivedView = true;
        refreshChatListUI();
    }

    public void refreshArchivedListUI() {
        chatListContainer.getChildren().clear();

        // Back row (بماند)
        HBox backRow = new HBox();
        Label back = new Label("← Back to Active");
        back.getStyleClass().add("archived-back");
        back.setOnMouseClicked(e -> {
            Session.inArchivedView = false;
            refreshChatListUI();
        });
        backRow.getChildren().add(back);
        backRow.setStyle("-fx-padding: 6 10 8 10;");
        chatListContainer.getChildren().add(backRow);

        // ✅ از همین سل‌ساز FXML استفاده کن تا دقیقا هم‌شکل اکتیو باشد
        if (Session.archivedChats != null && !Session.archivedChats.isEmpty()) {
            for (ChatEntry e : Session.archivedChats) {
                addChatNode(e);  // ⬅️ همونی که در اکتیو استفاده می‌کنی
            }
        } else {
            Label empty = new Label("No archived chats");
            empty.getStyleClass().add("muted");
            chatListContainer.getChildren().add(empty);
        }
    }



    // ساخت یک سل آیتم (برای اکتیو/آرشیو)
    private HBox buildChatCell(ChatEntry e, boolean archivedView) {
        HBox row = new HBox(8);
        ImageView avatar = new ImageView(); avatar.setFitWidth(36); avatar.setFitHeight(36);
        // ... لود تصویر از e.getImageUrl() اگر داری
        Label name = new Label(e.getName());
        Region spacer = new Region(); HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label last = new Label(safeLastLine(e)); // آخرین پیام/زمان (اختیاری)

        row.getChildren().addAll(avatar, name, spacer, last);
        row.getStyleClass().add("chat-row");

        // کلیک روی آیتم → openChat
        row.setOnMouseClicked(ev -> openChat(e));

        return row;
    }

    private String safeLastLine(ChatEntry e) {
        String s = e.getLastMessagePreview(); // اگر داری
        return s != null ? s : "";
    }


    // title fallback: اول name بعد displayId، در نهایت پیش‌فرض
    private String safeTitle(ChatEntry e) {
        if (e.getName() != null && !e.getName().isBlank()) return e.getName();
        if (e.getDisplayId() != null && !e.getDisplayId().isBlank()) return e.getDisplayId();
        if ("saved".equalsIgnoreCase(e.getType()) || e.isSavedMessages()) return "Saved Messages";
        return "Unknown";
    }

    // image fallback: اگر خالی بود، آواتار پیش‌فرض
    private String safeImage(String img) {
        return (img != null && !img.isBlank())
                ? img
                : "/org/to/telegramfinalproject/Avatars/default_user_profile.png";
    }



}