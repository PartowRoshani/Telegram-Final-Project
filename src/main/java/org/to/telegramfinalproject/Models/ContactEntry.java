package org.to.telegramfinalproject.Models;

import org.json.JSONObject;

import java.util.UUID;

public class ContactEntry {
    private UUID contactId;        // internal UUID
    private String userId;         // public ID
    private String profileName;
    private String imageUrl;
    private boolean isBlocked;

    public ContactEntry(UUID contactId, String userId, String profileName, String imageUrl, boolean isBlocked) {
        this.contactId = contactId;
        this.userId = userId;
        this.profileName = profileName;
        this.imageUrl = imageUrl;
        this.isBlocked = isBlocked;
    }

    public UUID getContactId() {
        return contactId;
    }

    public String getUserId() {
        return userId;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    @Override
    public String toString() {
        return profileName + " (" + userId + ")" + (isBlocked ? " [Blocked]" : "");
    }


    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("contact_id", contactId.toString());
        obj.put("user_id", userId);
        obj.put("profile_name", profileName);
        obj.put("image_url", imageUrl);
        obj.put("is_blocked", isBlocked);
        return obj;
    }

}
