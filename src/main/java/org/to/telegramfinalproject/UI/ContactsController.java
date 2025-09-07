package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Client.ActionHandler;
import org.to.telegramfinalproject.Client.Session;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Models.ContactEntry;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ContactsController {

    @FXML private VBox contactsList;
    @FXML private VBox contactsCard;
    @FXML private TextField searchField;
    @FXML private Button closeFooterButton;
    @FXML private Pane overlayBackground;
    @FXML private ScrollPane contactsScroll;
    @FXML private Button searchIcon;

    /** منبع داده UI — با کانتکت‌های واقعی پر می‌شود */
    private final List<ContactVM> allContacts = new ArrayList<>();

    @FXML
    public void initialize() {
        // 1) لود اولیه‌ی کانتکت‌ها از سشن/سرور
        loadContactsAndRender();

        // 2) سرچ محلی روی لیست
        searchField.textProperty().addListener((obs, ov, nv) -> {
            String f = nv == null ? "" : nv.trim().toLowerCase();
            List<ContactVM> filtered = allContacts.stream()
                    .filter(c -> c.profileName.toLowerCase().contains(f)
                            || (c.userId != null && c.userId.toLowerCase().contains(f)))
                    .sorted(Comparator.comparing(c -> c.profileName.toLowerCase()))
                    .collect(Collectors.toList());
            renderContacts(filtered);
        });

        // 3) فوکوس خودکار روی سرچ
        Platform.runLater(() -> searchField.requestFocus());

        // 4) بستن اورلی
        closeFooterButton.setOnAction(e -> MainController.getInstance().closeOverlay(contactsCard.getParent()));
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(contactsCard.getParent()));

        // 5) اسکرول نرم
        contactsScroll.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        contactsScroll.setPannable(true);
        contactsScroll.setFitToWidth(true);
        contactsScroll.setFitToHeight(false);
        contactsScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            contactsScroll.setVvalue(contactsScroll.getVvalue() - deltaY);
        });

        // 6) ثبت صحنه برای ThemeManager
        Platform.runLater(() -> {
            if (contactsCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(contactsCard.getScene());
            }
        });

        // 7) واکنش به تغییر تم
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> updateSearchIcon(newVal));
        updateSearchIcon(ThemeManager.getInstance().isDarkMode());
    }

    // -----------------------
    // لود داده و رندر لیست
    // -----------------------
    private void loadContactsAndRender() {
        // اگر Session.contactEntries از قبل لود شده، از همان استفاده می‌کنیم.
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        if (Session.contactEntries != null && !Session.contactEntries.isEmpty()) {
                            return new ArrayList<>(Session.contactEntries);
                        }


                        JSONObject req = new JSONObject()
                                .put("action", "view_contacts")
                                .put("user_id", Session.getUserUUID());

                        JSONObject res = ActionHandler.sendWithResponse(req);
                        if (res == null || !"success".equals(res.optString("status"))) {
                            return Collections.emptyList();
                        }
                        JSONObject data = res.optJSONObject("data");
                        JSONArray arr = data != null ? data.optJSONArray("contacts") : null;
                        if (arr == null) return Collections.emptyList();

                        List<ContactEntry> fetched = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject c = arr.getJSONObject(i);
                            UUID contactId        = UUID.fromString(c.getString("contact_id"));
                            String userId         = c.optString("user_id", null);
                            String contactDisplay = c.optString("contact_display_id", userId);
                            String profileName    = c.optString("profile_name", contactDisplay);
                            String imageUrl       = c.optString("image_url", "/org/to/telegramfinalproject/Avatars/default_user_profile.png");
                            boolean isBlocked     = c.optBoolean("is_blocked", false);
                            String lastSeenStr    = c.optString("last_seen", null);

                            LocalDateTime lastSeen = null;
                            if (lastSeenStr != null && !lastSeenStr.isEmpty()) {
                                try { lastSeen = LocalDateTime.parse(lastSeenStr); } catch (Exception ignore) {}
                            }

                            fetched.add(new ContactEntry(contactId, userId, contactDisplay, profileName, imageUrl, isBlocked, lastSeen));
                        }
                        if (Session.contactEntries == null) Session.contactEntries = new ArrayList<>();
                        Session.contactEntries.clear();
                        Session.contactEntries.addAll(fetched);
                        return fetched;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Collections.<ContactEntry>emptyList();
                    }
                })
                .thenAccept(entries -> Platform.runLater(() -> {
                    allContacts.clear();
                    for (Object ce : entries) {
                        allContacts.add(ContactVM.from((ContactEntry) ce));
                    }
                    allContacts.sort(Comparator.comparing(vm -> vm.profileName.toLowerCase()));
                    renderContacts(allContacts);
                }));
    }

    private void renderContacts(List<ContactVM> contacts) {
        contactsList.getChildren().clear();

        if (contacts.isEmpty()) {
            StackPane emptyPane = new StackPane();
            emptyPane.setPrefHeight(300);
            emptyPane.setAlignment(Pos.CENTER);
            Label emptyLabel = new Label("No contacts found");
            emptyLabel.getStyleClass().add("no-contacts-label");
            emptyPane.getChildren().add(emptyLabel);
            contactsList.getChildren().add(emptyPane);
            return;
        }

        for (ContactVM c : contacts) {
            HBox item = new HBox(10);
            item.getStyleClass().add("contact-item");
            item.setCursor(Cursor.HAND);

            ImageView avatar = new ImageView(loadAvatarSafe(c.imageUrl));
            avatar.setFitWidth(58);
            avatar.setFitHeight(58);
            avatar.setPreserveRatio(true);

            VBox details = new VBox(2);
            Label nameLabel = new Label(c.profileName);
            nameLabel.getStyleClass().add("contact-name");


            details.getChildren().addAll(nameLabel);
            item.getChildren().addAll(avatar, details);

            item.setOnMouseClicked(e -> openOrStartPrivateChat(c));

            contactsList.getChildren().add(item);
        }
    }

    private Image loadAvatarSafe(String urlOrResource) {
        try {
            if (urlOrResource != null) {
                // حالت Resource داخلی
                if (urlOrResource.startsWith("/")) {
                    return new Image(Objects.requireNonNull(
                            getClass().getResourceAsStream(urlOrResource)));
                }
                // حالت URL وب یا مسیر فایل
                if (urlOrResource.startsWith("http://") ||
                        urlOrResource.startsWith("https://") ||
                        urlOrResource.startsWith("file:")) {
                    return new Image(urlOrResource, true); // true = لود async
                }
            }
            // fallback به پیش‌فرض
            return new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/org/to/telegramfinalproject/Avatars/default_user_profile.png")));
        } catch (Exception e) {
            // هر مشکلی → پیش‌فرض
            return new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/org/to/telegramfinalproject/Avatars/default_user_profile.png")));
        }
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


    private void openOrStartPrivateChat(ContactVM contact) {
        ChatEntry existing = findExistingPrivateChatWith(contact.contactId);
        if (existing != null) {
            MainController.getInstance().openChat(existing);
            MainController.getInstance().closeOverlay(contactsCard.getParent());
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        UUID myId = UUID.fromString(Session.currentUser.getString("internal_uuid"));
                        JSONObject req = new JSONObject()
                                .put("action", "get_or_create_private_chat")
                                .put("user1", myId.toString())
                                .put("user2", contact.contactId.toString());

                        JSONObject res = ActionHandler.sendWithResponse(req);
                        if (res == null || !"success".equals(res.optString("status"))) {
                            throw new RuntimeException(res != null ? res.optString("message", "Unknown error")
                                    : "null response");
                        }
                        JSONObject data = res.getJSONObject("data");
                        UUID chatId = UUID.fromString(data.getString("chat_id"));

                        ChatEntry entry = new ChatEntry(
                                chatId,
                                contact.userId,
                                contact.profileName,
                                contact.imageUrl,
                                "private",
                                null,
                                false,
                                false
                        );
                        entry.setOtherUserId(contact.contactId);

                        return entry;
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .thenAccept(entry -> Platform.runLater(() -> {
                    MainController.getInstance().onJoinedOrAdded(entry);
                    MainController.getInstance().openChat(entry);
                    MainController.getInstance().closeOverlay(contactsCard.getParent());
                }))
                .exceptionally(err -> {
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.ERROR, "Failed to start chat: " + err.getMessage(), ButtonType.OK);
                        a.showAndWait();
                    });
                    return null;
                });
    }


    private ChatEntry findExistingPrivateChatWith(UUID otherUserUuid) {
        if (Session.chatList != null) {
            for (ChatEntry ce : Session.chatList) {
                if (!"private".equalsIgnoreCase(ce.getType())) continue;
                UUID stored = ce.getOtherUserId();
                if (stored != null && stored.equals(otherUserUuid)) return ce;
            }
        }
        if (Session.activeChats != null) {
            for (ChatEntry ce : Session.activeChats) {
                if (!"private".equalsIgnoreCase(ce.getType())) continue;
                UUID stored = ce.getOtherUserId();
                if (stored != null && stored.equals(otherUserUuid)) return ce;
            }
        }
        return null;
    }


    // -----------------------
    // ViewModel ساده‌ی کانتکت
    // -----------------------
    private static class ContactVM {
        final UUID contactId;      // internal UUID (واقعی)
        final String userId;       // @id نمایش (اختیاری)
        final String profileName;
        final String imageUrl;

        static ContactVM from(ContactEntry ce) {
            return new ContactVM(ce.getContactId(), ce.getUserId(), ce.getProfileName(), ce.getImageUrl());
        }

        ContactVM(UUID contactId, String userId, String profileName, String imageUrl) {
            this.contactId = contactId;
            this.userId = userId;
            this.profileName = profileName != null ? profileName : (userId != null ? userId : "Unknown");
            this.imageUrl = imageUrl != null ? imageUrl : "/org/to/telegramfinalproject/Avatars/default_user_profile.png";
        }


    }
}