package org.to.telegramfinalproject.Utils;

import org.json.JSONObject;
import org.to.telegramfinalproject.Database.GroupDatabase;

import java.util.UUID;

public class GroupPermissionUtil {

    public static boolean canAddMembers(UUID groupId, UUID userId) {
        String role = GroupDatabase.getGroupRole(groupId, userId);
        if (role.equals("owner")) return true;
        if (role.equals("admin")) {
            JSONObject permissions = GroupDatabase.getGroupPermissions(groupId, userId);
            return permissions.optBoolean("can_add_members", false);
        }
        return false;
    }

    public static boolean canRemoveMembers(UUID groupId, UUID userId) {
        String role = GroupDatabase.getGroupRole(groupId, userId);
        if (role.equals("owner")) return true;
        if (role.equals("admin")) {
            JSONObject permissions = GroupDatabase.getGroupPermissions(groupId, userId);
            return permissions.optBoolean("can_remove_members", false);
        }
        return false;
    }

    public static boolean canAddAdmins(UUID groupId, UUID userId) {
        String role = GroupDatabase.getGroupRole(groupId, userId);
        if (role.equals("owner")) return true;
        if (role.equals("admin")) {
            JSONObject permissions = GroupDatabase.getGroupPermissions(groupId, userId);
            return permissions.optBoolean("can_add_admins", false);
        }
        return false;
    }

    public static boolean canRemoveAdmins(UUID groupId, UUID userId) {
        String role = GroupDatabase.getGroupRole(groupId, userId);
        if (role.equals("owner")) return true;
        if (role.equals("admin")) {
            JSONObject permissions = GroupDatabase.getGroupPermissions(groupId, userId);
            return permissions.optBoolean("can_remove_admins", false);
        }
        return false;
    }

    public static boolean canEditGroup(UUID groupId, UUID userId) {
        String role = GroupDatabase.getGroupRole(groupId, userId);
        if (role.equals("owner")) return true;
        if (role.equals("admin")) {
            JSONObject permissions = GroupDatabase.getGroupPermissions(groupId, userId);
            return permissions.optBoolean("can_edit_group", false);
        }
        return false;
    }
}
