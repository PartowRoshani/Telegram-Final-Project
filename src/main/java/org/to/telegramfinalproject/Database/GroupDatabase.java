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


}