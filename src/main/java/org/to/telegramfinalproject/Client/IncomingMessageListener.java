package org.to.telegramfinalproject.Client;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class IncomingMessageListener implements Runnable {
    private final BufferedReader in;
    private volatile boolean running = true;

    public IncomingMessageListener(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            System.out.println("👂 Real-Time Listener started.");

            while (running) {
                if (TelegramClient.mediaBusy.get()) {
                    try { Thread.sleep(15); } catch (InterruptedException ignored) {}
                    continue;
                }

                if (!in.ready()) {
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                    continue;
                }

                String line = in.readLine();
                if (line == null) {
                    break;
                }

                if (line.isBlank()) continue;

                final JSONObject response;
                try {
                    response = new JSONObject(line);
                } catch (Exception badJson) {
                    System.out.println("⚠️ [Listener] Non-JSON line ignored: " + line);
                    continue;
                }

                System.out.println("📥 Received raw line: " + line);

                // --- Media ACK routing by message_id ---
                String mid = response.optString("message_id", "");
                if (!mid.isEmpty()) {
                    BlockingQueue<JSONObject> q = TelegramClient.pendingResponses.get(mid);
                    if (q != null) {
                        q.put(response);
                        continue;
                    }
                }

                // --- General request_id response routing ---
                if (response.has("request_id")) {
                    String requestId = response.optString("request_id", "");
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

                // --- Real-time actions ---
                if (response.has("action")) {
                    String action = response.optString("action", "");
                    System.out.println("🎯 [Listener] Action received: " + response.toString(2));
                    System.out.println("🎯 Received action: " + action);

                    if (isRealTimeEvent(action)) {
                        handleRealTimeEvent(response);
                    } else {
                        TelegramClient.responseQueue.put(response);
                    }
                } else if (response.has("status") && response.has("message")) {
                    // General success/error
                    TelegramClient.responseQueue.put(response);
                } else {
                    // Fallback
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
            case "new_message", "message_edited", "message_deleted_global",
                 "user_status_changed", "added_to_group", "added_to_channel",
                 "update_group_or_channel", "chat_deleted",
                 "blocked_by_user", "unblocked_by_user", "message_seen",
                 "removed_from_group", "removed_from_channel",
                 "became_admin", "removed_admin", "ownership_transferred",
                 "admin_permissions_updated", "created_private_chat",
                 "message_reacted", "message_unreacted" , "chat_updated"-> true;
            default -> false;
        };
    }

    void handleRealTimeEvent(JSONObject response) throws IOException {
        String action = response.optString("action", "");
        JSONObject msg = response.has("data") ? response.optJSONObject("data") : new JSONObject();

        switch (action) {
            case "added_to_group", "added_to_channel",
                 "removed_from_group", "removed_from_channel",
                 "chat_deleted", "created_private_chat" -> {
                System.out.println("🔄 Chat list changed. Updating...");
                Session.forceRefreshChatList = true;
                System.out.println("🧪 Calling requestChatList() after being added");

                String chatId = msg.optString("chat_id", "");
                String chatType = msg.optString("chat_type", "");
                if (!chatId.isBlank() && !chatType.isBlank()) {
                    ActionHandler.requestChatInfo(chatId, chatType);
                }

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
                        try { handleAdminRoleChanged(msg); } catch (IOException e) { e.printStackTrace(); }
                    }).start();
                }
            }

            case "became_admin", "removed_admin", "ownership_transferred", "admin_permissions_updated" -> {
                System.out.println("🧩 Detected admin/owner role change. Calling handler...");
                new Thread(() -> {
                    try { handleAdminRoleChanged(msg); } catch (IOException e) { e.printStackTrace(); }
                }).start();
            }

            default -> displayRealTimeMessage(action, msg);
        }

        System.out.print(">> ");
    }

