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
        searchBar.textProperty().addListener((obs, oldV, newV) -> {
            if (!chatSearchPane.isVisible()) return; // only react if in search mode

            if (newV.trim().isEmpty()) {
                chatSearchResults.setVisible(false);
                chatSearchResults.setManaged(false);
            } else {
                chatSearchResults.setVisible(true);
                chatSearchResults.setManaged(true);
                chatSearchResults.getItems().setAll(
                        "Result 1: " + newV,
                        "Result 2: " + newV,
                        "Result 3: " + newV
                );
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


    private static final java.time.format.DateTimeFormatter FMT_HHMM =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter FMT_DATE_TIME =
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final String YESTERDAY_LABEL = "Yesterday";

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

        for (ChatEntry c : list) {
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

            cc.setChatData(chat.getName(), preview, timeText, chat.getUnreadCount());
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
}