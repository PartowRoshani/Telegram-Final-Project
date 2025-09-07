package org.to.telegramfinalproject.Database;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.Group;
import org.to.telegramfinalproject.Models.User;

import java.sql.*;
import java.time.LocalDateTime;
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


    public static boolean updateGroupInfo(UUID internalUUID, String newGroupId, String name, String description, String imageUrl) {
        String sql = "UPDATE groups SET group_id = ?, group_name = ?, description = ?, image_url = ? WHERE internal_uuid = ?";

        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newGroupId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            if (imageUrl == null) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, imageUrl);
            }
            stmt.setObject(5, internalUUID);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean isGroupIdUnique(String groupId, UUID excludeUUID) {
        String sql = "SELECT COUNT(*) FROM groups WHERE group_id = ? AND internal_uuid != ?";

        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, groupId);
            stmt.setObject(2, excludeUUID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
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
    public static boolean createGroup(Group group, UUID creatorId) {
        String sql = """
            INSERT INTO groups (
                internal_uuid, group_id, group_name,
                creator_id, image_url, description, created_at
            )
            VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?)
            RETURNING internal_uuid
        """;

        String memberSql = """
            INSERT INTO group_members (group_id, user_id) VALUES (?, ?)
        """;

        try (Connection conn = ConnectionDb.connect()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, group.getGroup_id());
            stmt.setString(2, group.getGroup_name());
            stmt.setObject(3, creatorId);
            stmt.setString(4, group.getImage_url());
            stmt.setString(5, group.getDescription());
            stmt.setObject(6, group.getCreated_at());

            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return false;

            UUID internalUUID = (UUID) rs.getObject("internal_uuid");
            group.setInternal_uuid(internalUUID);

            PreparedStatement memberStmt = conn.prepareStatement(memberSql);
            memberStmt.setObject(1, internalUUID);
            memberStmt.setObject(2, creatorId);
            memberStmt.executeUpdate();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean insertGroup(UUID internalUUID, String groupId, String groupName, UUID creatorId, String imageUrl, LocalDateTime createdAt) {
        String sql = "INSERT INTO groups (internal_uuid, group_id, group_name, creator_id, image_url, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, internalUUID);
            stmt.setString(2, groupId);
            stmt.setString(3, groupName);
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

    public static void addMember(UUID groupId, UUID userId, String role) {
        String sql = "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static Group findByInternalUUID(UUID internalUUID) {
        String sql = "SELECT * FROM groups WHERE internal_uuid = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, internalUUID);
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


    public static boolean addOwnerToGroup(UUID groupId, UUID userId) {
        String sql = "INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'owner')";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean addAdminToGroup(UUID groupId, UUID userId, JSONObject permissions) {
        String sql = """
        UPDATE group_members
        SET role = 'admin',
            permissions = ?::jsonb
        WHERE group_id = ? AND user_id = ? AND role = 'member'
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, permissions.toString());
            stmt.setObject(2, groupId);
            stmt.setObject(3, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public static String getGroupRole(UUID groupId, UUID userId) {
        String sql = "SELECT role FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "member";
    }





    public static boolean updateGroupAdminPermissions(UUID groupId, UUID userId, JSONObject permissions) {
        String sql = """
        UPDATE group_members
        SET permissions = ?::jsonb
        WHERE group_id = ? AND user_id = ? AND role = 'admin'
    """;
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, permissions.toString());
            stmt.setObject(2, groupId);
            stmt.setObject(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static List<JSONObject> getGroupAdminsAndOwner(UUID groupId) {
        List<JSONObject> admins = new ArrayList<>();

        String sql = "SELECT gm.user_id, gm.role, gm.permissions, u.profile_name " +
                "FROM group_members gm " +
                "JOIN users u ON gm.user_id = u.internal_uuid " +
                "WHERE gm.group_id = ? AND gm.role IN ('owner', 'admin')";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("user_id", rs.getObject("user_id").toString());
                obj.put("role", rs.getString("role"));
                obj.put("permissions", new JSONObject(rs.getString("permissions")));
                obj.put("profile_name", rs.getString("profile_name"));
                admins.add(obj);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return admins;
    }


    public static boolean isOwner(UUID groupId, UUID userId) {
        String sql = "SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ? AND role = 'owner'";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isAdmin(UUID groupId, UUID userId) {
        String sql = "SELECT role FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return "admin".equals(role) || "owner".equals(role);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static JSONObject getGroupPermissions(UUID groupId, UUID userId) {
        String sql = "SELECT permissions FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String permissions = rs.getString("permissions");
                if (permissions != null && !permissions.isBlank()) {
                    return new JSONObject(permissions);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }



    public static JSONArray getGroupMembers(UUID groupId) {
        String sql = """
        SELECT u.profile_name, u.user_id, u.internal_uuid, gm.role, gm.permissions
        FROM group_members gm
        JOIN users u ON gm.user_id = u.internal_uuid
        WHERE gm.group_id = ?
    """;

        JSONArray members = new JSONArray();

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, groupId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject member = new JSONObject();
                member.put("profile_name", rs.getString("profile_name"));
                member.put("user_id", rs.getString("user_id"));
                member.put("internal_uuid", rs.getObject("internal_uuid").toString());
                member.put("role", rs.getString("role"));

                String permissions = rs.getString("permissions");
                if (permissions != null && !permissions.isBlank()) {
                    member.put("permissions", new JSONObject(permissions));
                }

                members.put(member);
            }

            return members;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static boolean demoteAdminToMember(UUID groupId, UUID userId) {
        String sql = "UPDATE group_members SET role = 'member', permissions = '{}'::jsonb WHERE group_id = ? AND user_id = ? AND role = 'admin'";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeMemberFromGroup(UUID groupId, UUID userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean transferOwnership(UUID groupId, UUID newOwnerId) {
        String demoteOldOwner = "UPDATE group_members SET role = 'admin' WHERE group_id = ? AND role = 'owner'";
        String promoteNewOwner = "UPDATE group_members SET role = 'owner', permissions = '{}'::jsonb WHERE group_id = ? AND user_id = ?";

        try (Connection conn = ConnectionDb.connect()) {
            conn.setAutoCommit(false);

            try (PreparedStatement demoteStmt = conn.prepareStatement(demoteOldOwner);
                 PreparedStatement promoteStmt = conn.prepareStatement(promoteNewOwner)) {

                demoteStmt.setObject(1, groupId);
                demoteStmt.executeUpdate();

                promoteStmt.setObject(1, groupId);
                promoteStmt.setObject(2, newOwnerId);
                promoteStmt.executeUpdate();

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


    public static boolean deleteGroup(UUID groupId) {
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM groups WHERE internal_uuid = ?")) {

            stmt.setObject(1, groupId);
            int affectedRows = stmt.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static List<UUID> getGroupMemberUUIDs(UUID groupId) {
        List<UUID> memberIds = new ArrayList<>();
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM group_members WHERE group_id = ?")) {
            stmt.setObject(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                memberIds.add(UUID.fromString(rs.getString("user_id")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return memberIds;
    }

    public static boolean updateAdminPermissions(UUID groupId, UUID userId, JSONObject permissions) {
        String sql = "UPDATE group_members SET permissions = ?::jsonb WHERE group_id = ? AND user_id = ? AND role = 'admin'";
        try (Connection conn = ConnectionDb.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, permissions.toString());
            stmt.setObject(2, groupId);
            stmt.setObject(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isMember(UUID groupId, UUID userId) {
        String sql = "SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, groupId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

//    public static List<User> searchGroupMembers(UUID groupId, String keyword) {
//        List<User> result = new ArrayList<>();
//        String sql = """
//        SELECT u.*
//        FROM users u
//        JOIN group_members gm ON gm.user_id = u.internal_uuid
//        WHERE gm.group_id = ?
//          AND (
//                LOWER(u.profile_name) LIKE ?
//                OR LOWER(u.username) LIKE ?
//                OR LOWER(u.user_id) LIKE ?
//              )
//    """;
//
//        try (Connection conn = ConnectionDb.connect();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//
//            stmt.setObject(1, groupId);
//            String likePattern = "%" + keyword.toLowerCase() + "%";
//            stmt.setString(2, likePattern);
//            stmt.setString(3, likePattern);
//            stmt.setString(4, likePattern);
//
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                User user = User.fromResultSet(rs);
//                result.add(user);
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return result;
//    }

    public static JSONObject getGroupInfo(UUID groupId, UUID viewerUuid) throws SQLException {
        JSONObject result = new JSONObject();

        try (Connection conn = ConnectionDb.connect()) {
            // === Group header (name, member count, etc.)
            String groupQuery = """
            SELECT g.internal_uuid, g.group_id, g.group_name, g.image_url,
                   COUNT(m.user_id) as member_count
            FROM groups g
            LEFT JOIN group_members m ON g.internal_uuid = m.group_id
            WHERE g.internal_uuid = ?
            GROUP BY g.internal_uuid, g.group_id, g.group_name, g.image_url
        """;

            try (PreparedStatement ps = conn.prepareStatement(groupQuery)) {
                ps.setObject(1, groupId); // ✅ compare UUID with UUID
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    result.put("internal_uuid", rs.getString("internal_uuid"));
                    result.put("group_id", rs.getString("group_id")); // varchar handle
                    result.put("group_name", rs.getString("group_name"));
                    result.put("member_count", rs.getInt("member_count"));
                    result.put("image_url", rs.getString("image_url"));
                } else {
                    return null; // no such group
                }
            }

            // === Members (id, name, role, status, image)
            JSONArray membersArr = new JSONArray();

            String membersQuery = """
            SELECT u.internal_uuid, u.profile_name, u.user_id, u.image_url,
                   gm.role, u.status, u.last_seen
            FROM group_members gm
            JOIN users u ON gm.user_id = u.internal_uuid
            WHERE gm.group_id = ?
        """;

            try (PreparedStatement ps = conn.prepareStatement(membersQuery)) {
                ps.setObject(1, groupId); // ✅ gm.group_id is UUID
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    JSONObject member = new JSONObject();
                    member.put("user_id", rs.getString("internal_uuid"));
                    member.put("profile_name", rs.getString("profile_name"));
                    member.put("username", rs.getString("user_id"));
                    member.put("image_url", rs.getString("image_url"));
                    member.put("role", rs.getString("role"));
                    member.put("status", rs.getString("status"));
                    member.put("last_seen", rs.getString("last_seen"));
                    membersArr.put(member);
                }
            }

            result.put("members", membersArr);

            // === Viewer role (to decide if they can delete group, etc.)
            String roleQuery = """
            SELECT role
            FROM group_members
            WHERE group_id = ? AND user_id = ?
        """;

            try (PreparedStatement ps = conn.prepareStatement(roleQuery)) {
                ps.setObject(1, groupId);
                ps.setObject(2, viewerUuid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    result.put("my_role", rs.getString("role"));
                } else {
                    result.put("my_role", ""); // not a member
                }
            }
        }

        return result;
    }

}