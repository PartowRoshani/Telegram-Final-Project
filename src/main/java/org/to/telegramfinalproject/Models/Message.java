package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Message {
    private UUID message_id;
    private UUID sender_id;
    private String receiver_type;
    private UUID receiver_id;
    private String content;
    private String message_type;
    private LocalDateTime send_at;
    private String status;
    private UUID reply_to_id;
    private boolean is_edited;
    private UUID original_message_id;
    private UUID forwarded_by;
    private UUID forwarded_from;
    private List<FileAttachment> attachments;
    private boolean is_deleted_globally;
    private LocalDateTime edited_at;
    private transient String sender_name;
    private transient String receiver_name;


    // ✅ Full Constructor
    public Message(UUID message_id, UUID sender_id, String receiver_type, UUID receiver_id, String content,
                   String message_type, LocalDateTime send_at, String status,
                   UUID reply_to_id, boolean is_edited, boolean is_deleted_globally,
                   UUID original_message_id, UUID forwarded_by, UUID forwarded_from) {
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
        this.is_deleted_globally = is_deleted_globally;
        this.original_message_id = original_message_id;
        this.forwarded_by = forwarded_by;
        this.forwarded_from = forwarded_from;
    }

    public Message(UUID message_id, UUID sender_id, String receiver_type, UUID receiver_id, String content,
                   String message_type, LocalDateTime send_at, String status,
                   UUID reply_to_id, boolean is_edited, UUID original_message_id,
                   UUID forwarded_by, UUID forwarded_from,boolean is_deleted_globally, LocalDateTime edited_at) {
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
        this.is_deleted_globally = is_deleted_globally;
        this.edited_at = edited_at;
    }


    // ✅ Short Constructors
    //for normal messages
    public Message(UUID messageId, UUID senderId, UUID receiverId, String receiverType,
                   String content, String messageType, LocalDateTime sendAt) {
        this.message_id = messageId;
        this.sender_id = senderId;
        this.receiver_id = receiverId;
        this.receiver_type = receiverType;
        this.content = content;
        this.message_type = messageType;
        this.send_at = sendAt;
    }

    //for reply messages
    public Message(UUID messageId, UUID senderId, String receiverType, UUID receiverId,
                   String content, String messageType, LocalDateTime sendAt, String status,
                   UUID replyToId) {
        this.message_id = messageId;
        this.sender_id = senderId;
        this.receiver_type = receiverType;
        this.receiver_id = receiverId;
        this.content = content;
        this.message_type = messageType;
        this.send_at = sendAt;
        this.status = status;
        this.reply_to_id = replyToId;
        this.is_edited = false;
        this.is_deleted_globally = false;
    }




    public Message(UUID messageId, UUID senderId, String receiverType, UUID receiverId,
                   String content, String messageType, LocalDateTime sendAt, String status,
                   UUID replyToId, boolean isEdited, boolean isDeletedGlobally,
                   UUID originalMessageId, UUID forwardedBy, UUID forwardedFrom,
                   LocalDateTime editedAt) {

        this.message_id = messageId;
        this.sender_id = senderId;
        this.receiver_type = receiverType;
        this.receiver_id = receiverId;
        this.content = content;
        this.message_type = messageType;
        this.send_at = sendAt;
        this.status = status;
        this.reply_to_id = replyToId;
        this.is_edited = isEdited;
        this.is_deleted_globally = isDeletedGlobally;
        this.original_message_id = originalMessageId;
        this.forwarded_by = forwardedBy;
        this.forwarded_from = forwardedFrom;
        this.edited_at = editedAt;
    }



    // ✅ Getters & Setters
    public UUID getMessage_id() { return message_id; }
    public void setMessage_id(UUID messageId) { this.message_id = messageId; }

    public UUID getSender_id() { return sender_id; }
    public void setSender_id(UUID sender_id) { this.sender_id = sender_id; }

    public String getReceiver_type() { return receiver_type; }
    public void setReceiver_type(String receiver_type) { this.receiver_type = receiver_type; }

    public UUID getReceiver_id() { return receiver_id; }
    public void setReceiver_id(UUID receiver_id) { this.receiver_id = receiver_id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessage_type() { return message_type; }
    public void setMessage_type(String message_type) { this.message_type = message_type; }

    public LocalDateTime getSend_at() { return send_at; }
    public void setSend_at(LocalDateTime send_at) { this.send_at = send_at; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getReply_to_id() { return reply_to_id; }
    public void setReply_to_id(UUID reply_to_id) { this.reply_to_id = reply_to_id; }

    public boolean isIs_edited() { return is_edited; }
    public void setIs_edited(boolean is_edited) { this.is_edited = is_edited; }

    public boolean isIs_deleted_globally() { return is_deleted_globally; }
    public void setIs_deleted_globally(boolean is_deleted_globally) {
        this.is_deleted_globally = is_deleted_globally;
    }

    public UUID getOriginal_message_id() { return original_message_id; }
    public void setOriginal_message_id(UUID original_message_id) { this.original_message_id = original_message_id; }

    public UUID getForwarded_by() { return forwarded_by; }
    public void setForwarded_by(UUID forwarded_by) { this.forwarded_by = forwarded_by; }

    public UUID getForwarded_from() { return forwarded_from; }
    public void setForwarded_from(UUID forwarded_from) { this.forwarded_from = forwarded_from; }

    public List<FileAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<FileAttachment> attachments) { this.attachments = attachments; }
    public String getSender_name() {
        return sender_name;
    }

    public void setSender_name(String sender_name) {
        this.sender_name = sender_name;
    }

    public String getReceiver_name() {
        return receiver_name;
    }

    public void setReceiver_name(String receiver_name) {
        this.receiver_name = receiver_name;
    }

   public LocalDateTime getEdited_at() {
       return edited_at;
     }

    public void setEdited_at(LocalDateTime edited_at) {
        this.edited_at = edited_at;
    }


    public boolean getIs_deleted_globally() {
        return is_deleted_globally;
    }
//
//    public void setEdited_at(LocalDateTime edited_at) {
//        this.edited_at = edited_at;
//    }
//
//    public LocalDateTime getEdited_at() {
//        return edited_at;
//    }
}
