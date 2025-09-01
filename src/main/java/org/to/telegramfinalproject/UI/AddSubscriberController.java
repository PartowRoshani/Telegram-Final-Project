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

    // Keep selected contacts
    private final Set<Contact> selectedContacts = new HashSet<>();

    private String channelName;
    private File channelImageFile;
    private String description;

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
        renderContacts(allContacts.stream()
                .sorted(Comparator.comparing(Contact::getName))
                .collect(Collectors.toList()));

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

        // Skip → ignore selection and create chat
        skipButton.setOnAction(e -> {
            createChannel(); // always creates, even if no members selected
        });

        // Add → requires at least one member
        addButton.setOnAction(e -> {
            if (!selectedContacts.isEmpty()) {
                createChannel();
            }
        });

        // Close when clicking outside
        overlayBackground.setOnMouseClicked(e -> MainController.getInstance().closeOverlay(addMembersCard.getParent()));

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

    private void createChannel() {
        // TODO: real server request → for now just print and close overlay
        System.out.println("✅ Creating group/channel: " + channelName);
        System.out.println("Selected members: " + selectedContacts.stream()
                .map(Contact::getName).collect(Collectors.joining(", ")));

        MainController.getInstance().closeOverlay(addMembersCard.getParent());

        // TODO: open chat immediately (like you did with newGroup)
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

    public void setChannelInfo(String name, String description, File image) {
        this.channelName = name;
        this.channelImageFile = image;
        this.description = description;
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
