package org.to.telegramfinalproject.Database;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.Contact;
import org.to.telegramfinalproject.Models.ContactEntry;
import org.to.telegramfinalproject.Models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContactDatabase {
    public ContactDatabase(){}


    private static Connection getConnection() throws SQLException {
        return ConnectionDb.connect();
    }

    public static boolean addContact(UUID userId, UUID contactId) {
        String sql = """
        INSERT INTO contacts (user_id, contact_id)
        VALUES (?, ?)
        ON CONFLICT DO NOTHING
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, userId);
            stmt.setObject(2, contactId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<UUID> getContactUUIDs(UUID userId) {
        List<Contact> contacts = getContacts(userId);
        List<UUID> contactIds = new ArrayList<>();
        for (Contact c : contacts) {
            contactIds.add(c.getContact_id());
        }
        return contactIds;
    }



    public static boolean removeContact(UUID user_id, UUID contact_id)  {
        String sql = "DELETE FROM contacts WHERE user_id = ? AND contact_id = ?";
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setObject(1, user_id);
            stmt.setObject(2, contact_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean toggleBlock(UUID userId, UUID targetId) {
        String selectSql = "SELECT is_blocked FROM contacts WHERE user_id = ? AND contact_id = ?";
        String updateSql = "UPDATE contacts SET is_blocked = ? WHERE user_id = ? AND contact_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setObject(1, userId);
            selectStmt.setObject(2, targetId);

            ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                boolean currentlyBlocked = rs.getBoolean("is_blocked");

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setBoolean(1, !currentlyBlocked);
                    updateStmt.setObject(2, userId);
                    updateStmt.setObject(3, targetId);
                    updateStmt.executeUpdate();
                }
                return !currentlyBlocked;
            } else {
                String insertSql = "INSERT INTO contacts (user_id, contact_id, is_blocked) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setObject(1, userId);
                    insertStmt.setObject(2, targetId);
                    insertStmt.setBoolean(3, true);
                    insertStmt.executeUpdate();
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static List<JSONObject> getBlockedUsers(UUID userId) {
        String sql = """
            SELECT u.internal_uuid, u.user_id, u.profile_name
            FROM contacts c
            JOIN users u ON u.internal_uuid = c.contact_id
            WHERE c.user_id = ? AND c.is_blocked = TRUE
            ORDER BY u.profile_name NULLS LAST, u.user_id
        """;
        List<JSONObject> out = new ArrayList<>();
        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject j = new JSONObject();
                    j.put("internal_uuid", rs.getObject("internal_uuid").toString());
                    j.put("user_id", rs.getString("user_id"));
                    j.put("profile_name", rs.getString("profile_name"));
                    out.add(j);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static UUID findOtherUserInPrivateChat(UUID chatId, UUID viewerId) {
        if (chatId == null || viewerId == null) return null;

        final String sql = """
            SELECT user1_id, user2_id
            FROM private_chat
            WHERE chat_id = ?
            """;

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setObject(1, chatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UUID u1 = (UUID) rs.getObject("user1_id");
                UUID u2 = (UUID) rs.getObject("user2_id");

                if (viewerId.equals(u1)) return u2;
                if (viewerId.equals(u2)) return u1;

                // اگر viewer عضو این چت نیست
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean unblockContact(UUID user_id, UUID contact_id) {
        String sql = "UPDATE contacts SET is_blocked = FALSE WHERE user_id = ? AND contact_id = ?";
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setObject(1, user_id);
            stmt.setObject(2, contact_id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Contact> getContacts(UUID user_id) {
        List<Contact> contacts = new ArrayList<>();
        String sql = "SELECT * FROM contacts WHERE user_id = ?";
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setObject(1, user_id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID contactId = (UUID) rs.getObject("contact_id");
                boolean isBlocked = rs.getBoolean("is_blocked");
                Timestamp addedAt = rs.getTimestamp("added_at");

                Contact contact = new Contact(user_id, contactId);
                contact.setIs_blocked(isBlocked);
                contact.setAdd_at(addedAt.toLocalDateTime());
                contacts.add(contact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }


    public static boolean existsContact(UUID user_id, UUID contact_id) {
        String sql = "SELECT 1 FROM contacts WHERE user_id = ? AND contact_id = ? LIMIT 1"; // stop searching when find the first item in DB(LIMIT 1)
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setObject(1, user_id);
            stmt.setObject(2, contact_id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean eitherBlocks(UUID userA, UUID userB) {
        return isBlocked(userA, userB) || isBlocked(userB, userA);
    }



    public static boolean isBlocked(UUID user_id, UUID contact_id) {
        String sql = "SELECT is_blocked FROM contacts WHERE user_id = ? AND contact_id = ?";
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setObject(1, user_id);
            stmt.setObject(2, contact_id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_blocked");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean deleteChatOneSide(UUID currentUserId, UUID otherUserId) {
        String sql = """
            UPDATE private_chat
            SET user1_deleted = CASE WHEN user1_id = ? THEN TRUE ELSE user1_deleted END,
                user2_deleted = CASE WHEN user2_id = ? THEN TRUE ELSE user2_deleted END
            WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, currentUserId);
            stmt.setObject(2, currentUserId);
            stmt.setObject(3, currentUserId);
            stmt.setObject(4, otherUserId);
            stmt.setObject(5, otherUserId);
            stmt.setObject(6, currentUserId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteChatBoth(UUID currentUserId, UUID otherUserId) {
        String sqlDeleteMessages = """
            DELETE FROM messages
            WHERE receiver_type = 'private' AND (
                (sender_id = ? AND receiver_id = ?) OR
                (sender_id = ? AND receiver_id = ?)
            )
        """;

        String sqlDeleteChat = """
            DELETE FROM private_chat
            WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)
        """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmtMsg = conn.prepareStatement(sqlDeleteMessages);
                 PreparedStatement stmtChat = conn.prepareStatement(sqlDeleteChat)) {

                stmtMsg.setObject(1, currentUserId);
                stmtMsg.setObject(2, otherUserId);
                stmtMsg.setObject(3, otherUserId);
                stmtMsg.setObject(4, currentUserId);
                stmtMsg.executeUpdate();

                stmtChat.setObject(1, currentUserId);
                stmtChat.setObject(2, otherUserId);
                stmtChat.setObject(3, otherUserId);
                stmtChat.setObject(4, currentUserId);
                stmtChat.executeUpdate();

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<ContactEntry> getContactEntries(UUID userId) {
        List<ContactEntry> entries = new ArrayList<>();

        String sql = """
        SELECT c.contact_id, c.is_blocked, u.user_id, u.profile_name, u.image_url
        FROM contacts c
        JOIN users u ON c.contact_id = u.internal_uuid
        WHERE c.user_id = ?
    """;

        try (Connection conn = ConnectionDb.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UUID contactId = UUID.fromString(rs.getString("contact_id"));
                boolean isBlocked = rs.getBoolean("is_blocked");
                String userIdStr = rs.getString("user_id");
                String profileName = rs.getString("profile_name");
                String imageUrl = rs.getString("image_url");

                ContactEntry entry = new ContactEntry(contactId, userIdStr, profileName, imageUrl, isBlocked);
                entries.add(entry);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    public static List<ContactEntry> searchContacts(UUID userId, String searchTerm) {
        List<ContactEntry> results = new ArrayList<>();

        String sql = """
        SELECT u.internal_uuid, u.user_id, c.is_blocked
        FROM contacts c
        JOIN users u ON c.contact_id = u.internal_uuid
        WHERE c.user_id = ? 
          AND (u.user_id ILIKE ? OR u.profile_name ILIKE ?)
    """;

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setObject(1, userId);
            stmt.setString(2, "%" + searchTerm + "%");
            stmt.setString(3, "%" + searchTerm + "%");

            ResultSet rs = stmt.executeQuery();

            userDatabase userDB = new userDatabase();
            while (rs.next()) {
                UUID contactId = (UUID) rs.getObject("internal_uuid");
                String displayId = rs.getString("user_id");
                String contact_displayId = userDB.getUserId(contactId);
                String profileName = userDB.getProfileName(contactId);
                String imageUrl = userDB.getProfilePicture(contactId);
                boolean isBlocked = rs.getBoolean("is_blocked");

                String lastSeenString = userDB.getLastSeen(contactId);

                // Convert to LocalDateTime
                LocalDateTime lastSeen = null;
                if (!"Unknown".equals(lastSeenString)) {
                    lastSeen = LocalDateTime.parse(lastSeenString);
                }

                results.add(new ContactEntry(contactId, displayId, contact_displayId, profileName, imageUrl, isBlocked, lastSeen));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

}
