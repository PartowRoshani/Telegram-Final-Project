package org.to.telegramfinalproject.Server;


import org.to.telegramfinalproject.Database.ContactDatabase;
import org.to.telegramfinalproject.Models.ResponseModel;

import java.util.UUID;

public class PrivateChatService {

    public static ResponseModel deletePrivateChat(UUID currentUserId, UUID targetUserId, boolean both) {
        boolean success = both ?
                ContactDatabase.deleteChatBoth(currentUserId, targetUserId) :
                ContactDatabase.deleteChatOneSide(currentUserId, targetUserId);

        if (success) {
            return new ResponseModel("success", "Chat deleted successfully");
        } else {
            return new ResponseModel("error", "Chat not found or failed to delete");
        }
    }
}
