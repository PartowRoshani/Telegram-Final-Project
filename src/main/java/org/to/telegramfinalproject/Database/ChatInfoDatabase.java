package org.to.telegramfinalproject.Database;

import org.json.JSONObject;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ChatInfoDatabase {

    /**
     * هدر چت را بر اساس نوع آن برمی‌گرداند.
     * private: name,image_url, online,last_seen
     * group : name,image_url, member_count
     * channel: name,image_url, member_count
     */
    public static JSONObject getHeaderInfo(String type, UUID receiverId, UUID viewerId) throws SQLException {
        try (Connection conn = ConnectionDb.connect()) {
            switch (type.toLowerCase()) {
                case "private":
                    return getPrivateHeader(conn, receiverId, viewerId);
                case "group":
                    return getGroupHeader(conn, receiverId);
                case "channel":
                    return getChannelHeader(conn, receiverId);
                default:
                    throw new IllegalArgumentException("Unsupported receiver_type: " + type);
            }
        }
    }

    /** هدر چت خصوصی: other-user نسبت به viewerId را پیدا می‌کنیم */
    private static JSONObject getPrivateHeader(Connection conn, UUID chatId, UUID viewerId) throws SQLException {
        if (viewerId == null) throw new IllegalArgumentException("viewerId is required for private header.");

        final String sql = """
        SELECT
            u.profile_name            AS name,
            COALESCE(u.image_url, '') AS image_url,
            (u.status = 'online')     AS online,
            u.status                  AS status,
            u.last_seen               AS last_seen
        FROM private_chat pc
        JOIN users u
          ON u.internal_uuid = CASE WHEN pc.user1_id = ? THEN pc.user2_id ELSE pc.user1_id END
        WHERE pc.chat_id = ?
          AND (pc.user1_id = ? OR pc.user2_id = ?)
        LIMIT 1
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setObject(i++, viewerId);
            ps.setObject(i++, chatId);
            ps.setObject(i++, viewerId);
            ps.setObject(i++, viewerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Private chat not found or viewer is not a participant.");

                JSONObject j = new JSONObject();
                j.put("type", "private");
                j.put("name", rs.getString("name"));
                j.put("image_url", rs.getString("image_url"));

                // خواندن بولینِ online با سازگاری DB
                Object onlineObj = rs.getObject("online");
                boolean online = (onlineObj instanceof Boolean) ? (Boolean) onlineObj
                        : rs.getInt("online") == 1;
                j.put("online", online);
                j.put("status", rs.getString("status")); // رشته‌ی وضعیت هم می‌آید

                Timestamp ts = rs.getTimestamp("last_seen");
                j.put("last_seen", ts == null ? JSONObject.NULL : ts.toInstant().toString());
                return j;
            }
        }
    }


    /** هدر گروه: نام، عکس و تعداد اعضا */
    private static JSONObject getGroupHeader(Connection conn, UUID groupInternalId) throws SQLException {
        String sql = """
            SELECT
                g.group_name                        AS name,
                COALESCE(g.image_url, '')           AS image_url,
                (SELECT COUNT(*) FROM group_members gm WHERE gm.group_id = g.internal_uuid) AS member_count
            FROM groups g
            WHERE g.internal_uuid = ?
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, groupInternalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Group not found.");
                JSONObject j = new JSONObject();
                j.put("type", "group");
                j.put("name", rs.getString("name"));
                j.put("image_url", rs.getString("image_url"));
                j.put("member_count", rs.getInt("member_count"));
                return j;
            }
        }
    }

    /** هدر کانال: نام، عکس و تعداد سابسکرایبر */
    private static JSONObject getChannelHeader(Connection conn, UUID channelInternalId) throws SQLException {
        String sql = """
            SELECT
                c.channel_name                      AS name,
                COALESCE(c.image_url, '')           AS image_url,
                (SELECT COUNT(*) FROM channel_subscribers cs WHERE cs.channel_id = c.internal_uuid) AS member_count
            FROM channels c
            WHERE c.internal_uuid = ?
            LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, channelInternalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Channel not found.");
                JSONObject j = new JSONObject();
                j.put("type", "channel");
                j.put("name", rs.getString("name"));
                j.put("image_url", rs.getString("image_url"));
                j.put("member_count", rs.getInt("member_count"));
                return j;
            }
        }
    }
}
