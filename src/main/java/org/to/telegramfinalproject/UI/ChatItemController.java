package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.util.Objects;

public class ChatItemController {

    @FXML private ImageView profileImage;
    @FXML private Label chatName;
    @FXML private Label lastMessage;
    @FXML private Label chatTime;
    @FXML private Label unreadCount;
    @FXML private StackPane systemAvatar;
    @FXML private ImageView profileImageUser;
    @FXML private ImageView profileImageSystem;
    @FXML private Circle systemCircle;

    // Default avatar resource path
    private static final String DEFAULT_AVATAR =
            "/org/to/telegramfinalproject/Avatars/default_avatar.png";

    /**
     * Set chat item data, including a profile image (or default).
     *
     * @param name    Chat name
     * @param lastMsg Last message text
     * @param time    Last message time
     * @param unread  Unread message count
     * @param imageUrl Path/URL of profile image (can be null/empty)
     */
    public void setChatData(String name, String lastMsg, String time, int unread, String imageUrl) {
        chatName.setText(name);
        lastMessage.setText(lastMsg);
        chatTime.setText(time);

        // Unread count
        if (unread > 0) {
            unreadCount.setVisible(true);
            unreadCount.setText(String.valueOf(unread));
        } else {
            unreadCount.setVisible(false);
        }

        // Reset all avatar states
        profileImageUser.setVisible(false);
        profileImageUser.setManaged(false);
        systemAvatar.setVisible(false);
        systemAvatar.setManaged(false);

        if ("Saved Messages".equals(name)) {
            systemCircle.setFill(javafx.scene.paint.Color.web("#2ca4ff")); // Telegram blue
            profileImageSystem.setImage(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(
                            "/org/to/telegramfinalproject/Icons/saved_messages_light.png"))
            ));
            systemAvatar.setVisible(true);
            systemAvatar.setManaged(true);

        } else if ("Archived Chats".equals(name)) {
            systemCircle.setFill(javafx.scene.paint.Color.web("#808080")); // gray
            profileImageSystem.setImage(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(
                            "/org/to/telegramfinalproject/Icons/archived_chats_light.png"))
            ));
            systemAvatar.setVisible(true);
            systemAvatar.setManaged(true);

        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            profileImageUser.setImage(new Image(imageUrl, true));
            profileImageUser.setVisible(true);
            profileImageUser.setManaged(true);

        } else {
            profileImageUser.setImage(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(
                            "/org/to/telegramfinalproject/Icons/default_profile.png"))
            ));
            profileImageUser.setVisible(true);
            profileImageUser.setManaged(true);
        }
    }

    public void setUnread(int unread) {
        boolean show = unread > 0;
        unreadCount.setVisible(show);
        unreadCount.setManaged(show);
        if (show) unreadCount.setText(String.valueOf(unread));
    }
}
