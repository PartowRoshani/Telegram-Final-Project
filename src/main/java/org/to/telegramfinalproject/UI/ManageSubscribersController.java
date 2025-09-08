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
import java.util.UUID;

public class ManageSubscribersController {

    @FXML private VBox subscribersCard;
    @FXML private Pane overlayBackground;
    @FXML private Button closeButton;
    @FXML private Button closeFooterButton;
    @FXML private Button addSubscribersButton;
    @FXML private ScrollPane subscribersScroll;
    @FXML private VBox subscribersList;

    private String channelId; // internal UUID

    @FXML
    private void initialize() {
        closeButton.setOnAction(e -> MainController.getInstance().closeOverlay(subscribersCard.getParent()));
        closeFooterButton.setOnAction(e -> MainController.getInstance().closeOverlay(subscribersCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(subscribersCard.getParent()));

        addSubscribersButton.setOnAction(e -> openAddSubscribersOverlay(channelId));

        // Smooth scroll
        subscribersScroll.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm()
        );
        subscribersScroll.setPannable(true);
        subscribersScroll.setFitToWidth(true);
        subscribersScroll.setFitToHeight(false);
        subscribersScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            subscribersScroll.setVvalue(subscribersScroll.getVvalue() - deltaY);
        });
    }

    public void setChannelData(String channelId, JSONArray subscribers) {
        this.channelId = channelId;
        subscribersList.getChildren().clear();

        for (int i = 0; i < subscribers.length(); i++) {
            Object raw = subscribers.get(i);
            JSONObject sub = (raw instanceof JSONObject)
                    ? (JSONObject) raw
                    : new JSONObject((java.util.Map<?, ?>) raw);

            addSubscriberRow(sub);
        }
    }

    private void addSubscriberRow(JSONObject sub) {
        HBox row = new HBox(10);
        row.getStyleClass().add("member-row");
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);

        String imgUrl = sub.optString("image_url", "");
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
        Label name = new Label(sub.optString("profile_name", "Unknown"));
        name.getStyleClass().add("member-name");

        Label status = new Label(
                sub.optBoolean("is_online", false) ? "online" : "last seen recently"
        );
        status.getStyleClass().add("member-status");

        details.getChildren().addAll(name, status);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(avatar, details, spacer);

        // Only non-owners can be removed
        String role = sub.optString("role", "subscriber");
        if (!"owner".equalsIgnoreCase(role)) {
            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("link-btn");
            removeBtn.setOnAction(e -> removeSubscriber(sub.optString("user_id"), row));
            row.getChildren().add(removeBtn);
        }

        subscribersList.getChildren().add(row);
    }

    private void removeSubscriber(String userId, HBox row) {
        JSONObject req = new JSONObject()
                .put("action", "remove_subscriber_from_channel")
                .put("channel_id", channelId)
                .put("user_id", userId);

        row.setDisable(true);

        new Thread(() -> {
            JSONObject resp = ActionHandler.sendWithResponse(req);

            if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
                Platform.runLater(() -> subscribersList.getChildren().remove(row));
            } else {
                Platform.runLater(() -> {
                    row.setDisable(false);
                    Alert a = new Alert(Alert.AlertType.ERROR,
                            resp != null ? resp.optString("message", "Failed to remove subscriber.")
                                    : "No response from server.",
                            ButtonType.OK);
                    a.show();
                });
            }
        }).start();
    }

    private void openAddSubscribersOverlay(String channelId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/to/telegramfinalproject/Fxml/add_subscriber.fxml"));
            Node overlay = loader.load();

            AddSubscriberController controller = loader.getController();
            controller.setChannelInfo(UUID.fromString(channelId), "", "", null, "");

            MainController.getInstance().showOverlay(overlay);
        } catch (IOException e) {
            e.printStackTrace();
            MainController.getInstance().showAlert(
                    "Error", "Could not load Add Subscribers scene.", Alert.AlertType.ERROR
            );
        }
    }
}
