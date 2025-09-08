package org.to.telegramfinalproject.Server;

import org.to.telegramfinalproject.Models.Channel;
import org.to.telegramfinalproject.Database.ChannelDatabase;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChannelService {
    public static boolean createChannel(String channelId, String channelName, UUID creatorUUID, String imageUrl,String description) {
        UUID internalUUID = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        boolean inserted = ChannelDatabase.insertChannel(internalUUID, channelId, channelName, creatorUUID, imageUrl, description,now);

        if (inserted) {
            ChannelDatabase.addSubscriber(internalUUID, creatorUUID,"owner");
            return true;
        }
        return false;
    }

}
