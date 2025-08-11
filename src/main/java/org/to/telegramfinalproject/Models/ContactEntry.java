package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import org.json.JSONObject;
import java.util.UUID;

public class ContactEntry {
    private UUID contactId;        // internal UUID
    private String userId;         // public ID
    private String profileName;
    private String imageUrl;
    private boolean isBlocked;
    private LocalDateTime lastSeenTime;
    private String contact_displayId;


    public ContactEntry(UUID contactId, String userId, String profileName, String imageUrl, boolean isBlocked) {
        this.contactId = contactId;
        this.userId = userId;
        this.profileName = profileName;
        this.imageUrl = imageUrl;
        this.isBlocked = isBlocked;
    }

    public ContactEntry(UUID contactId, String userId,String contact_displayId , String profileName, String imageUrl, boolean isBlocked, LocalDateTime lastSeenTime){
        this.contactId = contactId;
        this.userId = userId;
        this.contact_displayId = contact_displayId;
        this.profileName = profileName;
        this.imageUrl = imageUrl;
        this.isBlocked = isBlocked;
        this.lastSeenTime = lastSeenTime;
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

    public LocalDateTime getLastSeenTime() {
        return lastSeenTime;
    }

    public void setLastSeenTime(LocalDateTime lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    public String getContact_displayId() {
        return contact_displayId;
    }

    public void setContact_displayId(String contact_displayId) {
        this.contact_displayId = contact_displayId;
    }

    @Override
    public String toString() {
        return profileName + " (@" + contact_displayId + ")" + (isBlocked ? " [Blocked]" : "") + " Last Seen:" + (lastSeenTime != null ? " " + lastSeenTime.toString() : "");
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
