package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;

import java.util.UUID;

public class SetGroupAdminPermissionsController {

    @FXML private VBox permissionsCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    @FXML private CheckBox canAddMember;
    @FXML private CheckBox canRemoveMember;
    @FXML private CheckBox canAddAdmin;
    @FXML private CheckBox canRemoveAdmin;
    @FXML private CheckBox canEditGroup;

    private UUID groupId;
    private UUID userId;
    private Runnable onSuccess;

    private JSONObject oldPermissions; // store existing permissions
    private boolean isEditMode = false; // true if editing existing admin

    public void setTarget(UUID groupId, UUID userId, boolean isEditMode) {
        this.groupId = groupId;
        this.userId = userId;
        this.isEditMode = isEditMode;

        if (isEditMode) {
            loadCurrentPermissions();
        }
    }

    public void setTarget(UUID groupId, UUID userId, boolean isEditMode, JSONObject oldPermissions) {
        this.groupId = groupId;
        this.userId = userId;
        this.isEditMode = isEditMode;
        this.oldPermissions = oldPermissions;
    }

    public void setOnSuccess(Runnable r) {
        this.onSuccess = r;
    }

    @FXML
    private void initialize() {
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(permissionsCard.getParent()));
        cancelButton.setOnAction(e -> MainController.getInstance().closeOverlay(permissionsCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(permissionsCard.getParent()));

        saveButton.setOnAction(e -> savePermissions());

        // Register for dark mode updates
        Platform.runLater(() -> {
            if (permissionsCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(permissionsCard.getScene());
            }
        });
    }

    private void loadCurrentPermissions() {
        JSONObject req = new JSONObject()
                .put("action", "get_group_admin_permissions")
                .put("group_id", groupId.toString())
                .put("admin_id", userId.toString());

        JSONObject resp = ActionHandler.sendWithResponse(req);
        System.out.println("Permissions response: " + resp); // full server response

        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            // "data" is an object containing { "permissions": { ... } }
            JSONObject data = resp.optJSONObject("data");
            System.out.println("Data object: " + data);

            if (data != null) {
                JSONObject perms = data.optJSONObject("permissions");
                System.out.println("Extracted permissions object: " + perms);

                if (perms != null) {
                    oldPermissions = perms;

                    // Update checkboxes safely on FX thread
                    Platform.runLater(() -> {
                        canAddMember.setSelected(perms.optBoolean("can_add_member", false));
                        canRemoveMember.setSelected(perms.optBoolean("can_remove_member", false));
                        canAddAdmin.setSelected(perms.optBoolean("can_add_admin", false));
                        canRemoveAdmin.setSelected(perms.optBoolean("can_remove_admin", false));
                        canEditGroup.setSelected(perms.optBoolean("can_edit_group", false));
                    });
                } else {
                    System.out.println("⚠️ Permissions object was null inside data.");
                }
            } else {
                System.out.println("⚠️ Data object was null in response.");
            }
        } else {
            System.out.println("⚠️ Response was null or status != success");
        }
    }

    private void savePermissions() {
        JSONObject newPermissions = new JSONObject()
                .put("can_add_member", canAddMember.isSelected())
                .put("can_remove_member", canRemoveMember.isSelected())
                .put("can_add_admin", canAddAdmin.isSelected())
                .put("can_remove_admin", canRemoveAdmin.isSelected())
                .put("can_edit_group", canEditGroup.isSelected());

        JSONObject req;
        if (isEditMode) {
            // --- Editing existing admin permissions ---
            req = new JSONObject()
                    .put("action", "edit_admin_permissions")
                    .put("chat_id", groupId.toString())
                    .put("chat_type", "group") // since this controller is only for groups
                    .put("admin_id", userId.toString())
                    .put("permissions", newPermissions);
        } else {
            // --- Adding a new admin ---
            req = new JSONObject()
                    .put("action", "add_admin_to_group")
                    .put("group_id", groupId.toString())
                    .put("user_id", userId.toString())
                    .put("permissions", newPermissions);
        }

        JSONObject resp = ActionHandler.sendWithResponse(req);
        if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
            MainController.getInstance().closeOverlay(permissionsCard.getParent());
            if (onSuccess != null) onSuccess.run(); // update UI after success
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR,
                    resp != null ? resp.optString("message") : "Failed to save admin permissions.",
                    ButtonType.OK);
            a.show();
        }
    }
}
