package org.to.telegramfinalproject.Utils;

import org.to.telegramfinalproject.Database.*;
import org.to.telegramfinalproject.Models.*;

public class ChatUtil {

    public static String getChatName(String receiverId, String receiverType) {
        switch (receiverType) {
            case "private":
                User user = userDatabase.findByPublicID(receiverId);
                return user != null ? user.getProfile_name() : "Unknown User";

            case "group":
                Group group = GroupDatabase.findByGroupID(receiverId);
                return group != null ? group.getGroup_name() : "Unknown Group";

            case "channel":
                Channel channel = ChannelDatabase.findByChannelID(receiverId);
                return channel != null ? channel.getChannel_name() : "Unknown Channel";

            default:
                return "Unknown Chat";
        }
    }
}
