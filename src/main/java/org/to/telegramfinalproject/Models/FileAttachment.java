package org.to.telegramfinalproject.Models;

import org.json.JSONObject;
import java.util.Objects;
import java.util.UUID;

public class FileAttachment {

    private UUID attachmentId;     // اختیاری؛ اگر null بود، تولید می‌کنیم
    private UUID mediaKey;
    private String fileUrl;
    private String fileType;          // IMAGE, VIDEO, AUDIO, FILE, GIF, STICKER
    private String fileName;
    private Long   fileSize;
    private String mimeType;          // e.g., image/png
    private Integer width;
    private Integer height;
    private Integer durationSeconds;  // for audio/video
    private String  thumbnailUrl;
    private String storagePath;

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
        this(fileUrl, fileType, null, null, null, null, null, null, null);
    }

    public FileAttachment() {
    }


    // ساخت از JSON /upload
    public static FileAttachment fromUploadJson(JSONObject j) {
        return new FileAttachment(
                j.optString("file_url", ""),
                j.optString("file_type", "FILE"),
                emptyToNull(j.optString("file_name", null)),
                j.has("file_size") && !j.isNull("file_size") ? j.getLong("file_size") : null,
                emptyToNull(j.optString("mime_type", null)),
                j.has("width") && !j.isNull("width") ? j.getInt("width") : null,
                j.has("height") && !j.isNull("height") ? j.getInt("height") : null,
                j.has("duration_seconds") && !j.isNull("duration_seconds") ? j.getInt("duration_seconds") : null,
                j.isNull("thumbnail_url") ? null : emptyToNull(j.optString("thumbnail_url", null))
        );
    }

    public JSONObject toJson() {
        JSONObject out = new JSONObject()
                .put("file_url", fileUrl)
                .put("file_type", fileType);

        out.put("file_name", fileName == null ? JSONObject.NULL : fileName);
        out.put("file_size", fileSize == null ? JSONObject.NULL : fileSize);
        out.put("mime_type", mimeType == null ? JSONObject.NULL : mimeType);
        out.put("width", width == null ? JSONObject.NULL : width);
        out.put("height", height == null ? JSONObject.NULL : height);
        out.put("duration_seconds", durationSeconds == null ? JSONObject.NULL : durationSeconds);
        out.put("thumbnail_url", thumbnailUrl == null ? JSONObject.NULL : thumbnailUrl);
        return out;
    }

    // Helpers
    public boolean isImage() { return "IMAGE".equalsIgnoreCase(fileType) || "GIF".equalsIgnoreCase(fileType); }
    public boolean isAudio() { return "AUDIO".equalsIgnoreCase(fileType); }
    public boolean hasDimensions() { return width != null && height != null; }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // Getters
    public String getFileUrl() { return fileUrl; }
    public String getFileType() { return fileType; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public String getThumbnailUrl() { return thumbnailUrl; }

    // equals/hashCode/toString
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileAttachment)) return false;
        FileAttachment that = (FileAttachment) o;
        return Objects.equals(fileUrl, that.fileUrl) &&
                Objects.equals(fileType, that.fileType) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(fileSize, that.fileSize) &&
                Objects.equals(mimeType, that.mimeType) &&
                Objects.equals(width, that.width) &&
                Objects.equals(height, that.height) &&
                Objects.equals(durationSeconds, that.durationSeconds) &&
                Objects.equals(thumbnailUrl, that.thumbnailUrl);
    }

    @Override public int hashCode() {
        return Objects.hash(fileUrl, fileType, fileName, fileSize, mimeType, width, height, durationSeconds, thumbnailUrl);
    }

    @Override public String toString() {
        return "FileAttachment{" +
                "fileUrl='" + fileUrl + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", mimeType='" + mimeType + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", durationSeconds=" + durationSeconds +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                '}';
    }

    public UUID getAttachmentId() {return attachmentId;
    }

    public UUID getMediaKey() {return mediaKey;
    }

    public String getStoragePath() {return storagePath;
    }

    public void setAttachmentId(UUID attachmentId) {this.attachmentId = attachmentId;
    }

    public void setMediaKey(UUID mediaKey) {this.mediaKey = mediaKey;
    }


    public void setFileUrl(String fileUrl) {this.fileUrl = fileUrl;
    }
    public void setFileType(String fileType){this.fileType = fileType;}
    public void setFileName(String fileName){this.fileName = fileName;}
    public void setFileSize(Long fileSize){this.fileSize = fileSize;}
    public void setMimeType(String mimeType){this.mimeType = mimeType;}
    public void setWidth(int width){this.width = width;}
    public void setHeight(int height){this.height = height;}
    public void setDurationSeconds(Integer durationSeconds){this.durationSeconds = durationSeconds;}
    public void setThumbnailUrl(String thumbnailUrl){this.thumbnailUrl = thumbnailUrl;}
    public void setStoragePath(String storagePath) {this.storagePath = storagePath;
    }
}
