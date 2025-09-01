package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

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

    // Keep selected contacts
    private final Set<Contact> selectedContacts = new HashSet<>();

    private String groupName;
    private File groupImageFile;

    // Sample data for testing
    private final List<Contact> allContacts = Arrays.asList(
            new Contact("Ali", "last seen recently", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Iman", "last seen a long time ago", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Amir", "last seen within a month", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Sara", "online", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Ali", "last seen recently", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Iman", "last seen a long time ago", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Amir", "last seen within a month", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Sara", "online", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Ali", "last seen recently", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Iman", "last seen a long time ago", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Amir", "last seen within a month", "/org/to/telegramfinalproject/Avatars/default_user_profile.png"),
            new Contact("Sara", "online", "/org/to/telegramfinalproject/Avatars/default_user_profile.png")
    );

    @FXML
    public void initialize() {
        updateMemberCount();

        // Sort + render initially
        List<Contact> sorted = allContacts.stream()
                .sorted(Comparator.comparing(Contact::getName))
                .collect(Collectors.toList());
        renderContacts(sorted);

        // Search filter
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            List<Contact> filtered = allContacts.stream()
                    .filter(c -> c.getName().toLowerCase().contains(filter))
                    .sorted(Comparator.comparing(Contact::getName))
                    .collect(Collectors.toList());
            renderContacts(filtered);
        });

        // Auto-focus
        Platform.runLater(() -> searchField.requestFocus());

        // Cancel closes overlay
        cancelButton.setOnAction(e -> MainController.getInstance().closeOverlay(addMembersCard.getParent()));

        // Close when clicking outside
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(addMembersCard.getParent()));

        // Create action
        createButton.setOnAction(e -> {
            if (selectedContacts.isEmpty()) {
                return;
            }
            createGroup();
        });

        // Smooth scroll
        contactsScroll.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        contactsScroll.setPannable(true);
        contactsScroll.setFitToWidth(true);
        contactsScroll.setFitToHeight(false);
        contactsScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003;
            contactsScroll.setVvalue(contactsScroll.getVvalue() - deltaY);
        });

        // Theme icon handling
        Platform.runLater(() -> {
            if (addMembersCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(addMembersCard.getScene());
            }
        });
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> updateSearchIcon(newVal));
        updateSearchIcon(ThemeManager.getInstance().isDarkMode());
    }

    private void createGroup() {
//        String groupName = this.groupName; // set earlier from setGroupInfo()
//        File groupImage = this.groupImageFile; // also passed earlier
//
//        // Collect selected members
//        List<String> memberIds = selectedContacts.stream()
//                .map(Contact::getId) // you need some unique identifier for contacts
//                .collect(Collectors.toList());
//
//        // Build JSON payload for server
//        JSONObject req = new JSONObject();
//        req.put("action", "create_group");
//        req.put("name", groupName);
//        req.put("members", memberIds);
//
//        if (groupImage != null) {
//            req.put("image_path", groupImage.getAbsolutePath());
//            // or upload the file separately depending on your backend design
//        }
//
//        try {
//            JSONObject res = NetworkClient.sendWithResponse(req); // your socket wrapper
//            if ("success".equals(res.getString("status"))) {
//                // Get new group chat ID from server
//                String chatId = res.getString("chat_id");
//
//                // ✅ Close overlay
//                MainController.getInstance().closeOverlay(overlayRoot);
//
//                // ✅ Open chat immediately
//                FXMLLoader loader = new FXMLLoader(getClass().getResource(
//                        "/org/to/telegramfinalproject/Fxml/chat_page.fxml"));
//                Node chatPage = loader.load();
//
//                ChatPageController chatController = loader.getController();
//                chatController.setChat(groupName,
//                        groupImage != null ? groupImage.toURI().toString()
//                                : "/org/to/telegramfinalproject/Avatars/default_group.png");
//
//                MainController.getInstance().getChatDisplayArea().getChildren().setAll(chatPage);
//
//            } else {
//                showAlert("Failed to create group: " + res.getString("message"));
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            showAlert("Error creating group.");
//        }
    }

    private void renderContacts(List<Contact> contacts) {
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

        for (Contact c : contacts) {
            HBox item = new HBox(10);
            item.getStyleClass().add("contact-item");

            // Avatar
            ImageView avatar = new ImageView(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(c.getImageUrl()))
            ));
            avatar.setFitWidth(48);
            avatar.setFitHeight(48);
            avatar.setPreserveRatio(true);

            VBox details = new VBox(2);
            Label nameLabel = new Label(c.getName());
            nameLabel.getStyleClass().add("contact-name");
            Label statusLabel = new Label(c.getStatus());
            statusLabel.getStyleClass().add("contact-status");

            details.getChildren().addAll(nameLabel, statusLabel);

            item.getChildren().addAll(avatar, details);

            // Click to toggle selection
            item.setOnMouseClicked(e -> toggleSelection(c));

            // Highlight if already selected
            if (selectedContacts.contains(c)) {
                item.getStyleClass().add("contact-selected");
            }

            contactsList.getChildren().add(item);
        }
    }

    private void toggleSelection(Contact contact) {
        if (selectedContacts.contains(contact)) {
            selectedContacts.remove(contact);
        } else {
            selectedContacts.add(contact);
        }
        updateMemberCount();
        updateSelectedMembersPane();
        renderContacts(allContacts);
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

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

    public void setGroupInfo(String groupName, File groupImageFile) {
        this.groupName = groupName;
        this.groupImageFile = groupImageFile;

        // You can use these later when creating the group
        System.out.println("Group name passed: " + groupName);
        if (groupImageFile != null) {
            System.out.println("Group image: " + groupImageFile.getName());
        }
    }

    // Inner class for contact data
    public static class Contact {
        private final String name;
        private final String status;
        private final String imageUrl;

        public Contact(String name, String status, String imageUrl) {
            this.name = name;
            this.status = status;
            this.imageUrl = imageUrl;
        }

        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getImageUrl() { return imageUrl; }
    }
}
