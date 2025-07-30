package org.to.telegramfinalproject.Models;

public class FileAttachment {
    private String fileUrl;
    private String fileType;

    public FileAttachment(String fileUrl, String fileType) {
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileType() {
        return fileType;
    }
}
