package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.util.Scanner;

public class SidebarHandler {
    private final Scanner scanner;

    public SidebarHandler(Scanner scanner) {
        this.scanner = scanner;
    }

    public void handleSidebarAction(SidebarAction action) {
        switch (action) {
            case MY_PROFILE:
                openUserProfile();
                break;
            case NEW_GROUP:
                createNewGroup();
                break;
            case NEW_CHANNEL:
                createNewChannel();
                break;
            case CONTACTS:
                showContacts();
                break;
            case SAVED_MESSAGES:
                showSavedMessages();
                break;
            case SETTINGS:
                openSettings();
                break;
            case FEATURES:
                showTelegramFeatures();
                break;
            case Q_AND_A:
                showTelegramQA();
                break;
        }
    }

    private void openUserProfile() {
        // Step 1: Request profile info from the server
        JSONObject request = new JSONObject();
        request.put("action", "get_user_profile");

        JSONObject response = ActionHandler.sendWithResponse(request);
        if (response == null || !response.optString("status", "fail").equals("success")) {
            System.out.println("Failed to load profile information.");
            return;
        }

        JSONObject profile = response.getJSONObject("data");

        String profileName = profile.optString("profile_name", "NOT-AVAILABLE");
        String userId = profile.optString("user_id", "NOT-AVAILABLE");
        String status = profile.optString("status", "NOT-AVAILABLE");
        String bio = profile.optString("bio", "NOT-AVAILABLE");
        String profilePictureUrl = profile.optString("profile_picture_url", "NOT-AVAILABLE");

        // Step 2: Show profile info
        System.out.println("===== My Profile =====");
        System.out.println("Profile Picture URL : " + profilePictureUrl);
        System.out.println("Profile Name        : " + profileName);
        System.out.println("User ID             : " + userId);
        System.out.println("Status              : " + status);
        System.out.println("Bio                 : " + bio);
        System.out.println("======================");

        // Step 3: Ask if user wants to edit anything
        System.out.println("Do you want to edit any of these? (yes/no)");
        while (true) {
            String choice = scanner.nextLine().trim().toLowerCase();
            if (choice.equals("no")) return;
            if (!choice.equals("yes")) {
                System.out.println("Invalid input. Please enter 'yes' or 'no'.");
                continue;
            }

            // Step 4: Show editable fields
            System.out.println("Which field do you want to edit?");
            System.out.println("1. Profile Name");
            System.out.println("2. User ID");
            System.out.println("3. Bio");
            System.out.println("4. Profile Picture URL");
            System.out.println("0. Cancel");

            String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> editProfileName();
                case "2" -> editUserId();
                case "3" -> editBio();
                case "4" -> editProfilePictureUrl();
                case "0" -> { return; }
                default -> System.out.println("Invalid choice. Try again.");
            }

            // Re-fetch and re-display profile after editing
            openUserProfile();
            return;
        }
    }

    private void editProfileName() {

    }

    private void editUserId() {

    }

    private void editBio() {

    }

    private void editProfilePictureUrl() {

    }

    private void createNewGroup() {
        System.out.println("👥 Creating a new group...");
    }

    private void createNewChannel() {
        System.out.println("📢 Creating a new channel...");
    }

    private void showContacts() {
        System.out.println("📇 Showing contacts...");
    }

    private void showSavedMessages() {
        System.out.println("💾 Showing saved messages...");
    }

    private void openSettings() {
        System.out.println("⚙️ Opening settings...");
    }

    private void showTelegramFeatures() {
        System.out.println("🌟 Showing Telegram features...");
    }

    private void showTelegramQA() {
        System.out.println("❓ Showing Q&A...");
    }
}