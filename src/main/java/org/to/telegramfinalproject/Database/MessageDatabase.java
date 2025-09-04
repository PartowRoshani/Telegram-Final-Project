package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.FileAttachment;
import org.to.telegramfinalproject.Models.MediaRow;
import org.to.telegramfinalproject.Models.Message;
import org.to.telegramfinalproject.Utils.ChannelPermissionUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class MessageDatabase {





    public static boolean insertMessage(UUID messageId, UUID senderId, UUID receiverId,
                                        String receiverType, String content, String messageType) {
        String sql = "INSERT INTO messages (message_id, sender_id, receiver_type, receiver_id, content, message_type) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, messageId);
            ps.setObject(2, senderId);
            ps.setString(3, receiverType);
            ps.setObject(4, receiverId);
            ps.setString(5, content);
            ps.setString(6, messageType);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public static boolean insertAttachments(UUID messageId, List<FileAttachment> attachments) {
        String sql = "INSERT INTO message_attachments (attachment_id, message_id, file_url, file_type) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (FileAttachment att : attachments) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, messageId);
                ps.setString(3, att.getFileUrl());
                ps.setString(4, att.getFileType());
                ps.addBatch();
            }

            ps.executeBatch();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void markGloballyDeleted(UUID chatId) {
        String sql = "UPDATE messages SET is_deleted_globally = true WHERE receiver_id = ? AND receiver_type = 'private'";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, chatId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void logDeletedMessagesFor(UUID chatId, UUID userId) {
        List<Message> messages = MessageDatabase.privateChatHistory(chatId, userId);
        for (Message message : messages) {
            if (!isMessageDeleted(message.getMessage_id(), userId)) {
                String sql = """
                INSERT INTO deleted_messages (message_id, user_id)
                VALUES (?, ?)
                ON CONFLICT (message_id, user_id) DO NOTHING
                """;
                try (Connection conn = ConnectionDb.connect();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, message.getMessage_id());
                    ps.setObject(2, userId);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isMessageDeleted(UUID messageId, UUID userId) {
        String sql = "SELECT 1 FROM deleted_messages WHERE message_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ps.setObject(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Message> getMessagesForChat(UUID chatId, String chatType, UUID currentUserId, int offset, int limit) {
        List<Message> messages = new ArrayList<>();

        String sql = """
        SELECT * FROM messages m
        WHERE m.receiver_type = ?
          AND m.receiver_id = ?
          AND m.is_deleted_globally = FALSE
          AND NOT EXISTS (
              SELECT 1 FROM deleted_messages d
              WHERE d.message_id = m.message_id
                AND d.user_id = ?
          )
        ORDER BY m.send_at DESC
        LIMIT ? OFFSET ?
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, chatType);
            ps.setObject(2, chatId);
            ps.setObject(3, currentUserId);
            ps.setInt(4, limit);
            ps.setInt(5, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message(
                            UUID.fromString(rs.getString("message_id")),
                            UUID.fromString(rs.getString("sender_id")),
                            rs.getString("receiver_type"),
                            UUID.fromString(rs.getString("receiver_id")),
                            rs.getString("content"),
                            rs.getString("message_type"),
                            rs.getTimestamp("send_at").toLocalDateTime(),
                            rs.getString("status"),
                            rs.getObject("reply_to_id") != null ? UUID.fromString(rs.getString("reply_to_id")) : null,
                            rs.getBoolean("is_edited"),
                            rs.getBoolean("is_deleted_globally"),
                            rs.getObject("original_message_id") != null ? UUID.fromString(rs.getString("original_message_id")) : null,
                            rs.getObject("forwarded_by") != null ? UUID.fromString(rs.getString("forwarded_by")) : null,
                            rs.getObject("forwarded_from") != null ? UUID.fromString(rs.getString("forwarded_from")) : null,
                            rs.getTimestamp("edited_at") != null
                                    ? rs.getTimestamp("edited_at").toLocalDateTime()
                                    : null


                    );

                    messages.add(msg);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public static Message findById(UUID messageId) {
        String sql = "SELECT * FROM messages WHERE message_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Message(
                        UUID.fromString(rs.getString("message_id")),
                        UUID.fromString(rs.getString("sender_id")),
                        rs.getString("receiver_type"),
                        UUID.fromString(rs.getString("receiver_id")),
                        rs.getString("content"),
                        rs.getString("message_type"),
                        rs.getTimestamp("send_at").toLocalDateTime(),
                        rs.getString("status"),
                        rs.getObject("reply_to_id") != null ? UUID.fromString(rs.getString("reply_to_id")) : null,
                        rs.getBoolean("is_edited"),
                        rs.getBoolean("is_deleted_globally"),
                        rs.getObject("original_message_id") != null ? UUID.fromString(rs.getString("original_message_id")) : null,
                        rs.getObject("forwarded_by") != null ? UUID.fromString(rs.getString("forwarded_by")) : null,
                        rs.getObject("forwarded_from") != null ? UUID.fromString(rs.getString("forwarded_from")) : null
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean markAsDeletedForUser(UUID messageId, UUID userId) {
        String sql = "INSERT INTO deleted_messages (message_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ps.setObject(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean markAsGloballyDeleted(UUID messageId) {
        String sql = "UPDATE messages SET is_deleted_globally = true WHERE message_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateContentAndMarkEdited(UUID messageId, String newContent) {
        String sql = """
        UPDATE messages
        SET content = ?, is_edited = TRUE, edited_at = CURRENT_TIMESTAMP
        WHERE message_id = ?
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newContent);
            ps.setObject(2, messageId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveReplyMessage(Message message) {
        String sql = """
        INSERT INTO messages (
            message_id, sender_id, receiver_type, receiver_id, content, message_type,
            send_at, status, reply_to_id, is_edited, is_deleted_globally,
            original_message_id, forwarded_by, forwarded_from, edited_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, message.getMessage_id());
            ps.setObject(2, message.getSender_id());
            ps.setString(3, message.getReceiver_type());
            ps.setObject(4, message.getReceiver_id());
            ps.setString(5, message.getContent());
            ps.setString(6, message.getMessage_type());
            ps.setTimestamp(7, Timestamp.valueOf(message.getSend_at()));
            ps.setString(8, message.getStatus());
            ps.setObject(9, message.getReply_to_id()); //Important part because of reply message id
            ps.setBoolean(10, message.isIs_edited());
            ps.setBoolean(11, message.isIs_deleted_globally());
            ps.setObject(12, message.getOriginal_message_id());
            ps.setObject(13, message.getForwarded_by());
            ps.setObject(14, message.getForwarded_from());
            ps.setTimestamp(15, message.getEdited_at() != null ? Timestamp.valueOf(message.getEdited_at()) : null);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveForwardedMessage(Message message) {
        String sql = """
        INSERT INTO messages (
            message_id, sender_id, receiver_type, receiver_id,
            content, message_type, send_at, status,
            reply_to_id, is_edited, is_deleted_globally,
            original_message_id, forwarded_by, forwarded_from
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, message.getMessage_id());
            ps.setObject(2, message.getSender_id());
            ps.setString(3, message.getReceiver_type());
            ps.setObject(4, message.getReceiver_id());
            ps.setString(5, message.getContent());
            ps.setString(6, message.getMessage_type());
            ps.setTimestamp(7, Timestamp.valueOf(message.getSend_at()));
            ps.setString(8, message.getStatus());
            ps.setObject(9, message.getReply_to_id());
            ps.setBoolean(10, message.isIs_edited());
            ps.setBoolean(11, message.isIs_deleted_globally());
            ps.setObject(12, message.getOriginal_message_id()); //original id
            ps.setObject(13, message.getForwarded_by());       //forwarded by
            ps.setObject(14, message.getForwarded_from());      //forwarded from

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getExcerpt(UUID messageId) {
        return getExcerpt(messageId, 80);
    }

    public static String getExcerpt(UUID messageId, int maxLen) {
        String sql = "SELECT content, message_type, is_deleted_globally FROM messages WHERE message_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return "Message isn't available";

                String content = rs.getString("content");
                String type = rs.getString("message_type"); // e.g. TEXT, IMAGE, VIDEO, AUDIO, FILE, STICKER...
                boolean deleted = rs.getBoolean("is_deleted_globally");

                if (deleted) return "This message was deleted";

                if (type == null || type.equalsIgnoreCase("TEXT")) {
                    if (content == null || content.isBlank()) return "(empty)";
                    return shorten(content, maxLen);
                }

                //for media
                switch (type.toUpperCase()) {
                    case "IMAGE":  return "[Photo]";
                    case "VIDEO":  return "[Video]";
                    case "AUDIO":  return "[Audio]";
                    case "VOICE":  return "[Voice]";
                    case "FILE":   return "[File]";
                    case "STICKER":return "[Sticker]";
                    default:       return "[" + type + "]";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private static String shorten(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, Math.max(0, maxLen - 1)).trim() + "…";
    }

    public static boolean saveMessageWithOptionalAttachments(
            UUID messageId, UUID senderId, UUID receiverId,
            String receiverType, String content, String messageType,
            List<FileAttachment> attachments
    ) {
        Connection conn = null;
        try {
            conn = ConnectionDb.connect();
            conn.setAutoCommit(false);

            boolean isText  = "TEXT".equalsIgnoreCase(messageType);
            boolean isImage = "IMAGE".equalsIgnoreCase(messageType);
            boolean isAudio = "AUDIO".equalsIgnoreCase(messageType);
            if (!isText && !isImage && !isAudio) {
                throw new IllegalArgumentException("messageType must be TEXT, IMAGE, or AUDIO");
            }

            if (isText) {
                if (attachments != null && !attachments.isEmpty())
                    throw new IllegalArgumentException("TEXT must not have attachments");
                if (content == null || content.isBlank())
                    throw new IllegalArgumentException("TEXT must have non-empty content");
            } else {
                if (attachments == null || attachments.isEmpty())
                    throw new IllegalArgumentException("Non-TEXT must have at least one attachment");

                for (FileAttachment a : attachments) {
                    if (a == null) throw new IllegalArgumentException("Attachment is null");
                    String ft = a.getFileType();
                    if (isImage && !"IMAGE".equalsIgnoreCase(ft))
                        throw new IllegalArgumentException("All attachments must be IMAGE for messageType=IMAGE");
                    if (isAudio && !"AUDIO".equalsIgnoreCase(ft))
                        throw new IllegalArgumentException("All attachments must be AUDIO for messageType=AUDIO");
                }
            }

            insertMessageTx(conn, messageId, senderId, receiverId, receiverType, content, messageType.toUpperCase());
            if (!isText) insertAttachmentsTx(conn, messageId, attachments);

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }


    public static boolean insertMessageTx(Connection conn, UUID messageId, UUID senderId, UUID receiverId,
                                          String receiverType, String content, String messageType) throws SQLException {
        String sql = "INSERT INTO messages (message_id, sender_id, receiver_type, receiver_id, content, message_type) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ps.setObject(2, senderId);
            ps.setString(3, receiverType);
            ps.setObject(4, receiverId);
            if (content == null || content.isBlank()) ps.setNull(5, java.sql.Types.VARCHAR); else ps.setString(5, content);
            ps.setString(6, messageType);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean insertAttachmentsTx(Connection conn, UUID messageId, List<FileAttachment> attachments) throws SQLException {
        if (attachments == null || attachments.isEmpty()) return true;

        final String sql = """
        INSERT INTO message_attachments(
            attachment_id, message_id,
            file_url, file_type, file_name, file_size, mime_type,
            width, height, duration_seconds, thumbnail_url,
            media_key, storage_path
        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (FileAttachment att : attachments) {
                if (att == null) throw new IllegalArgumentException("Attachment is null");
                UUID attachmentId = att.getAttachmentId() != null ? att.getAttachmentId() : UUID.randomUUID();
                UUID mediaKey     = att.getMediaKey()     != null ? att.getMediaKey()     : attachmentId; // ساده‌ترین حالت

                String ft = att.getFileType();
                if (!"IMAGE".equalsIgnoreCase(ft) && !"AUDIO".equalsIgnoreCase(ft)) {
                    throw new IllegalArgumentException("file_type must be IMAGE or AUDIO");
                }
                if (att.getStoragePath() == null || att.getStoragePath().isBlank()) {
                    throw new IllegalArgumentException("storage_path is required for socket downloads");
                }

                int i = 1;
                ps.setObject(i++, attachmentId);
                ps.setObject(i++, messageId);
                //file url (display link)
                if (att.getFileUrl() == null || att.getFileUrl().isBlank()) ps.setNull(i++, java.sql.Types.VARCHAR);
                else ps.setString(i++, att.getFileUrl());

                ps.setString(i++, ft.toUpperCase());
                ps.setString(i++, att.getFileName());
                if (att.getFileSize() == null) ps.setNull(i++, java.sql.Types.BIGINT); else ps.setLong(i++, att.getFileSize());
                if (att.getMimeType() == null) ps.setNull(i++, java.sql.Types.VARCHAR); else ps.setString(i++, att.getMimeType());
                if (att.getWidth() == null) ps.setNull(i++, java.sql.Types.INTEGER); else ps.setInt(i++, att.getWidth());
                if (att.getHeight() == null) ps.setNull(i++, java.sql.Types.INTEGER); else ps.setInt(i++, att.getHeight());
                if (att.getDurationSeconds() == null) ps.setNull(i++, java.sql.Types.INTEGER); else ps.setInt(i++, att.getDurationSeconds());
                if (att.getThumbnailUrl() == null || att.getThumbnailUrl().isBlank()) ps.setNull(i++, java.sql.Types.VARCHAR);
                else ps.setString(i++, att.getThumbnailUrl());

                ps.setObject(i++, mediaKey);
                ps.setString(i++, att.getStoragePath());

                ps.addBatch();

                att.setAttachmentId(attachmentId);
                att.setMediaKey(mediaKey);
            }
            ps.executeBatch();
            return true;
        }
    }

    public static MediaRow findMediaByKey(UUID mediaKey) throws SQLException {
        String sql = """
        SELECT
          ma.attachment_id,
          ma.message_id,
          ma.media_key,
          ma.file_name,
          ma.file_size,
          ma.mime_type,
          ma.file_type,
          ma.width,
          ma.height,
          ma.duration_seconds,
          ma.thumbnail_url,
          ma.file_url,
          ma.storage_path,
          m.receiver_type,
          m.receiver_id,
          m.sender_id
        FROM message_attachments ma
        JOIN messages m ON m.message_id = ma.message_id
        WHERE ma.media_key = ?
        LIMIT 1
    """;

        try (Connection c = ConnectionDb.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, mediaKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                MediaRow a = new MediaRow();
                a.attachmentId    = (UUID) rs.getObject("attachment_id");
                a.messageId       = (UUID) rs.getObject("message_id");
                a.mediaKey        = (UUID) rs.getObject("media_key");
                a.fileName        = rs.getString("file_name");

                long sz = rs.getLong("file_size");
                a.fileSize = rs.wasNull() ? null : sz; // MediaRow.fileSize = Long

                a.mimeType        = rs.getString("mime_type");
                a.fileType        = rs.getString("file_type");
                int w = rs.getInt("width");            a.width  = rs.wasNull() ? null : w;
                int h = rs.getInt("height");           a.height = rs.wasNull() ? null : h;
                int d = rs.getInt("duration_seconds"); a.durationSeconds = rs.wasNull() ? null : d;
                a.thumbnailUrl    = rs.getString("thumbnail_url");
                a.fileUrl         = rs.getString("file_url");
                a.storagePath     = rs.getString("storage_path");
                a.receiverType    = rs.getString("receiver_type");
                a.receiverId      = (UUID) rs.getObject("receiver_id");
                a.senderId        = (UUID) rs.getObject("sender_id");
                return a;
            }
        }
    }

    public static boolean canAccess(UUID requester, MediaRow mr) {
        if (requester == null || mr == null || mr.receiverType == null) return false;

        // اختیاری: فرستنده همیشه مجاز
        if (requester.equals(mr.senderId)) return true;

        switch (mr.receiverType.toLowerCase(Locale.ROOT)) {
            case "private":
                // receiver_id در پیام‌های private = UUID چت خصوصی
                return PrivateChatDatabase.isParticipant(mr.receiverId, requester);

            case "group":
                return GroupDatabase.isMember(mr.receiverId, requester);

            case "channel":
                return ChannelPermissionUtil.isUserInChannel(requester, mr.receiverId);

            default:
                return false;
        }
    }



    public void markMessageAsRead(UUID messageId, UUID userId) {
        String sql = "INSERT INTO message_receipts (message_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, messageId);
            stmt.setObject(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




//    public static List<Message> getUnreadMessages(UUID userId) {
//        List<Message> messages = new ArrayList<>();
//
//        String sql = """
//        SELECT m.*
//        FROM messages m
//        LEFT JOIN message_receipts r ON m.message_id = r.message_id AND r.user_id = ?
//        LEFT JOIN deleted_messages d ON m.message_id = d.message_id AND d.user_id = ?
//        WHERE r.user_id IS NULL
//          AND d.message_id IS NULL
//          AND m.is_deleted_globally = FALSE
//          AND (
//              (m.receiver_type = 'private' AND m.receiver_id = ?)
//              OR
//              (m.receiver_type = 'group' AND EXISTS (
//                  SELECT 1 FROM group_members gm WHERE gm.group_id = m.receiver_id AND gm.user_id = ?
//              ))
//              OR
//              (m.receiver_type = 'channel' AND EXISTS (
//                  SELECT 1 FROM channel_subscribers cs WHERE cs.channel_id = m.receiver_id AND cs.user_id = ?
//              ))
//          )
//        ORDER BY m.send_at DESC
//    """;
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setObject(1, userId); // for message_receipts
//            stmt.setObject(2, userId); // for deleted_messages
//            stmt.setObject(3, userId); // for private messages
//            stmt.setObject(4, userId); // for group members
//            stmt.setObject(5, userId); // for channel subscribers
//
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                Message message = new Message(
//                        UUID.fromString(rs.getString("message_id")),
//                        rs.getObject("sender_id") != null ? UUID.fromString(rs.getString("sender_id")) : null,
//                        rs.getString("receiver_type"),
//                        UUID.fromString(rs.getString("receiver_id")),
//                        rs.getString("content"),
//                        rs.getString("message_type"),
//                        rs.getTimestamp("send_at").toLocalDateTime(),
//                        rs.getString("status"),
//                        rs.getObject("reply_to_id") != null ? UUID.fromString(rs.getString("reply_to_id")) : null,
//                        rs.getBoolean("is_edited"),
//                        rs.getObject("original_message_id") != null ? UUID.fromString(rs.getString("original_message_id")) : null,
//                        rs.getObject("forwarded_by") != null ? UUID.fromString(rs.getString("forwarded_by")) : null,
//                        rs.getObject("forwarded_from") != null ? UUID.fromString(rs.getString("forwarded_from")) : null,
//                        rs.getBoolean("is_deleted_globally"),
//                        rs.getTimestamp("edited_at") != null ? rs.getTimestamp("edited_at").toLocalDateTime() : null
//                        );
//
//                messages.add(message);
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return messages;
//    }



    public static List<Message> getUnreadMessages(UUID userId) {
        return getUnreadMessages(userId, 200);
    }

    public static List<Message> getUnreadMessages(UUID userId, int limit) {
        List<Message> messages = new ArrayList<>();

        String sql = """
        SELECT m.*
        FROM messages m
        LEFT JOIN message_receipts r
               ON r.message_id = m.message_id AND r.user_id = ?
        LEFT JOIN deleted_messages d
               ON d.message_id = m.message_id AND d.user_id = ?
        WHERE r.message_id IS NULL
          AND d.message_id IS NULL
          AND m.is_deleted_globally = FALSE
          AND m.sender_id <> ?
          AND (
                (m.receiver_type = 'private' AND EXISTS (
                    SELECT 1
                    FROM private_chat pc
                    WHERE pc.chat_id = m.receiver_id
                      AND (pc.user1_id = ? OR pc.user2_id = ?)
                ))
             OR (m.receiver_type = 'group' AND EXISTS (
                    SELECT 1
                    FROM group_members gm
                    WHERE gm.group_id = m.receiver_id
                      AND gm.user_id  = ?
                ))
             OR (m.receiver_type = 'channel' AND EXISTS (
                    SELECT 1
                    FROM channel_subscribers cs
                    WHERE cs.channel_id = m.receiver_id
                      AND cs.user_id    = ?
                ))
          )
        ORDER BY m.send_at ASC
        LIMIT ?
        """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setObject(i++, userId); // r.user_id
            ps.setObject(i++, userId); // d.user_id
            ps.setObject(i++, userId); // m.sender_id <> ?
            ps.setObject(i++, userId); // pc.user1_id
            ps.setObject(i++, userId); // pc.user2_id
            ps.setObject(i++, userId); // gm.user_id
            ps.setObject(i++, userId); // cs.user_id
            ps.setInt(i, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }



    private static java.util.UUID readUUID(ResultSet rs, String col) throws SQLException {
        Object o = rs.getObject(col);
        if (o == null) return null;
        if (o instanceof java.util.UUID) return (java.util.UUID) o;
        return java.util.UUID.fromString(o.toString());
    }

    private static java.time.LocalDateTime readLdt(ResultSet rs, String col) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(col);
        return (ts == null) ? null : ts.toLocalDateTime();
    }

    private static boolean readBool(ResultSet rs, String col) throws SQLException {
        boolean v = rs.getBoolean(col);
        return v;
    }

    private static Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();

        m.setMessage_id(          readUUID(rs, "message_id")            );
        m.setSender_id(           readUUID(rs, "sender_id")             );
        m.setReceiver_type(       rs.getString("receiver_type")         );
        m.setReceiver_id(         readUUID(rs, "receiver_id")           );
        m.setContent(             rs.getString("content")               );
        m.setMessage_type(        rs.getString("message_type")          );
        m.setSend_at(             readLdt(rs, "send_at")                );
        m.setStatus(              rs.getString("status")                );
        m.setReply_to_id(         readUUID(rs, "reply_to_id")           );
        m.setIs_edited(           readBool(rs, "is_edited")             );
        m.setEdited_at(           readLdt(rs, "edited_at")              );
        m.setOriginal_message_id( readUUID(rs, "original_message_id")   );
        m.setForwarded_by(        readUUID(rs, "forwarded_by")          );
        m.setForwarded_from(      readUUID(rs, "forwarded_from")        );
        m.setIs_deleted_globally( readBool(rs, "is_deleted_globally")   );

        return m;
    }

    public static List<FileAttachment> getAttachments(UUID messageId) {
        List<FileAttachment> attachments = new ArrayList<>();
        String sql = "SELECT file_url, file_type FROM message_attachments WHERE message_id = ?";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, messageId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                attachments.add(new FileAttachment(
                        rs.getString("file_url"),
                        rs.getString("file_type")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return attachments;
    }



    public static LocalDateTime getLastMessageTimeBetween(UUID user1, UUID user2, String type) {
        String sql = """
        SELECT MAX(send_at) FROM messages
        WHERE receiver_type = ?
        AND (
            (sender_id = ? AND receiver_id = ?)
            OR (sender_id = ? AND receiver_id = ?)
        )
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            stmt.setObject(2, user1);
            stmt.setObject(3, user2);
            stmt.setObject(4, user2);
            stmt.setObject(5, user1);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static LocalDateTime getLastMessageTime(UUID receiverId, String type) {
        String sql = "SELECT MAX(send_at) FROM messages WHERE receiver_id = ? AND receiver_type = ?";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, receiverId);
            stmt.setString(2, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                return ts != null ? ts.toLocalDateTime() : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static Message extractMessage(ResultSet rs) throws SQLException {
        return new Message(
                UUID.fromString(rs.getString("message_id")),
                UUID.fromString(rs.getString("sender_id")),
                rs.getString("receiver_type"),
                UUID.fromString(rs.getString("receiver_id")),
                rs.getString("content"),
                rs.getString("message_type"),
                rs.getTimestamp("send_at").toLocalDateTime(),
                rs.getString("status"),
                (UUID) rs.getObject("reply_to_id"),
                rs.getBoolean("is_edited"),
                (UUID) rs.getObject("original_message_id"),
                (UUID) rs.getObject("forwarded_by"),
                (UUID) rs.getObject("forwarded_from"),
                rs.getBoolean("is_deleted_globally"),
                (rs.getTimestamp("edited_at") != null) ? rs.getTimestamp("edited_at").toLocalDateTime() : null
        );
    }




//    public static List<Message> searchMessagesForUser(UUID userId, String keyword) {
//        List<Message> result = new ArrayList<>();
//        String sql = """
//        SELECT * FROM messages
//        WHERE receiver_type = 'private'
//          AND (sender_id = ? OR receiver_id = ?)
//          AND content ILIKE ?
//        ORDER BY send_at DESC
//    """;
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setObject(1, userId);
//            stmt.setObject(2, userId);
//            stmt.setString(3, "%" + keyword + "%");
//
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                result.add(extractMessage(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }

    public static List<Message> searchMessagesForUser(UUID userId, String keyword) {
        String sql = """
        SELECT * FROM messages m
        WHERE m.receiver_type = 'private'
          AND m.content ILIKE ?
          AND m.is_deleted_globally = FALSE
          AND (m.sender_id = ? OR m.receiver_id = ?)
          AND NOT EXISTS (
              SELECT 1 FROM deleted_messages d
              WHERE d.message_id = m.message_id AND d.user_id = ?
          )
        ORDER BY m.send_at DESC
        """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setObject(2, userId); // m.sender_id
            ps.setObject(3, userId); // m.receiver_id
            ps.setObject(4, userId); // deleted_messages

            ResultSet rs = ps.executeQuery();
            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            return messages;

        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        return new Message(
                UUID.fromString(rs.getString("message_id")),
                rs.getObject("sender_id") != null ? UUID.fromString(rs.getString("sender_id")) : null,
                rs.getString("receiver_type"),
                UUID.fromString(rs.getString("receiver_id")),
                rs.getString("content"),
                rs.getString("message_type"),
                rs.getTimestamp("send_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getObject("reply_to_id") != null ? UUID.fromString(rs.getString("reply_to_id")) : null,
                rs.getBoolean("is_edited"),
                rs.getBoolean("is_deleted_globally"),
                rs.getObject("original_message_id") != null ? UUID.fromString(rs.getString("original_message_id")) : null,
                rs.getObject("forwarded_by") != null ? UUID.fromString(rs.getString("forwarded_by")) : null,
                rs.getObject("forwarded_from") != null ? UUID.fromString(rs.getString("forwarded_from")) : null
        );
    }



//    public static List<Message> privateChatHistory(UUID chatId) {
//        List<Message> result = new ArrayList<>();
//        String sql = """
//        SELECT * FROM messages
//        WHERE receiver_type = 'private'
//        AND receiver_id = ?
//        ORDER BY send_at
//    """;
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setObject(1, chatId);
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                result.add(extractMessage(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return result;
//    }

    public static List<Message> privateChatHistory(UUID chatId, UUID userId) {
        List<Message> result = new ArrayList<>();
        String sql = """
        SELECT * FROM messages
        WHERE receiver_type = 'private'
          AND receiver_id = ?
          AND is_deleted_globally = FALSE
          AND message_id NOT IN (
              SELECT message_id FROM deleted_messages WHERE user_id = ?
          )
        ORDER BY send_at
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, chatId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }





//    public static List<Message> groupChatHistory(UUID groupId) {
//        List<Message> result = new ArrayList<>();
//        String sql = """
//        SELECT * FROM messages
//        WHERE receiver_type = 'group' AND receiver_id = ?
//        ORDER BY send_at
//    """;
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setObject(1, groupId);
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                result.add(extractMessage(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return result;
//    }


    public static List<Message> groupChatHistory(UUID groupId,UUID userId) {
        List<Message> result = new ArrayList<>();
        String sql = """
        SELECT * FROM messages
        WHERE receiver_type = 'group'
          AND receiver_id = ?
          AND is_deleted_globally = FALSE
          AND message_id NOT IN (
              SELECT message_id FROM deleted_messages WHERE user_id = ?
          )
        ORDER BY send_at
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }



//    public static List<Message> channelChatHistory(UUID channelId) {
//        List<Message> result = new ArrayList<>();
//        String sql = """
//        SELECT * FROM messages
//        WHERE receiver_type = 'channel' AND receiver_id = ?
//        ORDER BY send_at
//    """;
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setObject(1, channelId);
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                result.add(extractMessage(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return result;
//    }


    public static List<Message> channelChatHistory(UUID channelId, UUID userId) {
        List<Message> result = new ArrayList<>();
        String sql = """
        SELECT * FROM messages
        WHERE receiver_type = 'channel'
          AND receiver_id = ?
          AND is_deleted_globally = FALSE
          AND message_id NOT IN (
              SELECT message_id FROM deleted_messages WHERE user_id = ?
          )
        ORDER BY send_at
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }



//    public static List<Message> searchMessagesInGroups(List<UUID> groupIds, String keyword) {
//        List<Message> result = new ArrayList<>();
//        if (groupIds.isEmpty()) return result;
//
//        String placeholders = groupIds.stream().map(id -> "?").collect(Collectors.joining(", "));
//        String sql = """
//        SELECT * FROM messages
//        WHERE receiver_type = 'group'
//        AND receiver_id IN (%s)
//        AND content ILIKE ?
//        ORDER BY send_at DESC
//    """.formatted(placeholders);
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            int i = 1;
//            for (UUID id : groupIds) {
//                stmt.setObject(i++, id);
//            }
//            stmt.setString(i, "%" + keyword + "%");
//
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                result.add(extractMessage(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }



    public static List<Message> searchMessagesInGroups(List<UUID> groupIds, String keyword, UUID userId) {
        List<Message> result = new ArrayList<>();
        if (groupIds.isEmpty()) return result;

        String placeholders = groupIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
        SELECT m.*
        FROM messages m
        LEFT JOIN deleted_messages d ON m.message_id = d.message_id AND d.user_id = ?
        WHERE m.receiver_type = 'group'
          AND m.receiver_id IN (%s)
          AND d.message_id IS NULL
          AND m.is_deleted_globally = FALSE
          AND m.content ILIKE ?
        ORDER BY m.send_at DESC
    """.formatted(placeholders);

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            stmt.setObject(i++, userId);
            for (UUID groupId : groupIds) {
                stmt.setObject(i++, groupId);
            }

            stmt.setString(i, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }


//    public static List<Message> searchMessagesInChannels(List<UUID> channelIds, String keyword) {
//        List<Message> result = new ArrayList<>();
//        if (channelIds.isEmpty()) return result;
//
//        String placeholders = channelIds.stream().map(id -> "?").collect(Collectors.joining(", "));
//        String sql = """
//        SELECT * FROM messages
//        WHERE receiver_type = 'channel'
//        AND receiver_id IN (%s)
//        AND content ILIKE ?
//        ORDER BY send_at DESC
//    """.formatted(placeholders);
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            int i = 1;
//            for (UUID id : channelIds) {
//                stmt.setObject(i++, id);
//            }
//            stmt.setString(i, "%" + keyword + "%");
//
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                result.add(extractMessage(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }



    public static List<Message> searchMessagesInChannels(List<UUID> channelIds, String keyword) {
        List<Message> result = new ArrayList<>();
        if (channelIds.isEmpty()) return result;

        String placeholders = channelIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
        SELECT m.*
        FROM messages m
        WHERE m.receiver_type = 'channel'
          AND m.receiver_id IN (%s)
          AND m.is_deleted_globally = FALSE
          AND m.content ILIKE ?
        ORDER BY m.send_at DESC
    """.formatted(placeholders);

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (UUID channelId : channelIds) {
                stmt.setObject(i++, channelId);
            }

            stmt.setString(i, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapResultSetToMessage(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }


    public List<Message> getMessagesForPrivateChat(UUID user1, UUID user2) {
        UUID chatId = PrivateChatDatabase.findChatIdByUsers(user1, user2);
        if (chatId == null) return new ArrayList<>();
        return findByReceiver("private", chatId);
    }

    public static List<Message> findByReceiver(String receiverType, UUID receiverId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE receiver_type = ? AND receiver_id = ? ORDER BY send_at";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, receiverType);
            ps.setObject(2, receiverId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID messageId = (UUID) rs.getObject("message_id");
                UUID senderId = (UUID) rs.getObject("sender_id");
                UUID recId = (UUID) rs.getObject("receiver_id");
                String type = rs.getString("receiver_type");
                String content = rs.getString("content");
                String messageType = rs.getString("message_type");
                LocalDateTime sendAt = rs.getTimestamp("send_at").toLocalDateTime();

                Message message = new Message(messageId, senderId, recId, type, content, messageType, sendAt);
                messages.add(message);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public static List<Message> getMessages(UUID chatId, UUID userId) {
        String sql = """
        SELECT * FROM messages
        WHERE receiver_id = ?
          AND is_deleted_globally = false
          AND message_id NOT IN (
              SELECT message_id FROM deleted_messages
              WHERE user_id = ?
          )
        ORDER BY send_at
    """;

        List<Message> messages = new ArrayList<>();
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, chatId);
            ps.setObject(2, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = extractMessage(rs);
                messages.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public static boolean insertSavedMessage(Message message) {
        String sql = "INSERT INTO messages (message_id, sender_id, receiver_type, receiver_id, content, message_type, send_at, status, reply_to_id, is_edited, edited_at," +
                " original_message_id, forwarded_by, forwarded_from, is_deleted_globally) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, message.getMessage_id());                        // message_id
            ps.setObject(2, message.getSender_id());                         // sender_id
            ps.setString(3, message.getReceiver_type());                     // receiver_type (private)
            ps.setObject(4, message.getReceiver_id());                       // receiver_id (same as sender_id for saved messages)
            ps.setString(5, message.getContent());                          // content
            ps.setString(6, message.getMessage_type());                      // message_type
            ps.setTimestamp(7, Timestamp.valueOf(message.getSend_at()));     // send_at
            ps.setString(8, message.getStatus());                           // status
            if (message.getReply_to_id() != null)
                ps.setObject(9, message.getReply_to_id());                    // reply_to_id
            else
                ps.setNull(9, Types.OTHER);

            ps.setBoolean(10, message.isIs_edited());                          // is_edited

            if (message.getEdited_at() != null)
                ps.setTimestamp(11, Timestamp.valueOf(message.getEdited_at())); // edited_at
            else
                ps.setNull(11, Types.TIMESTAMP);

            if (message.getOriginal_message_id() != null)
                ps.setObject(12, message.getOriginal_message_id());           // original_message_id
            else
                ps.setNull(12, Types.OTHER);

            if (message.getForwarded_by() != null)
                ps.setObject(13, message.getForwarded_by());                 // forwarded_by
            else
                ps.setNull(13, Types.OTHER);

            if (message.getForwarded_from() != null)
                ps.setObject(14, message.getForwarded_from());               // forwarded_from
            else
                ps.setNull(14, Types.OTHER);

            ps.setBoolean(15, message.getIs_deleted_globally());                 // is_deleted_globally

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static Message getLastMessage(UUID targetId, String type) {
        final String sql =
                "SELECT * FROM messages " +
                        "WHERE receiver_type = ? AND receiver_id = ? " +
                        "ORDER BY send_at DESC LIMIT 1";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, type.toLowerCase()); // "private" | "group" | "channel"
            ps.setObject(2, targetId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

//    public static int getUnreadCount(UUID me, UUID targetId, String type) {
//        final String sql =
//                "SELECT COUNT(*) FROM messages " +
//                        "WHERE receiver_type = ? AND receiver_id = ? " +
//                        "AND sender_id <> ? " +
//                        "AND (status IS NULL OR status <> 'SEEN')";
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//
//            ps.setString(1, type.toLowerCase());
//            ps.setObject(2, targetId);
//            ps.setObject(3, me);
//
//            try (ResultSet rs = ps.executeQuery()) {
//                return rs.next() ? rs.getInt(1) : 0;
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return 0;
//        }
//
//
//    }

    private static Message mapRow(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setMessage_id((UUID) rs.getObject("message_id"));
        m.setSender_id((UUID) rs.getObject("sender_id"));
        m.setReceiver_id((UUID) rs.getObject("receiver_id"));
        m.setReceiver_type(rs.getString("receiver_type"));
        if ("private".equalsIgnoreCase(m.getReceiver_type())) {
            m.setReceiver_id(m.getReceiver_id());
        } else {
            m.setReceiver_id(null);
        }
        m.setMessage_type(rs.getString("message_type"));
        m.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("send_at");
        if (ts != null) m.setSend_at(ts.toLocalDateTime());
        m.setIs_edited(rs.getBoolean("is_edited"));
        m.setStatus(rs.getString("status"));
        Object rtid = rs.getObject("reply_to_id");
        m.setReply_to_id(rtid == null ? null : (UUID) rtid);
        return m;
    }



    public static List<UUID> getUnreadMessageIds(UUID me, UUID chatId, String chatType, int limit) {
        String sql = """
        SELECT m.message_id
        FROM messages m
        WHERE m.receiver_id = ? AND m.receiver_type = ?
          AND m.sender_id <> ?
          AND NOT EXISTS (
              SELECT 1 FROM message_receipts r
              WHERE r.message_id = m.message_id
                AND r.user_id = ?
          )
        ORDER BY m.send_at ASC
        LIMIT ?
    """;
        List<UUID> ids = new ArrayList<>();
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, chatId);
            ps.setString(2, chatType);
            ps.setObject(3, me);
            ps.setObject(4, me);
            ps.setInt(5, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add((UUID) rs.getObject(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    // تعداد ناخوانده‌ها (برای چت‌لیست)
    public static int getUnreadCount(UUID me, UUID chatId, String chatType) {
        String sql = """
        SELECT COUNT(1)
        FROM messages m
        WHERE m.receiver_id = ?
          AND m.receiver_type = ?
          AND m.sender_id <> ?
          AND NOT EXISTS (
              SELECT 1 FROM message_receipts r
              WHERE r.message_id = m.message_id
                AND r.user_id = ?
          )
    """;
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, chatId);
            ps.setString(2, chatType);
            ps.setObject(3, me);
            ps.setObject(4, me);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }


    public static int insertReceiptIfAbsent(UUID messageId, UUID userId) {
        String sql = """
        INSERT INTO message_receipts(message_id, user_id, read_at)
        VALUES (?, ?, now())
        ON CONFLICT (message_id, user_id) DO NOTHING
    """;
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ps.setObject(2, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    //Read status if it's first reader
    public static int setMessageReadIfNeeded(UUID messageId, UUID viewerId) {
        String sql = """
        UPDATE messages m
        SET status = 'READ'
        WHERE m.message_id = ?
          AND m.sender_id <> ?
          AND m.status <> 'READ'
    """;
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ps.setObject(2, viewerId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }







}