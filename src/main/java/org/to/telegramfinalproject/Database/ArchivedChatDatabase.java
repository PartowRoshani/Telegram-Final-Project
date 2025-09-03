package org.to.telegramfinalproject.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArchivedChatDatabase {
    public static boolean archiveChat(UUID userId, UUID chatId, String chatType) {
        String sql = "INSERT INTO archived_chats (user_id, chat_id, chat_type) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setObject(2, chatId);
            ps.setString(3, chatType);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean unarchiveChat(UUID userId, UUID chatId) {
        String sql = "DELETE FROM archived_chats WHERE user_id = ? AND chat_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setObject(2, chatId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isChatArchived(UUID userId, UUID chatId) {
        String sql = "SELECT 1 FROM archived_chats WHERE user_id = ? AND chat_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setObject(2, chatId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<UUID> getArchivedChats(UUID userId) {
        List<UUID> list = new ArrayList<>();
        String sql = "SELECT chat_id FROM archived_chats WHERE user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(UUID.fromString(rs.getString("chat_id")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }



}