//    private void updateLastMessageTime(JSONObject msg) {
//        try {
//            UUID chatUUID = UUID.fromString(msg.getString("chat_id"));
//            String newTime = msg.optString("last_message_time", null);
//
//            Session.chatList.stream().filter(chat -> chat.getId().equals(chatUUID)).findFirst()
//                    .ifPresent(chat -> {
//                        chat.setLastMessageTime(newTime);
//                        System.out.println("✅ Updated last message time for chat: " + chat.getDisplayId());
//                    });
//
//            Session.activeChats.stream().filter(chat -> chat.getId().equals(chatUUID)).findFirst()
//                    .ifPresent(chat -> {
//                        chat.setLastMessageTime(newTime);
//                        System.out.println("✅ Updated last message time for chat: " + chat.getDisplayId());
//                    });
//
//            Session.archivedChats.stream().filter(chat -> chat.getId().equals(chatUUID)).findFirst()
//                    .ifPresent(chat -> {
//                        chat.setLastMessageTime(newTime);
//                        System.out.println("✅ Updated last message time for chat: " + chat.getDisplayId());
//                    });
//
//            Session.chatList.sort((c1, c2) -> {
//                if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
//                if (c1.getLastMessageTime() == null) return 1;
//                if (c2.getLastMessageTime() == null) return -1;
//                return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
//            });
//            Session.activeChats.sort((c1, c2) -> {
//                if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
//                if (c1.getLastMessageTime() == null) return 1;
//                if (c2.getLastMessageTime() == null) return -1;
//                return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
//            });
//            Session.archivedChats.sort((c1, c2) -> {
//                if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
//                if (c1.getLastMessageTime() == null) return 1;
//                if (c2.getLastMessageTime() == null) return -1;
//                return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
//            });
//
//            if (Session.inChatListMenu) {
//                ActionHandler.displayChatList();
//                System.out.print("Select a chat by number: ");
//            }
//
//        } catch (Exception e) {
//            System.out.println("❌ Failed to update last message time: " + e.getMessage());
//        }
//    }


    private void updateLastMessageTime(JSONObject msg) {
        try {
            UUID chatUUID = UUID.fromString(msg.getString("chat_id"));
            String newTime = msg.optString("last_message_time", null);
            if (newTime == null || newTime.isBlank()) return;

            updateOneList(Session.chatList, chatUUID, newTime);
            updateOneList(Session.activeChats, chatUUID, newTime);
            updateOneList(Session.archivedChats, chatUUID, newTime);

            sortByLastMessageTime(Session.chatList);
            sortByLastMessageTime(Session.activeChats);
            sortByLastMessageTime(Session.archivedChats);

            if (Session.inChatListMenu) {
                ActionHandler.displayChatList();
                System.out.print("Select a chat by number: ");
            }
        } catch (Exception e) {
            System.out.println("❌ Failed to update last message time: " + e.getMessage());
        }
    }

    private void updateOneList(List<ChatEntry> list, UUID chatUUID, String newTime) {
        if (list == null) return;
        for (ChatEntry chat : list) {
            if (chatUUID.equals(chat.getId())) {            // ✅ internal UUID
                chat.setLastMessageTime(newTime);
                System.out.println("✅ Updated last message time for chat: " + chat.getDisplayId());
                break;
            }
        }
    }

    private static void sortByLastMessageTime(java.util.List<ChatEntry> list) {
        if (list == null) return;
        list.sort((a, b) -> {
            var ta = parseTs(String.valueOf(a.getLastMessageTime()));
            var tb = parseTs(String.valueOf(b.getLastMessageTime()));
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });


        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isSavedMessages()) {
                list.add(0, list.remove(i));
                break;
            }
        }
    }

    private static java.time.LocalDateTime parseTs(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        try { return java.time.OffsetDateTime.parse(s).toLocalDateTime(); } catch (Exception ignore) {}
        try { return java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME); } catch (Exception ignore) {}
        return null;
    }



    private void handleAdminRoleChanged(JSONObject data) throws IOException {
        String chatType = data.optString("chat_type", "");
        String chatId = data.optString("group_id",
                data.optString("channel_id", data.optString("chat_id", "")));

        if (chatId.isBlank()) {
            System.out.println("⚠️ No valid ID found in real-time data: " + data.toString(2));
            return;
        }

        System.out.println("\n🔄 Your admin status changed. Updating chat info...");

        try {
            JSONObject chatInfoReq = new JSONObject()
                    .put("action", "get_chat_info")
                    .put("receiver_id", chatId)
                    .put("receiver_type", chatType);
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

            JSONObject permissionReq = new JSONObject();
            if (chatType.equalsIgnoreCase("group")) {
                permissionReq.put("action", "get_group_permissions").put("group_id", chatId);
            } else {
                permissionReq.put("action", "get_channel_permissions").put("channel_id", chatId);
            }

            JSONObject permissionResp = ActionHandler.sendWithResponse(permissionReq);
            JSONObject perm = permissionResp.getJSONObject("data");
            entry.ifPresent(chat -> chat.setPermissions(perm));

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
                String senderName = msg.optString("sender_name","Unknown");
                String content    = msg.optString("content","");
                String sendAt     = msg.optString("send_at","-");
                String chatId     = msg.optString("receiver_id", msg.optString("chat_id",""));
                String kind       = msg.optString("kind","plain");
                JSONObject meta   = msg.optJSONObject("meta");

                String prefix = "";
                if ("reply".equals(kind) && meta != null) {
                    var rt = meta.optJSONObject("reply_to");
                    prefix = "[reply → " + (rt!=null?rt.optString("excerpt",""):"") + "] ";
                } else if ("forward".equals(kind) && meta != null) {
                    var ff = meta.optJSONObject("forwarded_from");
                    prefix = "[forwarded from " + (ff!=null?ff.optString("sender_name","unknown"):"unknown") + "] ";
                }

                boolean isInCurrentChat = Session.inChatMenu &&
                        Session.currentChatId != null && Session.currentChatId.equals(chatId);

                if (isInCurrentChat) {
                    if (content.isBlank()) content = "(no content)"; // برای مدیا بدون کپشن
                    System.out.println(senderName + ": " + prefix + content + "   (" + sendAt + ")");
                } else {
                    String preview = content.isBlank() ? "[media]" : content;
                    System.out.println("💬 Message from " + senderName + ": " + prefix + preview);
                    Session.forceRefreshChatList = true;
                }
            }

            case "message_edited" -> {
                System.out.println("\n✏️ Message Edited:");
                System.out.println("ID: " + msg.optString("message_id",""));
                System.out.println("New Content: " + msg.optString("new_content",""));
                System.out.println("Edit Time: " + msg.optString("edited_at",""));
            }
            case "message_deleted_global" -> {
                System.out.println("\n🗑️ Message Deleted:");
                System.out.println("Message ID: " + msg.optString("message_id",""));
            }
            case "message_reacted", "message_unreacted" -> {
                String mid    = msg.optString("message_id","");
                String emoji  = msg.optString("emoji","");
                int n = msg.optInt("count_for_emoji", 0);
                System.out.println("\n⭐ Reaction update on " + mid + " : " + emoji + " → " + n);
            }

            case "user_status_changed" -> {
                System.out.println("\n🔄 User Status Changed:");
                System.out.println("User: " + msg.optString("user_id",""));
                System.out.println("Status: " + msg.optString("status",""));
            }
            case "blocked_by_user" -> {
                System.out.println("\n⛔ You were blocked by user: " + msg.optString("blocker_id",""));
            }
            case "unblocked_by_user" -> {
                System.out.println("\n✅ You were unblocked by user: " + msg.optString("unblocker_id",""));
            }
            case "message_seen" -> {
                System.out.println("\n👁️ Your message was seen:");
                System.out.println("Message ID: " + msg.optString("message_id",""));
                System.out.println("Seen at: " + msg.optString("seen_at",""));
            }
            default -> {
                System.out.println("\n❓ Unknown real-time action: " + action);
                System.out.println(msg.toString(2));
            }
        }
    }



}
