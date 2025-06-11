package org.to.telegramfinalproject.Server;

import java.util.UUID;
import org.to.telegramfinalproject.Database.userDatabase;
import org.to.telegramfinalproject.Database.ContactDatabase;
public class ContactService {
    public static boolean addContact(UUID userId, UUID contactId) {
        if (userId.equals(contactId)) return false;
        if (userDatabase.findByInternalUUID(contactId) == null) return false;
        if (userDatabase.findByInternalUUID(userId) == null) return false;
        if (ContactDatabase.existsContact(userId, contactId)) return false;

        return ContactDatabase.addContact(userId, contactId);
    }
}
