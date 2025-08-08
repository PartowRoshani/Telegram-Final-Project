package org.to.telegramfinalproject.Server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Database.MessageDatabase;
import org.to.telegramfinalproject.Database.PrivateChatDatabase;
import org.to.telegramfinalproject.Database.userDatabase;
import org.to.telegramfinalproject.Models.Message;
import org.to.telegramfinalproject.Models.ResponseModel;
import org.to.telegramfinalproject.Models.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SidebarService {
    private static userDatabase userDB = new userDatabase();

    // Returns current user's profile data (name, bio, status, etc.)
    public static JSONObject getUserProfile(UUID userUUID) {
        User user = userDB.findByInternalUUID(userUUID);

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
    public static ResponseModel updateProfilePicture(UUID userUUID, String newImageUrl) {
        User user = userDB.findByInternalUUID(userUUID);
        if (user == null) {
            return new ResponseModel("error", "User not found.");
        }

        String currentUrl = user.getImage_url();
        if (Objects.equals(currentUrl, newImageUrl)) {
            return new ResponseModel("error", "No changes made. Profile picture is the same.");
        }

        user.setImage_url(newImageUrl); // Can be null
        boolean success = userDB.updateByUUID(userUUID, user);

        if (success) {
            return new ResponseModel("success", "Profile picture updated successfully.");
        } else {
            user.setImage_url(currentUrl);
            return new ResponseModel("error", "Failed to update profile picture.");
        }
    }

    // Changes user's bio
    public static ResponseModel updateBio(UUID userUUID, String newBio) {
        if (newBio.length() > 70) {
            return new ResponseModel("error", "Bio is too long.");
        }

        User user = userDB.findByInternalUUID(userUUID);
        if (user == null) {
            return new ResponseModel("error", "User not found.");
        }

        String currentBio = user.getBio();
        if (Objects.equals(currentBio, newBio)) {
            return new ResponseModel("error", "No changes made. Bio is the same.");
        }

        user.setBio(newBio);
        boolean success = userDB.updateByUUID(userUUID, user);

        if (success) {
            return new ResponseModel("success", "Bio updated successfully.");
        } else {
            user.setBio(currentBio);
            return new ResponseModel("error", "Failed to update bio.");
        }
    }

    // Changes user-id
    public static ResponseModel updateUserId(UUID userUUID, String newUserId) {
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

        User user = userDB.findByInternalUUID(userUUID);
        if (user == null) {
            return new ResponseModel("error", "User not found.");
        }

        String currentUserId = user.getUser_id();
        if (Objects.equals(currentUserId, newUserId)) {
            return new ResponseModel("error", "No changes made. User ID is the same.");
        }

        user.setUser_id(newUserId);

        boolean saved = userDB.updateByUUID(userUUID, user);
        if (saved) {
            return new ResponseModel("success", "User ID updated successfully.");
        } else {
            user.setUser_id(currentUserId);
            return new ResponseModel("error", "Failed to update user ID due to server error.");
        }
    }

    // Changes user's profile name
    public static ResponseModel updateProfileName(UUID userUUID, String newProfileName) {
        if (newProfileName == null || newProfileName.trim().isEmpty()) {
            return new ResponseModel("error", "Invalid input."); // Invalid input (empty or just spaces)
        }

        // Fetch the user from database
        User user = userDB.findByInternalUUID(userUUID);
        if (user == null) {
            return new ResponseModel("error", "User not found."); // User doesn't exist
        }

        String oldName = user.getProfile_name();
        if (Objects.equals(oldName, newProfileName)) {
            return new ResponseModel("error", "No changes made. Profile name is the same.");
        }

        // Only set the profile name *after* successful DB update
        user.setProfile_name(newProfileName);

        boolean saved = userDB.updateByUUID(userUUID, user);

        if (saved) {
            return new ResponseModel("success", "Profile name updated successfully.");
        } else {
            user.setProfile_name(oldName);
            return new ResponseModel("error", "Failed to update profile name.");
        }
    }

    // Get saved messages data
    public static ResponseModel handleGetSavedMessages(UUID userId) {
        try {

            UUID chatId = PrivateChatDatabase.getOrCreateSavedMessagesChat(userId);
            if (chatId == null) {
                return new ResponseModel("error", "Failed to create or find saved messages chat.");
            }

            List<Message> messages = MessageDatabase.privateChatHistory(chatId);

            JSONArray messageArray = new JSONArray();
            if (!messages.isEmpty()) {
                for (Message msg : messages) {
                    JSONObject msgJson = new JSONObject();
                    msgJson.put("message_id", msg.getMessage_id().toString());
                    msgJson.put("sender_id", msg.getSender_id().toString());
                    msgJson.put("receiver_type", msg.getReceiver_type());
                    msgJson.put("receiver_id", msg.getReceiver_id().toString());
                    msgJson.put("content", msg.getContent());
                    msgJson.put("message_type", msg.getMessage_type());
                    msgJson.put("send_at", msg.getSend_at().toString()); // LocalDateTime
                    msgJson.put("status", msg.getStatus());
                    msgJson.put("reply_to_id", msg.getReply_to_id() != null ? msg.getReply_to_id().toString() : JSONObject.NULL);
                    msgJson.put("is_edited", msg.isIs_edited());
                    msgJson.put("original_message_id", msg.getOriginal_message_id() != null ? msg.getOriginal_message_id().toString() : JSONObject.NULL);
                    msgJson.put("forwarded_by", msg.getForwarded_by() != null ? msg.getForwarded_by().toString() : JSONObject.NULL);
                    msgJson.put("forwarded_from", msg.getForwarded_from() != null ? msg.getForwarded_from().toString() : JSONObject.NULL);

                    messageArray.put(msgJson);
                }
            }

            JSONObject data = new JSONObject();
            data.put("chat_id", chatId.toString());
            data.put("messages", messageArray);

            return new ResponseModel("success", "Saved messages retrieved successfully", data);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseModel("error", "Unexpected server error.");
        }
    }

    // Save messages to DB
    public static ResponseModel handleSendMessage(JSONObject requestJson) {
        try {
            Message message = new Message(
                    UUID.fromString(requestJson.getString("message_id")),
                    UUID.fromString(requestJson.getString("sender_id")),
                    requestJson.getString("receiver_type"),
                    UUID.fromString(requestJson.getString("receiver_id")),
                    requestJson.optString("content", null),
                    requestJson.optString("message_type", "TEXT"),
                    LocalDateTime.now(), // send_at
                    requestJson.optString("status", "SEND"),
                    requestJson.isNull("reply_to_id") ? null : UUID.fromString(requestJson.getString("reply_to_id")),
                    requestJson.optBoolean("is_edited", false),
                    requestJson.isNull("original_message_id") ? null : UUID.fromString(requestJson.getString("original_message_id")),
                    requestJson.isNull("forwarded_by") ? null : UUID.fromString(requestJson.getString("forwarded_by")),
                    requestJson.isNull("forwarded_from") ? null : UUID.fromString(requestJson.getString("forwarded_from")),
                    requestJson.optBoolean("is_deleted_globally", false),
                    requestJson.isNull("edited_at") ? null :
                            LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(requestJson.getLong("edited_at")),
                                    ZoneId.systemDefault()
                            )
                    );

            MessageDatabase.insertSavedMessage(message);
            return new ResponseModel("success", "Message saved successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseModel("error", "Unexpected server error.");
        }
    }

    // Changes username
    public static boolean updateUserName(String userId, String newUserName) {
        return false;
    }
}