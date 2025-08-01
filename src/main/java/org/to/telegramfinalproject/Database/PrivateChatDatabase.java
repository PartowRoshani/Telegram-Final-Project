package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.PrivateChat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

}
