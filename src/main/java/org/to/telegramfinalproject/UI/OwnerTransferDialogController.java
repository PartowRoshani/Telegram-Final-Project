package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.UUID;

import static org.to.telegramfinalproject.UI.LeaveGroupFlow.confirm;

public class OwnerTransferDialogController {

    @FXML private TextField searchField;
    @FXML private ListView<AdminRow> adminList;
    @FXML private Button okBtn;
    @FXML private Button cancelBtn;

    private UUID groupId;
    private String viewerUuid; // internal UUID string
    private Runnable onSuccess; // callback برای refresh UI

    private FilteredList<AdminRow> filtered;

    // ===== API =====
    public void init(UUID groupId, String viewerUuid, Runnable onSuccess) {
        this.groupId = groupId;
        this.viewerUuid = viewerUuid;
        this.onSuccess = onSuccess;
        setupUI();
        loadAdmins();
    }

    private void setupUI() {
        okBtn.setDisable(true);
        adminList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> okBtn.setDisable(n == null));
        cancelBtn.setOnAction(e -> cancel());

        // Double-click = OK
        adminList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && adminList.getSelectionModel().getSelectedItem() != null) {
                onOkClick();
            }
        });

        // Cell factory (Avatar + name + id)
        adminList.setCellFactory(v -> new ListCell<>() {
            private final HBox row = new HBox(10);
            private final ImageView avatar = new ImageView();
            private final Label name = new Label();
            private final Label id = new Label();
            {
                avatar.setFitWidth(32); avatar.setFitHeight(32); avatar.setPreserveRatio(true);
                row.getChildren().addAll(avatar, name, id);
            }
            @Override protected void updateItem(AdminRow it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) {
                    setGraphic(null); setText(null);
                } else {
                    name.setText(it.name);
                    id.setText("• " + it.userId);
                    if (it.avatarUrl != null && !it.avatarUrl.isBlank()) {
                        try { avatar.setImage(new Image(it.avatarUrl, true)); } catch (Exception ex) { avatar.setImage(null); }
                    } else avatar.setImage(null);
                    setGraphic(row); setText(null);
                }
            }
        });

        // search
        searchField.textProperty().addListener((obs, o, n) -> {
            final String q = (n == null) ? "" : n.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(makePredicate(q));
        });

        okBtn.setOnAction(e -> onOkClick());
    }

    private Predicate<AdminRow> makePredicate(String q) {
        if (q.isBlank()) return r -> true;
        return r -> r.name.toLowerCase(Locale.ROOT).contains(q)
                || normalizeDisplayId(r.userId).contains(q);
    }

    private void loadAdmins() {
        ProgressIndicator pi = modalSpinner("Loading admins...");

        Task<List<AdminRow>> t = new Task<>() {
            @Override protected List<AdminRow> call() {
                JSONObject req = new JSONObject()
                        .put("action", "view_group_admins")
                        .put("group_id", groupId.toString())
                        .put("exclude_self", true)
                        .put("viewer_id", viewerUuid);
                JSONObject res = ActionHandler.sendWithResponse(req);
                if (res == null || !"success".equalsIgnoreCase(res.optString("status"))) {
                    throw new RuntimeException(res != null ? res.optString("message","Failed to fetch admins.")
                            : "null response");
                }
                JSONArray arr = res.getJSONObject("data").optJSONArray("admins");
                List<AdminRow> list = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject a = arr.getJSONObject(i);
                        list.add(new AdminRow(
                                a.optString("internal_uuid",""),
                                a.optString("user_id",""),
                                a.optString("profile_name","Unknown"),
                                a.optString("avatar_url","")
                        ));
                    }
                }
                return list;
            }
        };

        t.setOnSucceeded(ev -> {
            closeSpinner(pi);
            List<AdminRow> admins = t.getValue();
            if (admins == null) admins = List.of();
            if (admins.isEmpty()) {
                alert(Alert.AlertType.WARNING, "No other admins available. Promote an admin first, then try again.");
            }
            filtered = new FilteredList<>(FXCollections.observableArrayList(admins), r -> true);
            adminList.setItems(filtered);
        });

        t.setOnFailed(ev -> {
            closeSpinner(pi);
            alert(Alert.AlertType.ERROR, "Failed to load admins: " + safeMsg(t.getException()));
        });

        new Thread(t, "load-admins").start();
    }

    private void onOkClick() {
        AdminRow sel = adminList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if (!confirm("Confirm Ownership Transfer",
                "Transfer ownership to " + sel.display() + "?\nYou will leave the group afterward.")) return;

        ProgressIndicator pi = modalSpinner("Transferring & leaving...");

        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                // 1) تلاش برای اکشن اتمیک
                JSONObject atomic = new JSONObject()
                        .put("action", "transfer_and_leave_group")
                        .put("group_id", groupId.toString())
                        .put("viewer_id", viewerUuid)
                        .put("new_owner_id", sel.requireUuid().toString());

                JSONObject res = ActionHandler.sendWithResponse(atomic);
                if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
                    return null;
                }

                // 2) Fallback: دو مرحله‌ای
                JSONObject tReq = new JSONObject()
                        .put("action", "transfer_group_ownership")
                        .put("group_id", groupId.toString())
                        .put("new_owner_id", sel.requireUuid().toString());
                JSONObject tRes = ActionHandler.sendWithResponse(tReq);
                if (tRes == null || !"success".equalsIgnoreCase(tRes.optString("status"))) {
                    throw new RuntimeException(tRes != null ? tRes.optString("message","Ownership transfer failed.")
                            : "null response");
                }

                JSONObject lReq = new JSONObject()
                        .put("action", "leave_chat")
                        .put("user_id", viewerUuid)
                        .put("chat_id", groupId.toString())
                        .put("chat_type", "group");
                JSONObject lRes = ActionHandler.sendWithResponse(lReq);
                if (lRes == null || !"success".equalsIgnoreCase(lRes.optString("status"))) {
                    throw new RuntimeException(lRes != null ? lRes.optString("message","Leave failed.")
                            : "null response");
                }
                return null;
            }
        };

        t.setOnSucceeded(ev -> {
            closeSpinner(pi);
            alert(Alert.AlertType.INFORMATION, "Done. You left the group.");
            closeDialog();
            if (onSuccess != null) onSuccess.run();
        });

        t.setOnFailed(ev -> {
            closeSpinner(pi);
            alert(Alert.AlertType.ERROR, "Operation failed: " + safeMsg(t.getException()));
        });

        new Thread(t, "transfer-leave").start();
    }

    private void cancel() { closeDialog(); }

    // ===== Utilities =====
    private ProgressIndicator modalSpinner(String title) {
        Alert a = new Alert(Alert.AlertType.NONE);
        a.setTitle(title);
        ProgressIndicator pi = new ProgressIndicator();
        a.getDialogPane().setContent(pi);
        a.getButtonTypes().clear();
        a.show();
        pi.getProperties().put("alert", a);
        return pi;
    }
    private void closeSpinner(ProgressIndicator pi) {
        Object a = pi.getProperties().get("alert");
        if (a instanceof Alert) ((Alert)a).close();
    }

    private void closeDialog() {
        // بستن پنجره میزبان کنترلر (DialogStage):
        adminList.getScene().getWindow().hide();
    }

    private static String normalizeDisplayId(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        return t.toLowerCase(Locale.ROOT);
    }
    private static String safeMsg(Throwable t) { return (t == null || t.getMessage() == null) ? "Unknown error" : t.getMessage(); }

    // ===== Row model =====
    public static final class AdminRow {
        public final UUID internalUuid;
        public final String userId;
        public final String name;
        public final String avatarUrl;

        public AdminRow(String internalUuidStr, String userId, String name, String avatarUrl) {
            this.internalUuid = parseUuidOrNull(internalUuidStr);
            this.userId = userId == null ? "" : userId;
            this.name = name == null ? "Unknown" : name;
            this.avatarUrl = avatarUrl;
        }
        public UUID requireUuid() {
            if (internalUuid == null) throw new IllegalStateException("admin.internal_uuid missing");
            return internalUuid;
        }
        public String display() { return name + " (" + userId + ")"; }

        private static UUID parseUuidOrNull(String s) {
            try { return (s == null || s.isBlank()) ? null : UUID.fromString(s); }
            catch (Exception e) { return null; }
        }
    }

    private static void alert(Alert.AlertType type, String msg) {
        new Alert(type, msg, ButtonType.OK).show();
    }
}
