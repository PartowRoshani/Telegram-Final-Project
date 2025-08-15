package org.to.telegramfinalproject.UI;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {

    @FXML private StackPane mainRoot;
    @FXML private VBox chatListContainer;
    @FXML private StackPane chatDisplayArea;
    @FXML private Pane overlay;
    @FXML private ImageView menuIcon;
    @FXML private ScrollPane scrollPane;
    @FXML private SplitPane mainSplitPane;
    @FXML private VBox leftPane;
    @FXML private Button menuButton;
    @FXML private TextField searchBar;

    private Node sidebarRoot;
    private boolean isSidebarOpen = false;

    // Add ThemeManager handle
    private final ThemeManager themeManager = ThemeManager.getInstance();

    @FXML
    public void initialize() {
        addSampleChats();

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
    }

    private void addSampleChats() {
        addChat("Archived Chats", "Your archived chats", "10:45", 0);
        addChat("Saved Messages", "Keep messages for later", "Yesterday", 0);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
        addChat("Alice", "Hey, how are you?", "14:12", 3);
        addChat("Bob", "Let's meet tomorrow", "12:08", 1);
    }

    private void addChat(String name, String lastMsg, String time, int unread) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/to/telegramfinalproject/Fxml/chat_item.fxml"));
            Node chatItem = loader.load();
            ChatItemController controller = loader.getController();
            controller.setChatData(name, lastMsg, time, unread);

            chatItem.setOnMouseClicked(e -> openChat(name));
            chatListContainer.getChildren().add(chatItem);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openChat(String chatName) {
        chatDisplayArea.getChildren().clear();
        chatDisplayArea.getChildren().add(new javafx.scene.control.Label("Chat with " + chatName));
    }

    @FXML
    private void toggleSidebar() {
        if (isSidebarOpen) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }

    @FXML
    private void openSidebar() {
        try {
            if (sidebarRoot == null) {
                sidebarRoot = FXMLLoader.load(getClass().getResource("/org/to/telegramfinalproject/Fxml/Sidebar_menu.fxml"));
                StackPane.setAlignment(sidebarRoot, Pos.CENTER_LEFT);
                sidebarRoot.setTranslateX(-getSidebarWidth());
                mainRoot.getChildren().add(sidebarRoot);
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