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



    public static List<UUID> getSubscriberUUIDs(UUID channelInternalUUID) {
        List<UUID> subscriberIds = new ArrayList<>();
        String sql = "SELECT user_id FROM channel_subscribe WHERE channel_id = ?";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelInternalUUID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subscriberIds.add((UUID) rs.getObject("user_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return subscriberIds;
    }

    public static Channel findByChannelId(String channelId) {
        String sql = "SELECT * FROM channels WHERE channel_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Channel channel = new Channel();
                channel.setInternal_uuid(UUID.fromString(rs.getString("internal_uuid")));
                channel.setChannel_id(rs.getString("channel_id"));
                channel.setChannel_name(rs.getString("channel_name"));
                channel.setImage_url(rs.getString("image_url"));
                channel.setCreator_id(UUID.fromString(rs.getString("creator_id")));
                channel.setDescription(rs.getString("description"));
                channel.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
                return channel;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }




    public static boolean isUserSubscribed(UUID userId, UUID channelInternalId) {
        String sql = "SELECT * FROM channel_subscribe WHERE user_id = ? AND channel_id = ?";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            stmt.setObject(2, channelInternalId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addSubscriber(UUID channelInternalId, UUID userId) {
        String sql = "INSERT INTO channel_subscribe (channel_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelInternalId);
            stmt.setObject(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static UUID findInternalUUIDByChannelId(String channelId) {
        String sql = "SELECT internal_uuid FROM channels WHERE channel_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("internal_uuid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean addSubscriberToChannel(UUID userId, UUID channelUUID) {
        String sql = """
        INSERT INTO channel_subscribers (channel_id, user_id)
        VALUES (?, ?)
        ON CONFLICT DO NOTHING
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelUUID);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}
