package org.to.telegramfinalproject.Database;

import org.to.telegramfinalproject.Models.Contact;
import org.to.telegramfinalproject.Models.User;

import java.sql.*;
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



    public boolean removeContact(UUID user_id, UUID contact_id)  {
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
                // اگر رابطه وجود نداره، اول باید کاربر رو به contact ها اضافه کنیم
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


    public boolean isBlocked(UUID user_id, UUID contact_id) {
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

    public List<Contact> searchContacts(UUID user_id, String searchTerm) {
        List<Contact> results = new ArrayList<>(); //ILIKE case-insensitive
        String sql = """
        SELECT c.contact_id, c.added_at, c.is_blocked
        FROM contacts c
        JOIN users u ON c.contact_id = u.internal_uuid
        WHERE c.user_id = ? AND (u.user_id ILIKE ? OR u.profile_name ILIKE ?)
        """;
        try (Connection connection = getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setObject(1, user_id);
            stmt.setString(2, "%" + searchTerm + "%");
            stmt.setString(3, "%" + searchTerm + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID contactId = (UUID) rs.getObject("contact_id");
                boolean isBlocked = rs.getBoolean("is_blocked");
                Timestamp addedAt = rs.getTimestamp("added_at");
                Contact contact = new Contact(user_id, contactId);
                contact.setIs_blocked(isBlocked);
                contact.setAdd_at(addedAt.toLocalDateTime());
                results.add(contact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
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


}
