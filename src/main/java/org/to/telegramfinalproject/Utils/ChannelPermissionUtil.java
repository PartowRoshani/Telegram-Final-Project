package org.to.telegramfinalproject.Utils;

import org.json.JSONObject;
import org.to.telegramfinalproject.Database.ChannelDatabase;
import org.to.telegramfinalproject.Database.ConnectionDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChannelPermissionUtil {

    public static boolean canAddSubscribers(UUID channelId, UUID userId) {
        if (ChannelDatabase.isOwner(channelId, userId)) return true;
        if (ChannelDatabase.isAdmin(channelId, userId)) {
            JSONObject perms = ChannelDatabase.getChannelPermissions(channelId, userId);
            return perms.optBoolean("can_add_members", false);
        }
        return false;
    }

    public static boolean canAddAdmins(UUID channelId, UUID userId) {
        if (ChannelDatabase.isOwner(channelId, userId)) return true;
        if (ChannelDatabase.isAdmin(channelId, userId)) {
            JSONObject perms = ChannelDatabase.getChannelPermissions(channelId, userId);
            return perms.optBoolean("can_add_admins", false);
        }
        return false;
    }

    public static boolean canEditChannel(UUID channelId, UUID userId) {
        if (ChannelDatabase.isOwner(channelId, userId)) return true;
        if (ChannelDatabase.isAdmin(channelId, userId)) {
            JSONObject perms = ChannelDatabase.getChannelPermissions(channelId, userId);
            return perms.optBoolean("can_edit_channel", false);
        }
        return false;
    }

    public static boolean canRemoveAdmins(UUID channelId, UUID userId) {
        if (ChannelDatabase.isOwner(channelId, userId)) return true;
        if (ChannelDatabase.isAdmin(channelId, userId)) {
            JSONObject perms = ChannelDatabase.getChannelPermissions(channelId, userId);
            return perms.optBoolean("can_remove_admins", false);
        }
        return false;
    }

    public static boolean canRemoveSubscribers(UUID channelId, UUID userId) {
        if (ChannelDatabase.isOwner(channelId, userId)) return true;
        if (ChannelDatabase.isAdmin(channelId, userId)) {
            JSONObject perms = ChannelDatabase.getChannelPermissions(channelId, userId);
            return perms.optBoolean("can_remove_members", false);
        }
        return false;
    }

    public static boolean canDeleteMessage(UUID channelId, UUID userId) {
        String roleSql = "SELECT role FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(roleSql)) {
            ps.setObject(1, channelId);
            ps.setObject(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return role.equalsIgnoreCase("owner") || role.equalsIgnoreCase("admin");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }



    public static boolean isUserInChannel(UUID userId, UUID channelId) {
        final String SQL = "SELECT 1 FROM channel_subscribers WHERE channel_id = ? AND user_id = ? LIMIT 1";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setObject(1, channelId, java.sql.Types.OTHER); // 👈 مهم برای Postgres UUID
            ps.setObject(2, userId,    java.sql.Types.OTHER);

            System.out.println("[SQL] isUserInChannel ch=" + channelId + " user=" + userId
                    + " db=" + conn.getMetaData().getURL());

            try (ResultSet rs = ps.executeQuery()) {
                boolean ok = rs.next();
                System.out.println("[SQL] isUserInChannel -> " + ok);
                return ok;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
