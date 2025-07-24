package org.to.telegramfinalproject.Database;


import org.to.telegramfinalproject.Models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class userDatabase {
    
    
    public userDatabase() {
    }

    public static boolean isUserOnline(UUID userId) {
        String sql = "SELECT status FROM users WHERE internal_uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                return "online".equalsIgnoreCase(status);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }



    private static Connection getConnection() throws SQLException {
        return ConnectionDb.connect();
    }

    public static String getLastSeen(UUID userId) {
        String sql = "SELECT last_seen FROM users WHERE internal_uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp lastSeen = rs.getTimestamp("last_seen");
                if (lastSeen != null) {
                    return lastSeen.toLocalDateTime().toString();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Unknown";
    }


    public User findByUserId(String userId) {
        String query = "SELECT * FROM users WHERE user_id = ?";

        try {
            User var6;
            try (Connection conn = ConnectionDb.connect()) {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, userId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        return null;
                    }

                    var6 = this.extractUser(rs);
                }
            }

            return var6;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public User findByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";

        try {
            User var6;
            try (Connection conn = this.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        return null;
                    }

                    var6 = this.extractUser(rs);
                }
            }

            return var6;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean existsByUsername(String username) {
        String query = "SELECT 1 FROM users WHERE username = ?";

        try {
            boolean var6;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query);
            ) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                var6 = rs.next();
            }

            return var6;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean existsByUserId(String user_id) {
        String query = "SELECT 1 FROM users WHERE user_id = ?";

        try {
            boolean var6;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query);
            ) {
                stmt.setString(1, user_id);
                ResultSet rs = stmt.executeQuery();
                var6 = rs.next();
            }

            return var6;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean save(User user) {
        String query = "INSERT INTO users (user_id, internal_uuid, username, password, profile_name) VALUES (?, ?, ?, ?, ?)";

        try {
            boolean var5;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query);
            ) {
                stmt.setString(1, user.getUser_id());
                stmt.setObject(2, user.getInternal_uuid());
                stmt.setString(3, user.getUsername());
                stmt.setString(4, user.getPassword());
                stmt.setString(5, user.getProfile_name());
                var5 = stmt.executeUpdate() > 0;
            }

            return var5;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateByUUID(UUID uuid, User user) {
        String query = "UPDATE users SET user_id=?, username=?, password=?, profile_name=?, bio=?, image_url=?, status=?, last_seen=? WHERE internal_uuid=?";

        try {
            boolean var6;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query);
            ) {
                stmt.setString(1, user.getUser_id());
                stmt.setString(2, user.getUsername());
                stmt.setString(3, user.getPassword());
                stmt.setString(4, user.getProfile_name());
                stmt.setString(5, user.getBio());
                stmt.setString(6, user.getImage_url());
                stmt.setString(7, user.getStatus());
                stmt.setObject(8, user.getLast_seen());
                stmt.setObject(9, uuid);
                var6 = stmt.executeUpdate() > 0;
            }

            return var6;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> getAll() {
        List<User> users = new ArrayList();
        String query = "SELECT * FROM users";

        try (
                Connection conn = this.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
        ) {
            while(rs.next()) {
                users.add(this.extractUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    private User extractUser(ResultSet rs) throws SQLException {
        return new User(rs.getString("user_id"), UUID.fromString(rs.getString("internal_uuid")), rs.getString("username"), rs.getString("password"), rs.getString("profile_name"));
    }

    public boolean deleteByUUID(UUID uuid) {
        String query = "DELETE FROM users WHERE internal_uuid = ?";

        try {
            boolean var5;
            try (
                    Connection conn = this.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query);
            ) {
                stmt.setObject(1, uuid);
                var5 = stmt.executeUpdate() > 0;
            }

            return var5;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void updateUserStatus(UUID uuid, String status) {
        String sql = "UPDATE users SET status = ? WHERE internal_uuid = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setObject(2, uuid);
            int rows = stmt.executeUpdate();
            System.out.println("🔁 updateUserStatus: set '" + status + "' for " + uuid + " → affected rows = " + rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void updateLastSeen(UUID uuid) {
        String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE internal_uuid = ?";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static User findByInternalUUID(UUID internalUuid) {
        String sql = "SELECT * FROM users WHERE internal_uuid = ?";

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, internalUuid);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getString("user_id"),
                        UUID.fromString(rs.getString("internal_uuid")),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("profile_name")
                );

                user.setBio(rs.getString("bio"));
                user.setImage_url(rs.getString("image_url"));
                user.setStatus(rs.getString("status"));

                Timestamp lastSeenTs = rs.getTimestamp("last_seen");
                if (lastSeenTs != null) {
                    user.setLast_seen(lastSeenTs.toLocalDateTime());
                }

                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
    public List<User> searchUsers(String keyword, UUID currentUserId) {
        String query = """
        SELECT * FROM users 
        WHERE (user_id ILIKE ? OR profile_name ILIKE ?)
        AND internal_uuid <> ?
    """;

        List<User> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            stmt.setObject(3, currentUserId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(extractUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }



    public static void setAllUsersOffline() {
        String sql = "UPDATE users SET status = 'offline'";
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int affected = stmt.executeUpdate();
            System.out.println("🔁 All users set to offline. Rows affected: " + affected);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}

