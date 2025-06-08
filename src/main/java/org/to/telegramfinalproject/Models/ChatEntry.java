package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;

public class ChatEntry {
    private final String name;
    private final String id;
    private final String imageUrl;
    private final String type; // "private", "group", "channel"
    private final LocalDateTime lastMessageTime;

    public ChatEntry(String name, String id, String imageUrl, String type, LocalDateTime lastMessageTime) {
        this.name = name;
        this.id = id;
        this.imageUrl = imageUrl;
        this.type = type;
        this.lastMessageTime = lastMessageTime;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
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
}
