package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

public final class LeaveGroupFlow {

    private LeaveGroupFlow() {}

    // ==== Public API: call this from your controller when menu item clicked
    public static void start(ChatEntry entry) {
        if (entry == null || !"group".equalsIgnoreCase(entry.getType())) {
            info("This action is only available for groups.");
            return;
        }
        final String myUuidStr = Session.getUserUUID();
        if (myUuidStr == null || myUuidStr.isBlank()) {
            error("Cannot determine your identity (internal UUID missing).");
            return;
        }

        if (!entry.isOwner()) {
            // Non-owner: confirm and leave directly
            if (!confirm("Leave Group", "Are you sure you want to leave this group?")) return;
            leaveGroupAsync(entry.getId(), myUuidStr, () -> info("You left the group."));
            return;
        }

        // Owner: choose new owner (server returns list WITHOUT self)
        openOwnerPickerAndLeave(entry.getId(), myUuidStr);
    }

    // ==== Owner flow ====
    private static void openOwnerPickerAndLeave(UUID groupId, String myUuidStr) {
        ProgressIndicator pi = showLoading("Loading admins...");

        Task<List<AdminRow>> task = new Task<>() {
            @Override protected List<AdminRow> call() {
                JSONObject req = new JSONObject()
                        .put("action", "view_group_admins")
                        .put("group_id", groupId.toString())
                        .put("exclude_self", true)   // 👈 سرور خودش Owner را حذف کند
                        .put("viewer_id", myUuidStr); // 👈 برای تشخیص self

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

        task.setOnSucceeded(ev -> {
            hideLoading(pi);

            List<AdminRow> candidates = task.getValue();
            if (candidates == null || candidates.isEmpty()) {
                warn("No other admins available. Promote an admin first, then try again.");
                return;
            }

            // اگر فقط یکی بود، سریع‌تر پیش برو
            if (candidates.size() == 1) {
                AdminRow target = candidates.get(0);
                if (!confirm("Confirm Ownership Transfer",
                        "Transfer ownership to " + target.display() + "?\nYou will leave the group afterward.")) return;
                transferThenLeaveAtomic(groupId, target, myUuidStr);
                return;
            }

            // دیالوگ شیک با سرچ + لیست
            Optional<AdminRow> pick = AdminPickerDialog.show("Transfer Ownership", "Select the new owner", candidates);
            if (pick.isEmpty()) return;
            AdminRow target = pick.get();

            if (!confirm("Confirm Ownership Transfer",
                    "Transfer ownership to " + target.display() + "?\nYou will leave the group afterward.")) return;

            transferThenLeaveAtomic(groupId, target, myUuidStr);
        });

        task.setOnFailed(ev -> {
            hideLoading(pi);
            error("Failed to load admins: " + safeMsg(task.getException()));
        });

        new Thread(task, "load-admins").start();
    }

    // ==== Prefer ATOMIC request; fallback to classic ====
    private static void transferThenLeaveAtomic(UUID groupId, AdminRow target, String myUuidStr) {
        ProgressIndicator pi = showLoading("Transferring & leaving...");

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                // 1) Try atomic endpoint
                JSONObject atomic = new JSONObject()
                        .put("action", "transfer_and_leave_group") // 👈 اکشن اتمیک
                        .put("group_id", groupId.toString())
                        .put("viewer_id", myUuidStr)
                        .put("new_owner_id", target.requireUuid().toString()); // UUID ضروری

                JSONObject res = ActionHandler.sendWithResponse(atomic);
                if (res != null && "success".equalsIgnoreCase(res.optString("status"))) {
                    return null; // done
                }

                // 2) Fallback: classic two-step
                JSONObject tReq = new JSONObject()
                        .put("action", "transfer_group_ownership")
                        .put("group_id", groupId.toString())
                        .put("new_owner_id", target.requireUuid().toString());

                JSONObject tRes = ActionHandler.sendWithResponse(tReq);
                if (tRes == null || !"success".equalsIgnoreCase(tRes.optString("status"))) {
                    throw new RuntimeException(tRes != null ? tRes.optString("message","Ownership transfer failed.")
                            : "null response");
                }

                JSONObject lReq = new JSONObject()
                        .put("action", "leave_chat")
                        .put("user_id", myUuidStr)
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

        task.setOnSucceeded(ev -> {
            hideLoading(pi);
            info("Done. You left the group.");
            // TODO: refresh chat list / close current view
        });

        task.setOnFailed(ev -> {
            hideLoading(pi);
            error("Operation failed: " + safeMsg(task.getException()));
        });

        new Thread(task, "transfer-leave").start();
    }

    // ==== direct leave for non-owner ====
    private static void leaveGroupAsync(UUID groupId, String myUuidStr, Runnable onOk) {
        ProgressIndicator pi = showLoading("Leaving group...");

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                JSONObject req = new JSONObject()
                        .put("action", "leave_chat")
                        .put("user_id", myUuidStr)
                        .put("chat_id", groupId.toString())
                        .put("chat_type", "group");

                JSONObject res = ActionHandler.sendWithResponse(req);
                if (res == null || !"success".equalsIgnoreCase(res.optString("status"))) {
                    throw new RuntimeException(res != null ? res.optString("message","Leave failed.") : "null response");
                }
                return null;
            }
        };

        task.setOnSucceeded(ev -> {
            hideLoading(pi);
            if (onOk != null) onOk.run();
        });
        task.setOnFailed(ev -> {
            hideLoading(pi);
            error("Leave failed: " + safeMsg(task.getException()));
        });

        new Thread(task, "leave-group").start();
    }

