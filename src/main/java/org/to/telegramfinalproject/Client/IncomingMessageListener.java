package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.io.BufferedReader;

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
                    String action = response.getString("action");
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
            System.out.println("🔴 Listener stopped: " + e.getMessage());
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

    private void handleRealTimeEvent(JSONObject response) {
        String action = response.getString("action");
        JSONObject msg = response.getJSONObject("data");

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

            case "added_to_group" -> {
                System.out.println("\n👥 You were added to a group: " + msg.getString("chat_name"));
            }

            case "added_to_channel" -> {
                System.out.println("\n📢 You were added to a channel: " + msg.getString("chat_name"));
            }

            case "update_group_or_channel" -> {
                System.out.println("\n🔄 Group/Channel updated: " + msg.getString("new_name"));
            }

            case "chat_deleted" -> {
                System.out.println("\n🗑️ Chat deleted: " + msg.getString("chat_id"));
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

            case "removed_from_group" -> {
                System.out.println("\n🚫 You were removed from group: " + msg.getString("chat_id"));
            }

            case "removed_from_channel" -> {
                System.out.println("\n🚫 You were removed from channel: " + msg.getString("chat_id"));
            }

            case "became_admin" -> {
                System.out.println("\n⭐ You are now an admin in: " + msg.getString("chat_name"));
            }

            case "removed_admin" -> {
                System.out.println("\n⚠️ You are no longer an admin in: " + msg.getString("chat_name"));
            }

            default -> {
                System.out.println("\n❓ Unknown real-time action: " + action);
            }
        }

        System.out.print(">> ");
    }
}
