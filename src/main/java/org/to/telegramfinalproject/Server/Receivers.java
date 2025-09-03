package org.to.telegramfinalproject.Server;

import org.to.telegramfinalproject.Database.ChannelDatabase;
import org.to.telegramfinalproject.Database.GroupDatabase;
import org.to.telegramfinalproject.Database.PrivateChatDatabase;

import java.util.List;
import java.util.UUID;

public class Receivers {
    public static List<UUID> resolveFor(String type, UUID chatId, UUID exclude) {
        List<UUID> ids = switch (type) {
            case "private" -> PrivateChatDatabase.getMembers(chatId);
            case "group" -> GroupDatabase.getMemberUUIDs(chatId);
            case "channel" -> ChannelDatabase.getSubscriberUUIDs(chatId);
            default -> List.of();
        };
        if (exclude != null) ids.remove(exclude);
        return ids;
    }
}
