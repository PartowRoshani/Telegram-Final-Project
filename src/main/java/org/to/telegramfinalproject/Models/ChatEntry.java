package org.to.telegramfinalproject.Models;

import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatEntry {
    private UUID internalId;
    private String displayId;
    private String name;
    private String imageUrl;
    private String type;
    private LocalDateTime lastMessageTime;
    private boolean archived = false;
    private UUID otherUser;


    private boolean isOwner = false;
    private boolean isAdmin = false;
    private JSONObject permissions;


    public ChatEntry(UUID internalId, String displayId, String name, String imageUrl, String type, LocalDateTime lastMessageTime) {
        this.internalId = internalId;
        this.displayId = displayId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.type = type;
        this.lastMessageTime = lastMessageTime;
    }

    public ChatEntry(UUID internalId, String displayId, String name, String imageUrl, String type, LocalDateTime lastMessageTime, boolean isOwner, boolean isAdmin) {
        this(internalId, displayId, name, imageUrl, type, lastMessageTime);
        this.isOwner = isOwner;
        this.isAdmin = isAdmin;
        this.permissions = permissions;

    }

    public ChatEntry() {

    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public UUID getId() {
        return internalId;
    }

    public String getDisplayId() {
        return displayId;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }


    public JSONObject getPermissions() {
        return permissions;
    }

    public void setPermissions(JSONObject permissions) {
        this.permissions = permissions;
    }

    public void setName(String name) {this.name = name;
    }

    public void setDisplayId(String id) {this.displayId = id;
    }

    public void setImageUrl(String image_url) {this.imageUrl = image_url;
    }

    public void setType(String type) {this.type =type;
    }

    public void setId(String internalId) {this.internalId = UUID.fromString(internalId);
    }



    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

//    public void setLastMessageTime(String newTime) {this.lastMessageTime = LocalDateTime.parse(newTime);
//    }


    public void setLastMessageTime(String newTime) {
        if (newTime == null || newTime.isBlank()) {
            this.lastMessageTime = null;
            return;
        }

        try {
            this.lastMessageTime = LocalDateTime.parse(newTime);
        } catch (Exception e) {
            System.out.println("❌ Failed to parse lastMessageTime: " + newTime);
            this.lastMessageTime = null;
        }
    }

    public void setOtherUserId(UUID otherId) {this.otherUser =  otherId;
    }

    public UUID getOtherUserId(){
        return otherUser;
    }
}
