package org.to.telegramfinalproject.Client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// method for save data from server response
public class Session {
    public static JSONObject currentUser;
    public static List<ChatEntry> chatList = new ArrayList<>();
    public static List<ChatEntry> archivedChats = new ArrayList<>();
    public static List<ChatEntry> activeChats = new ArrayList<>();

    public static volatile boolean forceRefreshChatList = false;
    public static volatile boolean backToChatList = false;
    public static boolean inChatListMenu = false;
    public static String currentChatType = null;
    public static volatile boolean inChatMenu = false;
    public static volatile boolean refreshCurrentChatMenu = false;
    public static String currentChatId = null;
    public static ChatEntry currentChatEntry = null;





    public static String getUserUUID() {
        if (currentUser.has("uuid")) return currentUser.getString("uuid");
        if (currentUser.has("internal_uuid")) return currentUser.getString("internal_uuid");
        if (currentUser.has("internalUUID")) return currentUser.getString("internalUUID");
        throw new RuntimeException("❌ No UUID found in currentUser!");
    }

    public static void updateChatList(JSONArray chatArray) {
        chatList.clear();
        for (int i = 0; i < chatArray.length(); i++) {
            JSONObject obj = chatArray.getJSONObject(i);
            ChatEntry entry = new ChatEntry(
                    UUID.fromString(obj.getString("internal_id")),
                    obj.optString("id", ""),  // displayId
                    obj.optString("name", ""),  // name
                    obj.optString("image_url", ""),
                    obj.getString("type"),
                    null,  // last message time (if needed, parse it)
                    obj.optBoolean("is_owner", false),
                    obj.optBoolean("is_admin", false)
            );
            entry.setPermissions(obj.optJSONObject("permissions")); // اگر permissions وجود داره
            chatList.add(entry);
        }
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
}