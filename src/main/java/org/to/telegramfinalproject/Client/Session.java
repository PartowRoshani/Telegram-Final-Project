package org.to.telegramfinalproject.Client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Models.ContactEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// method for save data from server response
public class Session {
    public static JSONObject currentUser;
    public static List<ChatEntry> chatList = new ArrayList<>();
    public static List<ChatEntry> archivedChats = new ArrayList<>();
    public static List<ChatEntry> activeChats = new ArrayList<>();
    public static UUID currentPrivateChatUserId = null;
    public static volatile boolean forceRefreshChatList = false;
    public static volatile boolean backToChatList = false;
    public static boolean inChatListMenu = false;
    public static String currentChatType = null;
    public static volatile boolean inChatMenu = false;
    public static volatile boolean refreshCurrentChatMenu = false;
    public static String currentChatId = null;
    public static ChatEntry currentChatEntry = null;
    public static List<ContactEntry> contactEntries = new ArrayList<>();
    public static boolean inContactListMenu = false;
    public static DownloadsIndex downloadsIndex = null;

    public static String getUserUUID() {
        if (currentUser.has("uuid")) return currentUser.getString("uuid");
        if (currentUser.has("internal_uuid")) return currentUser.getString("internal_uuid");
        if (currentUser.has("internalUUID")) return currentUser.getString("internalUUID");
        throw new RuntimeException("❌ No UUID found in currentUser!");
    }

//    public static void updateChatList(JSONArray chatArray) {
//        chatList.clear();
//        for (int i = 0; i < chatArray.length(); i++) {
//            JSONObject obj = chatArray.getJSONObject(i);
//            ChatEntry entry = new ChatEntry(
//                    UUID.fromString(obj.getString("internal_id")),
//                    obj.optString("id", ""),  // displayId
//                    obj.optString("name", ""),  // name
//                    obj.optString("image_url", ""),
//                    obj.getString("type"),
//                    null,  // last message time (if needed, parse it)
//                    obj.optBoolean("is_owner", false),
//                    obj.optBoolean("is_admin", false)
//            );
//            entry.setPermissions(obj.optJSONObject("permissions"));
//            chatList.add(entry);
//        }
//    }


    public static void updateChatList(JSONArray chatArray) {
        chatList.clear();
        for (int i = 0; i < chatArray.length(); i++) {
            JSONObject obj = chatArray.getJSONObject(i);

            LocalDateTime lastMessageTime = null;
            if (obj.has("last_message_time") && !obj.isNull("last_message_time")) {
                String timeStr = obj.getString("last_message_time");
                if (!timeStr.isBlank()) {
                    lastMessageTime = LocalDateTime.parse(timeStr);
                }
            }

            ChatEntry entry = new ChatEntry(
                    UUID.fromString(obj.getString("internal_id")),
                    obj.optString("id", ""),  // displayId
                    obj.optString("name", ""),  // name
                    obj.optString("image_url", ""),
                    obj.getString("type"),
                    lastMessageTime,
                    obj.optBoolean("is_owner", false),
                    obj.optBoolean("is_admin", false)
            );

            entry.setPermissions(obj.optJSONObject("permissions"));
            chatList.add(entry);
        }

        chatList.sort((c1, c2) -> {
            if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
            if (c1.getLastMessageTime() == null) return 1;
            if (c2.getLastMessageTime() == null) return -1;
            return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
        });

        activeChats = chatList.stream().filter(c -> !c.isArchived()).toList();
        archivedChats = chatList.stream().filter(ChatEntry::isArchived).toList();
    }

    public static List<ChatEntry> getChatList() {
        return chatList;
    }


    public static void refreshChatLists() {
        Session.activeChats = Session.chatList.stream()
                .filter(c -> !c.isArchived())
                .toList();

        Session.archivedChats = Session.chatList.stream()
                .filter(ChatEntry::isArchived)
                .toList();
    }

    public void setDownloadIndex(DownloadsIndex idx){ this.downloadsIndex = idx; }


    // ==== ADD to Session class ====

    /** پیدا کردن چت با internal_id */
    public static ChatEntry findChatByInternalId(UUID internalId) {
        for (ChatEntry c : chatList) {
            if (c.getId().equals(internalId)) return c;
        }
        return null;
    }

    /** درج در ابتدای لیست (با جلوگیری از دوبل) و سپس resort + refresh */
    public static void prependChat(ChatEntry entry) {
        // اگر قبلاً تو لیست هست، اول پاکش کن
        chatList.removeIf(c -> c.getId().equals(entry.getId()));
        // اول لیست بذار
        chatList.add(0, entry);
        resortAndRefresh();
    }

    /** سورت بر اساس lastMessageTime (نزولی) و آپدیت active/archived */
    public static void resortAndRefresh() {
        chatList.sort((c1, c2) -> {
            if (c1.getLastMessageTime() == null && c2.getLastMessageTime() == null) return 0;
            if (c1.getLastMessageTime() == null) return 1;
            if (c2.getLastMessageTime() == null) return -1;
            return c2.getLastMessageTime().compareTo(c1.getLastMessageTime());
        });
        refreshChatLists();
    }


    public static ChatEntry upsertSavedMessages(UUID chatId, String name, String chatType, String imageUrlIfAny) {
        ChatEntry existing = findChatByInternalId(chatId);
        if (existing != null) {
            // برای اینکه بیاد بالا، می‌تونی زمان آخرین پیام رو الان بذاری (اختیاری)
            if (existing.getLastMessageTime() == null) {
                existing.setLastMessageTime(LocalDateTime.now());
                resortAndRefresh();
            }
            return existing;
        }

        ChatEntry entry = new ChatEntry(
                chatId,
                "",                         // displayId برای Saved لازم نیست
                (name == null || name.isBlank()) ? "Saved Messages" : name,
                imageUrlIfAny == null ? "" : imageUrlIfAny,
                (chatType == null || chatType.isBlank()) ? "private" : chatType,
                LocalDateTime.now(),        // بذار بالا بیاد
                true,                       // owner? برای self-chat می‌تونه true باشه (اثری روی permission نداره)
                true                        // admin? اختیاری—تأثیر خاصی نداره؛ می‌تونی false بذاری
        );
        // اگر توی UI برای Saved Messages آیکن خاص داری، اینجا ست کن:
        // entry.setImageUrl("/org/to/telegramfinalproject/Icons/saved_messages_dark.png");

        prependChat(entry);
        return entry;
    }

}