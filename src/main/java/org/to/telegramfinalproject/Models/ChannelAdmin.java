package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChannelAdmin {
    private String channelId;
    private UUID userId;
    private LocalDateTime addedAt;
    private UUID addedBy;
    private String role; // e.g., "owner", "admin"
    private boolean canPostMessage;
    private boolean canEditSetting;
    private boolean canDeleteMessages;
    private boolean canDeleteMembers;
    private boolean canAddMembers;

    public ChannelAdmin(String channelId, UUID userId, LocalDateTime addedAt, UUID addedBy,
                        String role, boolean canPostMessage, boolean canEditSetting, boolean canDeleteMessages,
                        boolean canDeleteMembers, boolean canAddMembers) {
        this.channelId = channelId;
        this.userId = userId;
        this.addedAt = addedAt;
        this.addedBy = addedBy;
        this.role = role;
        this.canPostMessage = canPostMessage;
        this.canEditSetting = canEditSetting;
        this.canDeleteMessages = canDeleteMessages;
        this.canDeleteMembers = canDeleteMembers;
        this.canAddMembers = canAddMembers;
    }

    // Overloaded constructor with default values from SQL
    public ChannelAdmin(String channelId, UUID userId, UUID addedBy) {
        this.channelId = channelId;
        this.userId = userId;
        this.addedBy = addedBy;
    }

    // Getters and setters
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public UUID getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(UUID addedBy) {
        this.addedBy = addedBy;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean getCanPostMessage() {
        return canPostMessage;
    }

    public void setCanPostMessage(boolean canPostMessage) {
        this.canPostMessage = canPostMessage;
    }

    public boolean getCanEditSetting() {
        return canEditSetting;
    }

    public void setCanEditSetting(boolean canEditSetting) {
        this.canEditSetting = canEditSetting;
    }

    public boolean getCanDeleteMessages() {
        return canDeleteMessages;
    }

    public void setCanDeleteMessages(boolean canDeleteMessages) {
        this.canDeleteMessages = canDeleteMessages;
    }

    public boolean getCanDeleteMembers() {
        return canDeleteMembers;
    }

    public void setCanDeleteMembers(boolean canDeleteMembers) {
        this.canDeleteMembers = canDeleteMembers;
    }

    public boolean getCanAddMembers() {
        return canAddMembers;
    }

    public void setCanAddMembers(boolean canAddMembers) {
        this.canAddMembers = canAddMembers;
    }

    @Override
    public String toString() {
        return "ChannelAdmin{" +
                "channelId='" + channelId + '\'' +
                ", userId=" + userId +
                ", addedAt=" + addedAt +
                ", addedBy=" + addedBy +
                ", role='" + role + '\'' +
                ", canPost=" + canPostMessage +
                ", canEdit=" + canEditSetting +
                ", canDeleteMessages=" + canDeleteMessages +
                ", canDeleteMembers=" + canDeleteMembers +
                ", canAddMembers=" + canAddMembers +
                '}';
    }
}
