package org.to.telegramfinalproject.UI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.AvatarLocalResolver;

import java.io.IOException;
import java.util.UUID;

public class AddAdminsController {

    @FXML private VBox addAdminsCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;
    @FXML private Button closeFooterButton;
    @FXML private ScrollPane membersScroll;
    @FXML private VBox membersList;

    private String groupId;

    @FXML
    private void initialize() {
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(addAdminsCard.getParent()));
        closeFooterButton.setOnAction(e -> MainController.getInstance().closeOverlay(addAdminsCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(addAdminsCard.getParent()));

        // Smooth scroll feel
        membersScroll.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        membersScroll.setPannable(true);
        membersScroll.setFitToWidth(true);
        membersScroll.setFitToHeight(false);
        membersScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            membersScroll.setVvalue(membersScroll.getVvalue() - deltaY);
        });
    }

    public void setGroupData(String groupId, JSONArray members) {
        this.groupId = groupId;
        membersList.getChildren().clear();

        for (int i = 0; i < members.length(); i++) {
            JSONObject m = members.getJSONObject(i);

            String role = m.optString("role", "member");
            if ("owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
                continue; // skip existing admins/owner
            }

            addMemberRow(m);
        }
    }

    private void addMemberRow(JSONObject m) {
        HBox row = new HBox(10);
        row.getStyleClass().add("member-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);

        String imgUrl = m.optString("image_url", "");
        if (!imgUrl.isBlank()) {
            Image img = AvatarLocalResolver.load(imgUrl);
            if (img != null) avatar.setImage(img);
        } else {
            avatar.setImage(new Image(getClass().getResourceAsStream(
                    "/org/to/telegramfinalproject/Avatars/default_user_profile.png"
            )));
        }

        // Name + Status
        VBox details = new VBox(2);
        Label name = new Label(m.optString("profile_name", "Unknown"));
        name.getStyleClass().add("member-name");

        Label status = new Label(m.optBoolean("is_online", false) ? "online" : "last seen recently");
        status.getStyleClass().add("member-status");

        details.getChildren().addAll(name, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button promoteBtn = new Button("Promote");
        promoteBtn.getStyleClass().add("link-btn");
        promoteBtn.setOnAction(e -> promoteToAdmin(m.optString("internal_uuid"), row));

        row.getChildren().addAll(avatar, details, spacer, promoteBtn);
        membersList.getChildren().add(row);
    }

    private void promoteToAdmin(String internalUuid, HBox row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/set_group_admin_permissions.fxml"));
            Node overlay = loader.load();

            SetGroupAdminPermissionsController controller = loader.getController();
            controller.setTarget(UUID.fromString(groupId), UUID.fromString(internalUuid), false);

            // Pass a callback to remove row if promotion succeeds
            controller.setOnSuccess(() -> membersList.getChildren().remove(row));

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error", "Could not load permissions scene.", Alert.AlertType.ERROR
            );
        }
    }
}
