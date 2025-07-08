package org.to.telegramfinalproject.Database;

import org.json.JSONArray;
import org.json.JSONObject;
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
        String sql = "SELECT user_id FROM channel_subscribers WHERE channel_id = ?";

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
        String sql = "SELECT * FROM channel_subscribers WHERE user_id = ? AND channel_id = ?";
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

    public static void addSubscriber(UUID channelId, UUID userId, String role) {
        String sql = "INSERT INTO channel_subscribers (channel_id, user_id, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);
            stmt.setString(3, role);

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


    public static boolean addOwnerToChannel(UUID channelId, UUID userId) {
        String sql = "INSERT INTO channel_subscribers (channel_id, user_id, role) VALUES (?, ?, 'owner')";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean addAdminToChannel(UUID channelId, UUID userId, JSONObject permissions) {
        String sql = "UPDATE channel_subscribers SET role = 'admin', permissions = ?::jsonb WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, permissions.toString());
            stmt.setObject(2, channelId);
            stmt.setObject(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static String getChannelRole(UUID channelId, UUID userId) {
        String sql = "SELECT role FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "subscriber"; // پیش‌فرض
    }



    public static JSONObject getChannelPermissions(UUID channelId, UUID userId) {
        String sql = "SELECT permissions FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new JSONObject(rs.getString("permissions"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }



    public static boolean updateChannelAdminPermissions(UUID channelId, UUID userId, JSONObject permissions) {
        String sql = "UPDATE channel_subscribers SET permissions = ?::jsonb WHERE channel_id = ? AND user_id = ? AND role = 'admin'";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, permissions.toString());
            stmt.setObject(2, channelId);
            stmt.setObject(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public static List<JSONObject> getChannelAdminsAndOwner(UUID channelId) {
        List<JSONObject> admins = new ArrayList<>();

        String sql = """
        SELECT u.internal_uuid, u.profile_name, u.user_id, cs.role, cs.permissions
        FROM channel_subscribers cs
        JOIN users u ON cs.user_id = u.internal_uuid
        WHERE cs.channel_id = ? AND (cs.role = 'owner' OR cs.role = 'admin')
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("internal_uuid", rs.getObject("internal_uuid").toString());
                    obj.put("profile_name", rs.getString("profile_name"));
                    obj.put("user_id", rs.getString("user_id"));
                    obj.put("role", rs.getString("role"));
                    obj.put("permissions", new JSONObject(rs.getString("permissions")));
                    admins.add(obj);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return admins;
    }


    public static boolean isOwner(UUID channelId, UUID userId) {
        String sql = "SELECT role FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return "owner".equals(rs.getString("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean isAdmin(UUID channelId, UUID userId) {
        String sql = "SELECT role FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return "admin".equals(role) || "owner".equals(role); // owner هم admin هست
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean isUserInChannel(UUID userId, UUID channelId) {
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM channel_subscribers WHERE channel_id = ? AND user_id = ?")) {
            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean removeSubscriberFromChannel(UUID channelId, UUID userId) {
        String sql = "DELETE FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";

        try (Connection conn =ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error removing subscriber from channel: " + e.getMessage());
            return false;
        }
    }
    public static JSONArray getChannelSubscribers(UUID channelId) {
        JSONArray subscribers = new JSONArray();

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT u.internal_uuid, u.user_id, u.profile_name, " +
                             "CASE WHEN cs.role = 'owner' THEN 'owner' " +
                             "     WHEN cs.role = 'admin' THEN 'admin' " +
                             "     ELSE 'subscriber' END AS role " +
                             "FROM channel_subscribers cs " +
                             "JOIN users u ON cs.user_id = u.internal_uuid " +
                             "WHERE cs.channel_id = ?")) {

            stmt.setObject(1, channelId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("internal_uuid", rs.getObject("internal_uuid").toString());
                obj.put("user_id", rs.getString("user_id"));
                obj.put("profile_name", rs.getString("profile_name"));
                obj.put("role", rs.getString("role"));
                subscribers.put(obj);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return subscribers;
    }



    public static boolean updateChannelInfo(UUID channelId, String newId, String name, String description, String imageUrl) {
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE channels SET channel_id = ?, channel_name = ?, description = ?, image_url = ? WHERE internal_uuid = ?")) {

            stmt.setString(1, newId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setString(4, imageUrl);
            stmt.setObject(5, channelId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean isChannelIdUnique(String channelId, UUID excludeChannelUUID) {
        String query = "SELECT COUNT(*) FROM channels WHERE channel_id = ? AND internal_uuid != ?";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setObject(2, excludeChannelUUID);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean demoteAdminToSubscriber(UUID channelId, UUID userId) {
        String sql = "UPDATE channel_subscribers SET role = 'member', permissions = '{}'::jsonb WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, channelId);
            stmt.setObject(2, userId);

            int affected = stmt.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteChannel(UUID channelId) {
        String sql = "DELETE FROM channels WHERE internal_uuid = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, channelId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean transferOwnership(UUID channelId, UUID newOwnerUUID) {
        String updateRoles = """
        UPDATE channel_subscribers
        SET role = CASE
            WHEN user_id = ? THEN 'owner'
            WHEN role = 'owner' THEN 'admin'
            ELSE role
        END
        WHERE channel_id = ?
    """;

        String clearPermissions = """
        UPDATE channel_subscribers
        SET permissions = '{}'::jsonb
        WHERE channel_id = ? AND user_id = ?
    """;

        try (Connection conn = ConnectionDb.connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement roleStmt = conn.prepareStatement(updateRoles);
                 PreparedStatement clearPermsStmt = conn.prepareStatement(clearPermissions)) {

                roleStmt.setObject(1, newOwnerUUID);
                roleStmt.setObject(2, channelId);
                roleStmt.executeUpdate();

                clearPermsStmt.setObject(1, channelId);
                clearPermsStmt.setObject(2, newOwnerUUID);
                clearPermsStmt.executeUpdate();

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    public static List<UUID> getChannelSubscriberUUIDs(UUID channelId) {
        List<UUID> subscriberIds = new ArrayList<>();
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM channel_subscribers WHERE channel_id = ?")) {
            stmt.setObject(1, channelId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                subscriberIds.add(UUID.fromString(rs.getString("user_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subscriberIds;
    }

}
