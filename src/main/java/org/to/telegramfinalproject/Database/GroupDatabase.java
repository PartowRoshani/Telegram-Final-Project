package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.Group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class GroupDatabase {

    public static List<Group> getGroupsByUser(UUID internalUuid) {
        List<Group> groups = new ArrayList<>();

        String sql = """
            SELECT g.* FROM groups g
            JOIN group_members gm ON g.internal_uuid = gm.group_id
            WHERE gm.user_id = ?
        """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, internalUuid);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Group group = new Group();

                group.setInternal_uuid(UUID.fromString(rs.getString("internal_uuid")));
                group.setGroup_id(rs.getString("group_id"));
                group.setGroup_name(rs.getString("group_name"));
                group.setCreator_id(UUID.fromString(rs.getString("creator_id")));
                group.setImage_url(rs.getString("image_url"));
                group.setDescription(rs.getString("description"));
                group.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());

                groups.add(group);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return groups;
    }

    public static List<Group> searchGroups(String keyword) {
        List<Group> result = new ArrayList<>();
        String sql = "SELECT * FROM groups WHERE group_name ILIKE ? OR group_id ILIKE ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, keyword);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Group group = new Group(
                        UUID.fromString(rs.getString("internal_uuid")),
                        rs.getString("group_name"),
                        UUID.fromString(rs.getString("creator_id")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                group.setGroup_id(rs.getString("group_id"));
                group.setImage_url(rs.getString("image_url"));
                group.setDescription(rs.getString("description"));
                result.add(group);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<UUID> getMemberUUIDs(UUID groupInternalUUID) {
        List<UUID> memberIds = new ArrayList<>();
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupInternalUUID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    memberIds.add((UUID) rs.getObject("user_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return memberIds;
    }



    public static Group findByGroupId(String groupId) {
        String sql = "SELECT * FROM groups WHERE group_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Group group = new Group();
                group.setInternal_uuid(UUID.fromString(rs.getString("internal_uuid")));
                group.setGroup_id(rs.getString("group_id"));
                group.setGroup_name(rs.getString("group_name"));
                group.setImage_url(rs.getString("image_url"));
                group.setCreator_id(UUID.fromString(rs.getString("creator_id")));
                group.setDescription(rs.getString("description"));
                group.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
                return group;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static boolean isUserInGroup(UUID userId, UUID groupInternalId) {
        String sql = "SELECT * FROM group_members WHERE user_id = ? AND group_id = ?";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            stmt.setObject(2, groupInternalId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static void addMember(UUID groupInternalId, UUID userId) {
        String sql = "INSERT INTO group_members (group_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupInternalId);
            stmt.setObject(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static UUID findInternalUUIDByGroupId(String groupId) {
        String sql = "SELECT internal_uuid FROM groups WHERE group_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("internal_uuid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean addMemberToGroup(UUID userId, UUID groupUUID) {
        String sql = """
        INSERT INTO group_members (group_id, user_id)
        VALUES (?, ?)
        ON CONFLICT DO NOTHING
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupUUID);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}