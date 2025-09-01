package org.to.telegramfinalproject.UI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.*;
import java.util.stream.Collectors;

public class ContactsController {

    @FXML private VBox contactsList;
    @FXML private VBox contactsCard;
    @FXML private TextField searchField;
    @FXML private Button closeFooterButton;
    @FXML private Pane overlayBackground;
    @FXML private ScrollPane contactsScroll;
    @FXML private Button searchIcon;

    // Sample data for testing (later fetch from DB/server)
    private final List<Contact> allContacts = Arrays.asList(
            new Contact("Ali", "last seen recently", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Iman", "last seen a long time ago", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Amir", "last seen within a month", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Sara", "online", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Ali", "last seen recently", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Iman", "last seen a long time ago", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Amir", "last seen within a month", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Sara", "online", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Ali", "last seen recently", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Iman", "last seen a long time ago", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Amir", "last seen within a month", "/org/to/telegramfinalproject/Avatars/default_profile.png"),
            new Contact("Sara", "online", "/org/to/telegramfinalproject/Avatars/default_profile.png")
    );

    @FXML
    public void initialize() {
        // Sort contacts alphabetically on load
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

        // Auto_focus search bar when overlay opens
        Platform.runLater(() -> searchField.requestFocus());

        // Close with footer button
        closeFooterButton.setOnAction(e -> MainController.getInstance().closeOverlay(contactsCard.getParent()));

        // Close when clicking outside card
        overlayBackground.setOnMouseClicked(e -> {
            MainController.getInstance().closeOverlay(contactsCard.getParent());
        });

        // Smooth scroll feel for contacts list
        contactsScroll.getStylesheets().add(getClass().getResource("/org/to/telegramfinalproject/CSS/scrollpane.css").toExternalForm());
        contactsScroll.setPannable(true);
        contactsScroll.setFitToWidth(true);
        contactsScroll.setFitToHeight(false);
        contactsScroll.getContent().setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 0.003; // smaller = smoother
            contactsScroll.setVvalue(contactsScroll.getVvalue() - deltaY);
        });

        // Register scene for ThemeManager → stylesheet swap will handle colors/icons
        Platform.runLater(() -> {
            if (contactsCard.getScene() != null) {
                ThemeManager.getInstance().registerScene(contactsCard.getScene());
            }
        });

        // Listener for theme change
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateSearchIcon(newVal);
        });

        // Set initial state
        updateSearchIcon(ThemeManager.getInstance().isDarkMode());
    }

    private void renderContacts(List<Contact> contacts) {
        contactsList.getChildren().clear();

        if (contacts.isEmpty()) {
            contactsList.getChildren().clear();

            StackPane emptyPane = new StackPane();
            emptyPane.setPrefHeight(300); // << pushes it lower
            emptyPane.setAlignment(Pos.CENTER);

            Label emptyLabel = new Label("No contacts found");
            emptyLabel.getStyleClass().add("no-contacts-label");

            emptyPane.getChildren().add(emptyLabel);

            // Do NOT give it prefHeight or Vgrow → prevents scroll bar
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
            avatar.setFitWidth(58);
            avatar.setFitHeight(58);
            avatar.setPreserveRatio(true);

            VBox details = new VBox(2);
            Label nameLabel = new Label(c.getName());
            nameLabel.getStyleClass().add("contact-name");

            Label statusLabel = new Label(c.getStatus());
            statusLabel.getStyleClass().add("contact-status");

            details.getChildren().addAll(nameLabel, statusLabel);

            item.getChildren().addAll(avatar, details);
            contactsList.getChildren().add(item);
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