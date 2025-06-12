package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.Channel;
import org.to.telegramfinalproject.Models.Group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
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


    public static boolean createChannel(Channel channel, UUID creatorId) {
        String sql = """
            INSERT INTO channels (
                internal_uuid, channel_id, channel_name,
                creator_id, image_url, description, created_at
            )
            VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?)
            RETURNING internal_uuid
        """;

        String subscriberSql = """
            INSERT INTO channel_subscribers (channel_id, user_id) VALUES (?, ?)
        """;

        try (Connection conn = ConnectionDb.connect()) {
            // مرحله اول: ساخت کانال
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, channel.getChannel_id());
            stmt.setString(2, channel.getChannel_name());
            stmt.setObject(3, creatorId);
            stmt.setString(4, channel.getImage_url());
            stmt.setString(5, channel.getDescription());
            stmt.setObject(6, channel.getCreated_at());

            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return false;

            UUID internalUUID = (UUID) rs.getObject("internal_uuid");
            channel.setInternal_uuid(internalUUID); // اختیاری برای پیگیری بعدی

            // مرحله دوم: افزودن کاربر به لیست سابسکرایبرها
            PreparedStatement subStmt = conn.prepareStatement(subscriberSql);
            subStmt.setObject(1, internalUUID);
            subStmt.setObject(2, creatorId);
            subStmt.executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean insertChannel(UUID internalUUID, String channelId, String channelName, UUID creatorId, String imageUrl, LocalDateTime createdAt) {
        String sql = "INSERT INTO channels (internal_uuid, channel_id, channel_name, creator_id, image_url, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, internalUUID);
            stmt.setString(2, channelId);
            stmt.setString(3, channelName);
            stmt.setObject(4, creatorId);
            stmt.setString(5, imageUrl);
            stmt.setObject(6, createdAt);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void addSubscriber(UUID channelId, UUID userId) {
        String sql = "INSERT INTO channel_subscribers (channel_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static Channel findByInternalUUID(UUID internalUUID) {
        String sql = "SELECT * FROM channels WHERE internal_uuid = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, internalUUID);
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


}
