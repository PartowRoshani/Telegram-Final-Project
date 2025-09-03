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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NewChannelController {

    @FXML private VBox newChannelCard;
    @FXML private Pane overlayBackground;
    @FXML private TextField channelNameField;
    @FXML private Label channelNameLabel;
    @FXML private TextArea channelDescField;
    @FXML private Label channelDescLabel;
    @FXML private Button cameraButton;
    @FXML private ImageView cameraIcon;
    @FXML private Button cancelButton;
    @FXML private Button createButton;
    @FXML private StackPane overlayRoot;   // the root
    @FXML private Label descCounter;
    @FXML private Label channelIdLabel;
    @FXML private TextField channelIdField;

    private File channelImageFile;

    @FXML
    public void initialize() {
        // Load default camera icon
        cameraIcon.setImage(new Image(
                getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/camera.png")
        ));

        // Camera button action → choose image
        cameraButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Channel Picture");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            File file = chooser.showOpenDialog(cameraButton.getScene().getWindow());
            if (file != null) {
                channelImageFile = file;
                cameraIcon.setImage(new Image(file.toURI().toString()));
            }
        });

        // Cancel → close overlay
        cancelButton.setOnAction(e -> {
            MainController.getInstance().closeOverlay(overlayRoot);
        });

        // Close when clicking outside card
        overlayBackground.setOnMouseClicked(e -> {
            MainController.getInstance().closeOverlay(overlayRoot);
        });

        // Create button → validate name + submit
        createButton.setOnAction(e -> {
            String channelName = channelNameField.getText().trim();
            String channelId = channelIdField.getText().trim();
            String description = channelDescField.getText().trim();

            if (channelName.isEmpty()) {
                // Apply error style
                channelNameField.getStyleClass().add("error");
                channelNameLabel.getStyleClass().add("error");
                return;
            }

            if (channelId.isEmpty()) {
                // Apply error style
                channelIdField.getStyleClass().add("error");
                channelIdLabel.getStyleClass().add("error");
                return;
            }

            try {
                // Load Add Members overlay
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/org/to/telegramfinalproject/Fxml/add_subscriber.fxml"));
                StackPane addSubscribersOverlay = loader.load();

                // Pass channel info to AddMembersController
                AddSubscriberController controller = loader.getController();
                controller.setChannelInfo(channelName, channelId, description, channelImageFile);

                // Close the current "New Channel" overlay
                MainController.getInstance().closeOverlay(overlayRoot);

                // Then open the "Add Members" overlay
                MainController.getInstance().showOverlay(addSubscribersOverlay);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        final int MAX_LENGTH = 255;
        channelDescField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> {
            if (channelDescField.getText().length() >= MAX_LENGTH) {
                e.consume(); // stop extra character from being typed
            }
        });

        channelDescField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.length() > MAX_LENGTH) {
                channelDescField.setText(newText.substring(0, MAX_LENGTH));
                channelDescField.positionCaret(MAX_LENGTH);
            }

            // Current length out of max
            int current = channelDescField.getText().length();
            descCounter.setText(current + " / " + MAX_LENGTH);

            // Style when limit reached
            if (current == MAX_LENGTH) {
                descCounter.setStyle("-fx-text-fill: red;");
            } else {
                descCounter.setStyle(""); // fallback to CSS
            }
        });

        // Initial value
        descCounter.setText("0 / " + MAX_LENGTH);

        // Reset error state when typing
        channelNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                channelNameField.getStyleClass().remove("error");
                channelNameLabel.getStyleClass().remove("error");
            }
        });

        // Reset error state when typing
        channelIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                channelIdField.getStyleClass().remove("error");
                channelIdLabel.getStyleClass().remove("error");
            }
        });

        // Auto-focus channel name on open
        Platform.runLater(() -> channelNameField.requestFocus());

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (newChannelCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(newChannelCard.getScene());
            }
        });
    }
}
