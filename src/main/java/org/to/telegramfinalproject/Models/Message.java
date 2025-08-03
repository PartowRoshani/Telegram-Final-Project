package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Message {
    private final UUID message_id;
    private UUID sender_id;
    private String receiver_type; // private, group, channel
    private UUID receiver_id;
    private String content;
    private String message_type; // TEXT, IMAGE, FILE, ...
    private LocalDateTime send_at;
    private String status; // SENT, DELIVERED, READ
    private UUID reply_to_id;
    private boolean is_edited;
    private UUID original_message_id;
    private UUID forwarded_by;
    private UUID forwarded_from;
    private List<FileAttachment> attachments;

    public Message(UUID message_id, UUID sender_id, String receiver_type, UUID receiver_id, String content,
                   String message_type, LocalDateTime send_at, String status,
                   UUID reply_to_id, boolean is_edited, UUID original_message_id,
                   UUID forwarded_by, UUID forwarded_from) {
        this.message_id = message_id;
        this.sender_id = sender_id;
        this.receiver_type = receiver_type;
        this.receiver_id = receiver_id;
        this.content = content;
        this.message_type = message_type;
        this.send_at = send_at;
        this.status = status;
        this.reply_to_id = reply_to_id;
        this.is_edited = is_edited;
        this.original_message_id = original_message_id;
        this.forwarded_by = forwarded_by;
        this.forwarded_from = forwarded_from;
    }

    public Message(UUID messageId, UUID senderId, UUID receiverId, String receiverType, String content, String messageType, LocalDateTime now) {
        this.message_id = messageId;
        this.sender_id = senderId;
        this.receiver_id = receiverId;
        this.receiver_type = receiverType;
        this.content = content;
        this.message_type = messageType;
        this.send_at =  now;
    }


    public UUID getMessage_id() {
        return message_id;
    }


    public UUID getSender_id() {
        return sender_id;
    }

    public void setSender_id(UUID sender_id) {
        this.sender_id = sender_id;
    }

    public String getReceiver_type() {
        return receiver_type;
    }

    public void setReceiver_type(String receiver_type) {
        this.receiver_type = receiver_type;
    }

    public UUID getReceiver_id() {
        return receiver_id;
    }

    public void setReceiver_id(UUID receiver_id) {
        this.receiver_id = receiver_id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public LocalDateTime getSend_at() {
        return send_at;
    }

    public void setSend_at(LocalDateTime send_at) {
        this.send_at = send_at;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getReply_to_id() {
        return reply_to_id;
    }

    public void setReply_to_id(UUID reply_to_id) {
        this.reply_to_id = reply_to_id;
    }

    public boolean isIs_edited() {
        return is_edited;
    }

    public void setIs_edited(boolean is_edited) {
        this.is_edited = is_edited;
    }

    public UUID getOriginal_message_id() {
        return original_message_id;
    }

    public void setOriginal_message_id(UUID original_message_id) {
        this.original_message_id = original_message_id;
    }

    public UUID getForwarded_by() {
        return forwarded_by;
    }

    public void setForwarded_by(UUID forwarded_by) {
        this.forwarded_by = forwarded_by;
    }

    public UUID getForwarded_from() {
        return forwarded_from;
    }

    public void setForwarded_from(UUID forwarded_from) {
        this.forwarded_from = forwarded_from;
    }

    public void setAttachments(List<FileAttachment> attachments) {
        this.attachments = attachments;
    }
    public List<FileAttachment> getAttachments() {
        return attachments;
    }
}
