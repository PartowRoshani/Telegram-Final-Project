package org.to.telegramfinalproject.Client;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.util.List;


// method for save data from server response
public class Session {
    public static JSONObject currentUser;
    public static List<ChatEntry> chatList;

    public static String getUserUUID() {
        if (currentUser.has("uuid")) return currentUser.getString("uuid");
        if (currentUser.has("internal_uuid")) return currentUser.getString("internal_uuid");
        if (currentUser.has("internalUUID")) return currentUser.getString("internalUUID");
        throw new RuntimeException("❌ No UUID found in currentUser!");
    }

}