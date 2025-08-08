package org.to.telegramfinalproject.Server;

import org.json.JSONObject;
import org.to.telegramfinalproject.Database.ChannelDatabase;
import org.to.telegramfinalproject.Database.GroupDatabase;
import org.to.telegramfinalproject.Database.userDatabase;
import org.to.telegramfinalproject.Models.Message;
import org.to.telegramfinalproject.Models.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RealTimeEventDispatcher {

    public static void sendToUser(UUID userId, JSONObject data) {
        Socket socket = SessionManager.getUserSocket(userId);

       
        if (socket != null && !socket.isClosed()) {
            try {


                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("🚀 Sending to user: " + userId + " → " + data);

                out.println(data.toString());
            } catch (IOException e) {
                System.err.println("❌ Error sending to user: " + e.getMessage());
            }

        }
        else {
            System.out.println("⚠️ User " + userId + " is offline. Skipping real-time send.");

        }
    }

    public static void broadcastToUsers(List<UUID> userIds, JSONObject data) {
        for (UUID userId : userIds) {
            sendToUser(userId, data);
        }
    }

    public static JSONObject buildEvent(String action, JSONObject payload) {
        JSONObject json = new JSONObject();
        json.put("action", action);
        json.put("data", payload);
        return json;
    }


    public static void notifyNewMessage(Message msg, User sender) {
        JSONObject data = new JSONObject();
        data.put("sender", sender.getUser_id());
        data.put("receiver_type", msg.getReceiver_type());
        data.put("receiver_id", msg.getReceiver_id());
        data.put("content", msg.getContent());
        data.put("time", msg.getSend_at().toString());

        JSONObject event = buildEvent("new_message", data);

        switch (msg.getReceiver_type()) {
            case "private" -> RealTimeEventDispatcher.sendToUser(msg.getReceiver_id(), event);
            case "group" -> {
                List<UUID> memberIds = GroupDatabase.getMemberUUIDs(msg.getReceiver_id());
                memberIds.remove(sender.getInternal_uuid());
                RealTimeEventDispatcher.broadcastToUsers(memberIds, event);
            }
            case "channel" -> {
                List<UUID> subscriberIds = ChannelDatabase.getSubscriberUUIDs(msg.getReceiver_id());
                subscriberIds.remove(sender.getInternal_uuid());
                subscriberIds.remove(sender.getInternal_uuid());
                RealTimeEventDispatcher.broadcastToUsers(subscriberIds, event);
            }
        }
    }



    public static void notifyMessageEdited(UUID chatId, UUID messageId, String newContent, LocalDateTime editedAt, List<UUID> receivers) {
        JSONObject data = new JSONObject();
        data.put("chat_id", chatId.toString());
        data.put("message_id", messageId.toString());
        data.put("new_content", newContent);
        data.put("edited_at", editedAt.toString());

        JSONObject event = new JSONObject();
        event.put("action", "message_edited");
        event.put("data", data);

        broadcastToUsers(receivers, event);
    }



    public static void sendNewMessage(Message message, List<UUID> receivers, String kind, JSONObject meta) {
        JSONObject payload = new JSONObject();
        payload.put("action", "new_message");

        JSONObject data = new JSONObject();
        data.put("id", message.getMessage_id().toString());
        data.put("chat_id", message.getReceiver_id().toString());
        data.put("sender_id", message.getSender_id().toString());
        data.put("receiver_id", message.getReceiver_id().toString());
        data.put("receiver_type", message.getReceiver_type());
        data.put("content", message.getContent());
        data.put("message_type", message.getMessage_type());
        data.put("send_at", message.getSend_at().toString());

        User sender = userDatabase.findByInternalUUID(message.getSender_id());
        if (sender != null) data.put("sender_name", sender.getProfile_name());

        data.put("kind", kind == null ? "plain" : kind); // plain|reply|forward
        if (meta != null) data.put("meta", meta); // reply_to {...} | forwarded_from {...}

        payload.put("data", data);
        for (UUID userId : receivers) sendToUser(userId, payload);
    }

    public static void notifyMessageDeletedGlobal(UUID chatId, UUID messageId, List<UUID> receivers) {
        JSONObject data = new JSONObject();
        data.put("chat_id", chatId.toString());
        data.put("message_id", messageId.toString());

        JSONObject event = new JSONObject();
        event.put("action", "message_deleted_global");
        event.put("data", data);

        broadcastToUsers(receivers, event);
    }


    public static void notifyReactionAdded(UUID chatId, UUID messageId, String emoji,
                                           int totalForEmoji, JSONObject countsAll, List<UUID> receivers) {
        JSONObject data = new JSONObject();
        data.put("chat_id", chatId.toString());
        data.put("message_id", messageId.toString());
        data.put("emoji", emoji);
        data.put("counts", countsAll); // {"❤️":3,"👍":1,...}
        data.put("count_for_emoji", totalForEmoji);

        JSONObject event = new JSONObject();
        event.put("action", "message_reacted");
        event.put("data", data);

        broadcastToUsers(receivers, event);
    }

    public static void notifyReactionRemoved(UUID chatId, UUID messageId, String emoji,
                                             int totalForEmoji, JSONObject countsAll, List<UUID> receivers) {
        JSONObject data = new JSONObject();
        data.put("chat_id", chatId.toString());
        data.put("message_id", messageId.toString());
        data.put("emoji", emoji);
        data.put("counts", countsAll);
        data.put("count_for_emoji", totalForEmoji);

        JSONObject event = new JSONObject();
        event.put("action", "message_unreacted");
        event.put("data", data);

        broadcastToUsers(receivers, event);
    }




    public static void notifyUserUpdated(UUID userId, String newProfileName, String newImageUrl, List<UUID> contactIds) {
        JSONObject data = new JSONObject();
        data.put("user_id", userId.toString());
        data.put("new_name", newProfileName);
        data.put("new_image_url", newImageUrl);

        JSONObject event = new JSONObject();
        event.put("action", "update_user");
        event.put("data", data);

        broadcastToUsers(contactIds, event);
    }

    public static void notifyChatDeleted(String type, UUID id, List<UUID> affectedUsers) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type); // private, group, channel
        data.put("chat_id", id.toString());

        JSONObject event = new JSONObject();
        event.put("action", "chat_deleted");
        event.put("data", data);

        broadcastToUsers(affectedUsers, event);
    }

    public static void notifyGroupOrChannelUpdated(String type, UUID id, String newName, String newImageUrl, List<UUID> affectedUsers) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type); // "group" or "channel"
        data.put("chat_id", id.toString());
        data.put("new_name", newName);
        data.put("new_image_url", newImageUrl);

        JSONObject event = new JSONObject();
        event.put("action", "update_group_or_channel");
        event.put("data", data);

        broadcastToUsers(affectedUsers, event);
    }

    public static void notifyMediaMessage(Message msg, User sender) {
        JSONObject data = new JSONObject();
        data.put("sender", sender.getUser_id());
        data.put("receiver_type", msg.getReceiver_type());
        data.put("receiver_id", msg.getReceiver_id());
        data.put("file_type", msg.getMessage_type()); // IMAGE, FILE, VIDEO...
        data.put("time", msg.getSend_at().toString());

        JSONObject event = new JSONObject();
        event.put("action", "new_media");
        event.put("data", data);

        switch (msg.getReceiver_type()) {
            case "private" -> sendToUser(msg.getReceiver_id(), event);
            case "group" -> {
                List<UUID> members = GroupDatabase.getMemberUUIDs(msg.getReceiver_id());
                members.remove(sender.getInternal_uuid());
                broadcastToUsers(members, event);
            }
            case "channel" -> {
                List<UUID> subs = ChannelDatabase.getSubscriberUUIDs(msg.getReceiver_id());
                subs.remove(sender.getInternal_uuid());
                broadcastToUsers(subs, event);
            }
        }
    }

    public static void notifyAddedToChat(String type, UUID chatId, String chatName, String imageUrl, UUID userId) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type);
        data.put("chat_id", chatId.toString());
        data.put("chat_name", chatName);
        data.put("image_url", imageUrl);

        JSONObject event = new JSONObject();
        event.put("action", type.equals("group") ? "added_to_group" : "added_to_channel");
        event.put("data", data);

        sendToUser(userId, event);
    }

    public static void notifyRemovedFromChat(String type, UUID chatId, UUID userId) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type);
        data.put("chat_id", chatId.toString());

        JSONObject event = new JSONObject();
        event.put("action", type.equals("group") ? "removed_from_group" : "removed_from_channel");
        event.put("data", data);

        sendToUser(userId, event);
    }
    public static void notifyMessageSeen(UUID messageId, UUID senderId) {
        JSONObject data = new JSONObject();
        data.put("message_id", messageId.toString());
        data.put("seen_at", LocalDateTime.now().toString());

        JSONObject event = new JSONObject();
        event.put("action", "message_seen");
        event.put("data", data);

        sendToUser(senderId, event);
    }


    public static void notifyBlocked(UUID blockerId, UUID blockedUserId) {
        JSONObject data = new JSONObject();
        data.put("blocker_id", blockerId.toString());

        JSONObject event = new JSONObject();
        event.put("action", "blocked_by_user");
        event.put("data", data);

        sendToUser(blockedUserId, event);
    }


    public static void notifyUnblocked(UUID unblockerId, UUID unblockedUserId) {
        JSONObject data = new JSONObject();
        data.put("unblocker_id", unblockerId.toString());

        JSONObject event = new JSONObject();
        event.put("action", "unblocked_by_user");
        event.put("data", data);

        sendToUser(unblockedUserId, event);
    }

    public static void notifyUserStatusChanged(UUID userId, String status, List<UUID> contacts) {
        JSONObject data = new JSONObject();
        data.put("user_id", userId.toString());
        data.put("status", status); // online | offline
        data.put("time", LocalDateTime.now().toString());

        JSONObject event = buildEvent("user_status_changed", data);

        broadcastToUsers(contacts, event);
    }

    public static void sendGroupOrChannelUpdate(String type, UUID chatId, String name, String imageUrl, String description, List<UUID> affectedUsers) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type);
        data.put("chat_id", chatId.toString());
        data.put("name", name);
        data.put("image_url", imageUrl);
        data.put("description", description != null ? description : "");

        JSONObject event = new JSONObject();
        event.put("action", "chat_updated");
        event.put("data", data);

        broadcastToUsers(affectedUsers, event);
    }


    public static void notifyBecameAdmin(String type, UUID chatId, String chatName, String imageUrl, UUID userId) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type); // group or channel
        data.put("chat_id", chatId.toString());
        data.put("chat_name", chatName);
        data.put("image_url", imageUrl);

        JSONObject event = new JSONObject();
        event.put("action", "became_admin");
        event.put("data", data);

        sendToUser(userId, event);
    }


    public static void notifyRemovedAdminFromChat(String type, UUID chatId, String chatName, String imageUrl, UUID userId) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type);
        data.put("chat_id", chatId.toString());
        data.put("chat_name", chatName);
        data.put("image_url", imageUrl);

        JSONObject event = new JSONObject();
        event.put("action", "removed_admin");
        event.put("data", data);

        sendToUser(userId, event);
    }


    public static void sendOwnershipTransferred(String type, UUID chatId, String chatName, List<UUID> affectedUsers) {
        JSONObject data = new JSONObject();
        data.put("chat_type", type);         // "group" or "channel"
        data.put("chat_id", chatId.toString());
        data.put("chat_name", chatName);

        JSONObject event = new JSONObject();
        event.put("action", "ownership_transferred");
        event.put("data", data);

        broadcastToUsers(affectedUsers, event);
    }


    public static void sendNewMessage(Message message, List<UUID> receivers) {
        JSONObject payload = new JSONObject();
        payload.put("action", "new_message");

        JSONObject data = new JSONObject();
        data.put("id", message.getMessage_id().toString());
        data.put("sender_id", message.getSender_id().toString());
        data.put("receiver_id", message.getReceiver_id().toString());
        data.put("receiver_type", message.getReceiver_type());
        data.put("content", message.getContent());
        data.put("send_at", message.getSend_at().toString());

        User sender = userDatabase.findByInternalUUID(message.getSender_id());
        if (sender != null) {
            data.put("sender_name", sender.getProfile_name());
        }

        payload.put("data", data);

        for (UUID userId : receivers) {
            sendToUser(userId, payload);
        }
    }


}
