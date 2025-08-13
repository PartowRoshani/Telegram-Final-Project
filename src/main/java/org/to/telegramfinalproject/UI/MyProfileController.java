package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.to.telegramfinalproject.Models.User;

public class MyProfileController {

    @FXML private ImageView profileImage;
    @FXML private Label displayName;
    @FXML private Label status;
    @FXML private Label bio;
    @FXML private Label userId;


//    public void initialize() {
//        // Load data from your logged-in user object
//        // Example:
//        displayName.setText(CurrentUser.getName());
//        status.setText(CurrentUser.isOnline() ? "online" : "offline");
//        bio.setText(CurrentUser.getBio());
//        userId.setText(CurrentUser.getUserId());
//
//        if (CurrentUser.getProfileImagePath() != null) {
//            profileImage.setImage(new Image(CurrentUser.getProfileImagePath()));
//        }
//    }
}