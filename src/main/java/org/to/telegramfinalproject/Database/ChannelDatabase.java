package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.Channel;
import org.to.telegramfinalproject.Models.Group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChannelDatabase {
    public static List<Channel> getChannelsByUser(UUID internalUuid) {
        List<Channel> channels = new ArrayList<>();

        String sql = """
            SELECT c.* FROM channels c
            JOIN channel_subscribers cs ON c.internal_uuid = cs.channel_id
            WHERE cs.user_id = ?
        """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, internalUuid);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Channel channel = new Channel();

                channel.setInternal_uuid(UUID.fromString(rs.getString("internal_uuid")));
                channel.setChannel_id(rs.getString("channel_id"));
                channel.setChannel_name(rs.getString("channel_name"));
                channel.setCreator_id(UUID.fromString(rs.getString("creator_id")));
                channel.setImage_url(rs.getString("image_url"));
                channel.setDescription(rs.getString("description"));
                channel.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());

                channels.add(channel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return channels;
    }


    public static List<Channel> searchChannels(String keyword) {
        List<Channel> result = new ArrayList<>();
        String sql = "SELECT * FROM channels WHERE channel_name ILIKE ? OR channel_id ILIKE ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, keyword);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Channel channel = new Channel(
                        UUID.fromString(rs.getString("internal_uuid")),
                        rs.getString("channel_name"),
                        UUID.fromString(rs.getString("creator_id")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                channel.setChannel_id(rs.getString("channel_id"));
                channel.setImage_url(rs.getString("image_url"));
                channel.setDescription(rs.getString("description"));
                result.add(channel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
