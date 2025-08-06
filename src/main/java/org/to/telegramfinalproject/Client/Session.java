package org.to.telegramfinalproject.Client;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


// method for save data from server response
public class Session {
    public static JSONObject currentUser;
    public static List<ChatEntry> chatList;
    //Receive message : thread added
    public static Thread receiverThread = null;
    public static String activeChatId = null;
    // Map for unread message counts per chat
    public static Map<String, Integer> unreadMessagesMap = new HashMap<>();
}