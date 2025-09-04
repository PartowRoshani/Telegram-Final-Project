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

public class BlockedUsersController {

    @FXML private Pane overlayBackground;
    @FXML private Button backButton;
    @FXML private Button closeButton;
    @FXML private Label countLabel;
    @FXML private ListView<HBox> blockedList;
    @FXML private VBox blockedCard;

    @FXML
    public void initialize() {
        // Back button icon
        backButton.setGraphic(makeIcon("/org/to/telegramfinalproject/Icons/back_button_dark.png"));

        // Example blocked users (replace with server data)
        addBlockedUser("Asal", "@Whales_suicide", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Replies", "@replies", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Asal", "@Whales_suicide", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Replies", "@replies", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Asal", "@Whales_suicide", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Replies", "@replies", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Asal", "@Whales_suicide", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Replies", "@replies", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Asal", "@Whales_suicide", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
        addBlockedUser("Replies", "@replies", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");

        updateCount();

        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(overlayBackground.getParent()));
        backButton.setOnAction(e -> MainController.getInstance().goBack((StackPane) overlayBackground.getParent()));

        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(blockedCard.getParent()));

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (blockedCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(blockedCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateBackIcon(newVal);
        });

        // Set initial state
        updateBackIcon(ThemeManager.getInstance().isDarkMode());

        // Add CSS
        blockedList.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm()
        );

        // Wait until skin is ready
        blockedList.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                // Look up vertical scrollbar inside the ListView
                ScrollBar vBar = (ScrollBar) blockedList.lookup(".scroll-bar:vertical");
                if (vBar != null) {
                    blockedList.setOnScroll(event -> {
                        double deltaY = event.getDeltaY() * 0.003; // smaller = smoother
                        double newValue = vBar.getValue() - deltaY;
                        vBar.setValue(Math.max(0, Math.min(newValue, 1))); // clamp between 0–1
                    });
                }
            }
        });
    }

    private void addBlockedUser(String name, String id, String avatarPath) {
        // Avatar
        ImageView avatar = new ImageView(new Image(getClass().getResourceAsStream(avatarPath)));
        avatar.setFitWidth(60);
        avatar.setFitHeight(60);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);
        avatar.setCache(true);

        // round mask
        Circle clip = new Circle(20, 20, 40); // x, y, radius (same as half of fit size)
        avatar.setClip(clip);

        // Name + ID
        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("blocked-name");
        Label idLabel = new Label(id);
        idLabel.getStyleClass().add("blocked-id");
        info.getChildren().addAll(nameLabel, idLabel);

        // Unblock button
        Button unblockBtn = new Button("Unblock");
        unblockBtn.getStyleClass().add("unblock-button");
        unblockBtn.setOnAction(e -> {
            blockedList.getItems().removeIf(h -> h.getChildren().contains(avatar));
            updateCount();
        });

        // Row
        HBox row = new HBox(10, avatar, info, unblockBtn);
        row.setSpacing(12);
        row.getStyleClass().add("blocked-row");
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        blockedList.getItems().add(row);
    }

    private void updateCount() {
        countLabel.setText(blockedList.getItems().size() + " blocked users");
    }

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
}
