package org.to.telegramfinalproject.Client;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

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

               //if it has reqID answer
                if (response.has("request_id")) {
                    String requestId = response.getString("request_id");
                    System.out.println("📬 Response with request_id: " + requestId);
                    System.out.println("📬 Full response: " + response.toString(2));

                    BlockingQueue<JSONObject> queue = TelegramClient.pendingResponses.get(requestId);
                    if (queue != null) {
                        queue.put(response);
                    } else {
                        System.out.println("⚠️ No pending queue for request_id = " + requestId + ". Putting in responseQueue...");
                        TelegramClient.responseQueue.put(response);
                    }

                    continue;
                }



                //if it has action check it
                if (response.has("action")) {
                    String action = response.getString("action");
                    System.out.println("🎯 [Listener] Action received: " + response.toString(2));
                    System.out.println("🎯 Received action: " + action);

                    if (isRealTimeEvent(action)) {
                        handleRealTimeEvent(response);
                    } else {
                        TelegramClient.responseQueue.put(response);
                    }

                } else if (response.has("status") && response.has("message")) {
                    TelegramClient.responseQueue.put(response); // general answer
                } else {
                    TelegramClient.responseQueue.put(response); // fallback
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
                 "became_admin", "removed_admin", "ownership_transferred","admin_permissions_updated" -> true;
            default -> false;
        };
    }

    void handleRealTimeEvent(JSONObject response) throws IOException {
        String action = response.getString("action");
        JSONObject msg = response.getJSONObject("data");


        switch (action) {
            case "added_to_group", "added_to_channel",
                 "removed_from_group", "removed_from_channel", "chat_deleted" -> {
                System.out.println("🔄 Chat list changed. Updating...");
                Session.forceRefreshChatList = true;
                System.out.println("🧪 Calling requestChatList() after being added");

                String chatId = msg.getString("chat_id");
                String chatType = msg.getString("chat_type");
                ActionHandler.requestChatInfo(chatId, chatType);

                if (action.equals("removed_from_group") || action.equals("removed_from_channel") || action.equals("chat_deleted")) {
                    System.out.println("🚫 You were removed from the chat or chat was deleted. Exiting...");
                    ActionHandler.forceExitChat = true;
                }
            }

            case "chat_updated" -> {
                System.out.println("\n🔄 Chat info updated.");

                if (msg.has("last_message_time")) {
                    updateLastMessageTime(msg); 
                } else {
                    new Thread(() -> {
                        try {
                            handleAdminRoleChanged(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }


            case "became_admin", "removed_admin", "ownership_transferred","admin_permissions_updated" -> {
                System.out.println("🧩 Detected admin/owner role change. Calling handler...");
                new Thread(() -> {
                    try {
                        handleAdminRoleChanged(msg); //new thread
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }


//



            default -> displayRealTimeMessage(action, msg);
        }

        System.out.print(">> ");
    }

    private void updateLastMessageTime(JSONObject msg) {
        try {
            UUID chatUUID = UUID.fromString(msg.getString("chat_id"));
            String newTime = msg.optString("last_message_time", null);

            Session.chatList.stream()
                    .filter(chat -> chat.getId().equals(chatUUID))
                    .findFirst()
                    .ifPresent(chat -> {
                        chat.setLastMessageTime(newTime);
                        System.out.println("✅ Updated last message time for chat: " + chat.getDisplayId());
                    });

            if (Session.inChatListMenu) {
                ActionHandler.displayChatList();
                System.out.print("Select a chat by number: ");
            }

        } catch (Exception e) {
            System.out.println("❌ Failed to update last message time: " + e.getMessage());
        }
    }


    private void handleAdminRoleChanged(JSONObject data) throws IOException {
        String chatType = data.getString("chat_type");
        String chatId = data.optString("group_id", data.optString("channel_id", data.optString("chat_id", null)));

        if (chatId == null) {
            System.out.println("⚠️ No valid ID found in real-time data: " + data.toString(2));
            return;
        }

        System.out.println("\n🔄 Your admin status changed. Updating chat info...");

        try {
            // 1. get chat info
            JSONObject chatInfoReq = new JSONObject();
            chatInfoReq.put("action", "get_chat_info");
            chatInfoReq.put("receiver_id", chatId);
            chatInfoReq.put("receiver_type", chatType);
            System.out.println("📤 Sending get_chat_info: " + chatInfoReq);
            JSONObject chatInfoResp = ActionHandler.sendWithResponse(chatInfoReq);
            JSONObject chatData = chatInfoResp.getJSONObject("data");

            UUID chatUUID = UUID.fromString(chatData.getString("internal_id"));

            Optional<ChatEntry> entry = Session.chatList.stream()
                    .filter(e -> e.getId().equals(chatUUID))
                    .findFirst();

            if (entry.isEmpty()) {
                System.out.println("❌ Chat not found in session.");
                return;
            }

            entry.ifPresent(chat -> {
                chat.setAdmin(chatData.optBoolean("is_admin", false));
                chat.setOwner(chatData.optBoolean("is_owner", false));
                chat.setName(chatData.optString("name", ""));
                chat.setDisplayId(chatData.optString("id", ""));
                chat.setImageUrl(chatData.optString("image_url", ""));
                chat.setType(chatData.optString("type", ""));
                Session.currentChatEntry = chat;
            });

            // 2. get permission
            JSONObject permissionReq = new JSONObject();
            if (chatType.equalsIgnoreCase("group")) {
                permissionReq.put("action", "get_group_permissions");
                permissionReq.put("group_id", chatId);
            } else {
                permissionReq.put("action", "get_channel_permissions");
                permissionReq.put("channel_id", chatId);
            }

            JSONObject permissionResp = ActionHandler.sendWithResponse(permissionReq);
            JSONObject perm = permissionResp.getJSONObject("data");
            entry.ifPresent(chat -> chat.setPermissions(perm));

            // 3. set currentChatId
            Session.currentChatId = chatUUID.toString();

            System.out.println("🧪 Checking refresh conditions...");
            System.out.println("🔹 inChatMenu: " + Session.inChatMenu);
            System.out.println("🔹 currentChatId: " + Session.currentChatId);
            System.out.println("🔹 chatUUID: " + chatUUID);

            if (Session.inChatMenu && Session.currentChatId != null && Session.currentChatId.equals(chatUUID.toString())) {
                synchronized (Session.class) {
                    Session.refreshCurrentChatMenu = true;
                }
                System.out.println("✅ Admin status updated. Refreshing menu...");
            } else {
                System.out.println("❌ Refresh conditions not met.");
            }

        } catch (Exception e) {
            System.out.println("❌ Exception while handling admin role change: " + e.getMessage());
            e.printStackTrace();
        }
    }










    private void displayRealTimeMessage(String action, JSONObject msg) {
        switch (action) {
            case "new_message" -> {
                System.out.println("\n🔔 New Message Received:");
                String senderName = msg.optString("sender_name", "Unknown");
                String content = msg.optString("content", "(empty)");
                String sendAt = msg.optString("send_at", "-");

                String receiverId = msg.optString("receiver_id", "");
                String receiverType = msg.optString("receiver_type", "");

                boolean isInCurrentChat = Session.inChatMenu &&
                        Session.currentChatId != null &&
                        Session.currentChatId.equals(receiverId);

                if (isInCurrentChat) {
                    System.out.println(senderName + ": " + content + "   (" + sendAt + ")");
                } else {
                    System.out.println("💬 Message from " + senderName + " in " + receiverType + " chat: " + content);
                    Session.forceRefreshChatList = true;
                }
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
