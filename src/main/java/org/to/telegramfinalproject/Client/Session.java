package org.to.telegramfinalproject.Client;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// method for save data from server response
public class Session {
    public static JSONObject currentUser;
    public static List<ChatEntry> chatList;
    public static List<UUID> archivedChatIds = new ArrayList<>();


    public static String getUserUUID() {
        if (currentUser.has("uuid")) return currentUser.getString("uuid");
        if (currentUser.has("internal_uuid")) return currentUser.getString("internal_uuid");
        if (currentUser.has("internalUUID")) return currentUser.getString("internalUUID");
        throw new RuntimeException("❌ No UUID found in currentUser!");
    }

}