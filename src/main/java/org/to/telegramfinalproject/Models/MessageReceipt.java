package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageReceipt {
    private UUID messageId;
    private UUID userId;
    private LocalDateTime readAt;

    public MessageReceipt(UUID messageId, UUID userId, LocalDateTime readAt) {
        this.messageId = messageId;
        this.userId = userId;
        this.readAt = readAt;
    }

    // Overloaded constructor if readAt is not provided (use current timestamp)
    public MessageReceipt(UUID messageId, UUID userId) {
        this.messageId = messageId;
        this.userId = userId;
        this.readAt = LocalDateTime.now();
    }

    // Getters
    public UUID getMessageId() {
        return messageId;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    // Setters
    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    @Override
    public String toString() {
        return "MessageReceipt{" +
                "messageId=" + messageId +
                ", userId=" + userId +
                ", readAt=" + readAt +
                '}';
    }
}
