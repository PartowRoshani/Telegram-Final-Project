package org.to.telegramfinalproject.Database;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.MediaRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessageReactionDatabase {

    public static boolean saveOrUpdateReaction(UUID messageId, UUID userId, String reaction) {
        String sql = """
                    INSERT INTO message_reactions (message_id, user_id, emoji)
                    VALUES (?, ?, ?)
                    ON CONFLICT (message_id, user_id)
                    DO UPDATE SET emoji = EXCLUDED.emoji, reacted_at = CURRENT_TIMESTAMP
                    """;
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ps.setObject(2, userId);
            ps.setString(3, reaction);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> getReactions(UUID messageId) {
        String sql = "SELECT emoji FROM message_reactions WHERE message_id = ?";
        List<String> reactions = new ArrayList<>();
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, messageId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reactions.add(rs.getString("emoji"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reactions;
    }
    public static JSONObject getCountsAsJson(UUID messageId) {
        String sql = """
        SELECT emoji, COUNT(*) AS c
        FROM message_reactions
        WHERE message_id = ?
        GROUP BY emoji
    """;

        JSONObject counts = new JSONObject();

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String emoji = rs.getString("emoji");
                    int c = rs.getInt("c");
                    counts.put(emoji, c);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts; // مثال: {"❤️":2,"👍":1}
    }





}
