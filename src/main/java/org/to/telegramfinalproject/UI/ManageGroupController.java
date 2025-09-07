package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ManageGroupController {

    @FXML private VBox manageGroupCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton, changePicButton, cancelButton, saveButton;
    @FXML private ImageView groupImage;
    @FXML private TextField groupNameField;
    @FXML private Label adminCount, memberCount;
    @FXML private TextField groupIdField;
    @FXML private Button manageAdminsButton;
    @FXML private Button manageMembersButton;

    private String groupId; // Internal UUID of the current group
    private String originalName;
    private String originalGroupId;
    private String originalImageUrl;
    private File selectedImageFile;

    private JSONObject data;

    @FXML
    public void initialize() {
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(manageGroupCard.getParent()));
        cancelButton.setOnAction(e -> MainController.getInstance().closeOverlay(manageGroupCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(manageGroupCard.getParent()));

        changePicButton.setOnAction(e -> choosePicture());
        saveButton.setOnAction(e -> saveChanges());

        manageAdminsButton.setOnAction(e -> openManageAdminsScene());
        manageMembersButton.setOnAction(e -> openManageMembersScene(data));
    }

    private void openManageAdminsScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/manage_admins.fxml"));
            Node overlay = loader.load();

//            ManageAdminsController controller = loader.getController();
//            controller.setGroupId(UUID.fromString(groupId));

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error", "Could not load Manage Admins scene.", Alert.AlertType.ERROR);
        }
    }

    private void openManageMembersScene(JSONObject data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/manage_members.fxml"));
            Node overlay = loader.load();

            ManageMembersController controller = loader.getController();
            // Pass groupId and members list from the JSON data
            controller.setGroupData(
                    data.optString("internal_uuid"),
                    data.optJSONArray("members")
            );

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error", "Could not load Manage Members scene.", Alert.AlertType.ERROR);
        }
    }

    public void setGroupData(JSONObject data) {
        this.data = data;
        this.groupId = data.optString("internal_uuid");

        originalName = data.optString("group_name", "");
        originalGroupId = data.optString("group_id", "");
        originalImageUrl = data.optString("image_url", "");

        groupNameField.setText(originalName);
        groupIdField.setText(originalGroupId);

        if (!originalImageUrl.isBlank()) {
            groupImage.setImage(new Image(originalImageUrl, true));
        }

        adminCount.setText(String.valueOf(countRole(data, "admin")));
        memberCount.setText(String.valueOf(countRole(data, "member")));
    }

    private int countRole(JSONObject groupData, String role) {
        var arr = groupData.optJSONArray("members");
        if (arr == null) return 0;
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            if (role.equalsIgnoreCase(arr.getJSONObject(i).optString("role"))) count++;
        }
        return count + 1;
    }

    private void choosePicture() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select group picture");
        selectedImageFile = fc.showOpenDialog(manageGroupCard.getScene().getWindow());
        if (selectedImageFile != null) {
            groupImage.setImage(new Image(selectedImageFile.toURI().toString()));
        }
    }

    private void saveChanges() {
        String newName = groupNameField.getText().trim();
        String newGroupId = groupIdField.getText().trim(); // make sure you added this field
        String newImageUrl = (selectedImageFile != null)
                ? selectedImageFile.toURI().toString()
                : originalImageUrl;

        // Check if anything actually changed
        boolean changed =
                !Objects.equals(originalName, newName) ||
                        !Objects.equals(originalGroupId, newGroupId) ||
                        !Objects.equals(originalImageUrl, newImageUrl);

        if (!changed) {
            // Nothing changed → just close overlay
            MainController.getInstance().closeOverlay(manageGroupCard.getParent());
            return;
        }

        // Build request with all fields (server expects them)
        JSONObject req = new JSONObject()
                .put("action", "edit_group_info")
                .put("group_id", groupId)        // internal_uuid
                .put("new_group_id", newGroupId) // display id
                .put("name", newName);

        if (newImageUrl != null && !newImageUrl.isBlank()) {
            req.put("image_url", newImageUrl);
        } else {
            req.put("image_url", JSONObject.NULL);
        }

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            MainController.getInstance().closeOverlay(manageGroupCard.getParent());
        } else {
            String msg = (resp != null)
                    ? resp.optString("message", "Failed to update group")
                    : "No response from server";
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.show();
        }
    }
}
