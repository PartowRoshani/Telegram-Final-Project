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

import java.io.File;

public class ChatPageController {

    // ===== messages area =====
    @FXML private VBox messageContainer;
    @FXML private ScrollPane messageScrollPane;

    // ===== input area =====
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;

    @FXML private Button attachmentButton;
    @FXML private ImageView attachmentIcon;   // <ImageView> inside the attachment button

    // ===== header =====
    @FXML private ImageView userAvatar;       // 36x36 in the FXML
    @FXML private Label chatTitle;            // contact/group title
    @FXML private Label chatStatus;           // last seen / online

    @FXML private Button searchInChatButton;  // magnifier button
    @FXML private ImageView searchIcon;

    @FXML private Button moreButton;          // 3-dots button
    @FXML private ImageView moreIcon;
    @FXML private ContextMenu moreMenu;
    @FXML private MenuItem viewProfileItem;
    @FXML private MenuItem deleteChatItem;

    // ===== send icon =====
    @FXML private ImageView sendIcon;

    // ===== state =====
    private String chatName;
    private final ThemeManager themeManager = ThemeManager.getInstance();

    // Where your icons live
    private static final String ICON_BASE = "/org/to/telegramfinalproject/Icons/";

    @FXML
    public void initialize() {
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

    /** Called by main controller when opening a chat. */
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

    /** Add a normal message bubble (very simple for now). */
    public void addMessage(String sender, String content) {
        Label msg = new Label(sender + ": " + content);
        msg.setWrapText(true);

        boolean dark = themeManager.isDarkMode();
        String bubbleColor = dark ? "#20405a" : "#4fa8f0";
        String textColor   = dark ? "#e8f1f8" : "#0f141a";
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

    /** Update all header/footer icons according to current theme. */
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
}
