package org.to.telegramfinalproject.Models;

import java.util.UUID;

public class MediaRow {
    public UUID messageId;
    public String storagePath;
    public String fileName;
    public String mimeType;
    public Long fileSize;
    public String receiverType;
    public UUID receiverId;
    public UUID senderId;
    public java.util.UUID attachmentId;
    public java.util.UUID mediaKey;
    public String fileType; // IMAGE/AUDIO/...
    public Integer width;
    public Integer height;
    public Integer durationSeconds; //for audio only
    public String thumbnailUrl;
    public String fileUrl;     //display link
}
