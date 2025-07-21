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

    // 🔹 نقش‌ها
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

    // ✅ کانستراکتور اضافه‌شده برای پشتیبانی از نقش‌ها (اختیاری، برای استفاده‌های جدید)
    public ChatEntry(UUID internalId, String displayId, String name, String imageUrl, String type, LocalDateTime lastMessageTime, boolean isOwner, boolean isAdmin) {
        this(internalId, displayId, name, imageUrl, type, lastMessageTime);
        this.isOwner = isOwner;
        this.isAdmin = isAdmin;
        this.permissions = permissions;

    }

    public ChatEntry() {

    }

    // 🟩 گتر و ستر جدید
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



}
