package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.PrivateChat;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PrivateChatDatabase {


    public static List<UUID> getMembers(UUID privateChatId) {
        String sql = "SELECT user1_id, user2_id FROM private_chat WHERE chat_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, privateChatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID user1 = (UUID) rs.getObject("user1_id");
                UUID user2 = (UUID) rs.getObject("user2_id");
                return new ArrayList<>(Arrays.asList(user1, user2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static UUID getOrCreateSavedMessagesChat(UUID userId) {
        String query = "SELECT chat_id FROM private_chat WHERE user1_id = ? AND user2_id = ?";
        try (Connection conn = ConnectionDb.connect()) {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setObject(1, userId);
            stmt.setObject(2, userId); // Saved Messages is self-chat

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("chat_id"); // Chat already exists
            } else {
                // Create a new chat
                UUID chatId = UUID.randomUUID();
                String insert = "INSERT INTO private_chat (chat_id, user1_id, user2_id) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                    insertStmt.setObject(1, chatId);
                    insertStmt.setObject(2, userId);
                    insertStmt.setObject(3, userId);
                    insertStmt.executeUpdate();
                    return chatId;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static UUID findChatIdByUsers(UUID user1, UUID user2) {
        UUID u1 = user1.compareTo(user2) < 0 ? user1 : user2;
        UUID u2 = user1.compareTo(user2) < 0 ? user2 : user1;

        String sql = "SELECT chat_id FROM private_chat WHERE user1_id = ? AND user2_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, u1);
            ps.setObject(2, u2);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("chat_id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static UUID getOrCreateChat(UUID user1, UUID user2) {
        UUID u1 = user1.compareTo(user2) < 0 ? user1 : user2;
        UUID u2 = user1.compareTo(user2) < 0 ? user2 : user1;

        String select = "SELECT chat_id FROM private_chat WHERE user1_id = ? AND user2_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setObject(1, u1);
            ps.setObject(2, u2);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("chat_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        UUID newChatId = UUID.randomUUID();
        String insert = "INSERT INTO private_chat(chat_id, user1_id, user2_id) VALUES (?, ?, ?)";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setObject(1, newChatId);
            ps.setObject(2, u1);
            ps.setObject(3, u2);
            ps.executeUpdate();
            return newChatId;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static List<PrivateChat> findChatsOfUser(UUID userId) {
        List<PrivateChat> chats = new ArrayList<>();
        String sql = "SELECT * FROM private_chat WHERE user1_id = ? OR user2_id = ?";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setObject(2, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID chatId = (UUID) rs.getObject("chat_id");
                UUID user1 = (UUID) rs.getObject("user1_id");
                UUID user2 = (UUID) rs.getObject("user2_id");

                chats.add(new PrivateChat(chatId, user1, user2));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return chats;
    }


    public static UUID getOtherUserInChat(UUID chatId, UUID currentUserId) {
        String sql = "SELECT user1_id, user2_id FROM private_chat WHERE chat_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, chatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID user1 = (UUID) rs.getObject("user1_id");
                UUID user2 = (UUID) rs.getObject("user2_id");
                return currentUserId.equals(user1) ? user2 : user1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static PrivateChat extractPrivateChat(ResultSet rs) throws SQLException {
        UUID chatId = UUID.fromString(rs.getString("chat_id"));
        UUID user1 = rs.getObject("user1_id", UUID.class);
        UUID user2 = rs.getObject("user2_id", UUID.class);
        boolean user1Deleted = rs.getBoolean("user1_deleted");
        boolean user2Deleted = rs.getBoolean("user2_deleted");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new PrivateChat(chatId, user1, user2, user1Deleted, user2Deleted, createdAt);
    }

    public static void markChatDeleted(UUID userId, UUID chatId) {
        String sql = """
        UPDATE private_chat 
        SET user1_deleted = CASE WHEN user1_id = ? THEN true ELSE user1_deleted END,
            user2_deleted = CASE WHEN user2_id = ? THEN true ELSE user2_deleted END
        WHERE chat_id = ?
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            stmt.setObject(2, userId);
            stmt.setObject(3, chatId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void unmarkChatDeleted(UUID userId, UUID chatId) {
        String sql = """
        UPDATE private_chat 
        SET user1_deleted = CASE WHEN user1_id = ? THEN false ELSE user1_deleted END,
            user2_deleted = CASE WHEN user2_id = ? THEN false ELSE user2_deleted END
        WHERE chat_id = ?
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            stmt.setObject(2, userId);
            stmt.setObject(3, chatId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void markUser1Deleted(UUID chatId) {
        updateBoolField(chatId, "user1_deleted", true);
    }

    public static void markUser2Deleted(UUID chatId) {
        updateBoolField(chatId, "user2_deleted", true);
    }

    public static void markBothDeleted(UUID chatId) {
        String sql = "UPDATE private_chat SET user1_deleted = true, user2_deleted = true WHERE chat_id = ?";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, chatId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void updateBoolField(UUID chatId, String field, boolean value) {
        String sql = "UPDATE private_chat SET " + field + " = ? WHERE chat_id = ?";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, value);
            ps.setObject(2, chatId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static PrivateChat findById(UUID chatId) {
        String sql = "SELECT * FROM private_chat WHERE chat_id = ?";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, chatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID user1_id = (UUID) rs.getObject("user1_id");
                UUID user2_id = (UUID) rs.getObject("user2_id");
                boolean user1_deleted = rs.getBoolean("user1_deleted");
                boolean user2_deleted = rs.getBoolean("user2_deleted");
                LocalDateTime created_at = rs.getTimestamp("created_at").toLocalDateTime();

                return new PrivateChat(chatId, user1_id, user2_id, user1_deleted, user2_deleted, created_at);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void clearDeletedFlag(UUID senderId, UUID chatId) {
        String sql = """
        UPDATE private_chat
        SET user1_deleted = CASE WHEN user1_id = ? THEN FALSE ELSE user1_deleted END,
            user2_deleted = CASE WHEN user2_id = ? THEN FALSE ELSE user2_deleted END
        WHERE chat_id = ?
    """;

        try (Connection conn = ConnectionDb.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, senderId);
            ps.setObject(2, senderId);
            ps.setObject(3, chatId);
            int rows = ps.executeUpdate();
            System.out.println("✅ clearDeletedFlag updated rows = " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static UUID findChatBetween(UUID user1, UUID user2) {
        String sql = """
        SELECT chat_id FROM private_chat
        WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, user1);
            ps.setObject(2, user2);
            ps.setObject(3, user2);
            ps.setObject(4, user1);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("chat_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static UUID getOtherParticipant(UUID chatId, UUID me) {
        List<UUID> members = getMembers(chatId);
        for (UUID u : members) {
            if (!u.equals(me)) return u;
        }
        return null;
    }

}
