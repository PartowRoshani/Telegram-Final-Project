package org.to.telegramfinalproject.Client;

import org.json.JSONException;
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

        JSONObject profile;
        try {
            profile = response.getJSONObject("data");
        } catch (JSONException e) {
            System.out.println("Received malformed profile data from server.");
            return;
        }

        String profileName = profile.optString("profile_name", "NOT-AVAILABLE");
        String userId = profile.optString("user_id", "NOT-AVAILABLE");
        String status = profile.optString("status", "NOT-AVAILABLE");
        String bio = profile.optString("bio", "NOT-AVAILABLE");
        String profilePictureUrl = profile.optString("profile_picture_url", "NOT-AVAILABLE");

        // Step 2: Show profile info
        System.out.println("===== My Profile =====");
        System.out.println("Profile Picture URL : " + profilePictureUrl);
        System.out.println("Profile Name        : " + profileName);
        System.out.println("User ID             : @" + userId);
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
        System.out.println("Enter your new profile name:");
        String newProfileName = scanner.nextLine().trim();

        // Step 1: Validate input
        while (newProfileName.trim().isEmpty()) {
            System.out.println("Profile name cannot be empty. Enter another name.");
            newProfileName = scanner.nextLine().trim();
        }

        // Step 2: Create request
        JSONObject request = new JSONObject();
        request.put("action", "edit_profile_name");
        request.put("new_profile_name", newProfileName);

        // Step 3: Send request and receive response
        JSONObject response = ActionHandler.sendWithResponse(request);

        // Step 4: Handle response
        if (response == null || !response.optString("status", "fail").equals("success")) {
            System.out.println("Failed to update profile name.");
        } else {
            System.out.println("Profile name updated successfully!");
        }
    }

    private void editUserId() {
        while (true) {
            System.out.println("Enter your new user ID: ");
            String newUserId = scanner.nextLine().trim();

            if (newUserId.isEmpty()) {
                System.out.println("User ID cannot be empty.");
                continue;
            }

            if (newUserId.contains(" ")) {
                System.out.println("User ID cannot contain spaces.");
                continue;
            }

            if (!newUserId.matches("^[a-zA-Z0-9_]+$")) {
                System.out.println("User ID can only contain letters, digits, and underscores.");
                continue;
            }

            // Send to server
            JSONObject request = new JSONObject();
            request.put("action", "edit_user_id");
            request.put("new_user_id", newUserId);
            JSONObject response = ActionHandler.sendWithResponse(request);

            if (response.getString("status").equals("success")) {
                System.out.println("User ID updated successfully.");
                break;
            } else {
                // Server-side error message (e.g., ID already exists)
                System.out.println(response.getString("message"));
            }
        }
    }

    private void editBio() {
        System.out.println("Enter your new bio:");
        String newBio = scanner.nextLine().trim();

        // Limit the bio length
        if (newBio.length() > 70) {
            System.out.println("Bio cannot be more than 70 characters.");
            return;
        }

        JSONObject request = new JSONObject();
        request.put("action", "edit_bio");
        request.put("new_bio", newBio);

        JSONObject response = ActionHandler.sendWithResponse(request);

        if (response.getString("status").equals("success")) {
            System.out.println("Bio updated successfully.");
        } else {
            System.out.println(response.getString("message"));
        }
    }

    private void editProfilePictureUrl() {
        while (true) {
            System.out.println("Enter new profile picture URL (or leave empty to remove):");
            String newImageUrl = scanner.nextLine().trim();

            if (newImageUrl.contains(" ")) {
                System.out.println("URL cannot contain spaces.");
                continue;
            }

            if (!newImageUrl.isEmpty() && !newImageUrl.matches("^(http|https)://.*$")) {
                System.out.println("Invalid URL format. Please enter a valid HTTP/HTTPS link.");
                continue;
            }

            JSONObject request = new JSONObject();
            request.put("action", "edit_profile_picture");
            request.put("new_image_url", newImageUrl);
            JSONObject response = ActionHandler.sendWithResponse(request);

            if (response.getString("status").equals("success")) {
                System.out.println("Bio updated successfully.");
            } else {
                System.out.println(response.getString("message"));
            }
            break;
        }
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