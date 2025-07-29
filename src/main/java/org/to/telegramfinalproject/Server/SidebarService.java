package org.to.telegramfinalproject.Server;

import org.json.JSONObject;
import org.to.telegramfinalproject.Database.userDatabase;
import org.to.telegramfinalproject.Models.ResponseModel;
import org.to.telegramfinalproject.Models.User;

import java.util.Objects;

public class SidebarService {
    private static userDatabase userDB = new userDatabase();

    // Returns current user's profile data (name, bio, status, etc.)
    public static JSONObject getUserProfile(String userId) {
        User user = userDB.findByUserId(userId);

        if (user == null) {
            return null;
        }

        JSONObject profile = new JSONObject();
        profile.put("user_id", user.getUser_id());
        profile.put("profile_name", user.getProfile_name());
        profile.put("bio", user.getBio() != null ? user.getBio() : "");
        profile.put("status", "ONLINE");
        profile.put("profile_picture_url", user.getImage_url());

        return profile;
    }

    // Changes the user's profile picture
    public static ResponseModel updateProfilePicture(String userId, String newImageUrl) {
        User user = userDB.findByUserId(userId);
        if (user == null) {
            return new ResponseModel("error", "User not found.");
        }

        String currentUrl = user.getImage_url();
        if (Objects.equals(currentUrl, newImageUrl)) {
            return new ResponseModel("error", "No changes made. Profile picture is the same.");
        }

        user.setImage_url(newImageUrl); // Can be null
        boolean success = userDB.updateByUUID(user.getInternal_uuid(), user);

        if (success) {
            return new ResponseModel("success", "Profile picture updated successfully.");
        } else {
            user.setImage_url(currentUrl);
            return new ResponseModel("error", "Failed to update profile picture.");
        }
    }

    // Changes user's bio
    public static ResponseModel updateBio(String userId, String newBio) {
        if (newBio.length() > 70) {
            return new ResponseModel("error", "Bio is too long.");
        }

        User user = userDB.findByUserId(userId);
        if (user == null) {
            return new ResponseModel("error", "User not found.");
        }

        String currentBio = user.getBio();
        if (Objects.equals(currentBio, newBio)) {
            return new ResponseModel("error", "No changes made. Bio is the same.");
        }

        user.setBio(newBio);
        boolean success = userDB.updateByUUID(user.getInternal_uuid(), user);

        if (success) {
            return new ResponseModel("success", "Bio updated successfully.");
        } else {
            user.setBio(currentBio);
            return new ResponseModel("error", "Failed to update bio.");
        }
    }

    // Changes user-id
    public static ResponseModel updateUserId(String currentUserId, String newUserId) {
        if (newUserId == null || newUserId.trim().isEmpty()) {
            return new ResponseModel("error", "User ID cannot be empty.");
        }

        newUserId = newUserId.trim();

        if (newUserId.contains(" ")) {
            return new ResponseModel("error", "User ID cannot contain spaces.");
        }

        if (!newUserId.matches("^[a-zA-Z0-9_]+$")) {
            return new ResponseModel("error", "User ID can only contain letters, digits, and underscores.");
        }

        if (userDB.findByUserId(newUserId) != null) {
            return new ResponseModel("error", "This user ID is already taken.");
        }

        User user = userDB.findByUserId(currentUserId);
        if (user == null) {
            return new ResponseModel("error", "User not found.");
        }

        if (Objects.equals(currentUserId, newUserId)) {
            return new ResponseModel("error", "No changes made. User ID is the same.");
        }

        user.setUser_id(newUserId);

        boolean saved = userDB.updateByUUID(user.getInternal_uuid(), user);
        if (saved) {
            return new ResponseModel("success", "User ID updated successfully.");
        } else {
            user.setUser_id(currentUserId);
            return new ResponseModel("error", "Failed to update user ID due to server error.");
        }
    }

    // Changes user's profile name
    public static ResponseModel updateProfileName(String userId, String newProfileName) {
        if (newProfileName == null || newProfileName.trim().isEmpty()) {
            return new ResponseModel("error", "Invalid input."); // Invalid input (empty or just spaces)
        }

        // Fetch the user from database
        User user = userDB.findByUserId(userId);
        if (user == null) {
            return new ResponseModel("error", "User not found."); // User doesn't exist
        }

        String oldName = user.getProfile_name();
        if (Objects.equals(oldName, newProfileName)) {
            return new ResponseModel("error", "No changes made. Profile name is the same.");
        }

        // Only set the profile name *after* successful DB update
        user.setProfile_name(newProfileName);

        boolean saved = userDB.updateByUUID(user.getInternal_uuid(), user);

        if (saved) {
            return new ResponseModel("success", "Profile name updated successfully.");
        } else {
            user.setProfile_name(oldName);
            return new ResponseModel("error", "Failed to update profile name.");
        }
    }

    // Changes username
    public static boolean updateUserName(String userId, String newUserName) {
        return false;
    }
}