package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MessageDatabase {


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





    public static List<Message> getUnreadMessages(UUID userId) {
        List<Message> messages = new ArrayList<>();

        String sql = """
        SELECT m.* FROM messages m
        LEFT JOIN message_receipts r ON m.message_id = r.message_id AND r.user_id = ?
        WHERE r.user_id IS NULL
        AND (
            (m.receiver_type = 'private' AND m.receiver_id = ?)
            OR
            (m.receiver_type = 'group' AND EXISTS (
                SELECT 1 FROM group_members gm WHERE gm.group_id = m.receiver_id AND gm.user_id = ?
            ))
            OR
            (m.receiver_type = 'channel' AND EXISTS (
                SELECT 1 FROM channel_subscribers cs WHERE cs.channel_id = m.receiver_id AND cs.user_id = ?
            ))
        )
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            stmt.setObject(2, userId);
            stmt.setObject(3, userId);
            stmt.setObject(4, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        UUID.fromString(rs.getString("message_id")),
                        rs.getObject("sender_id") != null ? UUID.fromString(rs.getString("sender_id")) : null,
                        rs.getString("receiver_type"),
                        UUID.fromString(rs.getString("receiver_id")),
                        rs.getString("content"),
                        rs.getString("message_type"),
                        rs.getString("file_url"),
                        rs.getTimestamp("send_at").toLocalDateTime(),
                        rs.getString("status"),
                        rs.getObject("reply_to_id") != null ? UUID.fromString(rs.getString("reply_to_id")) : null,
                        rs.getBoolean("is_edited"),
                        rs.getObject("original_message_id") != null ? UUID.fromString(rs.getString("original_message_id")) : null,
                        rs.getObject("forwarded_by") != null ? UUID.fromString(rs.getString("forwarded_by")) : null,
                        rs.getObject("forwarded_from") != null ? UUID.fromString(rs.getString("forwarded_from")) : null
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
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
                rs.getString("file_url"),
                rs.getTimestamp("send_at").toLocalDateTime(),
                rs.getString("status"),
                (UUID) rs.getObject("reply_to_id"),
                rs.getBoolean("is_edited"),
                (UUID) rs.getObject("original_message_id"),
                (UUID) rs.getObject("forwarded_by"),
                (UUID) rs.getObject("forwarded_from")
        );
    }



    public static List<Message> searchMessagesForUser(UUID userId, String keyword) {
        List<Message> result = new ArrayList<>();
        String sql = """
        SELECT * FROM messages
        WHERE receiver_type = 'private'
          AND (sender_id = ? OR receiver_id = ?)
          AND content ILIKE ?
        ORDER BY send_at DESC
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            stmt.setObject(2, userId);
            stmt.setString(3, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }


    public static List<Message> searchMessagesInGroups(List<UUID> groupIds, String keyword) {
        List<Message> result = new ArrayList<>();
        if (groupIds.isEmpty()) return result;

        String placeholders = groupIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
        SELECT * FROM messages
        WHERE receiver_type = 'group'
        AND receiver_id IN (%s)
        AND content ILIKE ?
        ORDER BY send_at DESC
    """.formatted(placeholders);

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (UUID id : groupIds) {
                stmt.setObject(i++, id);
            }
            stmt.setString(i, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }


    public static List<Message> searchMessagesInChannels(List<UUID> channelIds, String keyword) {
        List<Message> result = new ArrayList<>();
        if (channelIds.isEmpty()) return result;

        String placeholders = channelIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
        SELECT * FROM messages
        WHERE receiver_type = 'channel'
        AND receiver_id IN (%s)
        AND content ILIKE ?
        ORDER BY send_at DESC
    """.formatted(placeholders);

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;
            for (UUID id : channelIds) {
                stmt.setObject(i++, id);
            }
            stmt.setString(i, "%" + keyword + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }


}
