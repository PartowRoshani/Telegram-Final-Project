package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class  SidebarMenuController {
    @FXML private ImageView profileImage;
    @FXML private Label usernameLabel;

    @FXML private Button myProfileButton;
    @FXML private Button newGroupButton;
    @FXML private Button newChannelButton;
    @FXML private Button contactsButton;
    @FXML private Button savedMessagesButton;
    @FXML private Button settingsButton;
    @FXML private Button telegramFeaturesButton;
    @FXML private Button telegramQnAButton;

    @FXML private ToggleButton nightModeToggle;

    @FXML
    public void initialize() {
        Image image = new Image(getClass().getResource("/org/to/telegramfinalproject/Images/profile.png").toExternalForm());
        profileImage.setImage(image);

        // Called automatically after FXML is loaded
        usernameLabel.setText("Asal");
    }
}
