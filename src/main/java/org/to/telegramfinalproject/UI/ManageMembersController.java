package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
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
import java.util.Comparator;
import java.util.UUID;

public class ManageMembersController {

    @FXML private VBox membersCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;
    @FXML private Button closeFooterButton;
    @FXML private Button addMembersButton;
    @FXML private ScrollPane membersScroll;
    @FXML private VBox membersList;

    private String groupId;

    @FXML
    private void initialize() {
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(membersCard.getParent()));
        closeFooterButton.setOnAction(e -> MainController.getInstance().closeOverlay(membersCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(membersCard.getParent()));

        addMembersButton.setOnAction(e -> {
            // open add members overlay
            openAddMembersOverlay(groupId);
        });

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

        // Convert safely into JSONObject list
        java.util.List<JSONObject> parsed = new java.util.ArrayList<>();

        for (int i = 0; i < members.length(); i++) {
            Object raw = members.get(i);

            if (raw instanceof JSONObject obj) {
                parsed.add(obj);
            } else if (raw instanceof java.util.Map<?, ?> map) {
                parsed.add(new JSONObject(map));
            } else {
                System.err.println("Skipping invalid member element at index " + i + ": " + raw);
            }
        }

        // Sort: owners first, then others
        parsed.sort((a, b) -> {
            boolean aOwner = "owner".equalsIgnoreCase(a.optString("role"));
            boolean bOwner = "owner".equalsIgnoreCase(b.optString("role"));
            return Boolean.compare(!aOwner, !bOwner); // false < true → owner first
        });

        // Add rows
        for (JSONObject m : parsed) {
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

        Label status = new Label(
                m.optBoolean("is_online", false) ? "online"
                        : "last seen recently"
        );
        status.getStyleClass().add("member-status");

        details.getChildren().addAll(name, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(avatar, details, spacer);

        // Only non-owners can be removed
        String role = m.optString("role", "member");
        if (!"owner".equalsIgnoreCase(role)) {
            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("link-btn");
            removeBtn.setOnAction(e -> removeMember(m.optString("user_id"), row));
            row.getChildren().add(removeBtn);
        }

        membersList.getChildren().add(row);
    }

    private void removeMember(String userId, HBox row) {
        JSONObject req = new JSONObject()
                .put("action", "remove_member_from_group")
                .put("group_id", groupId)   // must be the group's internal_uuid
                .put("user_id", userId);    // target user's internal_uuid

        // Disable the button while request is in progress
        row.setDisable(true);

        new Thread(() -> {
            JSONObject resp = ActionHandler.sendWithResponse(req);

            if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
                Platform.runLater(() -> {
                    membersList.getChildren().remove(row);
                });
            } else {
                Platform.runLater(() -> {
                    row.setDisable(false); // re-enable on error
                    Alert a = new Alert(Alert.AlertType.ERROR,
                            resp != null ? resp.optString("message", "Failed to remove member.")
                                    : "No response from server.",
                            ButtonType.OK);
                    a.show();
                });
            }
        }).start();
    }

    public void openAddMembersOverlay(String groupId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/add_member.fxml"));
            Node overlay = loader.load();

            AddMembersController controller = loader.getController();
            // Pass the groupId as UUID
            controller.setGroupForAdd(UUID.fromString(groupId), "");

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error",
                    "Could not load Add Members scene.",
                    Alert.AlertType.ERROR
            );
        }
    }
}
