package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class ManageChannelController {

    @FXML private VBox manageChannelCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton, changePicButton, cancelButton, saveButton;
    @FXML private ImageView channelImage;
    @FXML private TextField channelNameField;
    @FXML private TextField channelIdField;
    @FXML private TextArea channelDescriptionField;
    @FXML private Label adminCount, subscriberCount;
    @FXML private Button manageAdminsButton;
    @FXML private Button manageSubscribersButton;

    private String channelId; // Internal UUID of the channel
    private String originalName;
    private String originalDisplayId;
    private String originalImageUrl;
    private String originalDescription;
    private File selectedImageFile;

    private JSONObject data;

    @FXML
    public void initialize() {
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(manageChannelCard.getParent()));
        cancelButton.setOnAction(e -> MainController.getInstance().closeOverlay(manageChannelCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(manageChannelCard.getParent()));

        changePicButton.setOnAction(e -> choosePicture());
        saveButton.setOnAction(e -> saveChanges());

        manageAdminsButton.setOnAction(e -> openManageAdminsScene(data));
        manageSubscribersButton.setOnAction(e -> openManageSubscribersScene(data));
    }

    public void setChannelData(JSONObject data) {
        this.data = data;
        this.channelId = data.optString("internal_uuid");

        originalName = data.optString("channel_name", "");
        originalDisplayId = data.optString("channel_id", "");
        originalImageUrl = data.optString("image_url", "");
        originalDescription = data.optString("description", "");

        channelNameField.setText(originalName);
        channelIdField.setText(originalDisplayId);
        channelDescriptionField.setText(originalDescription);

        if (!originalImageUrl.isBlank()) {
            channelImage.setImage(new Image(originalImageUrl, true));
        }
        adminCount.setText(String.valueOf(countRole(data, "admin") + 1)); // owner + admins
        subscriberCount.setText(String.valueOf(countRole(data, "subscriber") + Integer.parseInt(adminCount.getText())));
    }

    private int countRole(JSONObject channelData, String role) {
        var arr = channelData.optJSONArray("subscribers");
        if (arr == null) return 0;
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            if (role.equalsIgnoreCase(arr.getJSONObject(i).optString("role"))) count++;
        }
        return count;
    }

    private void choosePicture() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select channel picture");
        selectedImageFile = fc.showOpenDialog(manageChannelCard.getScene().getWindow());
        if (selectedImageFile != null) {
            channelImage.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    private void saveChanges() {
        String newName = channelNameField.getText().trim();
        String newChannelId = channelIdField.getText().trim();
        String newDescription = channelDescriptionField.getText().trim();
        String newImageUrl = (selectedImageFile != null)
                ? selectedImageFile.toURI().toString()
                : originalImageUrl;

        boolean changed =
                !Objects.equals(originalName, newName) ||
                        !Objects.equals(originalDisplayId, newChannelId) ||
                        !Objects.equals(originalDescription, newDescription) ||
                        !Objects.equals(originalImageUrl, newImageUrl);

        if (!changed) {
            MainController.getInstance().closeOverlay(manageChannelCard.getParent());
            return;
        }

        JSONObject req = new JSONObject()
                .put("action", "edit_channel_info")
                .put("channel_id", channelId) // internal UUID
                .put("new_channel_id", newChannelId)
                .put("name", newName)
                .put("description", newDescription);

        if (newImageUrl != null && !newImageUrl.isBlank()) {
            req.put("image_url", newImageUrl);
        } else {
            req.put("image_url", JSONObject.NULL);
        }

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            MainController.getInstance().closeOverlay(manageChannelCard.getParent());
        } else {
            String msg = (resp != null)
                    ? resp.optString("message", "Failed to update channel")
                    : "No response from server";
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.show();
        }
    }

    private void openManageAdminsScene(JSONObject data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/manage_channel_admins.fxml"));
            Node overlay = loader.load();

            ManageChannelAdminsController controller = loader.getController();
            // Pass channelId and subscribers list from JSON
            controller.setChannelData(
                    data.optString("internal_uuid"),
                    data.optJSONArray("subscribers")
            );

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error", "Could not load Manage Channel Admins scene.", Alert.AlertType.ERROR
            );
        }
    }

    private void openManageSubscribersScene(JSONObject data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/manage_subscribers.fxml"));
            Node overlay = loader.load();

            ManageSubscribersController controller = loader.getController();
            // Pass channelId and subscribers list from the JSON data
            controller.setChannelData(
                    data.optString("internal_uuid"),
                    data.optJSONArray("subscribers")
            );

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error", "Could not load Manage Subscribers scene.", Alert.AlertType.ERROR);
        }
    }
}
