package org.to.telegramfinalproject.Server;

import org.json.JSONObject;
import org.to.telegramfinalproject.Database.userDatabase;
import org.to.telegramfinalproject.Models.User;

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

    // Updates one or more profile fields for the user
    public static JSONObject updateUserProfile(String userId, JSONObject updateData) {
        return null;
    }

    // Changes the user's profile picture
    public static JSONObject changeProfilePicture(String userId, byte[] imageData) {
        return null;
    }

    // Changes user-id
    public static JSONObject updateUserId(String userId, String newUserId) {
        return null;
    }

    // (Optional) Changes username
    public static JSONObject updateUserName(String userId, String newUserName) {
        return null;
    }

    // Changes user's profile name
    public static JSONObject updateProfileName(String userId, String newProfileName) {
        return null;
    }

    // Changes or sets the user's date of birth
    public static JSONObject updateDateOfBirth(String userId, String newDateOfBirth) {
        return null;
    }
}