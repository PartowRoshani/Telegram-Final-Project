package org.to.telegramfinalproject.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PrivateChatDatabase {


    public static List<UUID> getMembers(UUID privateChatId) {
        String sql = "SELECT user1, user2 FROM private_chats WHERE chat_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, privateChatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UUID user1 = (UUID) rs.getObject("user1");
                UUID user2 = (UUID) rs.getObject("user2");
                return Arrays.asList(user1, user2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}
