package org.to.telegramfinalproject.Utils;

import org.json.JSONObject;
import org.to.telegramfinalproject.Database.ChannelDatabase;

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
}
