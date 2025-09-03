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

import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

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
    @FXML private TextField groupIdField;
    @FXML private Label groupIdLabel;

    private File groupImageFile;

    @FXML
    public void initialize() {
        cameraIcon.setImage(new Image(
                getClass().getResourceAsStream("/org/to/telegramfinalproject/Icons/camera.png")
        ));

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

        cancelButton.setOnAction(e -> MainController.getInstance().closeOverlay(overlayRoot));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(overlayRoot));

        nextButton.setOnAction(e -> onNext());

        // clear errors on typing
        groupNameField.textProperty().addListener((obs, ov, nv) -> {
            if (!nv.trim().isEmpty()) {
                groupNameField.getStyleClass().remove("error");
                groupNameLabel.getStyleClass().remove("error");
            }
        });
        groupIdField.textProperty().addListener((obs, ov, nv) -> {
            if (!nv.trim().isEmpty()) {
                groupIdField.getStyleClass().remove("error");
                groupIdLabel.getStyleClass().remove("error");
            }
        });

        Platform.runLater(() -> groupNameField.requestFocus());
    }

    private void onNext() {
        String groupName = groupNameField.getText() == null ? "" : groupNameField.getText().trim();
        String groupId   = groupIdField.getText()   == null ? "" : groupIdField.getText().trim();

        boolean ok = true;
        if (groupName.isEmpty()) {
            groupNameField.getStyleClass().add("error");
            groupNameLabel.getStyleClass().add("error");
            ok = false;
        }
        if (groupId.isEmpty()) {
            groupIdField.getStyleClass().add("error");
            groupIdLabel.getStyleClass().add("error");
            ok = false;
        }
        if (!ok) return;

        // ساخت گروه روی سرور
        final String me = Session.getUserUUID(); // internal_uuid
        if (me == null || me.isBlank()) {
            showToast("Cannot create group: current user UUID missing.");
            return;
        }

        // اگر آپلود تصویر داری، اینجا عکس رو آپلود کن و imageUrl واقعی بفرست (TODO)
        final String imageUrl = null;

        JSONObject req = new JSONObject()
                .put("action", "create_group")
                .put("group_id", groupId)
                .put("group_name", groupName)
                .put("user_id", me)          // ← internal_uuid سازنده
                .put("image_url", imageUrl); // ← اختیاری

        new Thread(() -> {
            JSONObject resp = ActionHandler.sendWithResponse(req);
            if (resp == null || !"success".equalsIgnoreCase(resp.optString("status"))) {
                Platform.runLater(() -> showToast("Create failed: " +
                        (resp == null ? "no response" : resp.optString("message",""))));
                return;
            }

            JSONObject data = resp.optJSONObject("data");
            if (data == null) {
                Platform.runLater(() -> showToast("Create failed: empty data."));
                return;
            }

            UUID internalId = UUID.fromString(data.optString("internal_id"));
            String returnedName = data.optString("name", groupName);
            String returnedDisp = data.optString("id", groupId);
            String returnedImg  = data.optString("image_url", "");

            // 1) به‌صورت لوکال یک ChatEntry بساز و به سایدبار اضافه و سِلکت کن
            Platform.runLater(() -> {
                ChatEntry entry = ChatEntry.fromServer(
                        internalId,
                        "group",
                        returnedName,
                        returnedDisp,
                        returnedImg,
                        /*isOwner*/ true,
                        /*isAdmin*/ true
                );
                MainController.getInstance().addChatAndSelect(entry);
            });

            // 2) پنجره‌ی Add Members را باز کن و internal_id گروه را پاس بده
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/org/to/telegramfinalproject/Fxml/add_member.fxml"));
                    StackPane addMembersOverlay = loader.load();
                    AddMembersController controller = loader.getController();
                    controller.setGroupInfo(internalId, returnedName, returnedDisp, groupImageFile);

                    // این اُورلی را روی UI نشان بده
                    MainController.getInstance().showOverlay(addMembersOverlay);

                    // این اُورلی NewGroup را ببند
                    MainController.getInstance().closeOverlay(overlayRoot);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showToast("Failed to open Add Members.");
                }
            });
        }).start();
    }

    private void showToast(String msg) {
        // جایگزینش کن با سیستم نوتی شما
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.initOwner(overlayRoot.getScene().getWindow());
        a.show();
    }
}
