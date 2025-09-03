package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.scene.control.Button;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Client.TelegramClient;
import org.to.telegramfinalproject.Models.User;
import org.to.telegramfinalproject.Utils.FileUtil;


import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

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

    @FXML
    private ImageView profileImageView;

    @FXML
    private Button uploadButton;

    private Socket clientSocket;

    public MyProfileController(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @FXML
    private void initialize() {
        String currentImageUrl = Session.currentUser.optString("image_url", "default.png");
        profileImageView.setImage(new Image("file:" + currentImageUrl));
    }

    @FXML
    private void handleUploadProfilePicture() {
        System.out.println("[MyProfileController] Upload button clicked.");//Log 1

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (selectedFile != null) {
            System.out.println("[MyProfileController] File selected: " + selectedFile.getAbsolutePath() +
                    " (size=" + selectedFile.length() + ")"); // log 2

            try {
                TelegramClient client = TelegramClient.getInstance();
                System.out.println("[MyProfileController] Calling FileUtil.uploadProfilePicture...");
                JSONObject response = FileUtil.uploadProfilePicture(
                        selectedFile,
                        UUID.fromString(Session.getUserUUID()),
                        client.getOut(),
                        client.getIn(),
                        client.getOutBin()
                );

                System.out.println("[MyProfileController] Server response: " + response); // log 3

                if ("success".equals(response.getString("status"))) {
                    profileImageView.setImage(new Image(selectedFile.toURI().toString()));
                    Session.currentUser.put("image_url",
                            response.getJSONObject("data").getString("image_url"));
                    System.out.println("Profile picture updated successfully!");
                } else {
                    System.out.println("Failed to update picture: " + response.getString("message"));
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error while uploading file.");
            }
        }else {
            System.out.println("[MyProfileController] No file selected.");
        }
    }
}