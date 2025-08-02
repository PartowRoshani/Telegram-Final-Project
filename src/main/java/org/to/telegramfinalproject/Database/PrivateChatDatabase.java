package org.to.telegramfinalproject.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PrivateChatDatabase {

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
}