    // ====== Admin Picker Dialog (with search + avatars) ======
    private static final class AdminPickerDialog {

        static Optional<AdminRow> show(String title, String header, List<AdminRow> admins) {
            Dialog<AdminRow> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setHeaderText(header);

            ButtonType okType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

            TextField search = new TextField();
            search.setPromptText("Search admin by name or id...");
            search.setMinHeight(36);

            ListView<AdminRow> list = new ListView<>();
            list.getItems().setAll(admins);
            list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            list.setPrefWidth(480);
            list.setPrefHeight(360);

            list.setCellFactory(v -> new ListCell<>() {
                private final HBox row = new HBox(10);
                private final ImageView avatar = new ImageView();
                private final Label name = new Label();
                private final Label id = new Label();
                {
                    row.setAlignment(Pos.CENTER_LEFT);
                    avatar.setFitWidth(36);
                    avatar.setFitHeight(36);
                    avatar.setPreserveRatio(true);
                    name.getStyleClass().add("admin-name");
                    id.getStyleClass().add("admin-id");
                    HBox text = new HBox(8, name, id);
                    row.getChildren().addAll(avatar, text);
                }
                @Override protected void updateItem(AdminRow it, boolean empty) {
                    super.updateItem(it, empty);
                    if (empty || it == null) {
                        setGraphic(null); setText(null);
                    } else {
                        name.setText(it.name);
                        id.setText("• " + it.userId);
                        // آواتار (اختیاری): اگر URL هست نمایش بده
                        if (it.avatarUrl != null && !it.avatarUrl.isBlank()) {
                            try { avatar.setImage(new Image(it.avatarUrl, true)); }
                            catch (Exception e) { avatar.setImage(null); }
                        } else avatar.setImage(null);
                        setGraphic(row); setText(null);
                    }
                }
            });

            // جستجو
            search.textProperty().addListener((obs, o, n) -> {
                String q = (n == null) ? "" : n.trim().toLowerCase(Locale.ROOT);
                List<AdminRow> filtered = admins.stream()
                        .filter(a -> a.name.toLowerCase(Locale.ROOT).contains(q)
                                || normalizeDisplayId(a.userId).contains(q))
                        .collect(Collectors.toList());
                list.getItems().setAll(filtered);
            });

            // دابل کلیک = OK
            list.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
                    dialog.setResult(list.getSelectionModel().getSelectedItem());
                    dialog.close();
                }
            });

            Node okBtn = dialog.getDialogPane().lookupButton(okType);
            okBtn.setDisable(true);
            list.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> okBtn.setDisable(sel == null));

            VBox content = new VBox(12, search, list);
            content.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(bt -> bt == okType ? list.getSelectionModel().getSelectedItem() : null);
            return dialog.showAndWait();
        }
    }

    // ===== Models & helpers =====
    private static final class AdminRow {
        final UUID internalUuid;
        final String userId;
        final String name;
        final String avatarUrl;

        AdminRow(String internalUuidStr, String userId, String name, String avatarUrl) {
            this.internalUuid = parseUuidOrNull(internalUuidStr);
            this.userId = userId == null ? "" : userId;
            this.name = name == null ? "Unknown" : name;
            this.avatarUrl = avatarUrl;
        }

        UUID requireUuid() {
            if (internalUuid == null)
                throw new IllegalStateException("Admin has no internal_uuid; server must provide it.");
            return internalUuid;
        }

        String display() { return name + " (" + userId + ")"; }

        @Override public String toString() {
            return "AdminRow{name='"+name+"', userId='"+userId+"', uuid="+internalUuid+"}";
        }
    }

    private static String normalizeDisplayId(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        return t.toLowerCase(Locale.ROOT);
    }

    private static UUID parseUuidOrNull(String s) {
        try { return (s == null || s.isBlank()) ? null : UUID.fromString(s); }
        catch (Exception e) { return null; }
    }

    private static ProgressIndicator showLoading(String title) {
        Alert a = new Alert(Alert.AlertType.NONE);
        a.setTitle(title);
        ProgressIndicator pi = new ProgressIndicator();
        a.getDialogPane().setContent(pi);
        a.getButtonTypes().clear();
        a.show();
        pi.getProperties().put("alert", a);
        return pi;
    }
    private static void hideLoading(ProgressIndicator pi) {
        Object a = pi.getProperties().get("alert");
        if (a instanceof Alert) ((Alert) a).close();
    }
    static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }
    private static void info(String msg)  { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).show(); }
    private static void warn(String msg)  { new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).show(); }
    private static void error(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).show(); }
    private static String safeMsg(Throwable t) { return (t == null || t.getMessage() == null) ? "Unknown error" : t.getMessage(); }
}
