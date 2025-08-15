package org.to.telegramfinalproject.Utils;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.Message;
import org.to.telegramfinalproject.Models.User;
import org.to.telegramfinalproject.Database.MessageDatabase;
import org.to.telegramfinalproject.Database.userDatabase;

import java.util.UUID;

public class JsonUtil {

    public static JSONObject messageToJson(Message msg, UUID viewerId) {
        JSONObject json = new JSONObject();

        json.put("id", msg.getMessage_id());

        User sender = userDatabase.findByInternalUUID(msg.getSender_id());
        String senderUsername = sender != null ? sender.getProfile_name() : "Unknown";
        json.put("sender", senderUsername);

        json.put("content", msg.getContent());
        json.put("timestamp", msg.getSend_at().toString());
        json.put("content_type", msg.getMessage_type());

        if (msg.getSender_id().equals(viewerId)) {
            json.put("status", msg.getStatus()); // SENT, DELIVERED, READ
        }

        if (msg.getReply_to_id() != null) {
            Message replied = MessageDatabase.findById(msg.getReply_to_id());
            if (replied != null) {
                JSONObject preview = new JSONObject();
                preview.put("content_type", replied.getMessage_type());
                preview.put("content", replied.getContent());
                preview.put("file_url", replied.getFile_url());
                json.put("reply_to", replied.getMessage_id());
                json.put("reply_preview", preview);
            }
        }


        if (msg.getForwarded_from() != null) {
            json.put("forward_from", msg.getForwarded_from());
        }

        if ("file".equalsIgnoreCase(msg.getMessage_type())) {
            String fileName = extractFileName(msg.getFile_url());
            String fileMime = extractMimeType(fileName);
            json.put("file_name", fileName);
            json.put("file_mime", fileMime);
        }

        return json;
    }

    private static String extractFileName(String fileUrl) {
        if (fileUrl == null) return null;
        int lastSlash = fileUrl.lastIndexOf('/');
        return lastSlash >= 0 ? fileUrl.substring(lastSlash + 1) : fileUrl;
    }

    private static String extractMimeType(String fileName) {
        if (fileName == null) return null;
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }
}
