package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.layout.Pane;
import java.io.File;
import java.io.IOException;

public class NewGroupController {

    @FXML private VBox newGroupCard;
    @FXML private Pane overlayBackground;
    @FXML private TextField groupNameField;
    @FXML private Label groupNameLabel;
    @FXML private Button cameraButton;
    @FXML private ImageView cameraIcon;
    @FXML private Button cancelButton;
    @FXML private Button nextButton;
    @FXML private StackPane overlayRoot;   // the root

    private File groupImageFile;

    @FXML
    public void initialize() {
        // Load default camera icon
        cameraIcon.setImage(new Image(
                getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/camera.png")
        ));

        // Camera button action → choose image
        cameraButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Group Picture");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            File file = chooser.showOpenDialog(cameraButton.getScene().getWindow());
            if (file != null) {
                groupImageFile = file;
                cameraIcon.setImage(new Image(file.toURI().toString()));
            }
        });

        cancelButton.setOnAction(e -> {
            MainController.getInstance().closeOverlay(overlayRoot);
        });

        overlayBackground.setOnMouseClicked(e -> {
            MainController.getInstance().closeOverlay(overlayRoot);
        });

        nextButton.setOnAction(e -> {
            String groupName = groupNameField.getText().trim();

            if (groupName.isEmpty()) {
                // Apply error style
                groupNameField.getStyleClass().add("error");
                groupNameLabel.getStyleClass().add("error");

                return;
            }

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/org/to/telegramfinalproject/Fxml/add_member.fxml"));
                StackPane addMembersOverlay = loader.load();

                // Optional: pass groupName and image to AddMembersController
                AddMembersController controller = loader.getController();
                controller.setGroupInfo(groupName, groupImageFile);

                // Close the current "New Group" overlay
                MainController.getInstance().closeOverlay(overlayRoot);

                // Then open the Add Members overlay
                MainController.getInstance().showOverlay(addMembersOverlay);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        groupNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                groupNameField.getStyleClass().remove("error");
                groupNameLabel.getStyleClass().remove("error");
            }
        });

        // Auto_focus search bar when overlay opens
        Platform.runLater(() -> groupNameField.requestFocus());
    }
}
