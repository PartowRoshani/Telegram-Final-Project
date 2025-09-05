package org.to.telegramfinalproject.Client;

import javafx.application.Platform;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.UI.MainController;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class IncomingMessageListener implements Runnable {
    private final BufferedReader in;
    public enum UIMode { CONSOLE, UI }
    private final UIMode uiMode; // runtime mode

    private final java.util.Set<String> seenMessageIds =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());


    public IncomingMessageListener(BufferedReader in, UIMode uiMode) {
        this.in = in;
        this.uiMode = uiMode;
    }

//    public IncomingMessageListener(BufferedReader in) {
//        this.in = in;
//    }

    @Override
    public void run() {
        try {
            System.out.println("👂 Real-Time Listener started.");

            while (true) {

                if (TelegramClient.mediaBusy.get()) {
                    try { Thread.sleep(15); } catch (InterruptedException ignored) {}
                    continue;
                }

                String line = in.readLine();
                if (line == null) break;
                if (line.isBlank()) continue;

                final JSONObject response;
                try {
                    response = new JSONObject(line);
                } catch (Exception badJson) {
                    System.out.println("⚠️ [Listener] Non-JSON line ignored: " + line);
                    continue;
                }

                System.out.println("📥 Received raw line: " + line);

                // 1) اول message_id را روت کن (برای ACK نهایی مدیا)
                String mid = response.optString("message_id", "");
                if (!mid.isEmpty()) {
                    BlockingQueue<JSONObject> q = TelegramClient.pendingResponses.get(mid);
                    if (q != null) {
                        q.put(response);
                        continue; // مصرف شد
                    }
                }

                // 2) بعد request_id را روت کن (برای INIT و بقیه درخواست‌ها)
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

                // 3) رویدادهای real-time
                if (response.has("action")) {
                    String action = response.getString("action");
                    System.out.println("🎯 [Listener] Action received: " + response.toString(2));
                    System.out.println("🎯 Received action: " + action);

                    if (isRealTimeEvent(action)) {
                        handleRealTimeEvent(response);
                    } else {
                        TelegramClient.responseQueue.put(response);
                    }
                    continue;
                }

                // 4) سایر پاسخ‌های عمومی
                if (response.has("status") && response.has("message")) {
                    TelegramClient.responseQueue.put(response);
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
            case "new_message",
                 "message_edited",
                 "message_deleted_global", "message_deleted_one_sided", "message_deleted",
                 "message_reacted", "message_unreacted",
                 "user_status_changed",
                 "added_to_group", "added_to_channel",
                 "update_group_or_channel", "chat_deleted",
                 "blocked_by_user", "unblocked_by_user", "message_seen",
                 "removed_from_group", "removed_from_channel",
                 "became_admin", "removed_admin", "ownership_transferred",
                 "admin_permissions_updated",
                 "created_private_chat",
                 "chat_updated" -> true;
            default -> false;
        };
    }


    void handleRealTimeEvent(JSONObject response) throws IOException {
        String action = response.getString("action");
        JSONObject msg = response.has("data") ? response.getJSONObject("data") : new JSONObject();

        JSONObject finalMsg = msg;
        JSONObject finalMsg1 = msg;
        switch (action) {
//            case "added_to_group", "added_to_channel",
//                 "removed_from_group", "removed_from_channel",
//                 "chat_deleted", "created_private_chat" -> {
//                // این قسمت مستقل از UI/کنسول است
//                System.out.println("🔄 Chat list changed. Updating...");
//                Session.forceRefreshChatList = true;
//
//                String chatId = msg.getString("chat_id");
//                String chatType = msg.getString("chat_type");
//                ActionHandler.requestChatInfo(chatId, chatType);
//
//                if (action.equals("removed_from_group") || action.equals("removed_from_channel") || action.equals("chat_deleted")) {
//                    System.out.println("🚫 You were removed from the chat or chat was deleted. Exiting...");
//                    ActionHandler.forceExitChat = true;
//                }
//            }

//            case "chat_updated" -> {
//                if (uiMode == UIMode.UI) {
//                    bumpChatListFromUpdate(msg);   // برای UI (سایدبار و سورت)
//                } else {
//                    updateLastMessageTime(msg);    // برای کنسول (لیست‌های Session)
//                }
//            }

            case "added_to_group":
            case "added_to_channel":
            case "created_private_chat": {
                // داده‌ها
                UUID chatId   = UUID.fromString(msg.getString("chat_id"));
                String type   = msg.getString("chat_type");      // "group" | "channel" | "private"
                String name   = msg.optString("name", "");
                String imgUrl = msg.optString("image_url", "");

                // یک ChatEntry مینیمال بساز (تا UI سریع واکنش بده)
                ChatEntry ce = new ChatEntry();
                ce.setId(chatId.toString());
                ce.setType(type);
                ce.setName(name);
                ce.setImageUrl(imgUrl);

                Platform.runLater(() -> {
                    var mc = MainController.getInstance();
                    if (mc == null) return;

                    // به لیست‌ها اضافه و UI را رفرش می‌کند (متد خودت)
                    mc.onJoinedOrAdded(ce);

                    // اگر همین چت الان بازه، مود مناسب را اعمال کن
                    var cpc = mc.getChatPageController();
                    if (cpc != null && cpc.isSameChat(chatId, type)) {
                        // ❗ اگر applyMode در ChatPageController private است،
                        // یا publicش کن یا این دو خط را حذف کن.
                        // گروه → NORMAL ، کانال → READ_ONLY (مگر اینکه اجازه پست داشته باشی)
                        // cpc.applyMode("group".equalsIgnoreCase(type) ? ChatViewMode.NORMAL : ChatViewMode.READ_ONLY);
                        // cpc.fetchAndRenderHeader(ce); // اختیاری: هدر را تازه کن
                    }
                });
                break;
            }

            case "removed_from_group":
            case "removed_from_channel": {
                UUID chatId = UUID.fromString(msg.getString("chat_id"));
                String type = msg.getString("chat_type");

                Platform.runLater(() -> {
                    var mc = MainController.getInstance();
                    if (mc == null) return;

                    removeFromAllLists(chatId);
                    mc.refreshChatListUI();

                    // اگر همین چت باز است → به حالت نیاز به Join برگرد
                    var cpc = mc.getChatPageController();
                    if (cpc != null && cpc.isSameChat(chatId, type)) {
                        // اگر applyMode private است، این خط را کامنت کن یا publicش کن
                        // cpc.applyMode(ChatViewMode.NEEDS_JOIN);
                    }
                });
                break;
            }

            case "chat_deleted": {
                UUID chatId = UUID.fromString(msg.getString("chat_id"));
                String type = msg.getString("chat_type");

                Platform.runLater(() -> {
                    var mc = MainController.getInstance();
                    if (mc == null) return;

                    removeFromAllLists(chatId);
                    mc.refreshChatListUI();

                    var cpc = mc.getChatPageController();
                    if (cpc != null && cpc.isSameChat(chatId, type)) {
                        // حداقل ورودی را ببندیم/غیرفعال کنیم
                        // اگر applyMode private است، این خط را کامنت کن یا publicش کن
                        // cpc.applyMode(ChatViewMode.READ_ONLY);
                        // و یک پیام سیستمی هم نشان بده
                        cpc.addSystemMessage("This chat was deleted.");
                    }
                });
                break;
            }


            case "became_admin", "removed_admin", "ownership_transferred", "admin_permissions_updated" : {
                System.out.println("🧩 Detected admin/owner role change. Calling handler...");
                new Thread(() -> {
                    try {
                        handleAdminRoleChanged(finalMsg1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            case "new_message" :{
                JSONObject data = response.optJSONObject("data");
                if (data == null) break;

                String mid = data.optString("id", data.optString("message_id",""));
                if (mid.isEmpty()) break;
                if (!seenMessageIds.add(mid)) break;

                bumpChatListFromMessage(data);

                JSONObject uiMsg = new JSONObject(data.toString());
                if (!uiMsg.has("message_id") && uiMsg.has("id")) {
                    uiMsg.put("message_id", uiMsg.getString("id"));
                }

                Platform.runLater(() -> {
                    var mc = MainController.getInstance();
                    var chatCtl = (mc != null) ? mc.getChatPageController() : null;
                    if (chatCtl != null) {
                        chatCtl.onRealTimeNewMessage(uiMsg);
                    }
                });
            }

            case "message_edited": {
                JSONObject ui = normalizeMessageId(msg);
                // (اختیاری) اگر ایونت زمان و چت را هم می‌دهد، می‌توانی چت‌لیست را آپدیت کنی
                Platform.runLater(() -> {
                    var mc = MainController.getInstance();
                    var chatCtl = (mc != null) ? mc.getChatPageController() : null;
                    if (chatCtl != null) chatCtl.onRealTimeMessageEdited(ui);
                });
            }

            case "message_deleted_global", "message_deleted_one_sided", "message_deleted" : {
                JSONObject ui = normalizeMessageId(msg);
                Platform.runLater(() -> {
                    var mc = MainController.getInstance();
                    var chatCtl = (mc != null) ? mc.getChatPageController() : null;
                    if (chatCtl != null) chatCtl.onRealTimeMessageDeleted(ui);
                });
            }

            case "message_reacted", "message_unreacted" : {
                JSONObject ui = normalizeMessageId(msg);
                Platform.runLater(() -> {
                    var mc = MainController.getInstance();
                    var chatCtl = (mc != null) ? mc.getChatPageController() : null;
                    if (chatCtl != null) chatCtl.onRealTimeReaction(ui);
                });
            }



            case "chat_updated": {
                var data = response.getJSONObject("data");
                bumpChatListFromUpdate(data);
            }

            case "user_status_changed" : {

                displayRealTimeMessage(action, msg);
                Platform.runLater(() -> {
                    var mc = org.to.telegramfinalproject.UI.MainController.getInstance();
                    var cp = (mc != null) ? mc.getChatPageController() : null;
                    if (cp != null) cp.onUserStatusChanged(
                            msg.optString("user_id",""),
                            msg.optString("status",""),
                            msg.optString("last_seen","")
                    );
                });
            }




            case "blocked_by_user", "unblocked_by_user", "message_seen" : {
                displayRealTimeMessage(action, msg);
            }



            default :{
                System.out.println("\n❓ Unknown real-time action: " + action);
                System.out.println(msg.toString(2));
            }
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

            Session.activeChats.stream()
                    .filter(chat -> chat.getId().equals(chatUUID))
                    .findFirst()
                    .ifPresent(chat -> {
                        chat.setLastMessageTime(newTime);
                        System.out.println("✅ Updated last message time for chat: " + chat.getDisplayId());
                    });

            Session.archivedChats.stream()
                    .filter(chat -> chat.getId().equals(chatUUID))
                    .findFirst()
                    .ifPresent(chat -> {
                        chat.setLastMessageTime(newTime);
                        System.out.println("✅ Updated last message time for chat: " + chat.getDisplayId());
                    });

            Session.chatList.sort((c1, c2) -> {
                if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
                if (c1.getLastMessageTime() == null) return 1;
                if (c2.getLastMessageTime() == null) return -1;
                return c2.getLastMessageTime().compareTo(c1.getLastMessageTime()); // descending
            });
            Session.activeChats.sort((c1, c2) -> {
                if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
                if (c1.getLastMessageTime() == null) return 1;
                if (c2.getLastMessageTime() == null) return -1;
                return c2.getLastMessageTime().compareTo(c1.getLastMessageTime()); // descending
            });
            Session.archivedChats.sort((c1, c2) -> {
                if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
                if (c1.getLastMessageTime() == null) return 1;
                if (c2.getLastMessageTime() == null) return -1;
                return c2.getLastMessageTime().compareTo(c1.getLastMessageTime()); // descending
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
                String senderName = msg.optString("sender_name","Unknown");
                String content    = msg.optString("content","(empty)");
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
                    System.out.println(senderName + ": " + prefix + content + "   (" + sendAt + ")");
                } else {
                    System.out.println("💬 Message from " + senderName + ": " + prefix + content);
                    Session.forceRefreshChatList = true;
                }
            }


            case "message_edited" -> {
                System.out.println("\n✏️ Message Edited:");
                System.out.println("ID: " + msg.getString("message_id"));
                System.out.println("New Content: " + msg.getString("new_content"));
                System.out.println("Edit Time: " + msg.getString("edited_at"));
            }
            case "message_deleted_global" -> {
                System.out.println("\n🗑️ Message Deleted:");
                System.out.println("Message ID: " + msg.getString("message_id"));
            }
            case "message_reacted", "message_unreacted" -> {
                String mid    = msg.getString("message_id");
                String emoji  = msg.getString("emoji");
                JSONObject counts = msg.optJSONObject("counts");
                int n = msg.optInt("count_for_emoji", 0);
                System.out.println("\n⭐ Reaction update on " + mid + " : " + emoji + " → " + n);
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



    private static LocalDateTime parseIsoFlexible(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return LocalDateTime.parse(iso); } catch (Exception ignore) {}
        try { return OffsetDateTime.parse(iso).toLocalDateTime(); } catch (Exception ignore) {}
        return null;
    }



    private void bumpChatListFromUpdate(JSONObject data) {
        try {
            UUID chatId = UUID.fromString(data.optString("chat_id",""));
            String chatType = data.optString("chat_type","");
            LocalDateTime ts = parseIsoFlexible(data.optString("last_message_time", null));

            Platform.runLater(() -> {
                var mc = MainController.getInstance();
                if (mc != null) mc.onChatUpdated(chatId, chatType, ts, /*isIncoming*/ false, null);
            });
        } catch (Exception e) { System.err.println("[RT] bumpChatListFromUpdate: " + e.getMessage()); }
    }



    private String previewOf(String type, String content) {
        String t = type == null ? "" : type.trim().toUpperCase();
        return switch (t) {
            case "IMAGE" -> "[Image]";
            case "AUDIO" -> "[Audio]";
            case "VIDEO" -> "[Video]";
            case "FILE"  -> "[File]";
            default      -> (content == null ? "" : content);
        };
    }

    private void bumpChatListFromMessage(JSONObject m) {
        try {
            UUID chatId = UUID.fromString(m.optString("receiver_id",""));
            String chatType = m.optString("receiver_type","");
            LocalDateTime ts = parseIsoFlexible(m.optString("send_at", null));

            String preview = previewOf(m.optString("message_type","TEXT"),
                    m.optString("content",""));

            Platform.runLater(() -> {
                var mc = MainController.getInstance();
                if (mc != null) mc.onChatUpdated(chatId, chatType, ts, /*isIncoming*/ true, preview);
            });
        } catch (Exception e) { System.err.println("[RT] bumpChatListFromMessage: " + e.getMessage()); }
    }

    // --- add this helper ---
    private static JSONObject normalizeMessageId(JSONObject j) {
        if (j == null) return new JSONObject();
        if (!j.has("message_id") && j.has("id")) {
            JSONObject copy = new JSONObject(j.toString());
            copy.put("message_id", copy.optString("id", ""));
            return copy;
        }
        return j;
    }


    private void removeFromAllLists(UUID chatId) {
        if (Session.chatList != null) {
            Session.chatList.removeIf(c -> chatId.toString().equals(String.valueOf(c.getId())));
        }
        if (Session.activeChats != null) {
            Session.activeChats.removeIf(c -> chatId.toString().equals(String.valueOf(c.getId())));
        }
        if (Session.archivedChats != null) {
            Session.archivedChats.removeIf(c -> chatId.toString().equals(String.valueOf(c.getId())));
        }
    }


}