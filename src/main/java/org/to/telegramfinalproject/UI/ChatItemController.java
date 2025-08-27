package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class ChatItemController {

    @FXML private ImageView profileImage;
    @FXML private Label chatName;
    @FXML private Label lastMessage;
    @FXML private Label chatTime;
    @FXML private Label unreadCount;

    public void setChatData(String name, String lastMsg, String time, int unread) {
        chatName.setText(name);
        lastMessage.setText(lastMsg);
        chatTime.setText(time);
        unreadCount.setVisible(unread > 0);
        unreadCount.setText(String.valueOf(unread));
    }

    public void setUnread(int unread) {
        boolean show = unread > 0;
        unreadCount.setVisible(show);
        unreadCount.setManaged(show);
        if (show) unreadCount.setText(String.valueOf(unread));
    }
}