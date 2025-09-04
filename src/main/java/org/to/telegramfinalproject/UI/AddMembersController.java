package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AddMembersController {

    @FXML private VBox contactsList;
    @FXML private VBox addMembersCard;
    @FXML private TextField searchField;
    @FXML private Pane overlayBackground;
    @FXML private ScrollPane contactsScroll;
    @FXML private Button searchIcon;
    @FXML private Label memberCountLabel;
    @FXML private FlowPane selectedMembersPane;
    @FXML private Button cancelButton;
    @FXML private Button createButton;

    // گروه هدف
    private UUID groupInternalId;
    private String groupName;
    private String groupDisplayId;
    private File groupImageFile;

    // انتخاب‌ها
    private final Set<Contact> selectedContacts = new HashSet<>();
    // همه‌ی کانتکت‌ها
    private final List<Contact> allContacts = new ArrayList<>();

    @FXML
    public void initialize() {
        // اسکرول نرم
        contactsScroll.getStylesheets().add(
                getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm()
        );
        contactsScroll.setPannable(true);
        contactsScroll.setFitToWidth(true);
        contactsScroll.setFitToHeight(false);
        contactsScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            contactsScroll.setVvalue(contactsScroll.getVvalue() - deltaY);
        });

        // آیکن سرچ با تم
        Platform.runLater(() -> {
            if (addMembersCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(addMembersCard.getScene());
            }
        });
        ThemeManager.getInstance().darkModeProperty().addListener((obs, ov, nv) -> updateSearchIcon(nv));
        updateSearchIcon(ThemeManager.getInstance().isDarkMode());

        // بستن اُورلی
        cancelButton.setOnAction(e -> MainController.getInstance().closeOverlay(addMembersCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(addMembersCard.getParent()));

        // دکمه Add (افزودن اعضا)
        createButton.setOnAction(e -> onAddMembers());

        // سرچ
        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));

        Platform.runLater(() -> searchField.requestFocus());

        // دیتا را از Session بارگذاری کن
        loadContactsFromSession();
        updateMemberCount();
        renderContacts(allContacts);
    }

    // این متد را NewGroupController بعد از ساخت گروه صدا بزند
    public void setGroupInfo(UUID internalId, String groupName, String displayId, File groupImageFile) {
        this.groupInternalId = internalId;
        this.groupName = groupName;
        this.groupDisplayId = displayId;
        this.groupImageFile = groupImageFile;
    }

    // — اگر هنوز امضای قدیمی را صدا می‌زنی، موقتاً این اوِرلود هست (displayId را می‌گیرد اما internal_id لازم است) —
    public void setGroupInfo(String groupName, String groupId, File groupImageFile) {
        // ⚠️ فقط برای سازگاری موقت؛ حتماً NewGroupController را طوری به‌روزرسانی کن
        // که internal_id را بدهد (امضای بالایی).
        this.groupName = groupName;
        this.groupDisplayId = groupId;
        this.groupImageFile = groupImageFile;
    }

    private void loadContactsFromSession() {
        allContacts.clear();
        var u = Session.currentUser;
        var arr = (u == null) ? null : u.optJSONArray("contact_list");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                var c = arr.optJSONObject(i);
                if (c == null) continue;
                String name = c.optString("profile_name", "");
                String id   = c.optString("contact_id", ""); // ← internal_uuid مخاطب
                if (id.isBlank()) continue;
                String imageUrl = c.optString("image_url", "").trim();
                if (imageUrl.isEmpty() || imageUrl.equals("null")) {
                    imageUrl = "/org/to/telegramfinalproject/Avatars/default_user_profile.png";
                }
                String status = Optional.ofNullable(c.optString("last_seen", ""))
                        .filter(s -> !s.isBlank()).map(s -> "last seen " + s).orElse("");
                allContacts.add(new Contact(id, name, status, imageUrl));
            }
        }
        // اگر خالی بود، چیزی نشون نده (یا می‌تونی دمو بسازی)
        allContacts.sort(Comparator.comparing(Contact::getName, String.CASE_INSENSITIVE_ORDER));
    }

    private void applyFilter(String q) {
        String f = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Contact> filtered = allContacts.stream()
                .filter(c -> c.getName().toLowerCase(Locale.ROOT).contains(f))
                .collect(Collectors.toList());
        renderContacts(filtered);
    }

    private void renderContacts(List<Contact> contacts) {
        contactsList.getChildren().clear();

        if (contacts.isEmpty()) {
            StackPane emptyPane = new StackPane();
            emptyPane.setPrefHeight(240);
            emptyPane.setAlignment(Pos.CENTER);
            Label emptyLabel = new Label("No contacts found");
            emptyLabel.getStyleClass().add("no-contacts-label");
            emptyPane.getChildren().add(emptyLabel);
            contactsList.getChildren().add(emptyPane);
            return;
        }

        for (Contact c : contacts) {
            HBox item = new HBox(10);
            item.getStyleClass().add("contact-item");

            // Avatar
            ImageView avatar = new ImageView(loadAvatar(c.getImageUrl()));
            avatar.setFitWidth(48);
            avatar.setFitHeight(48);
            avatar.setPreserveRatio(true);

            // Details (name + status)
            VBox details = new VBox(2);
            Label nameLabel = new Label(c.getName());
            nameLabel.getStyleClass().add("contact-name");
            Label statusLabel = new Label(c.getStatus());
            statusLabel.getStyleClass().add("contact-status");
            details.getChildren().addAll(nameLabel, statusLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Add elements (no checkbox here)
            item.getChildren().addAll(avatar, details, spacer);

            // Highlight if already selected
            if (selectedContacts.contains(c)) {
                item.getStyleClass().add("contact-selected");
            }

            // Toggle selection by clicking the whole row
            item.setOnMouseClicked(e -> {
                if (selectedContacts.contains(c)) {
                    selectedContacts.remove(c);
                    item.getStyleClass().remove("contact-selected");
                } else {
                    selectedContacts.add(c);
                    item.getStyleClass().add("contact-selected");
                }
                updateMemberCount();
                updateSelectedMembersPane();
            });

            contactsList.getChildren().add(item);
        }
    }

    private Image loadAvatar(String path) {
        try {
            // 1) اگر ریسورس داخلی باشد
            var in = getClass().getResourceAsStream(path);
            if (in != null) return new Image(in);

            // 2) اگر URL کامل یا file URI
            return new Image(path, true);
        } catch (Exception e) {
            return new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("/org/to/telegramfinalproject/Avatars/default_user_profile.png")
                    )
            );
        }
    }

    private void onAddMembers() {
        if (groupInternalId == null) {
            showToast("Group internal_id is missing. Make sure setGroupInfo(UUID, ...) was called.");
            return;
        }
        if (selectedContacts.isEmpty()) {
            MainController.getInstance().closeOverlay(addMembersCard.getParent());
            return;
        }

        List<String> ids = selectedContacts.stream().map(Contact::getId).toList();

        // تلاش برای batch
        JSONObject batchReq = new JSONObject()
                .put("action", "add_members_to_group")
                .put("group_id", groupInternalId.toString())
                .put("user_ids", new JSONArray(ids));

        new Thread(() -> {
            JSONObject resp = ActionHandler.sendWithResponse(batchReq);
            if (resp != null && "success".equalsIgnoreCase(resp.optString("status"))) {
                Platform.runLater(() -> {
                    MainController.getInstance().closeOverlay(addMembersCard.getParent());
                });
                return;
            }

            // fallback: تک‌به‌تک
            boolean allOk = true;
            for (String uid : ids) {
                JSONObject single = new JSONObject()
                        .put("action", "add_member_to_group")
                        .put("group_id", groupInternalId.toString())
                        .put("user_id", uid);
                JSONObject r = ActionHandler.sendWithResponse(single);
                if (r == null || !"success".equalsIgnoreCase(r.optString("status"))) {
                    allOk = false;
                }
            }
            boolean finalAllOk = allOk;
            Platform.runLater(() -> {
                if (!finalAllOk) {
                    showToast("Some members failed.");
                }
                MainController.getInstance().closeOverlay(addMembersCard.getParent());
            });
        }).start();
    }

    private void updateSelectedMembersPane() {
        selectedMembersPane.getChildren().clear();
        for (Contact c : selectedContacts) {
            Label chip = new Label(c.getName());
            chip.getStyleClass().add("member-chip");
            selectedMembersPane.getChildren().add(chip);
        }
    }

    private void updateMemberCount() {
        memberCountLabel.setText(selectedContacts.size() + " / 200000");
    }

    private void updateSearchIcon(boolean darkMode) {
        String iconPath = darkMode
                ? "/org/to/telegramfinalproject/Icons/search_light.png"
                : "/org/to/telegramfinalproject/Icons/search_dark.png";
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
        icon.setFitWidth(16);
        icon.setFitHeight(16);
        searchIcon.setGraphic(icon);
    }

    private void showToast(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.initOwner(addMembersCard.getScene().getWindow());
        a.show();
    }

    // ================== مدل Contact ==================
    public static class Contact {
        private final String id;      // internal_uuid کاربر
        private final String name;
        private final String status;
        private final String imageUrl;

        public Contact(String id, String name, String status, String imageUrl) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.imageUrl = imageUrl;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getImageUrl() { return imageUrl; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Contact c)) return false;
            return Objects.equals(id, c.id);
        }
        @Override public int hashCode() { return Objects.hash(id); }
    }
}
