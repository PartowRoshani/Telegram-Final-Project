package org.to.telegramfinalproject.Models;

import java.util.Objects;

public class FileAttachment {
    private  String fileUrl;
    private  String fileType;        // IMAGE, VIDEO, AUDIO, FILE, GIF, STICKER
    private  String fileName;
    private  Long fileSize;
    private  String mimeType;        // MIME type (example: image/png)
    private  Integer width;
    private  Integer height;
    private  Integer durationSeconds; //time for video and audio
    private  String thumbnailUrl;

    public FileAttachment(String fileUrl,
                          String fileType,
                          String fileName,
                          Long fileSize,
                          String mimeType,
                          Integer width,
                          Integer height,
                          Integer durationSeconds,
                          String thumbnailUrl) {
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.durationSeconds = durationSeconds;
        this.thumbnailUrl = thumbnailUrl;
    }

    public FileAttachment(String fileUrl, String fileType) {
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }

    // Getters
    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
