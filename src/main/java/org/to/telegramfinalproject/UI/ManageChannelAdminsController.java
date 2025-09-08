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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManageChannelAdminsController {

    @FXML private VBox adminsCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;
    @FXML private Button closeFooterButton;
    @FXML private Button addAdminButton;

    @FXML private ScrollPane adminsScroll;
    @FXML private VBox adminsList;

    private String channelId; // internal_uuid of channel

    @FXML
    private void initialize() {
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(adminsCard.getParent()));
        closeFooterButton.setOnAction(e -> MainController.getInstance().closeOverlay(adminsCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(adminsCard.getParent()));

        //addAdminButton.setOnAction(e -> openAddAdminOverlay());

        // Smooth scroll feel
        adminsScroll.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        adminsScroll.setPannable(true);
        adminsScroll.setFitToWidth(true);
        adminsScroll.setFitToHeight(false);
        adminsScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            adminsScroll.setVvalue(adminsScroll.getVvalue() - deltaY);
        });
    }

    public void setChannelData(String channelId, JSONArray admins) {
        this.channelId = channelId;

        adminsList.getChildren().clear();

        // Convert to list for sorting
        List<JSONObject> adminList = new ArrayList<>();
        for (int i = 0; i < admins.length(); i++) {
            adminList.add(admins.getJSONObject(i));
        }

        // Sort: owner first, then admins
        adminList.sort((a, b) -> {
            String roleA = a.optString("role", "subscriber");
            String roleB = b.optString("role", "subscriber");
            if ("owner".equalsIgnoreCase(roleA) && !"owner".equalsIgnoreCase(roleB)) return -1;
            if ("owner".equalsIgnoreCase(roleB) && !"owner".equalsIgnoreCase(roleA)) return 1;
            return 0; // keep relative order for admins
        });

        // Add rows
        for (JSONObject a : adminList) {
            addAdminRow(a);
        }
    }

    private void addAdminRow(JSONObject a) {
        HBox row = new HBox(10);
        row.getStyleClass().add("member-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);

        String imgUrl = a.optString("image_url", "");
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
        Label name = new Label(a.optString("profile_name", "Unknown"));
        name.getStyleClass().add("member-name");

        Label status = new Label(
                a.optBoolean("is_online", false) ? "online"
                        : "last seen recently"
        );
        status.getStyleClass().add("member-status");

        details.getChildren().addAll(name, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(avatar, details, spacer);

        // Remove admin (cannot remove owner)
        String role = a.optString("role", "admin");
        if (!"owner".equalsIgnoreCase(role)) {
            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("link-btn");
            removeBtn.setOnAction(e -> removeAdmin(a.optString("user_id"), row));
            row.getChildren().add(removeBtn);
        }

        adminsList.getChildren().add(row);

        // User clicks an admin → open set channel admin permissions scene
        row.setOnMouseClicked(e -> {
            if (!"owner".equalsIgnoreCase(role)) {
                try {
                    // request current permissions from server
                    JSONObject req = new JSONObject()
                            .put("action", "get_channel_admin_permissions")
                            .put("channel_id", channelId)
                            .put("admin_id", a.optString("user_id"));

                    JSONObject resp = ActionHandler.sendWithResponse(req);
                    JSONObject oldPerms = (resp != null) ? resp.optJSONObject("permissions") : null;

                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/org/to/telegramfinalproject/Fxml/set_channel_admin_permissions.fxml"));
                    Node overlay = loader.load();

//                    SetChannelAdminPermissionsController controller = loader.getController();
//                    controller.setTarget(
//                            UUID.fromString(channelId),
//                            UUID.fromString(a.optString("user_id")),
//                            true,
//                            oldPerms
//                    );

                    MainController.getInstance().showOverlay(overlay);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    MainController.getInstance().showAlert("Error", "Could not open permissions scene.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void removeAdmin(String userId, HBox row) {
        JSONObject req = new JSONObject()
                .put("action", "remove_admin_from_channel")
                .put("channel_id", channelId)
                .put("target_user_id", userId);

        System.out.println("Sending remove_admin_from_channel request: " + req.toString(2));

        row.setDisable(true);

        new Thread(() -> {
            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
                Platform.runLater(() -> adminsList.getChildren().remove(row));
            } else {
                Platform.runLater(() -> {
                    row.setDisable(false);
                    Alert a = new Alert(Alert.AlertType.ERROR,
                            resp != null ? resp.optString("message", "Failed to remove admin.") : "No response from server.",
                            ButtonType.OK);
                    a.show();
                });
            }
        }).start();
    }

//    private void openAddAdminOverlay() {
//        try {
//            // === Query the server for full subscribers list ===
//            JSONObject req = new JSONObject()
//                    .put("action", "view_channel_subscribers")
//                    .put("channel_id", channelId);
//
//            JSONObject resp = ActionHandler.sendWithResponse(req);
//
//            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
//                MainController.getInstance().showAlert(
//                        "Error",
//                        "Could not fetch subscribers.",
//                        Alert.AlertType.ERROR
//                );
//                return;
//            }
//
//            JSONObject data = resp.optJSONObject("data");
//            JSONArray subscribers = (data != null) ? data.optJSONArray("subscribers") : new JSONArray();
//
//            // === Load Add Admins FXML (same FXML reused) ===
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(
//                    "/org/to/telegramfinalproject/Fxml/add_admins.fxml"));
//            Node overlay = loader.load();
//
//            AddChannelAdminsController controller = loader.getController();
//            controller.setChannelData(channelId, subscribers);  // send raw subscribers, filtering is done in AddChannelAdminsController
//
//            MainController.getInstance().showOverlay(overlay);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            MainController.getInstance().showAlert(
//                    "Error", "Could not load Add Admins scene.", Alert.AlertType.ERROR
//            );
//        }
//    }
}
