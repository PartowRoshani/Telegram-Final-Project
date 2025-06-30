package org.to.telegramfinalproject.Server;

import org.to.telegramfinalproject.Models.Group;
import org.to.telegramfinalproject.Database.GroupDatabase;

import java.time.LocalDateTime;
import java.util.UUID;

public class GroupService {
    public static boolean createGroup(String groupId, String groupName, UUID creatorUUID, String imageUrl) {
        UUID internalUUID = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        boolean inserted = GroupDatabase.insertGroup(internalUUID, groupId, groupName, creatorUUID, imageUrl, now);

        if (inserted) {
            GroupDatabase.addMember(internalUUID, creatorUUID, "owner");
            return true;
        }
        return false;
    }




}
