package org.to.telegramfinalproject.Client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Models.FileAttachment;
import org.to.telegramfinalproject.Models.Message;

import java.time.LocalDateTime;
import java.util.*;

import static org.to.telegramfinalproject.Client.ActionHandler.sendWithResponse;

public class SidebarHandler {
    private final Scanner scanner;
    private  ActionHandler actionHandler ;
    private final String userUUID;

    public SidebarHandler(Scanner scanner, ActionHandler actionHandler, ActionHandler action) {
        this.scanner = scanner;
        this.actionHandler = action;
        this.userUUID = Session.getUserUUID();

    }

    public SidebarHandler(Scanner scanner, ActionHandler actionHandler) {
        this.scanner = scanner;
        this.actionHandler = actionHandler;
        this.userUUID = Session.getUserUUID();

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
                ensureSavedMessagesCreated();
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

        JSONObject response = sendWithResponse(request);
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
        JSONObject response = sendWithResponse(request);

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
            JSONObject response = sendWithResponse(request);

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

        JSONObject response = sendWithResponse(request);

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

            // URL validation
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
            JSONObject response = sendWithResponse(request);

            if (response.getString("status").equals("success")) {
                System.out.println("Bio updated successfully.");
            } else {
                System.out.println(response.getString("message"));
            }
            break;
        }
    }

    private void createNewGroup() {
        actionHandler.createGroup();
    }

    private void createNewChannel() {
        actionHandler.createChannel();
    }

    private void showContacts() {
        actionHandler.showContactList();
    }





    public void ensureSavedMessagesCreated() {
        // 1) Client-side quick check
        boolean alreadyExists = false;
        if (Session.activeChats != null) {
            for (ChatEntry e : Session.activeChats) {
                if (e != null && e.isSavedMessages()) { alreadyExists = true; break; }
            }
        }
        if (alreadyExists) {
            System.out.println("ℹ️ Saved Messages already exists.");
            return;
        }

        // 2) Ask server to get or create the self-chat
        JSONObject res = sendWithResponse(new JSONObject().put("action", "get_or_create_saved_messages"));
        if (res == null || !"success".equals(res.optString("status"))) {
            System.out.println("❌ Failed to create/open Saved Messages: " + (res != null ? res.optString("message","") : ""));
            return;
        }

        JSONObject data = res.optJSONObject("data");
        if (data == null) {
            System.out.println("❌ Invalid server response for Saved Messages.");
            return;
        }

        boolean created = data.optBoolean("created", true); // if server sends it
        String chatId = data.optString("chat_id", null);

        if (created) {
            System.out.println("✅ Saved Messages created." + (chatId != null ? " chat_id=" + chatId : ""));
             try {
                 UUID cid = UUID.fromString(chatId);
                 ChatEntry saved = new ChatEntry(cid, "Saved Messages", "Saved Messages", "", "private", null, true, false);
                 saved.setSavedMessages(true);
                 if (Session.activeChats == null) Session.activeChats = new ArrayList<>();
                 Session.activeChats.add(0, saved);
             } catch (Exception ignore) {}
        } else {
            System.out.println("ℹ️ Saved Messages already exists." + (chatId != null ? " chat_id=" + chatId : ""));
        }
    }



    private void openSettings() {
        while (true) {
            System.out.println("⚙️  Settings\n");

            String imageUrl    = Session.currentUser.optString("image_url", "—");
            String profileName = Session.currentUser.optString("profile_name", "—");
            String userId      = Session.currentUser.optString("user_id", "—");

            renderUserCard(imageUrl, profileName, userId);
            System.out.println();
            renderMenu();

            System.out.print("\nChoose an option (0-4): ");
            String pick = scanner.nextLine().trim();

            switch (pick) {
                case "1": openUserProfile(); break;
                case "2": showPrivacySettings(); break;
                case "3": showTelegramQA(); break;
                case "4": showTelegramFeatures(); break;
                case "0": return;
                default:
                    System.out.println("❌ Invalid choice. Press Enter to continue...");
                    scanner.nextLine();
            }
        }
    }



    private void showPrivacySettings() {
        while (true) {
            System.out.println("🔒 Privacy\n");
            System.out.println("1) Blocked users");
            System.out.println("2) Change username / password");
            System.out.println("0) Back");
            System.out.print("\nChoose: ");
            String pick = scanner.nextLine().trim();

            switch (pick) {
                case "1": viewBlockedUsers(); break;
                case "2": changeCredentialsFlow(); break;
                case "0": return;
                default:
                    System.out.println("❌ Invalid choice. Press Enter...");
                    scanner.nextLine();
            }
        }
    }

    private void viewBlockedUsers() {
        System.out.println("🚫 Blocked Users\n");

        org.json.JSONObject req = new org.json.JSONObject();
        req.put("action", "get_blocked_users");

        org.json.JSONObject res = sendWithResponse(req);
        if (res == null || !res.optString("status","error").equals("success")) {
            System.out.println("❌ Failed to fetch blocked users. Press Enter...");
            scanner.nextLine();
            return;
        }

        org.json.JSONArray arr = res.getJSONObject("data").optJSONArray("blocked_users");
        if (arr == null || arr.isEmpty()) {
            System.out.println("📭 No blocked users.");
            System.out.println("\nPress Enter...");
            scanner.nextLine();
            return;
        }

        for (int i = 0; i < arr.length(); i++) {
            org.json.JSONObject u = arr.getJSONObject(i);
            String profileName = u.optString("profile_name", "—");
            String userId = u.optString("user_id", "—");
            System.out.printf("%d) %s  (@%s)\n", i + 1, profileName, userId);
        }
        System.out.println("\n0) Back");
        System.out.print("\nChoose a user to UNBLOCK (number): ");
        String pick = scanner.nextLine().trim();

        if (pick.equals("0")) return;
        int idx;
        try { idx = Integer.parseInt(pick) - 1; } catch (Exception e) { idx = -1; }
        if (idx < 0 || idx >= arr.length()) {
            System.out.println("❌ Invalid index. Press Enter...");
            scanner.nextLine();
            return;
        }

        org.json.JSONObject target = arr.getJSONObject(idx);
        String targetDisplayId = target.optString("user_id", "");
        java.util.UUID targetInternalId = java.util.UUID.fromString(target.getString("internal_uuid"));

        System.out.printf("Unblock %s (@%s)? (yes/no): ", target.optString("profile_name","—"), targetDisplayId);
        if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) return;

        org.json.JSONObject unReq = new org.json.JSONObject();
        unReq.put("action", "toggle_block");
        unReq.put("user_id",Session.getUserUUID());
        unReq.put("target_id", targetInternalId.toString());

        org.json.JSONObject unRes = sendWithResponse(unReq);
        if (unRes != null && unRes.optString("status","error").equals("success")) {
            System.out.println("✅ User unblocked.");
        } else {
            System.out.println("❌ Failed to unblock.");
        }
        System.out.println("Press Enter...");
        scanner.nextLine();
    }


    private void changeCredentialsFlow() {
        System.out.println("🛡️  Change Username / Password\n");

        System.out.print("Enter current password: ");
        String currentPassword = scanner.nextLine();

        org.json.JSONObject verReq = new org.json.JSONObject();
        verReq.put("action", "verify_password");
        verReq.put("current_password", currentPassword);

        org.json.JSONObject verRes = sendWithResponse(verReq);
        if (verRes == null || !verRes.optString("status","error").equals("success")) {
            System.out.println("❌ Current password is incorrect.");
            System.out.println("Press Enter...");
            scanner.nextLine();
            return;
        }

        String currentUsername = Session.currentUser.optString("username", "—");
        System.out.println("\n✅ Verified.");
        System.out.println("Current username: " + currentUsername);
        System.out.println("Current password: ******** (hidden)");
        System.out.println("\nWhat do you want to change?");
        System.out.println("1) Username");
        System.out.println("2) Password");
        System.out.println("3) Both");
        System.out.println("0) Back");
        System.out.print("\nChoose: ");
        String pick = scanner.nextLine().trim();

        switch (pick) {
            case "1":
                changeUsername(currentPassword);
                break;
            case "2":
                changePassword(currentPassword);
                break;
            case "3":
                boolean uOk = changeUsername(currentPassword);
                boolean pOk = changePassword(currentPassword);
                if (uOk && pOk) System.out.println("✅ Username and password updated.");
                System.out.println("Press Enter...");
                scanner.nextLine();
                break;
            case "0":
                return;
            default:
                System.out.println("❌ Invalid choice. Press Enter...");
                scanner.nextLine();
        }
    }


    private boolean changeUsername(String currentPassword) {
        System.out.print("\nNew username: ");
        String newUsername = scanner.nextLine().trim();

        if (!isValidUsername(newUsername)) {
            System.out.println("❌ Invalid username. Use 4–32 chars: letters, digits, underscore.");
            return false;
        }

        org.json.JSONObject req = new org.json.JSONObject();
        req.put("action", "update_username");
        req.put("current_password", currentPassword);
        req.put("new_username", newUsername);

        org.json.JSONObject res = sendWithResponse(req);
        if (res != null && res.optString("status","error").equals("success")) {
            Session.currentUser.put("username", newUsername);
            System.out.println("✅ Username updated.");
            return true;
        } else {
            String msg = (res == null) ? "No response." : res.optString("message","Update failed.");
            System.out.println("❌ " + msg);
            return false;
        }
    }

    private boolean isValidUsername(String s) {
        //4-32 char
        return s != null && s.matches("^[A-Za-z0-9_]{4,32}$");
    }

    private boolean changePassword(String currentPassword) {
        System.out.print("\nNew password: ");
        String newPassword = scanner.nextLine();
        System.out.print("Repeat new password: ");
        String repeat = scanner.nextLine();

        if (!newPassword.equals(repeat)) {
            System.out.println("❌ Passwords do not match.");
            return false;
        }
        if (!isStrongPassword(newPassword)) {
            System.out.println("❌ Weak password. Min 8 chars, include letters and digits.");
            return false;
        }

        org.json.JSONObject req = new org.json.JSONObject();
        req.put("action", "update_password");
        req.put("current_password", currentPassword);
        req.put("new_password", newPassword);

        org.json.JSONObject res = sendWithResponse(req);
        if (res != null && res.optString("status","error").equals("success")) {
            System.out.println("✅ Password updated.");
            return true;
        } else {
            String msg = (res == null) ? "No response." : res.optString("message","Update failed.");
            System.out.println("❌ " + msg);
            return false;
        }
    }


    private void renderUserCard(String imageUrl, String profileName, String userId) {
        int w = 60;
        String top = "┌" + "─".repeat(w - 2) + "┐";
        String bot = "└" + "─".repeat(w - 2) + "┘";
        System.out.println(top);
        System.out.println(padBoxLine("Profile", w));
        System.out.println("├" + "─".repeat(w - 2) + "┤");
        System.out.println(padBoxLine("Image URL: "   + imageUrl, w));
        System.out.println(padBoxLine("Profile Name: "+ profileName, w));
        System.out.println(padBoxLine("User ID: "     + userId, w));
        System.out.println(bot);
    }

    private String padBoxLine(String text, int width) {
        final int inner = width - 2;
        if (text.length() > inner) {
            text = text.substring(0, inner - 1) + "…";
        }
        int spaces = inner - text.length();
        return "│" + text + " ".repeat(Math.max(0, spaces)) + "│";
    }

    private void renderMenu() {
        System.out.println("1) My Account");
        System.out.println("2) Privacy");
        System.out.println("3) Telegram Q&A");
        System.out.println("4) Telegram Features");
        System.out.println("0) Back");
    }

    private void showTelegramFeatures() {
        System.out.println("🌟 Showing Telegram features...");
    }

    private void showTelegramQA() {
        System.out.println("❓ Showing Q&A...");
    }

    private boolean isStrongPassword(String s) {
        boolean approved = s.matches("\\b(?=[^\\s]*[A-Z])(?=[^\\s]*[a-z])(?=[^\\s]*\\d)(?=[^\\s]*[!@#$%^&*])[^\\s]{8,}\\b");
        return approved ;
    }


}