package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

public class IncomingMessageListener implements Runnable {
    private final BufferedReader in;

    public IncomingMessageListener(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            System.out.println("👂 Real-Time Listener started.");

            String line;
            while ((line = in.readLine()) != null) {
                JSONObject response = new JSONObject(line);
                System.out.println("📥 Received raw line: " + line);

                if (response.has("action")) {
                    System.out.println("🎯 [Listener] Action received: " + response.toString(2));

                    String action = response.getString("action");
                    System.out.println("🎯 Received action: " + action);

                    if (isRealTimeEvent(action)) {
                        handleRealTimeEvent(response);
                    } else {
                        TelegramClient.responseQueue.put(response);
                    }
                } else if (response.has("status") && response.has("message")) {
                    TelegramClient.responseQueue.put(response);
                } else {
                    TelegramClient.responseQueue.put(response);
                }
            }

        } catch (Exception e) {
            System.out.println("🔴 [Listener] Crashed due to: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isRealTimeEvent(String action) {
        return switch (action) {
            case "new_message", "message_edited", "message_deleted",
                 "user_status_changed", "added_to_group", "added_to_channel",
                 "update_group_or_channel", "chat_deleted",
                 "blocked_by_user", "unblocked_by_user", "message_seen",
                 "removed_from_group", "removed_from_channel",
                 "became_admin", "removed_admin" -> true;
            default -> false;
        };
    }

    private void handleRealTimeEvent(JSONObject response) throws IOException {
        String action = response.getString("action");
        JSONObject msg = response.getJSONObject("data");

        switch (action) {
            case "added_to_group", "added_to_channel",
                 "removed_from_group", "removed_from_channel", "chat_deleted" -> {
                System.out.println("\n🔄 Chat list changed. Updating...");
                ActionHandler.requestChatList();

                if (action.equals("removed_from_group") || action.equals("removed_from_channel") || action.equals("chat_deleted")) {
                    System.out.println("🚫 You were removed from the chat or chat was deleted. Exiting...");
                    ActionHandler.forceExitChat = true;
                }
            }

            case "update_group_or_channel" -> {
                System.out.println("\n🔄 Group/Channel info updated.");
                String chatId = msg.getString("chat_id");
                String chatType = msg.getString("chat_type");
                ActionHandler.requestChatInfo(chatId, chatType);
            }

            case "became_admin", "removed_admin" -> {
                System.out.println("\n🔄 Your admin status changed. Updating chat info...");
                String chatId = msg.getString("chat_id");
                String chatType = msg.getString("chat_type");
                ActionHandler.requestChatInfo(chatId, chatType);
            }

            default -> displayRealTimeMessage(action, msg);
        }

        System.out.print(">> ");
    }


    private void displayRealTimeMessage(String action, JSONObject msg) {
        switch (action) {
            case "new_message" -> {
                System.out.println("\n🔔 New Message:");
                System.out.println("From: " + msg.getString("sender"));
                System.out.println("Time: " + msg.getString("time"));
                System.out.println("Content: " + msg.getString("content"));
            }
            case "message_edited" -> {
                System.out.println("\n✏️ Message Edited:");
                System.out.println("ID: " + msg.getString("message_id"));
                System.out.println("New Content: " + msg.getString("new_content"));
                System.out.println("Edit Time: " + msg.getString("edited_at"));
            }
            case "message_deleted" -> {
                System.out.println("\n🗑️ Message Deleted:");
                System.out.println("Message ID: " + msg.getString("message_id"));
            }
            case "user_status_changed" -> {
                System.out.println("\n🔄 User Status Changed:");
                System.out.println("User: " + msg.getString("user_id"));
                System.out.println("Status: " + msg.getString("status"));
            }
            case "blocked_by_user" -> {
                System.out.println("\n⛔ You were blocked by user: " + msg.getString("blocker_id"));
            }
            case "unblocked_by_user" -> {
                System.out.println("\n✅ You were unblocked by user: " + msg.getString("unblocker_id"));
            }
            case "message_seen" -> {
                System.out.println("\n👁️ Your message was seen:");
                System.out.println("Message ID: " + msg.getString("message_id"));
                System.out.println("Seen at: " + msg.getString("seen_at"));
            }
            default -> {
                System.out.println("\n❓ Unknown real-time action: " + action);
                System.out.println(msg.toString(2));
            }
        }
    }
}