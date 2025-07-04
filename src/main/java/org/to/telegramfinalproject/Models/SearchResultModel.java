package org.to.telegramfinalproject.Models;

public class SearchResultModel {
    private final String type;
    private final String id;           // ← UUID واقعی برای عملیات
    private final String displayId;    // ← user_id یا group_id برای نمایش
    private String name;
    private final String content;
    private final String sender;
    private final String time;

    public SearchResultModel(String type, String id, String displayId,
                             String content, String sender, String time) {
        this.type = type;
        this.id = id;
        this.displayId = displayId;
        this.content = content;
        this.sender = sender;
        this.time = time;
    }

    // فقط در صورت نیاز برای user/group/channel
    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getDisplayId() {
        return displayId;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "[" + type.toUpperCase() + "] " + name + " (ID: " + displayId + ")";
    }
}
