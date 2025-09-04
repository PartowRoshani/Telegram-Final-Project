package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
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

public class AddSubscriberController {

    @FXML private VBox contactsList;
    @FXML private VBox addMembersCard;
    @FXML private TextField searchField;
    @FXML private Pane overlayBackground;
    @FXML private ScrollPane contactsScroll;
    @FXML private Button searchIcon;
    @FXML private Label memberCountLabel;
    @FXML private FlowPane selectedMembersPane;
    @FXML private Button skipButton;
    @FXML private Button addButton;

    // اطلاعات کانال
    private UUID channelInternalId;
    private String channelName;
    private String channelDisplayId;
    private File channelImageFile;
    private String description;

    // انتخاب‌ها و منبع داده
    private final Set<Contact> selected = new HashSet<>();
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

        // تم آیکن
        Platform.runLater(() -> {
            if (addMembersCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(addMembersCard.getScene());
            }
        });
        ThemeManager.getInstance().darkModeProperty().addListener((obs, ov, nv) -> updateSearchIcon(nv));
        updateSearchIcon(ThemeManager.getInstance().isDarkMode());

        // بستن اُورلی
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(addMembersCard.getParent()));
        skipButton.setOnAction(e -> MainController.getInstance().closeOverlay(addMembersCard.getParent()));
        addButton.setOnAction(e -> onAddSubscribers());

        // سرچ
        searchField.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));
        Platform.runLater(() -> searchField.requestFocus());

        // لود کانتکت‌ها از Session
        loadContactsFromSession();
        updateCount();
        renderContacts(allContacts);
    }

    /** NewChannelController این را بعد از ساخت کانال صدا بزند */
    public void setChannelInfo(UUID internalId, String name, String displayId, File imageFile, String description) {
        this.channelInternalId = internalId;
        this.channelName = name;
        this.channelDisplayId = displayId;
        this.channelImageFile = imageFile;
        this.description = description;
    }

    // برای سازگاری با امضای قدیمی (اختیاری)
    public void setChannelInfo(String name, String id, String description, File image) {
        this.channelName = name;
        this.channelDisplayId = id;
        this.channelImageFile = image;
        this.description = description;
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
                String id   = c.optString("contact_id", ""); // internal_uuid
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

            // Details
            VBox details = new VBox(2);
            Label nameLabel = new Label(c.getName());
            nameLabel.getStyleClass().add("contact-name");
            Label statusLabel = new Label(c.getStatus());
            statusLabel.getStyleClass().add("contact-status");
            details.getChildren().addAll(nameLabel, statusLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Add elements (no checkbox)
            item.getChildren().addAll(avatar, details, spacer);

            // Highlight if already selected
            if (selected.contains(c)) {
                item.getStyleClass().add("contact-selected");
            }

            // Toggle selection by clicking the row
            item.setOnMouseClicked(e -> {
                if (selected.contains(c)) {
                    selected.remove(c);
                    item.getStyleClass().remove("contact-selected");
                } else {
                    selected.add(c);
                    item.getStyleClass().add("contact-selected");
                }
                updateCount();
                updateSelectedPane();
            });

            contactsList.getChildren().add(item);
        }
    }

    private Image loadAvatar(String path) {
        try {
            var in = getClass().getResourceAsStream(path);
            if (in != null) return new Image(in);
            return new Image(path, true);
        } catch (Exception e) {
            return new Image(
                    Objects.requireNonNull(
                            getClass().getResourceAsStream("/org/to/telegramfinalproject/Avatars/default_user_profile.png")
                    )
            );
        }
    }

    private void onAddSubscribers() {
        if (channelInternalId == null) {
            showToast("Channel internal_id is missing. Make sure setChannelInfo(UUID, ...) was called.");
            return;
        }
        if (selected.isEmpty()) {
            MainController.getInstance().closeOverlay(addMembersCard.getParent());
            return;
        }

        List<String> ids = selected.stream().map(Contact::getId).toList();

        JSONObject batchReq = new JSONObject()
                .put("action", "add_subscribers_to_channel")
                .put("channel_id", channelInternalId.toString())
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
                        .put("action", "add_subscriber_to_channel")
                        .put("channel_id", channelInternalId.toString())
                        .put("user_id", uid);
                JSONObject r = ActionHandler.sendWithResponse(single);
                if (r == null || !"success".equalsIgnoreCase(r.optString("status"))) {
                    allOk = false;
                }
            }
            boolean finalAllOk = allOk;
            Platform.runLater(() -> {
                if (!finalAllOk) {
                    showToast("Some subscribers failed.");
                }
                MainController.getInstance().closeOverlay(addMembersCard.getParent());
            });
        }).start();
    }

    private void updateSelectedPane() {
        selectedMembersPane.getChildren().clear();
        for (Contact c : selected) {
            Label chip = new Label(c.getName());
            chip.getStyleClass().add("member-chip");
            selectedMembersPane.getChildren().add(chip);
        }
    }

    private void updateCount() {
        memberCountLabel.setText(selected.size() + " / 200000");
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

    // ===== مدل Contact =====
    public static class Contact {
        private final String id;      // internal_uuid کاربر
        private final String name;
        private final String status;
        private final String imageUrl;

        public Contact(String id, String name, String status, String imageUrl) {
            this.id = id; this.name = name; this.status = status; this.imageUrl = imageUrl;
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
