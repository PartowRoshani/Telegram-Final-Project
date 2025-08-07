package org.to.telegramfinalproject.Client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Database.PrivateChatDatabase;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Models.ContactEntry;
import org.to.telegramfinalproject.Models.SearchRequestModel;
import org.to.telegramfinalproject.Models.SearchResultModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class ActionHandler {
    private final PrintWriter out;
    private final BufferedReader in;
    private final Scanner scanner;
    public static volatile boolean forceExitChat = false;
    public static ActionHandler instance;




    private void handleRealTime(JSONObject json) throws IOException {
        IncomingMessageListener listener = new IncomingMessageListener(this.in);
        listener.handleRealTimeEvent (json);
    }
    public ActionHandler(PrintWriter out, BufferedReader in, Scanner scanner) {
        this.out = out;
        this.in = in;
        this.scanner = scanner;
        ActionHandler.instance = this;

    }

    public void loginHandler() {
        System.out.println("Login form: \n");
        System.out.print("Username: ");
        String username = this.scanner.nextLine();
        System.out.print("Password: ");
        String password = this.scanner.nextLine();

        JSONObject request = new JSONObject();
        request.put("action", "login");
        request.put("user_id", JSONObject.NULL);
        request.put("username", username);
        request.put("password", password);
        request.put("profile_name", JSONObject.NULL);

        this.send(request);

    }

    public void register() {
        System.out.println("Register form: \n");

        String username, user_id, password, profile_name;

        while (true) {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("❌ Username cannot be empty.");
                continue;
            }

            System.out.print("User id: ");
            user_id = scanner.nextLine().trim();
            if (user_id.isEmpty()) {
                System.out.println("❌ User ID cannot be empty.");
                continue;
            }

            System.out.print("Password: ");
            password = scanner.nextLine();
            if (password.isEmpty()) {
                System.out.println("❌ Password cannot be empty.");
                continue;
            }

            System.out.print("Profile name: ");
            profile_name = scanner.nextLine().trim();
            if (profile_name.isEmpty()) {
                System.out.println("❌ Profile name cannot be empty.");
                continue;
            }

            break;
        }


        JSONObject request = new JSONObject();
        request.put("action", "register");
        request.put("user_id", user_id);
        request.put("username", username);
        request.put("password", password);
        request.put("profile_name", profile_name);

        this.send(request);
    }

    public void search() {
        System.out.print("Enter keyword to search: ");
        String keyword = scanner.nextLine();

        if (Session.currentUser == null || !Session.currentUser.has("user_id")) {
            System.out.println("You must be logged in to search.");
            return;
        }

        String userId = Session.currentUser.getString("user_id");
        SearchRequestModel model = new SearchRequestModel("search", keyword, userId);
        send(model.toJson());
    }


    public void searchInUsers(){
        System.out.println("Enter keyword to search: ");
        String keyword = scanner.nextLine();
        if (Session.currentUser == null || !Session.currentUser.has("user_id")) {
            System.out.println("You must be logged in to search.");
            return;
        }
        String userId = Session.currentUser.getString("user_id");
        SearchRequestModel model = new SearchRequestModel("searchInUsers", keyword, userId);
        send(model.toJson());

    }


    public void searchEligibleUsers(String entityType, UUID entityId) {
        System.out.print("Enter keyword to search: ");
        String keyword = scanner.nextLine().trim();

        if (Session.currentUser == null || !Session.currentUser.has("user_id")) {
            System.out.println("You must be logged in to search.");
            return;
        }

        JSONObject req = new JSONObject();
        req.put("action", "searchEligibleUsers");
        req.put("keyword", keyword);
        req.put("user_id", Session.currentUser.getString("user_id"));
        req.put("entity_id", entityId.toString());
        req.put("entity_type", entityType);  // group یا channel

        JSONObject res = sendWithResponse(req);

        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ " + (res != null ? res.getString("message") : "No response received."));
            return;
        }

        JSONArray results = res.getJSONObject("data").getJSONArray("results");
        if (results.isEmpty()) {
            System.out.println("⚠️ No eligible users found.");
            return;
        }

        System.out.println("\nEligible Users:");
        for (int i = 0; i < results.length(); i++) {
            JSONObject user = results.getJSONObject(i);
            System.out.println((i + 1) + ". " + user.getString("name") + " (ID: " + user.getString("id") + ")");
        }

        System.out.print("Select a user to add (or 0 to cancel): ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            return;
        }

        if (choice < 0 || choice >= results.length()) {
            System.out.println("Cancelled.");
            return;
        }

        JSONObject selected = results.getJSONObject(choice);
        UUID targetUUID = UUID.fromString(selected.getString("uuid"));

        if (entityType.equals("group")) {
            addMemberToGroup(entityId, targetUUID);
        } else if (entityType.equals("channel")) {
            addSubscriberToChannel(entityId, targetUUID);
        }
    }


    private void addContact(UUID contactId) {
        JSONObject req = new JSONObject();
        req.put("action", "add_contact");
        req.put("user_id", Session.currentUser.getString("user_id"));
        req.put("contact_id", contactId.toString());
        send(req);
    }


    private void joinGroupOrChannel(String type, String uuid) {
        JSONObject req = new JSONObject();
        req.put("action", "join_" + type);
        req.put("user_id", Session.getUserUUID());
        req.put("id", uuid);
        send(req);
    }



    private ChatEntry fetchChatInfo(String receiverId, String receiverType) {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", receiverId);
        req.put("receiver_type", receiverType);
        out.println(req.toString());

        try {
            JSONObject response = TelegramClient.responseQueue.take();
            if (response != null && response.getString("status").equals("success")) {
                JSONObject data = response.getJSONObject("data");

                return new ChatEntry(
                        UUID.fromString(data.getString("internal_id")),
                        receiverId,
                        data.getString("name"),
                        data.optString("image_url", ""),
                        receiverType,
                        null,
                        data.optBoolean("is_owner", false),
                        data.optBoolean("is_admin", false)
                );

            }
        } catch (Exception e) {
            System.err.println("Error fetching chat info: " + e.getMessage());
        }

        //if invalid make unknown modle
        return new ChatEntry(
                UUID.randomUUID(),
                receiverId,
                "[Unknown " + receiverType + "]",
                "",
                receiverType,
                null
        );
    }


    private void refreshChatList() {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_list");
        req.put("user_id", Session.currentUser.getString("user_id"));
        out.println(req.toString());

        try {
            JSONObject response = TelegramClient.responseQueue.take();

            if (response != null && response.getString("status").equals("success")) {
                if (response.has("data") && !response.isNull("data")) {
                    JSONObject data = response.getJSONObject("data");

                    if ((!data.has("active_chat_list") || data.isNull("active_chat_list")) &&
                            (!data.has("archived_chat_list") || data.isNull("archived_chat_list"))) {
                        System.out.println("❌ No chat list found in response data.");
                        return;
                    }

                    List<ChatEntry> activeChats = new ArrayList<>();
                    List<ChatEntry> archivedChats = new ArrayList<>();

                    // 📁 Parse active chat list
                    if (data.has("active_chat_list") && !data.isNull("active_chat_list")) {
                        JSONArray activeJson = data.getJSONArray("active_chat_list");
                        for (Object obj : activeJson) {
                            JSONObject chat = (JSONObject) obj;
                            ChatEntry entry = parseChatEntry(chat);
                            activeChats.add(entry);
                        }
                    }

                    // 📁 Parse archived chat list
                    if (data.has("archived_chat_list") && !data.isNull("archived_chat_list")) {
                        JSONArray archivedJson = data.getJSONArray("archived_chat_list");
                        for (Object obj : archivedJson) {
                            JSONObject chat = (JSONObject) obj;
                            ChatEntry entry = parseChatEntry(chat);
                            archivedChats.add(entry);
                        }
                    }

                    Session.chatList = new ArrayList<>();
                    Session.chatList.addAll(activeChats);
                    Session.chatList.addAll(archivedChats);

                    Session.activeChats = activeChats;
                    Session.archivedChats = archivedChats;

                    System.out.println("✅ Chat list updated.");
                    System.out.println("📂 Active Chats: " + activeChats.size());
                    System.out.println("📁 Archived Chats: " + archivedChats.size());
                } else {
                    System.out.println("⚠️ Response has no data object.");
                }
            } else {
                if (response.has("message") && !response.isNull("message")) {
                    System.out.println("❌ Failed to refresh chat list: " + response.getString("message"));
                } else {
                    System.out.println("❌ Failed to refresh chat list.");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error during refreshChatList: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ChatEntry parseChatEntry(JSONObject chat) {
        ChatEntry entry = new ChatEntry(
                UUID.fromString(chat.getString("internal_id")),
                chat.getString("id"),
                chat.getString("name"),
                chat.optString("image_url", ""),
                chat.getString("type"),
                chat.isNull("last_message_time") ? null : LocalDateTime.parse(chat.getString("last_message_time")),
                chat.optBoolean("is_owner", false),
                chat.optBoolean("is_admin", false)
        );

        if (chat.has("other_user_id") && !chat.isNull("other_user_id")) {
            entry.setOtherUserId(UUID.fromString(chat.getString("other_user_id")));
        }

        return entry;
    }



    public void createGroup() {
        String groupId = null;

        while (groupId == null){
            System.out.print("Enter group ID: ");
            String input = scanner.nextLine();
            if (!input.trim().isEmpty()) {
                groupId = input;
            } else {
                System.out.println("Group ID can't be empty.");
            }

        }

        String groupName = null;
        while (groupName == null) {
            System.out.print("Enter group name: ");
            String input = scanner.nextLine();
            if (!input.trim().isEmpty()) {
                groupName = input;
            } else {
                System.out.println("Group name can't be empty.");
            }
        }
        System.out.print("Enter image URL (optional): ");
        String imageUrl = scanner.nextLine();

        JSONObject req = new JSONObject();
        req.put("action", "create_group");
        req.put("user_id", Session.getUserUUID());
        req.put("group_id", groupId);
        req.put("group_name", groupName);
        req.put("image_url", imageUrl.isBlank() ? JSONObject.NULL : imageUrl);

        send(req);
    }



    public void createChannel() {
        String channelId = null;

        while (channelId == null){
            System.out.print("Enter channel ID: ");
            String input = scanner.nextLine();
            if (!input.trim().isEmpty()) {
                channelId = input;
            } else {
                System.out.println("Channel ID can't be empty.");
            }

        }
        String channelName = null;
        while (channelName == null) {
            System.out.print("Enter channel name: ");
            String input = scanner.nextLine();
            if (!input.trim().isEmpty()) {
                channelName = input;
            } else {
                System.out.println("Channel name can't be empty.");
            }
        }
        System.out.print("Enter image URL (optional): ");
        String imageUrl = scanner.nextLine();

        JSONObject req = new JSONObject();
        req.put("action", "create_channel");
        req.put("user_id", Session.getUserUUID());
        req.put("channel_id", channelId);
        req.put("channel_name", channelName);
        req.put("image_url", imageUrl.isBlank() ? JSONObject.NULL : imageUrl);

        send(req);
    }

    public void addMember(UUID memberId){
        JSONObject req = new JSONObject();
        req.put("action", "add_member");
        req.put("user_id", Session.currentUser.getString("user_id"));
        //req.put("group/channel",)
        req.put("member_id", memberId.toString());
        send(req);
    }




    private void send(JSONObject request) {
        try {
            if (!request.has("action") || request.isNull("action")) {
                System.err.println("❌ Invalid request: missing action.");
                return;
            }

            String action = request.getString("action");
            this.out.println(request.toString());

            JSONObject response = null;

            while (true) {
                JSONObject incoming = TelegramClient.responseQueue.take();

                if (incoming.has("status")) {
                    response = incoming;
                    break;
                } else {
                    //real time
                    handleRealTime(incoming);
                }
            }

            if (response == null) {
                System.out.println("⚠️ No usable response received.");
                return;
            }

            if (response.has("message") && !response.isNull("message")) {
                System.out.println("✅ Server Response: " + response.getString("message"));
            }

            String status = response.optString("status", "error");
            if (!"success".equals(status) || !response.has("data") || response.isNull("data"))
                return;


            switch (action) {
                case "login":
                case "register":
                    Session.currentUser = response.getJSONObject("data");

                    JSONArray chatListJson = Session.currentUser.getJSONArray("chat_list");
                    JSONArray Archived = Session.currentUser.getJSONArray("archived_chat_list");
                    JSONArray Active = Session.currentUser.getJSONArray("active_chat_list");
                    JSONArray contactList = Session.currentUser.getJSONArray("contact_list");
                    List<ChatEntry> chatList = new ArrayList<>();


                    for (Object obj : chatListJson) {
                        JSONObject chat = (JSONObject) obj;

                        ChatEntry entry = new ChatEntry(
                                UUID.fromString(chat.getString("internal_id")),
                                chat.getString("id"),
                                chat.getString("name"),
                                chat.optString("image_url", ""),
                                chat.getString("type"),
                                chat.isNull("last_message_time") ? null : LocalDateTime.parse(chat.getString("last_message_time")),
                                chat.optBoolean("is_owner", false),
                                chat.optBoolean("is_admin", false)


                        );

                        if (chat.has("other_user_id")) {
                            entry.setOtherUserId(UUID.fromString(chat.getString("other_user_id")));
                        }

                        chatList.add(entry);

                    }

                    List<ChatEntry> archivedChats = new ArrayList<>();
                    for (Object obj : Archived){
                        JSONObject chat = (JSONObject)  obj;

                        ChatEntry entry = new ChatEntry(
                                UUID.fromString(chat.getString("internal_id")),
                                chat.getString("id"),
                                chat.getString("name"),
                                chat.optString("image_url", ""),
                                chat.getString("type"),
                                chat.isNull("last_message_time") ? null : LocalDateTime.parse(chat.getString("last_message_time")),
                                chat.optBoolean("is_owner", false),
                                chat.optBoolean("is_admin", false)
                        );
                        if (chat.has("other_user_id")) {
                            entry.setOtherUserId(UUID.fromString(chat.getString("other_user_id")));  // 👈 اضافه کردن برای private chat
                        }
                        archivedChats.add(entry);

                    }

                    List<ChatEntry> activeChats = new ArrayList<>();
                    for (Object obj : Active){
                        JSONObject chat = (JSONObject)  obj;

                        ChatEntry entry = new ChatEntry(
                                UUID.fromString(chat.getString("internal_id")),
                                chat.getString("id"),
                                chat.getString("name"),
                                chat.optString("image_url", ""),
                                chat.getString("type"),
                                chat.isNull("last_message_time") ? null : LocalDateTime.parse(chat.getString("last_message_time")),
                                chat.optBoolean("is_owner", false),
                                chat.optBoolean("is_admin", false)
                        );
                        if (chat.has("other_user_id")) {
                            entry.setOtherUserId(UUID.fromString(chat.getString("other_user_id")));  // 👈 اضافه کردن برای private chat
                        }

                        activeChats.add(entry);

                    }

                    Session.contactEntries.clear();
                    for (Object obj : contactList) {
                        JSONObject c = (JSONObject) obj;

                        ContactEntry entry = new ContactEntry(
                                UUID.fromString(c.getString("contact_id")),
                                c.getString("user_id"),
                                c.getString("profile_name"),
                                c.optString("image_url", ""),
                                c.optBoolean("is_blocked", false)
                        );
                        Session.contactEntries.add(entry);
                    }




                    Session.activeChats = activeChats;
                    Session.archivedChats = archivedChats;
                    Session.chatList = chatList;
                    break;

                case "searchInUsers":
                    JSONArray result = response.getJSONObject("data").getJSONArray("results");
                    if (result.isEmpty()){
                        System.out.println("No results found.");
                    }
                    else{
                        System.out.println("Search Results:");
                        for (int i = 0 ; i < result.length(); i++){
                            JSONObject item = result.getJSONObject(i);
                            String type = item.getString("type");
                            if(type.equals("user")){
                                System.out.println((i + 1) + ". [" + type + "] "
                                        + item.getString("name") + " (ID: " + item.getString("id") + ")");
                            }
                        }
                        System.out.print("Select a result number to interact(0 for return): ");
                        int index = Integer.parseInt(scanner.nextLine()) - 1;
                        if (index < 0 || index >= result.length()) return;
                        JSONObject selected = result.getJSONObject(index);
                        String userId = selected.getString("id");
                        String uuid = selected.getString("uuid");
                        UUID memberId = UUID.fromString(uuid);
                        addMember(memberId);
                    }
                    break;
                case "search" :
                    JSONArray results = response.getJSONObject("data").getJSONArray("results");

                    if (results.isEmpty()) {
                        System.out.println("No results found.");
                    } else {
                        System.out.println("\nSearch Results:");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject item = results.getJSONObject(i);
                            String type = item.getString("type");

                            if (type.equals("message")) {
                                String senderName = item.optString("sender_name", item.optString("sender", "Unknown"));
                                String time = item.optString("time", "Unknown time");
                                String content = item.optString("content", "[No content]");
                                String context = "";

                                if (item.has("group_name")) {
                                    context = " in group: " + item.getString("group_name");
                                } else if (item.has("channel_name")) {
                                    context = " in channel: " + item.getString("channel_name");
                                }

                                System.out.println((i + 1) + ". [message] \"" + content + "\""
                                        + " (from: " + senderName + context + ", at: " + time + ")");
                            } else {
                                System.out.println((i + 1) + ". [" + type + "] "
                                        + item.getString("name") + " (ID: " + item.getString("id") + ")");
                            }

                        }

                        System.out.print("Select a result number to interact (or 0 to exit): ");
                        int index = Integer.parseInt(scanner.nextLine()) - 1;
                        if (index == -1) {
                            System.out.println("Exit...");
                            return;
                        }
                        if (index < -1 || index >= results.length()) return;

                        JSONObject selected = results.getJSONObject(index);
                        String type = selected.getString("type");

                        switch (type) {
                            case "user" -> {
                                String uuidStr = selected.getString("uuid");
                                UUID uuid = UUID.fromString(uuidStr);

                                System.out.println("🔍 Looking for chat with internal_id='" + uuid + "' type='private'");

                                ChatEntry existing = Session.chatList.stream()
                                        .filter(c -> c.getId().equals(uuid) && c.getType().equals("private"))
                                        .findFirst()
                                        .orElse(null);

                                if (existing != null) {
                                    openChat(existing);
                               } else {
                                    ChatEntry preview = new ChatEntry();
                                    preview.setId(String.valueOf(uuid));
                                    preview.setDisplayId(selected.getString("id"));
                                    preview.setName(selected.getString("name"));
                                    preview.setType("private");

                                    openForeignChat(preview);
                                }
//                                else {
//                                    System.out.println("ℹ Trying to add contact...");
//                                    addContact(uuid);
//
//                                    refreshChatList();
//                                    System.out.println("🔄 Rechecking chat list...");
//
//                                    existing = Session.chatList.stream()
//                                            .filter(c -> c.getId().equals(uuid) && c.getType().equals("private"))
//                                            .findFirst()
//                                            .orElse(null);
//
//                                    if (existing != null) {
//                                        openChat(existing);
//                                    } else {
//                                        System.out.println("❌ Failed to open chat. Try again later.");
//                                    }
//                                }
                            }

                            case "group", "channel" -> {
                                String uuidStr = selected.getString("uuid");
                                UUID uuid = UUID.fromString(uuidStr);

                                System.out.println("🔍 Looking for chat with internal_id='" + uuid + "' type='" + type + "'");

                                ChatEntry existing = Session.chatList.stream()
                                        .filter(c -> c.getId().equals(uuid) && c.getType().equals(type))
                                        .findFirst()
                                        .orElse(null);

                                if (existing != null) {
                                    openChat(existing);
                                }else {
                                    ChatEntry preview = new ChatEntry();
                                    preview.setId(String.valueOf(uuid));
                                    preview.setDisplayId(selected.getString("id"));
                                    preview.setName(selected.getString("name"));
                                    preview.setType(type);

                                    openForeignChat(preview);
                                }
//                                else {
//                                    System.out.println("ℹ Trying to join " + type + "...");
//                                    joinGroupOrChannel(type, uuidStr);
//
//                                    refreshChatList();
//                                    System.out.println("🔄 Rechecking chat list...");
//
//                                    existing = Session.chatList.stream()
//                                            .filter(c -> c.getId().equals(uuid) && c.getType().equals(type))
//                                            .findFirst()
//                                            .orElse(null);
//
//                                    if (existing != null) {
//                                        openChat(existing);
//                                    } else {
//                                        System.out.println("❌ Failed to open " + type + ". Try again later.");
//                                    }
//                                }
                            }

                            case "message" -> {
                                String receiverId = selected.getString("receiver_id");
                                String receiverType = selected.getString("receiver_type");
                                String senderName = selected.optString("sender_name", "Unknown");
                                String content = selected.optString("content", "[No content]");
                                String time = selected.optString("time", "Unknown time");

                                String context = "";
                                if (receiverType.equals("group")) {
                                    context = "in group: " + selected.optString("group_name", "Unknown group");
                                } else if (receiverType.equals("channel")) {
                                    context = "in channel: " + selected.optString("channel_name", "Unknown channel");
                                }

                                System.out.println("📩 Message preview:");
                                System.out.println("From: " + senderName);
                                if (!context.isEmpty()) System.out.println(context);
                                System.out.println("Time: " + time);
                                System.out.println("Content: \"" + content + "\"");

                                System.out.println("🔍 Looking for chat with displayId='" + receiverId + "' type='" + receiverType + "'");

                                ChatEntry chat = Session.chatList.stream()
                                        .filter(c -> c.getDisplayId().equals(receiverId) && c.getType().equals(receiverType))
                                        .findFirst()
                                        .orElseGet(() -> fetchChatInfo(receiverId, receiverType));

                                openChat(chat);
                            }


                            default -> System.out.println("No interaction available for type: " + type);
                        }
                    }




                break;


                case "create_group":
                case "create_channel":
                    if (response.has("data")) {
                    JSONObject chatJson = response.getJSONObject("data");

                        ChatEntry chat = new ChatEntry(
                                UUID.fromString(chatJson.getString("internal_id")),
                                chatJson.getString("id"),
                                chatJson.getString("name"),
                                chatJson.optString("image_url", ""),
                                chatJson.getString("type"),
                                null,
                                chatJson.optBoolean("is_owner", false),
                                chatJson.optBoolean("is_admin", false)
                        );


                        refreshChatList();
                    System.out.println("✅ Created and opening chat...");
                    refreshChatList();
                    openChat(chat);
                }

                break;

                case "get_messages":
                    JSONArray messages = response.getJSONObject("data").getJSONArray("messages");
                    System.out.println("\n🔓 Messages fetched:");
                    System.out.println("─────────────────────────────────────────────");
                    for (int i = 0; i < messages.length(); i++) {

                        JSONObject m = messages.getJSONObject(i);
                        String senderId = m.getString("sender_id");
                        String senderName = m.optString("sender_name", "Other");
                        String content = m.getString("content");
                        String time = m.getString("send_at");

                        String label = senderId.equals(Session.currentUser.getString("internal_uuid")) ? "You" : senderName;
                        System.out.println("[" + time + "] " + label + ": " + content);
                    }
                    System.out.println("─────────────────────────────────────────────");
                    break;


                case "logout":
                    Session.currentUser = null;
                    Session.chatList = null;
                    break;
            }

        } catch (Exception e) {
            System.err.println("❌ Error during send(): " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void userMenu(UUID internal_uuid) throws IOException {
        while (true) {


            if (Session.backToChatList) {
                Session.backToChatList = false;
                return;
            }




            System.out.println("\nUser Menu:");
            System.out.println("1. Show chat list");
            System.out.println("2. Search");
            System.out.println("3. Create Channel");
            System.out.println("4. Create group");
            System.out.println("5. View contacts");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> {
                    Session.inChatListMenu = true;
                    showChatListAndSelect();
                    Session.inChatListMenu = false;
                }
                case "2" -> search();
                case "3" -> createChannel();
                case "4" -> createGroup();
                case "5" -> showContactList();
                case "6" -> {
                    logout();
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public void showContactList() {
        List<ContactEntry> contacts = Session.contactEntries;
        if (contacts.isEmpty()) {
            System.out.println("📭 You have no contacts.");
            return;
        }

        System.out.println("👥 Your Contacts:");
        for (int i = 0; i < contacts.size(); i++) {
            System.out.println((i + 1) + ". " + contacts.get(i));
        }

        System.out.print("Select a contact (0 to go back): ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == 0) return;
        if (choice < 1 || choice > contacts.size()) {
            System.out.println("❌ Invalid choice.");
            return;
        }

        ContactEntry selected = contacts.get(choice - 1);
        System.out.println("\n📇 What do you want to do with " + selected.getProfileName() + "?");
        System.out.println("1. View Profile");
        System.out.println("2. Send Message");
        System.out.print("Enter your choice: ");
        int action = scanner.nextInt();
        scanner.nextLine();

        switch (action) {
            case 1 -> viewProfile(selected.getContactId());
            case 2 -> startPrivateChat(selected);
            default -> System.out.println("❌ Invalid option.");
        }
    }

    private void viewProfile(UUID targetId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_profile");
        req.put("target_id", targetId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch profile.");
            return;
        }

        JSONObject data = res.getJSONObject("data");
        String profileName = data.getString("profile_name");
        String userId = data.getString("user_id");
        String bio = data.optString("bio", "(no bio)");
        String imageUrl = data.optString("image_url", "(no image)");
        boolean isOnline = data.getBoolean("is_online");
        String lastSeen = data.getString("last_seen");

        System.out.println("\n📄 Profile Info:");
        System.out.println("Name: " + profileName);
        System.out.println("User ID: " + userId);
        System.out.println("Bio: " + bio);
        System.out.println("Image: " + imageUrl);
        System.out.println("Status: " + (isOnline ? "🟢 Online" : "🔘 Last seen at " + lastSeen));
    }


    private void startPrivateChat(ContactEntry contact) {
        UUID myId = UUID.fromString(Session.currentUser.getString("internal_uuid"));
        UUID contactId = contact.getContactId();

        JSONObject req = new JSONObject();
        req.put("action", "get_or_create_private_chat");
        req.put("user1", myId.toString());
        req.put("user2", contactId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to create or fetch private chat.");
            return;
        }

        JSONObject data = res.getJSONObject("data");
        UUID chatId = UUID.fromString(data.getString("chat_id"));

        ChatEntry entry = new ChatEntry(
                chatId,
                contact.getUserId(),
                contact.getProfileName(),
                contact.getImageUrl(),
                "private",
                null,
                false,
                false
        );
        entry.setOtherUserId(contactId);

        Session.chatList.add(0, entry); // اضافه به اول لیست
        System.out.println("✅ Chat with " + contact.getProfileName() + " started.");

        openChat(entry); // 👈 مستقیم وارد چت شو (اختیاری)
    }



//    public void showChatListAndSelect() {
//
//
//        List<ChatEntry> chatList = Session.getChatList();
//        if (chatList.isEmpty()) {
//            System.out.println("📭 You have no chats.");
//            return;
//        }
//
//        System.out.println("\nYour Chats:");
//        for (int i = 0; i < chatList.size(); i++) {
//            ChatEntry entry = chatList.get(i);
//            String last = entry.getLastMessageTime() == null ? "No messages yet" : entry.getLastMessageTime().toString();
//            System.out.printf("%d. [%s] %s - Last: %s\n", i + 1, entry.getType(), entry.getName(), last);
//        }
//
//        System.out.print("Select a chat by number: ");
//        int choice = Integer.parseInt(scanner.nextLine());
//        if (choice < 1 || choice > chatList.size()) {
//            System.out.println("❌ Invalid choice.");
//            return;
//        }
//
//        openChat(chatList.get(choice - 1));
//    }


    public void showChatListAndSelect() {
        if (Session.activeChats == null || Session.activeChats.isEmpty()) {
            System.out.println("No active chats.");
            return;
        }

        System.out.println("\nYour Chats:");
        System.out.println("0. 📦 Archived Chats");

        for (int i = 0; i < Session.activeChats.size(); i++) {
            ChatEntry entry = Session.activeChats.get(i);
            String time = (entry.getLastMessageTime() == null)
                    ? "No messages yet"
                    : entry.getLastMessageTime().toString();
            System.out.println((i + 1) + ". [" + entry.getType() + "] " +
                    entry.getName() + " - Last: " + time);
        }

        System.out.print("Select a chat by number: ");
        int choice = Integer.parseInt(scanner.nextLine());

        if (choice == 0) {
            showArchivedChats();
            return;
        }

        int index = choice - 1;

        if (index < 0 || index >= Session.activeChats.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        ChatEntry selected = Session.activeChats.get(index);
        openChat(selected);
    }



    private void showArchivedChats() {
        if (Session.archivedChats == null || Session.archivedChats.isEmpty()) {
            System.out.println("📭 No archived chats.");
            return;
        }

        System.out.println("\n📦 Archived Chats:");
        for (int i = 0; i < Session.archivedChats.size(); i++) {
            ChatEntry entry = Session.archivedChats.get(i);
            String time = (entry.getLastMessageTime() == null)
                    ? "No messages yet"
                    : entry.getLastMessageTime().toString();
            System.out.println((i + 1) + ". [" + entry.getType() + "] " +
                    entry.getName() + " - Last: " + time);
        }

        System.out.print("Select a chat by number (or 0 to return): ");
        int choice = Integer.parseInt(scanner.nextLine());

        if (choice == 0) {
            System.out.println("🔙 Returning to main chat list...");
            return;
        }

        int index = choice - 1;

        if (index < 0 || index >= Session.archivedChats.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        ChatEntry selected = Session.archivedChats.get(index);
        openChat(selected);
    }




    private void openChat(ChatEntry chat) {

        //for private chats only
        if (chat.getType().equalsIgnoreCase("private")) {
            JSONObject reqTarget = new JSONObject();
            reqTarget.put("action", "get_private_chat_target");
            reqTarget.put("chat_id", chat.getId());

            JSONObject resTarget = sendWithResponse(reqTarget);
            if (resTarget == null || !resTarget.getString("status").equals("success")) {
                System.out.println("❌ Failed to fetch target user for private chat.");
                return;
            }
            String otherUserId = resTarget.getJSONObject("data").getString("target_id");
            chat.setOtherUserId(UUID.fromString(otherUserId));
        }



        JSONObject req = new JSONObject();
        req.put("action", "get_messages");
        req.put("receiver_id",chat.getId());
        req.put("receiver_type", chat.getType());
//        String type = chat.getType();
//        UUID chatId = chat.getId();
//        if (type.equals("private")) {
//            UUID myId = UUID.fromString(Session.currentUser.getString("internal_uuid"));
//            UUID otherUserId = chat.getId();
//
//            JSONObject getChatIdReq = new JSONObject();
//            getChatIdReq.put("action", "get_or_create_private_chat");
//            getChatIdReq.put("user1", myId.toString());
//            getChatIdReq.put("user2", otherUserId.toString());
//
//            JSONObject chatIdRes = sendWithResponse(getChatIdReq);
//            if (chatIdRes == null || !chatIdRes.getString("status").equals("success")) {
//                System.out.println("❌ Failed to fetch private chat ID.");
//                return;
//            }
//
//            Session.currentPrivateChatUserId = otherUserId;
//            chatId = UUID.fromString(chatIdRes.getJSONObject("data").getString("chat_id"));
//            chat.setId(String.valueOf(chatId));
//
//        }





        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch messages.");
            return;
        }

        JSONArray messages = res.getJSONObject("data").getJSONArray("messages");
        System.out.println("\n🔓 Messages fetched:");
        System.out.println("─────────────────────────────────────────────");
        for (int i = 0; i < messages.length(); i++) {
            JSONObject m = messages.getJSONObject(i);
            String senderId = m.getString("sender_id");
            String senderName = m.optString("sender_name", "Other");
            String content = m.getString("content");
            String time = m.getString("send_at");

            String label = senderId.equals(Session.currentUser.getString("internal_uuid")) ? "You" : senderName;
            System.out.println("[" + time + "] " + label + ": " + content);
        }
        System.out.println("─────────────────────────────────────────────");

        if (chat.getType().equals("private") && chat.getOtherUserId() == null) {
            for (ContactEntry contact : Session.contactEntries) {
                if (contact.getContactId().equals(chat.getId())) {
                    chat.setOtherUserId(contact.getContactId());
                    break;
                }
            }
        }

        boolean stayInChat = true;


        Session.currentChatId = chat.getId().toString();
        Session.currentChatEntry = chat;
        Session.currentChatType = chat.getType().toLowerCase();
        Session.inChatMenu = true;
        startMenuRefresherThread();


        while (stayInChat) {
            if (forceExitChat) {
                System.out.println("⚠️ You have been removed from this chat or chat was deleted. Returning to chat list...");
                forceExitChat = false;
                break;
            }

            System.out.println("\n📍 Entered Chat:");
            System.out.println("🔷 Name: " + chat.getName());
            System.out.println("────────────────────────────────────────────");

            switch (chat.getType().trim().toLowerCase()) {
                case "private" -> stayInChat = showPrivateChatMenu(chat);
                case "group" -> stayInChat = showGroupChatMenu(chat);
                case "channel" -> stayInChat = showChannelChatMenu(chat);
                default -> {
                    System.out.println("❗ Unknown chat type: " + chat.getType());
                    stayInChat = false;
                }
            }
        }

        Session.inChatMenu = false;
        Session.currentChatId = null;
        Session.currentChatEntry = null;
        Session.currentChatType = null;

    }





    private boolean showPrivateChatMenu(ChatEntry chat) {

        if (forceExitChat) {
            forceExitChat = false;
            System.out.println("🚪 Exiting chat due to real-time update.");
            return false;
        }


            JSONObject reqTarget = new JSONObject();
            reqTarget.put("action", "get_private_chat_target");
            reqTarget.put("chat_id", chat.getId());

            JSONObject resTarget = sendWithResponse(reqTarget);
            if (resTarget == null || !resTarget.getString("status").equals("success")) {
                System.out.println("❌ Failed to fetch target user for private chat.");
                return false;
            }

            String otherUserId = resTarget.getJSONObject("data").getString("target_id");
            chat.setOtherUserId(UUID.fromString(otherUserId));


        JSONObject req = new JSONObject();
        req.put("action", "view_profile");
        req.put("target_id", chat.getOtherUserId());  // internal UUID

        JSONObject res = sendWithResponse(req);

        if (res.getString("status").equals("success")) {
            JSONObject data = res.getJSONObject("data");

            System.out.println("\n💬 Private Chat with: " + data.getString("profile_name"));

            if (data.getBoolean("is_online")) {
                System.out.println("✅ Status: Online");
            } else {
                System.out.println("📅 Last seen: " + data.getString("last_seen"));
            }

            System.out.println("────────────────────────────────────────────");
        }


        System.out.println("1. Send message");
        System.out.println("2. Block/Unblock");
        System.out.println("3. Delete chat (one-sided)");
        System.out.println("4. Delete chat (both sides)");
        System.out.println("5. View profile");
        System.out.println("6. Archive/Unarchived");
        System.out.println("7. View messages");
        System.out.println("8. Back");


        String input = scanner.nextLine();
        switch (input) {
            case "1" -> sendMessage(chat.getId(), "private");
            case "2" -> toggleBlock(chat.getOtherUserId());
            case "3" -> {
                deleteChat(chat.getId(), false);
                return true;
            }
            case "4" -> {
                deleteChat(chat.getId(), true);
                return true;
            }
            case "5" -> {
                JSONObject reqProfile = new JSONObject();
                reqProfile.put("action", "view_profile");
                reqProfile.put("target_id", chat.getOtherUserId());

                JSONObject resProfile = sendWithResponse(reqProfile);

                if (resProfile.getString("status").equals("success")) {
                    JSONObject profile = resProfile.getJSONObject("data");
                    System.out.println("\n👤 Profile Info:");
                    System.out.println("🔷 Name: " + profile.optString("profile_name", "Unknown"));
                    System.out.println("📄 Bio: " + profile.optString("bio", "No bio set"));
                    System.out.println("🖼️ Image URL: " + profile.optString("image_url", "N/A"));

                    if (profile.getBoolean("is_online")) {
                        System.out.println("✅ Status: Online");
                    } else {
                        System.out.println("📅 Last seen: " + profile.optString("last_seen", "Unknown"));
                    }
                    System.out.println("────────────────────────────────────────────");
                } else {
                    System.out.println("❌ Could not load profile.");
                }
                return true;
            }

            case "6" ->{
                toggleArchive(chat.getId() , "private");

                return true;
            }
            case "7" ->{
                viewMessagesInChat(chat);
            }

            case "8" -> {
                return false;
            }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }


    private JSONObject getGroupPermissions(UUID groupId) {
        JSONObject req = new JSONObject();
        req.put("action", "get_group_permissions");
        req.put("group_id", groupId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            return new JSONObject();
        }
        return res.getJSONObject("data");
    }



    private boolean showGroupChatMenu(ChatEntry chat) {
        if (forceExitChat) {
            forceExitChat = false;
            System.out.println("🚪 Exiting chat due to real-time update.");
            return false;
        }

        boolean isAdmin = chat.isAdmin();
        boolean isOwner = chat.isOwner();
        JSONObject perms = getGroupPermissions(chat.getId());

        System.out.println("\n--- Group Chat Menu ---");
        System.out.println("1. Send Message");
        System.out.println("2. View Members");

        if (isOwner || (isAdmin && perms.optBoolean("can_add_members", false))) {
            System.out.println("3. Add Member");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_edit_group", false))) {
            System.out.println("4. Edit Group Info");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_add_admins", false))) {
            System.out.println("5. Add Admin");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_remove_admins", false))) {
            System.out.println("6. Remove Admin");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_remove_members", false))) {
            System.out.println("7. Remove member");
        }
        if (isOwner) {
            System.out.println("8. Delete Group");
            System.out.println("9. Leave Group (Transfer ownership required)");
        } else {
            System.out.println("9. Leave Group");
        }
        System.out.println("10. View Profile");

        if (isOwner) {
            System.out.println("11. Edit Admin Permissions");
        }

        System.out.println("12. Archive/Unarchived");
        System.out.println("13. View messages");

        System.out.println("0. Back to Chat List");

        String input = scanner.nextLine();
        switch (input) {
            case "1" -> sendMessage(chat.getId(), "group");
            case "2" -> viewGroupMembers(chat.getId());
            case "3" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_add_members", false)))
                    searchEligibleUsers("group", chat.getId());
                else System.out.println("❌ You don't have permission.");
            }
            case "4" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_edit_group", false))) editGroupInfo(chat.getId());
                else System.out.println("❌ You don't have permission.");
            }
            case "5" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_add_admins", false))) addAdminToGroup(chat.getId());
                else System.out.println("❌ You don't have permission.");
            }
            case "6" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_remove_admins", false))) removeAdminFromGroup(chat.getId());
                else System.out.println("❌ You don't have permission.");
            }
            case "7" ->{
                if(isOwner || (isAdmin && perms.optBoolean("can_remove_members",false))) removeMemberFromGroup(chat.getId());
            }
            case "8" -> {
                if (isOwner) deleteGroup(chat.getId());
                else System.out.println("❌ You don't have permission.");
                return false;
            }
            case "9" -> {
                if (isOwner) {
                    transferOwnershipAndLeave(chat.getId());
                } else {
                    leaveChat(chat.getId(), "group");
                    //refreshChatList();
                }
                return false;
            }
            case "10" ->{
                GroupInfo(chat.getId());
            }
            case "11" -> {
                if (isOwner) {
                    editAdminPermissions(chat.getId(), "group");
                }
            }

            case "12"->{
                toggleArchive(chat.getId() , "group");
            }

            case "13" ->{
                viewMessagesInChat(chat);
            }

            case "0" -> {
                return false;
            }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }




    private boolean showChannelChatMenu(ChatEntry chat) {
        if (forceExitChat) {
            forceExitChat = false;
            System.out.println("🚪 Exiting chat due to real-time update.");
            return false;
        }

        chat = fetchChatInfo(chat.getId().toString(), chat.getType());
        boolean isAdmin = chat.isAdmin();
        boolean isOwner = chat.isOwner();

        JSONObject perms = getChannelPermissions(chat.getId());

        System.out.println("\n--- Channel Menu ---");

        if (isOwner || (isAdmin && perms.optBoolean("can_post", false))) {
            System.out.println("1. Send Post");
        }

        if (isOwner || isAdmin) {
            System.out.println("2. View Subscribers");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_add_members", false))) {
            System.out.println("3. Add Subscriber");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_remove_members", false))) {
            System.out.println("4. Remove Subscriber");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_edit_channel", false))) {
            System.out.println("5. Edit Channel Info");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_add_admins", false))) {
            System.out.println("6. Add Admin");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_remove_admins", false))) {
            System.out.println("7. Remove Admin");
        }

        if (isOwner) {
            System.out.println("8. Delete Channel");
            System.out.println("9. Leave Channel (Transfer ownership required)");
        } else {
            System.out.println("8. Leave Channel");
        }

        System.out.println("10. View Profile");
        if (isOwner) {
            System.out.println("11. Edit Admin Permissions");
        }

        System.out.println("12. Archive/Unarchived");
        System.out.println("13. View messages");
        System.out.println("0. Back to Chat List");

        String input = scanner.nextLine();
        switch (input) {
            case "1" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_post", false))) {
                    sendMessage(chat.getId(), "channel");
                } else {
                    System.out.println("❌ You don't have permission to post.");
                }
            }
            case "2" -> {
                if (isOwner || isAdmin) {
                    viewChannelSubscribers(chat.getId());
                } else {
                    System.out.println("❌ You don't have permission to view subscribers.");
                }
            }
            case "3" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_add_members", false))) {
                    searchEligibleUsers("channel", chat.getId());
                } else {
                    System.out.println("❌ You don't have permission to add subscribers.");
                }
            }
            case "4" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_remove_members", false))) {
                    removeSubscriberFromChannel(chat.getId());
                } else {
                    System.out.println("❌ You don't have permission to remove subscribers.");
                }
            }
            case "5" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_edit_channel", false))) {
                    editChannelInfo(chat.getId());
                } else {
                    System.out.println("❌ You don't have permission to edit channel info.");
                }
            }
            case "6" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_add_admins", false))) {
                    addAdminToChannel(chat.getId());
                } else {
                    System.out.println("❌ You don't have permission to add admins.");
                }
            }
            case "7" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_remove_admins", false))) {
                    removeAdminFromChannel(chat.getId());
                } else {
                    System.out.println("❌ You don't have permission to remove admins.");
                }
            }
            case "8" -> {
                if (isOwner) {
                    deleteChannel(chat.getId());
                    return false;
                } else {
                    leaveChat(chat.getId(), "channel");
                    return false;
                }
            }
            case "9" -> {
                if (isOwner) {
                    transferChannelOwnershipAndLeave(chat.getId());
                    return false;
                } else {
                    System.out.println("❌ You don't have permission.");
                }
            }
            case "10"->{
                ChannelInfo(chat.getId());
            }
            case "11" -> {
                if (isOwner) {
                    editAdminPermissions(chat.getId(), "channel");
                }
            }

            case "12"->{
                toggleArchive(chat.getId() , "channel");
            }
            case "13" ->{
                viewMessagesInChat(chat);
            }

            case "0" -> {
                return false;
            }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }








    private void transferOwnershipAndLeave(UUID groupId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_group_admins");
        req.put("group_id", groupId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch admins.");
            return;
        }

        JSONArray admins = res.getJSONObject("data").getJSONArray("admins");

        if (admins.length() == 0) {
            System.out.println("⚠️ No other admins available. You cannot leave without promoting someone to owner.");
            return;
        }

        UUID currentUserId = UUID.fromString(Session.getUserUUID());
        boolean success = false;

        while (!success) {
            System.out.println("\n--- Admins List ---");
            for (int i = 0; i < admins.length(); i++) {
                JSONObject admin = admins.getJSONObject(i);
                System.out.printf("%d. %s (%s)%s\n", i + 1,
                        admin.getString("profile_name"),
                        admin.getString("user_id"),
                        admin.getString("user_id").equals(currentUserId.toString()) ? " 👑 (You)" : "");
            }
            System.out.println("0. Cancel");

            System.out.print("Select a new owner by number: ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("❌ Ownership transfer canceled.");
                return;
            }

            int choice;
            try {
                choice = Integer.parseInt(input) - 1;
            } catch (NumberFormatException e) {
                System.out.println("❗ Invalid input. Please enter a number.");
                continue;
            }

            if (choice < 0 || choice >= admins.length()) {
                System.out.println("❗ Invalid selection. Please try again.");
                continue;
            }

            JSONObject selected = admins.getJSONObject(choice);
            String newOwnerId = selected.getString("user_id");

            if (newOwnerId.equals(currentUserId.toString())) {
                System.out.println("⚠️ You cannot transfer ownership to yourself. Please select a different admin.");
                continue;
            }

            JSONObject promoteReq = new JSONObject();
            promoteReq.put("action", "transfer_group_ownership");
            promoteReq.put("group_id", groupId.toString());
            promoteReq.put("new_owner_user_id", newOwnerId);

            JSONObject promoteRes = sendWithResponse(promoteReq);
            if (promoteRes == null || !promoteRes.getString("status").equals("success")) {
                System.out.println("❌ Failed to transfer ownership.");
                return;
            }

            System.out.println("✅ Ownership transferred successfully.");
            success = true;
        }

        leaveChat(groupId, "group");
    }


    private void removeMemberFromGroup(UUID groupId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_group_members");
        req.put("group_id", groupId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch members.");
            return;
        }

        JSONArray members = res.getJSONObject("data").getJSONArray("members");
        List<JSONObject> eligible = new ArrayList<>();

        System.out.println("\n--- Members List ---");
        for (int i = 0; i < members.length(); i++) {
            JSONObject m = members.getJSONObject(i);
            String role = m.getString("role");
            String profileName = m.getString("profile_name");
            String userId = m.getString("user_id");

            if (role.equals("owner")) continue;

            eligible.add(m);
            System.out.println((eligible.size()) + ". " + profileName + " (" + userId + ") [" + role + "]");
        }

        if (eligible.isEmpty()) {
            System.out.println("⚠️ No removable members.");
            return;
        }

        System.out.print("Select a member to remove: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            return;
        }

        if (choice < 0 || choice >= eligible.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        JSONObject selected = eligible.get(choice);
        String targetInternalUUID = selected.getString("internal_uuid");

        JSONObject removeReq = new JSONObject();
        removeReq.put("action", "remove_member_from_group");
        removeReq.put("group_id", groupId.toString());
        removeReq.put("user_id", targetInternalUUID);

        JSONObject removeRes = sendWithResponse(removeReq);
        if (removeRes != null)
            System.out.println(removeRes.getString("message"));
    }



    private void addAdminToChannel(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_channel_subscribers");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch subscribers.");
            return;
        }

        JSONArray subscribers = res.getJSONObject("data").getJSONArray("subscribers");
        List<JSONObject> eligible = new ArrayList<>();

        System.out.println("\n--- Subscribers List ---");
        for (int i = 0; i < subscribers.length(); i++) {
            JSONObject s = subscribers.getJSONObject(i);
            String role = s.getString("role");
            String profileName = s.getString("profile_name");
            String userId = s.getString("user_id");

            if (role.equals("subscriber")) {
                eligible.add(s);
                System.out.printf("%d. %s (%s)\n", eligible.size(), profileName, userId);
            }
        }

        if (eligible.isEmpty()) {
            System.out.println("⚠️ No eligible subscribers to promote.");
            return;
        }

        System.out.print("Select a subscriber to promote to admin: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            return;
        }

        if (choice < 0 || choice >= eligible.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        JSONObject selected = eligible.get(choice);
        String targetInternalUUID = selected.getString("internal_uuid");

        JSONObject permissions = new JSONObject();
        System.out.print("Can post? (true/false): ");
        permissions.put("can_post", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can edit channel info? (true/false): ");
        permissions.put("can_edit_channel", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can add members? (true/false): ");
        permissions.put("can_add_members", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can remove members? (true/false): ");
        permissions.put("can_remove_members", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can add admins? (true/false): ");
        permissions.put("can_add_admins", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can remove admins? (true/false): ");
        permissions.put("can_remove_admins", Boolean.parseBoolean(scanner.nextLine()));

        JSONObject promoteReq = new JSONObject();
        promoteReq.put("action", "add_admin_to_channel");
        promoteReq.put("channel_id", channelId.toString());
        promoteReq.put("target_user_id", targetInternalUUID);
        promoteReq.put("permissions", permissions);

        JSONObject promoteRes = sendWithResponse(promoteReq);
        if (promoteRes != null)
            System.out.println(promoteRes.getString("message"));
    }




    private void addAdminToGroup(UUID groupId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_group_members");
        req.put("group_id", groupId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch members.");
            return;
        }

        JSONArray members = res.getJSONObject("data").getJSONArray("members");
        List<JSONObject> eligible = new ArrayList<>();

        System.out.println("\n--- Members List ---");
        for (int i = 0; i < members.length(); i++) {
            JSONObject m = members.getJSONObject(i);
            if (m.getString("role").equals("member")) {
                eligible.add(m);
                System.out.println((eligible.size()) + ". " + m.getString("profile_name") + " (" + m.getString("user_id") + ")");
            }
        }

        if (eligible.isEmpty()) {
            System.out.println("⚠️ No eligible members.");
            return;
        }

        System.out.print("Select a member to promote: ");
        int choice = Integer.parseInt(scanner.nextLine()) - 1;
        if (choice < 0 || choice >= eligible.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        JSONObject selected = eligible.get(choice);
        String targetInternalUUID = selected.getString("internal_uuid");

        JSONObject permissions = new JSONObject();
        System.out.print("Can add members? (true/false): ");
        permissions.put("can_add_members", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can remove members? (true/false): ");
        permissions.put("can_remove_members", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can add admins? (true/false): ");
        permissions.put("can_add_admins", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can remove admins? (true/false): ");
        permissions.put("can_remove_admins", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can edit group info? (true/false): ");
        permissions.put("can_edit_group", Boolean.parseBoolean(scanner.nextLine()));

        JSONObject promoteReq = new JSONObject();
        promoteReq.put("action", "add_admin_to_group");
        promoteReq.put("group_id", groupId.toString());
        promoteReq.put("user_id", targetInternalUUID);
        promoteReq.put("permissions", permissions);

        JSONObject promoteRes = sendWithResponse(promoteReq);
        if (promoteRes != null)
            System.out.println(promoteRes.getString("message"));
    }


    private void viewGroupMembers(UUID groupId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_group_members");
        req.put("group_id", groupId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        if (res.getString("status").equals("success")) {
            JSONArray members = res.getJSONObject("data").getJSONArray("members");

            System.out.println("\n--- Group Members ---");
            for (int i = 0; i < members.length(); i++) {
                JSONObject m = members.getJSONObject(i);
                String name = m.getString("profile_name");
                String userId = m.getString("user_id");
                String role = m.getString("role");
                System.out.printf("- %s (ID: %s) [%s]\n", name, userId, role);
            }
        } else {
            System.out.println("❌ Failed to fetch members.");
        }
    }





    private void removeSubscriberFromChannel(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_channel_subscribers");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch subscribers.");
            return;
        }

        JSONArray subscribers = res.getJSONObject("data").getJSONArray("subscribers");
        List<JSONObject> eligible = new ArrayList<>();

        System.out.println("\n--- Subscribers List ---");
        for (int i = 0; i < subscribers.length(); i++) {
            JSONObject s = subscribers.getJSONObject(i);
            String role = s.getString("role");
            String profileName = s.getString("profile_name");
            String userId = s.getString("user_id");

            if (role.equals("owner")) continue;

            eligible.add(s);
            System.out.println((eligible.size()) + ". " + profileName + " (" + userId + ") [" + role + "]");
        }

        if (eligible.isEmpty()) {
            System.out.println("⚠️ No removable subscribers.");
            return;
        }

        System.out.print("Select a subscriber to remove: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            return;
        }

        if (choice < 0 || choice >= eligible.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        JSONObject selected = eligible.get(choice);
        String targetInternalUUID = selected.getString("internal_uuid");

        JSONObject removeReq = new JSONObject();
        removeReq.put("action", "remove_subscriber_from_channel");
        removeReq.put("channel_id", channelId.toString());
        removeReq.put("user_id", targetInternalUUID);

        JSONObject removeRes = sendWithResponse(removeReq);
        if (removeRes != null)
            System.out.println(removeRes.getString("message"));
    }


    private void transferChannelOwnershipAndLeave(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_channel_admins");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch admins.");
            return;
        }

        JSONArray admins = res.getJSONObject("data").getJSONArray("admins");

        if (admins.length() == 0) {
            System.out.println("⚠️ No admins available. You cannot leave without transferring ownership.");
            return;
        }

        UUID currentUserId = UUID.fromString(Session.getUserUUID());
        boolean success = false;

        while (!success) {
            System.out.println("\n--- Admins List ---");
            for (int i = 0; i < admins.length(); i++) {
                JSONObject admin = admins.getJSONObject(i);
                System.out.printf("%d. %s (%s)%s\n", i + 1,
                        admin.getString("profile_name"),
                        admin.getString("user_id"),
                        admin.getString("internal_uuid").equals(currentUserId.toString()) ? " 👑 (You)" : "");
            }
            System.out.println("0. Cancel");

            System.out.print("Select a new owner by number: ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("❌ Ownership transfer canceled.");
                return;
            }

            int choice;
            try {
                choice = Integer.parseInt(input) - 1;
            } catch (NumberFormatException e) {
                System.out.println("❗ Invalid input. Please enter a number.");
                continue;
            }

            if (choice < 0 || choice >= admins.length()) {
                System.out.println("❗ Invalid selection. Please try again.");
                continue;
            }

            JSONObject selected = admins.getJSONObject(choice);
            String newOwnerId = selected.getString("internal_uuid");

            if (newOwnerId.equals(currentUserId.toString())) {
                System.out.println("⚠️ You cannot transfer ownership to yourself. Please select a different admin.");
                continue;
            }

            // Send transfer request
            JSONObject promoteReq = new JSONObject();
            promoteReq.put("action", "transfer_channel_ownership");
            promoteReq.put("channel_id", channelId.toString());
            promoteReq.put("new_owner_user_id", newOwnerId);

            JSONObject promoteRes = sendWithResponse(promoteReq);
            if (promoteRes == null || !promoteRes.getString("status").equals("success")) {
                System.out.println("❌ Failed to transfer ownership.");
                return;
            }

            System.out.println("✅ Ownership transferred successfully.");
            success = true; // Break loop
        }

        // Continue execution after successful ownership transfer
        leaveChat(channelId, "channel");
    }



    private void deleteChannel(UUID channelId) {
        System.out.print("Are you sure you want to delete the channel? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("yes")) {
            System.out.println("❌ Delete cancelled.");
            return;
        }

        JSONObject req = new JSONObject();
        req.put("action", "delete_channel");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res != null && res.getString("status").equals("success")) {
            System.out.println("✅ Channel deleted successfully.");
            refreshChatList();
        } else {
            System.out.println("❌ Failed to delete channel.");
        }
    }


    private void deleteGroup(UUID groupId) {
        System.out.print("Are you sure you want to delete the group? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("yes")) {
            System.out.println("❌ Delete cancelled.");
            return;
        }

        JSONObject req = new JSONObject();
        req.put("action", "delete_group");
        req.put("group_id", groupId.toString());

        JSONObject res = sendWithResponse(req);
        if (res != null && res.getString("status").equals("success")) {
            System.out.println("✅ Group deleted successfully.");
            refreshChatList();
        } else {
            System.out.println("❌ Failed to delete group.");
        }
    }

    private void deleteChat(UUID targetId, boolean both) {
        JSONObject req = new JSONObject();
        req.put("action", "delete_private_chat");
        req.put("chat_id", targetId.toString());
        req.put("both", both);
        send(req);

        JSONObject resp = sendWithResponse(req);
        if (resp.getString("status").equals("success")) {
            System.out.println("✅ Chat deleted successfully.");
        } else {
            System.out.println("⚠️ Failed to delete chat.");
        }
    }



    private void toggleBlock(UUID userId) {


        JSONObject req = new JSONObject();
        req.put("action", "toggle_block");
        req.put("user_id", Session.getUserUUID());
        req.put("target_id", userId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        String status = res.getString("status");
        String message = res.getString("message");

        if (status.equals("success")) {
            System.out.println("🔒 " + message);
        } else {
            System.out.println("❌ " + message);
        }
    }


    private void leaveChat(UUID id, String type) {
        JSONObject req = new JSONObject();
        req.put("action", "leave_chat");
        req.put("user_id", Session.getUserUUID());
        req.put("chat_id", id.toString());
        req.put("chat_type", type);

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        String status = res.getString("status");
        String message = res.getString("message");

        if (status.equals("success")) {
            System.out.println("✅ You left the chat.");
        } else {
            System.out.println("❌ " + message);
        }
    }


    private void removeAdminFromGroup(UUID groupId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_group_admins");
        req.put("group_id", groupId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch admins.");
            return;
        }

        JSONArray admins = res.getJSONObject("data").getJSONArray("admins");
        List<JSONObject> eligible = new ArrayList<>();

        System.out.println("\n--- Admins List ---");
        for (int i = 0; i < admins.length(); i++) {
            JSONObject admin = admins.getJSONObject(i);
            String role = admin.getString("role");
            String profileName = admin.getString("profile_name");
            String userId = admin.getString("user_id");

            if (!role.equals("owner")) {
                eligible.add(admin);
                System.out.printf("%d. %s (%s)\n", eligible.size(), profileName, userId);
            }
        }

        if (eligible.isEmpty()) {
            System.out.println("⚠️ No removable admins.");
            return;
        }

        System.out.print("Select an admin to remove: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            return;
        }

        if (choice < 0 || choice >= eligible.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        JSONObject selected = eligible.get(choice);
        String targetInternalUUID = selected.getString("user_id");

        JSONObject removeReq = new JSONObject();
        removeReq.put("action", "remove_admin_from_group");
        removeReq.put("group_id", groupId.toString());
        removeReq.put("target_user_id", targetInternalUUID);

        JSONObject removeRes = sendWithResponse(removeReq);
        if (removeRes != null)
            System.out.println(removeRes.getString("message"));
    }


    private void removeAdminFromChannel(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_channel_admins");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch admins.");
            return;
        }

        JSONArray admins = res.getJSONObject("data").getJSONArray("admins");
        List<JSONObject> eligible = new ArrayList<>();

        System.out.println("\n--- Admins List ---");
        for (int i = 0; i < admins.length(); i++) {
            JSONObject admin = admins.getJSONObject(i);
            String role = admin.getString("role");
            String profileName = admin.getString("profile_name");
            String userId = admin.getString("user_id");

            if (!role.equals("owner")) {
                eligible.add(admin);
                System.out.printf("%d. %s (%s)\n", eligible.size(), profileName, userId);
            }
        }

        if (eligible.isEmpty()) {
            System.out.println("⚠️ No removable admins.");
            return;
        }

        System.out.print("Select an admin to remove: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            return;
        }

        if (choice < 0 || choice >= eligible.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        JSONObject selected = eligible.get(choice);
        String targetInternalUUID = selected.getString("internal_uuid");

        JSONObject removeReq = new JSONObject();
        removeReq.put("action", "remove_admin_from_channel");
        removeReq.put("channel_id", channelId.toString());
        removeReq.put("target_user_id", targetInternalUUID);

        JSONObject removeRes = sendWithResponse(removeReq);
        if (removeRes != null)
            System.out.println(removeRes.getString("message"));
    }






    private void addMemberToGroup(UUID groupId, UUID userId) {
        JSONObject req = new JSONObject();
        req.put("action", "add_member_to_group");
        req.put("group_id", groupId.toString());
        req.put("user_id", userId.toString());

        JSONObject res = sendWithResponse(req);
        if (res != null) {
            System.out.println(res.getString("message"));
        }
    }

    private void addSubscriberToChannel(UUID channelId, UUID targetUserId) {
        JSONObject req = new JSONObject();
        req.put("action", "add_subscriber_to_channel");
        req.put("channel_id", channelId.toString());
        req.put("user_id", targetUserId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        if (res.getString("status").equals("success")) {
            System.out.println("✅ Subscriber added successfully.");
        } else {
            System.out.println("❌ " + res.getString("message"));
        }
    }

    private void GroupInfo(UUID groupId){
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", groupId.toString());
        req.put("receiver_type", "group");

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch group info.");
            return;
        }

        JSONObject data = res.getJSONObject("data");

        String currentId = data.getString("id");
        String currentName = data.getString("name");
        String currentDesc = data.optString("description", null);
        String currentImage = data.optString("image_url", null);

        System.out.println("\n--- Group Info ---");
        System.out.println("1. Group ID: " + currentId);
        System.out.println("2. Name: " + currentName);
        System.out.println("3. Description: " + currentDesc);
        System.out.println("4. Image URL: " + currentImage);
        System.out.println("────────────────────────────────────────────");


    }

    private void editGroupInfo(UUID groupId) {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", groupId.toString());
        req.put("receiver_type", "group");

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch group info.");
            return;
        }

        JSONObject data = res.getJSONObject("data");

        String currentId = data.getString("id");
        String currentName = data.getString("name");
        String currentDesc = data.optString("description", null);
        String currentImage = data.optString("image_url", null);

        System.out.println("\n--- Current Group Info ---");
        System.out.println("1. Group ID: " + currentId);
        System.out.println("2. Name: " + currentName);
        System.out.println("3. Description: " + currentDesc);
        System.out.println("4. Image URL: " + currentImage);
        System.out.println("0. Cancel");

        System.out.print("Select the field you want to edit (0-4): ");
        String choice = scanner.nextLine().trim();

        String newGroupId = currentId;
        String newName = currentName;
        String newDesc = currentDesc;
        String newImage = currentImage;

        switch (choice) {
            case "1" -> {
                System.out.print("Enter new Group ID: ");
                newGroupId = scanner.nextLine().trim();
            }
            case "2" -> {
                System.out.print("Enter new Group Name: ");
                newName = scanner.nextLine().trim();
            }
            case "3" -> {
                System.out.print("Enter new Description: ");
                newDesc = scanner.nextLine().trim();
            }
            case "4" -> {
                System.out.print("Enter new Image URL: ");
                newImage = scanner.nextLine().trim();
            }
            case "0" -> {
                System.out.println("Cancelled.");
                return;
            }
            default -> {
                System.out.println("Invalid choice.");
                return;
            }
        }

        JSONObject editReq = new JSONObject();
        editReq.put("action", "edit_group_info");
        editReq.put("group_id", groupId.toString()); // internal_uuid
        editReq.put("new_group_id", newGroupId);
        editReq.put("name", newName);
        editReq.put("description", newDesc);
        editReq.put("image_url", newImage);

        JSONObject editRes = sendWithResponse(editReq);
        if (editRes != null)
            System.out.println(editRes.getString("message"));
    }



    private void viewChannelSubscribers(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_channel_subscribers");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        if (res.getString("status").equals("success")) {
            JSONObject data = res.getJSONObject("data");
            JSONArray subs = data.getJSONArray("subscribers");

            System.out.println("--- Subscribers ---");
            for (int i = 0; i < subs.length(); i++) {
                JSONObject s = subs.getJSONObject(i);
                System.out.println("- " + s.getString("profile_name"));
            }
        } else {
            System.out.println("❌ Failed to load subscribers.");
        }
    }

    private void ChannelInfo(UUID channelInternalId){
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", channelInternalId.toString());
        req.put("receiver_type", "channel");

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch channel info.");
            return;
        }

        JSONObject data = res.getJSONObject("data");

        String currentId = data.getString("id");
        String currentName = data.getString("name");
        String currentDesc = data.optString("description", null);
        String currentImage = data.optString("image_url", null);

        System.out.println("\n--- Channel Info ---");
        System.out.println("1. Channel ID: " + currentId);
        System.out.println("2. Name: " + currentName);
        System.out.println("3. Description: " + currentDesc);
        System.out.println("4. Image URL: " + currentImage);
        System.out.println("────────────────────────────────────────────");

    }


    private void editChannelInfo(UUID channelInternalId) {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", channelInternalId.toString());
        req.put("receiver_type", "channel");

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch channel info.");
            return;
        }

        JSONObject data = res.getJSONObject("data");

        String currentId = data.getString("id");
        String currentName = data.getString("name");
        String currentDesc = data.optString("description", null);
        String currentImage = data.optString("image_url", null);

        System.out.println("\n--- Current Channel Info ---");
        System.out.println("1. Channel ID: " + currentId);
        System.out.println("2. Name: " + currentName);
        System.out.println("3. Description: " + currentDesc);
        System.out.println("4. Image URL: " + currentImage);
        System.out.println("0. Cancel");

        System.out.print("Select the field you want to edit (0-4): ");
        String choice = scanner.nextLine().trim();

        String newChannelId = currentId;
        String newName = currentName;
        String newDesc = currentDesc;
        String newImage = currentImage;

        switch (choice) {
            case "1" -> {
                System.out.print("Enter new Channel ID: ");
                newChannelId = scanner.nextLine().trim();
            }
            case "2" -> {
                System.out.print("Enter new Channel Name: ");
                newName = scanner.nextLine().trim();
            }
            case "3" -> {
                System.out.print("Enter new Description: ");
                newDesc = scanner.nextLine().trim();
            }
            case "4" -> {
                System.out.print("Enter new Image URL: ");
                newImage = scanner.nextLine().trim();
            }
            case "0" -> {
                System.out.println("Cancelled.");
                return;
            }
            default -> {
                System.out.println("Invalid choice.");
                return;
            }
        }

        JSONObject editReq = new JSONObject();
        editReq.put("action", "edit_channel_info");
        editReq.put("channel_id", channelInternalId.toString()); // internal_uuid
        editReq.put("new_channel_id", newChannelId);
        editReq.put("name", newName);
        editReq.put("description", newDesc);
        editReq.put("image_url", newImage);

        JSONObject editRes = sendWithResponse(editReq);
        if (editRes != null)
            System.out.println(editRes.getString("message"));
    }


    private JSONObject getResponse() {
        try {
            return TelegramClient.responseQueue.take();

        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to get server response");
        }
    }


    public void logout() {
        if (Session.currentUser != null && Session.currentUser.has("user_id")) {
            JSONObject request = new JSONObject();
            request.put("action", "logout");
            request.put("user_id", Session.currentUser.getString("internal_uuid"));

            send(request);
            Session.currentUser = null;
            Session.chatList = null;
        } else {
            System.out.println("Not logged in.");
        }
    }







    private void editAdminPermissions(UUID chatId, String chatType) {
        JSONObject req = new JSONObject();
        req.put("action", chatType.equals("group") ? "view_group_admins" : "view_channel_admins");
        req.put(chatType + "_id", chatId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to fetch admins.");
            return;
        }

        JSONArray admins = res.getJSONObject("data").getJSONArray("admins");
        List<JSONObject> editableAdmins = new ArrayList<>();

        System.out.println("\n--- Admins List ---");
        for (int i = 0; i < admins.length(); i++) {
            JSONObject admin = admins.getJSONObject(i);
            if (!admin.getString("role").equals("owner")) {
                editableAdmins.add(admin);
                System.out.printf("%d. %s (%s)\n", editableAdmins.size(),
                        admin.getString("profile_name"),
                        admin.getString("user_id"));
            }
        }

        if (editableAdmins.isEmpty()) {
            System.out.println("⚠️ No editable admins found.");
            return;
        }

        System.out.print("Select an admin to edit permissions: ");
        int choice;
        try {
            choice = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            return;
        }

        if (choice < 0 || choice >= editableAdmins.size()) {
            System.out.println("❌ Invalid selection.");
            return;
        }

        JSONObject selected = editableAdmins.get(choice);
        String adminId = selected.getString("user_id");

        JSONObject permissions = new JSONObject();

        if (chatType.equals("channel")) {
            System.out.print("Can post? (true/false): ");
            permissions.put("can_post", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can edit channel info? (true/false): ");
            permissions.put("can_edit_channel", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can add members? (true/false): ");
            permissions.put("can_add_members", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can remove members? (true/false): ");
            permissions.put("can_remove_members", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can add admins? (true/false): ");
            permissions.put("can_add_admins", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can remove admins? (true/false): ");
            permissions.put("can_remove_admins", Boolean.parseBoolean(scanner.nextLine()));
        } else {
            System.out.print("Can add members? (true/false): ");
            permissions.put("can_add_members", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can remove members? (true/false): ");
            permissions.put("can_remove_members", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can add admins? (true/false): ");
            permissions.put("can_add_admins", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can remove admins? (true/false): ");
            permissions.put("can_remove_admins", Boolean.parseBoolean(scanner.nextLine()));

            System.out.print("Can edit group info? (true/false): ");
            permissions.put("can_edit_group", Boolean.parseBoolean(scanner.nextLine()));
        }

        JSONObject updateReq = new JSONObject();
        updateReq.put("action", "edit_admin_permissions");
        updateReq.put("chat_id", chatId.toString());
        updateReq.put("chat_type", chatType);
        updateReq.put("admin_id", adminId);
        updateReq.put("permissions", permissions);

        JSONObject updateRes = sendWithResponse(updateReq);
        if (updateRes != null)
            System.out.println(updateRes.getString("message"));
    }


    private void viewChannelAdmins(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_channel_admins");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        if (res.getString("status").equals("success")) {
            JSONArray admins = res.getJSONObject("data").getJSONArray("admins");

            System.out.println("\n--- Channel Admins ---");
            for (int i = 0; i < admins.length(); i++) {
                JSONObject a = admins.getJSONObject(i);
                System.out.printf("- %s (%s) [%s]\n",
                        a.getString("profile_name"),
                        a.getString("user_id"),
                        a.getString("role"));
            }
        } else {
            System.out.println("❌ Failed to fetch admins.");
        }
    }


    private JSONObject getChannelPermissions(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "get_channel_permissions");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            return new JSONObject();
        }
        return res.getJSONObject("data");
    }





    public void viewAdmins(String type, UUID entityId) {
        JSONObject req = new JSONObject();
        req.put("action", type.equals("group") ? "view_group_admins" : "view_channel_admins");
        req.put(type + "_id", entityId.toString());

        send(req);
        JSONObject res = getResponse();
        if (res.getString("status").equals("success")) {
            JSONArray admins = res.getJSONObject("data").getJSONArray("admins");
            System.out.println("📋 Admins in this " + type + ":");
            for (int i = 0; i < admins.length(); i++) {
                JSONObject admin = admins.getJSONObject(i);
                System.out.printf("- %s (%s)\n", admin.getString("name"), admin.getString("role"));
            }
        } else {
            System.out.println("❌ " + res.getString("message"));
        }
    }


    public static JSONObject sendWithResponse(JSONObject request) {
        try {
            if (!request.has("action") || request.isNull("action")) {
                System.err.println("❌ Invalid request: missing action.");
                return null;
            }
            System.out.println("📤 Sending request to server: " + request.toString(2));
            System.out.println("📤 [sendWithResponse] Action: " + request.optString("action", "unknown") + ", Full: " + request.toString(2));


            String requestId = UUID.randomUUID().toString();
            request.put("request_id", requestId);

            BlockingQueue<JSONObject> queue = new LinkedBlockingQueue<>();
            TelegramClient.pendingResponses.put(requestId, queue);

            TelegramClient.getInstance().getOut().println(request.toString());

            // Wait for response
            JSONObject response = queue.take();

            TelegramClient.pendingResponses.remove(requestId);

            if (response == null) {
                System.out.println("⚠️ No response received.");
                return null;
            }

            System.out.println("✅ Server Response: " + response.getString("message"));
            return response;

        } catch (Exception e) {
            System.err.println("❌ Error during sendWithResponse(): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    public static void requestChatList() {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_list");
        req.put("user_id", Session.getUserUUID());

        JSONObject res = sendWithResponse(req);
        if (res.getString("status").equals("success")) {
            JSONArray chats = res.getJSONObject("data").getJSONArray("chat_list");
            Session.updateChatList(chats);
            System.out.println("✅ Chat list updated.");
        } else {
            System.out.println("❌ Failed to update chat list.");
        }
    }


    public static void requestChatInfo(String chatId, String chatType) throws IOException {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", chatId);
        req.put("receiver_type", chatType);
        TelegramClient.send(req);
    }




    public static void displayChatList() {
        if (Session.chatList == null || Session.chatList.isEmpty()) {
            System.out.println("\n📭 No chats available.");
            return;
        }


        System.out.println("\n💬 Your Chats:");
        System.out.println("0. 📦 Archived chats");

        int index = 1;
        for (ChatEntry chat : Session.activeChats) {
            String time = (chat.getLastMessageTime() == null)
                    ? "No messages yet"
                    : chat.getLastMessageTime().toString();

            System.out.printf("%d. [%s] %s (%s) - Last: %s\n",
                    index++, chat.getType(), chat.getName(), chat.getDisplayId(), time);
        }
    }


    public static class ChatStateMonitor implements Runnable {
        private final PrintWriter out;

        public ChatStateMonitor(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (forceExitChat) {
                        System.out.println("🚪 You were removed from the chat. Returning to chat list...");
                        forceExitChat = false;
                        Session.backToChatList = true;
                    }

                    if (Session.forceRefreshChatList) {
                        Session.forceRefreshChatList = false;
                        System.out.println("🔁 Refresh triggered by real-time event.");
                        ActionHandler.requestChatList();
                        if (Session.inChatListMenu) {
                            System.out.println("\n📬 Updated Chat List:");
                            ActionHandler.displayChatList();  //Show chats
                            System.out.print("Select a chat by number: ");
                        }
                    }

                    Thread.sleep(300);
                } catch (Exception e) {
                    System.out.println("❌ ChatStateMonitor crashed: " + e.getMessage());
                }
            }
        }
    }


    public static class CurrentChatMenuRefresher implements Runnable {
        private final ActionHandler handler;

        public CurrentChatMenuRefresher(ActionHandler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            System.out.println("🔁 Menu refresher thread running...");

            while (true) {
                try {
                    if (Session.refreshCurrentChatMenu && Session.inChatMenu && Session.currentChatId != null) {
                        System.out.println("🔁 [Refresher] Refresh requested. Searching for chat...");


                        ChatEntry chat = Session.chatList.stream()
                                .filter(e -> e.getId().toString().equals(Session.currentChatId))
                                .findFirst()
                                .orElse(null);

                        if (chat != null) {
                            Session.currentChatEntry = chat;
                            Session.refreshCurrentChatMenu = false;

                            String type = chat.getType().toLowerCase();
                            System.out.println("\n🔁 Your permissions have changed. Menu updated:");

                            if (type.equals("group")) {
                                handler.printGroupMenuOnly(chat);
                            } else if (type.equals("channel")) {
                                handler.printChannelMenuOnly(chat);
                            }

                            System.out.print("Select an option: ");
                        } else {
                            System.out.println("⚠️ No matching chat found for ID: " + Session.currentChatId);
                        }
                    }

                    Thread.sleep(200);
                } catch (Exception e) {
                    System.out.println("❌ CurrentChatMenuRefresher crashed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }





    public void printGroupMenuOnly(ChatEntry chat) {
        boolean isAdmin = chat.isAdmin();
        boolean isOwner = chat.isOwner();
        JSONObject perms = getGroupPermissions(chat.getId());

        System.out.println("\n--- Group Chat Menu (Auto-refreshed) ---");
        System.out.println("1. Send Message");
        System.out.println("2. View Members");

        if (isOwner || (isAdmin && perms.optBoolean("can_add_members", false))) {
            System.out.println("3. Add Member");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_edit_group", false))) {
            System.out.println("4. Edit Group Info");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_add_admins", false))) {
            System.out.println("5. Add Admin");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_remove_admins", false))) {
            System.out.println("6. Remove Admin");
        }
        if (isOwner || (isAdmin && perms.optBoolean("can_remove_members", false))) {
            System.out.println("7. Remove member");
        }
        if (isOwner) {
            System.out.println("8. Delete Group");
            System.out.println("9. Leave Group (Transfer ownership required)");
        } else {
            System.out.println("9. Leave Group");
        }

        System.out.println("0. Back to Chat List");
    }


    public void printChannelMenuOnly(ChatEntry chat) {
        boolean isAdmin = chat.isAdmin();
        boolean isOwner = chat.isOwner();
        JSONObject perms = getChannelPermissions(chat.getId());

        System.out.println("\n--- Channel Menu (Auto-refreshed) ---");

        if (isOwner || (isAdmin && perms.optBoolean("can_post", false))) {
            System.out.println("1. Send Post");
        }

        if (isOwner || isAdmin) {
            System.out.println("2. View Subscribers");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_add_members", false))) {
            System.out.println("3. Add Subscriber");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_remove_members", false))) {
            System.out.println("4. Remove Subscriber");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_edit_channel", false))) {
            System.out.println("5. Edit Channel Info");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_add_admins", false))) {
            System.out.println("6. Add Admin");
        }

        if (isOwner || (isAdmin && perms.optBoolean("can_remove_admins", false))) {
            System.out.println("7. Remove Admin");
        }

        if (isOwner) {
            System.out.println("8. Delete Channel");
            System.out.println("9. Leave Channel (Transfer ownership required)");
        } else {
            System.out.println("8. Leave Channel");
        }

        System.out.println("0. Back to Chat List");
    }






    private void startMenuRefresherThread() {
        Thread refresher = new Thread(() -> {
            System.out.println("🟡 Refresher tick. refreshCurrentChatMenu = " + Session.refreshCurrentChatMenu);

            while (true) {
                try {
                    Thread.sleep(500);
                    if (Session.refreshCurrentChatMenu) {
                        Session.refreshCurrentChatMenu = false;
                        if (Session.currentChatEntry == null) {
                            System.out.println("⚠️ currentChatEntry is null. Skipping...");
                            continue;
                        }


                        switch (Session.currentChatEntry.getType()) {
                            case "group" -> showGroupChatMenu(Session.currentChatEntry);
                            case "channel" -> showChannelChatMenu(Session.currentChatEntry);
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
        refresher.setDaemon(true);
        refresher.start();
    }



    private void openForeignChat(ChatEntry chat) {
        System.out.println("🔍 Opening " + chat.getType() + " chat (not in your chat list)");

        JSONObject req = new JSONObject();
        req.put("action", "get_messages");
        req.put("receiver_id", chat.getId().toString());
        req.put("receiver_type", chat.getType());
        send(req);

        switch (chat.getType().toLowerCase()) {
            case "private" -> {
                System.out.println("\n--- Private Chat ---");
                System.out.println("1. View Profile");
                System.out.println("2. Add to Contact");
                System.out.println("0. Back");
                String input = scanner.nextLine();
                switch (input) {
                    case "1" ->{
                        viewUserProfile(chat.getId());
                        openForeignChat(chat);

                    }
                    case "2" -> {
                        addContact(chat.getId());
                        refreshContactList();
                    }
                    default -> System.out.println("Back...");
                }
            }

            case "group", "channel" -> {
                System.out.println("\n--- " + chat.getType().substring(0, 1).toUpperCase() + chat.getType().substring(1) + " Preview ---");
                System.out.println("1. View Info");
                System.out.println("2. Join " + chat.getType());
                System.out.println("0. Back");
                String input = scanner.nextLine();
                switch (input) {
                    case "1" -> {
                        viewGroupOrChannelInfo(chat.getId(), chat.getType());
                        openForeignChat(chat);
                    }

                    case "2" -> {
                        joinGroupOrChannel(chat.getType(), chat.getId().toString());
                        refreshChatList();
                        ChatEntry joined = Session.chatList.stream()
                                .filter(c -> c.getId().equals(chat.getId()) && c.getType().equals(chat.getType()))
                                .findFirst().orElse(null);
                        if (joined != null) {
                            openChat(joined);
                        } else {
                            System.out.println("❌ Failed to join.");
                        }
                    }
                    default -> System.out.println("Back...");
                }
            }
        }
    }





    private void viewUserProfile(UUID userId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_profile");
        req.put("target_id", userId.toString());

        JSONObject response = sendWithResponse(req);

        if (response.getString("status").equals("success")) {
            JSONObject profile = response.getJSONObject("data");
            System.out.println("\n👤 Profile Info:");
            System.out.println("🔷 Name: " + profile.getString("profile_name"));
            System.out.println("🔷User ID: "+profile.getString("user_id"));
            System.out.println("📄 Bio: " + profile.optString("bio", "N/A"));
            System.out.println("🖼️ Image URL: " + profile.optString("image_url", "N/A"));
            if (profile.getBoolean("is_online")) {
                System.out.println("✅ Status: Online");
            } else {
                System.out.println("📅 Last seen: " + profile.optString("last_seen", "Unknown"));
            }
            System.out.println("────────────────────────────────────────────");
        } else {
            System.out.println("❌ Could not load profile.");
        }
    }

    private void viewGroupOrChannelInfo(UUID id, String type) {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", id.toString());
        req.put("receiver_type", type);

        JSONObject response = sendWithResponse(req);

        if (response.getString("status").equals("success")) {
            JSONObject data = response.getJSONObject("data");
            System.out.println("\n📢 " + type.substring(0, 1).toUpperCase() + type.substring(1) + " Info:");
            System.out.println("🔷 Name: " + data.optString("name", "N/A"));
            System.out.println("🆔 ID: " + data.optString("id", "N/A"));
            System.out.println("📄 Description: " + data.optString("description", "N/A"));
            System.out.println("🖼️ Image: " + data.optString("image_url", "N/A"));
            System.out.println("────────────────────────────────────────────");
        } else {
            System.out.println("❌ Failed to fetch " + type + " info.");
            System.out.println("✅ Server Response: " + response.getString("message"));
        }
    }



    public void toggleArchive(UUID chatId, String chatType) {
        // پیدا کردن چت از Session.chatList
        Optional<ChatEntry> optional = Session.chatList.stream()
                .filter(c -> c.getId().equals(chatId))
                .findFirst();

        if (optional.isEmpty()) {
            System.out.println("❌ Chat not found.");
            return;
        }

        ChatEntry chat = optional.get();

        if (chat.isArchived()) {
            unarchiveChat(chatId, chatType);
            chat.setArchived(false);
            System.out.println("✅ Chat unarchived.");
        } else {
            archiveChat(chatId, chatType);
            chat.setArchived(true);
            System.out.println("✅ Chat archived.");
        }

        Session.refreshChatLists();
    }


    private void archiveChat(UUID chatId, String chatType) {
        JSONObject req = new JSONObject();
        req.put("action", "archive_chat");
        req.put("chat_id", chatId.toString());
        req.put("chat_type", chatType);
        JSONObject res = sendWithResponse(req);
        if (res != null) System.out.println(res.getString("message"));
    }

    private void unarchiveChat(UUID chatId, String chatType) {
        JSONObject req = new JSONObject();
        req.put("action", "unarchive_chat");
        req.put("chat_id", chatId.toString());
        req.put("chat_type", chatType);
        JSONObject res = sendWithResponse(req);
        if (res != null) System.out.println(res.getString("message"));
    }


//    public void sendMessage(UUID receiverId, String receiverType) {
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.print("Enter your message: ");
//        String content = scanner.nextLine();
//
//        System.out.print("Enter message type (TEXT / IMAGE / VIDEO / FILE): ");
//        String messageType = scanner.nextLine();
//        Set<String> allowedTypes = Set.of("TEXT", "IMAGE", "VIDEO", "FILE");
//        while (!allowedTypes.contains(messageType.toUpperCase())) {
//            System.out.println("❌ Invalid message type. Try again (TEXT / IMAGE / VIDEO / FILE): ");
//            messageType = scanner.nextLine();
//        }
//        messageType = messageType.toUpperCase();
//
//        JSONArray attachmentsArray = new JSONArray();
//
//        System.out.print("Do you want to attach files? (yes/no): ");
//        if (scanner.nextLine().equalsIgnoreCase("yes")) {
//            while (true) {
//                System.out.print("File URL: ");
//                String fileUrl = scanner.nextLine();
//
//                System.out.print("File Type (IMAGE / VIDEO / FILE): ");
//                String fileType = scanner.nextLine();
//
//                JSONObject fileJson = new JSONObject();
//                fileJson.put("file_url", fileUrl);
//                fileJson.put("file_type", fileType);
//                attachmentsArray.put(fileJson);
//
//                System.out.print("Add another file? (yes/no): ");
//                if (!scanner.nextLine().equalsIgnoreCase("yes")) {
//                    break;
//                }
//            }
//        }
//
//
//
//        JSONObject messageJson = new JSONObject();
//        messageJson.put("action", "send_message");
//        messageJson.put("receiver_type", receiverType);
//        messageJson.put("content", content);
//        messageJson.put("message_type", messageType);
//        if (receiverType.equals("private")) {
//            messageJson.put("receiver_user_id", receiverId.toString());
//        } else {
//            messageJson.put("receiver_id", receiverId.toString());
//        }
//
//        if (!attachmentsArray.isEmpty()) {
//            messageJson.put("attachments", attachmentsArray);
//        }
//
//        JSONObject response = sendWithResponse(messageJson);
//        if (response != null && response.getString("status").equals("success")) {
//            System.out.println("✅ Message sent successfully! ID: " + response.getJSONObject("data").getString("message_id"));
//        } else {
//            System.out.println("❌ Failed to send message: " + (response != null ? response.getString("message") : "no response"));
//        }
//
//    }



    public void sendMessage(UUID chatId, String receiverType) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your message: ");
        String content = scanner.nextLine();

        System.out.print("Enter message type (TEXT / IMAGE / VIDEO / FILE): ");
        String messageType = scanner.nextLine().toUpperCase();
        Set<String> allowedTypes = Set.of("TEXT", "IMAGE", "VIDEO", "FILE");
        while (!allowedTypes.contains(messageType)) {
            System.out.print("❌ Invalid type. Try again (TEXT / IMAGE / VIDEO / FILE): ");
            messageType = scanner.nextLine().toUpperCase();
        }

        JSONArray attachmentsArray = new JSONArray();
        System.out.print("Do you want to attach files? (yes/no): ");
        if (scanner.nextLine().equalsIgnoreCase("yes")) {
            while (true) {
                System.out.print("File URL: ");
                String fileUrl = scanner.nextLine();
                System.out.print("File Type (IMAGE / VIDEO / FILE): ");
                String fileType = scanner.nextLine().toUpperCase();

                JSONObject fileJson = new JSONObject();
                fileJson.put("file_url", fileUrl);
                fileJson.put("file_type", fileType);
                attachmentsArray.put(fileJson);

                System.out.print("Add another file? (yes/no): ");
                if (!scanner.nextLine().equalsIgnoreCase("yes")) break;
            }
        }

        // 🔹 فقط ارسال پیام با chat_id و receiver_type
        JSONObject messageJson = new JSONObject();
        messageJson.put("action", "send_message");
        messageJson.put("receiver_type", receiverType);
        messageJson.put("receiver_id", chatId.toString());
        messageJson.put("content", content);
        messageJson.put("message_type", messageType);

        if (!attachmentsArray.isEmpty()) {
            messageJson.put("attachments", attachmentsArray);
        }

        JSONObject response = sendWithResponse(messageJson);
        if (response != null && response.getString("status").equals("success")) {
            System.out.println("✅ Message sent! ID: " + response.getJSONObject("data").getString("message_id"));
        } else {
            System.out.println("❌ Failed to send message: " + (response != null ? response.getString("message") : "No response"));
        }
    }

    private void refreshContactList() {
        JSONObject req = new JSONObject();
        req.put("action", "get_contact_list");
        req.put("user_id", Session.currentUser.getString("user_id"));
        out.println(req.toString());

        try {
            JSONObject response = TelegramClient.responseQueue.take();

            if (response != null && response.getString("status").equals("success")) {
                if (response.has("data") && !response.isNull("data")) {
                    JSONObject data = response.getJSONObject("data");

                    if (!data.has("contact_list") || data.isNull("contact_list")) {
                        System.out.println("❌ contact_list not found in response data.");
                        return;
                    }

                    JSONArray contactListJson = data.getJSONArray("contact_list");
                    List<ContactEntry> contactList = new ArrayList<>();

                    for (Object obj : contactListJson) {
                        JSONObject c = (JSONObject) obj;

                        ContactEntry entry = new ContactEntry(
                                UUID.fromString(c.getString("contact_id")),
                                c.getString("user_id"),
                                c.getString("profile_name"),
                                c.optString("image_url", ""),
                                c.optBoolean("is_blocked", false)
                        );

                        contactList.add(entry);
                    }

                    Session.contactEntries = contactList;
                    System.out.println("✅ Contact list updated. Total: " + contactList.size());

                } else {
                    System.out.println("⚠️ Response has no data object.");
                }
            } else {
                if (response.has("message") && !response.isNull("message")) {
                    System.out.println("❌ Failed to refresh contact list: " + response.getString("message"));
                } else {
                    System.out.println("❌ Failed to refresh contact list.");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error during refreshContactList: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewMessagesInChat(ChatEntry chat) {
        int offset = 0;
        int limit = 10;

        while (true) {
            JSONObject req = new JSONObject();
            req.put("action", "get_chat_messages");
            req.put("chat_id", chat.getId());
            req.put("chat_type", chat.getType());
            req.put("offset", offset);
            req.put("limit", limit);

            JSONObject res = sendWithResponse(req);
            if (res == null || !res.getString("status").equals("success")) {
                System.out.println("❌ Failed to fetch messages.");
                return;
            }

            JSONArray messages = res.getJSONObject("data").getJSONArray("get_chat_messages");

            if (messages.isEmpty()) {
                if (offset == 0)
                    System.out.println("📭 No messages in this chat.");
                else
                    System.out.println("📭 No more messages.");
                return;
            }

            System.out.println("\n📥 Messages:");
            System.out.println("─────────────────────────────────────────────");
            for (int i = 0; i < messages.length(); i++) {
                JSONObject msg = messages.getJSONObject(i);
                String senderName = msg.optString("sender_name", "Unknown");
                String content = msg.optString("content", "(no content)");
                String time = msg.optString("time", "");
                boolean isEdited = msg.optBoolean("is_edited", false);
                String editedAt = msg.optString("edited_at", "");
                String label = isEdited ? "🖊️ (edited at: " + editedAt + ")" : "";
                System.out.printf("[%d] [%s] %s: %s %s\n", i + 1, time, senderName, content, label);
            }
            System.out.println("─────────────────────────────────────────────");

            System.out.print("""
            💬 Options:
            [number] - Interact with message
            N - Next page (older messages)
            0 - Back to chat menu
            ➤ Choice: """);

            String input = scanner.nextLine().trim();

            if (input.equals("0")) return;
            if (input.equalsIgnoreCase("N")) {
                offset += limit;
                continue;
            }

            try {
                int index = Integer.parseInt(input);
                if (index < 1 || index > messages.length()) {
                    System.out.println("❌ Invalid message number.");
                    continue;
                }

                JSONObject selected = messages.getJSONObject(index - 1);
                UUID messageId = UUID.fromString(selected.getString("message_id"));
                UUID senderId = UUID.fromString(selected.getString("sender_id"));
                boolean isSender = senderId.toString().equals(Session.currentUser.getString("internal_uuid"));
                boolean isChannel = chat.getType().equals("channel");
                boolean isOwnerOrAdmin = chat.isOwner() || chat.isAdmin();

                System.out.println("\n🎯 Selected message by " + selected.getString("sender_name"));

                // ✔ Show interaction menu based on access
                System.out.println("2. Forward");

                if (isChannel) {
                    if (isOwnerOrAdmin) {
                        System.out.println("1. Reply");
                        System.out.println("3. React");
                        if (isSender) {
                            System.out.println("4. Edit");
                        }
                        System.out.println("5. Delete");
                    }
                } else {
                    System.out.println("1. Reply");
                    System.out.println("3. React");
                    if (isSender) {
                        System.out.println("4. Edit");
                    }
                    if (isSender || isOwnerOrAdmin) {
                        System.out.println("5. Delete");
                    }
                }

                System.out.println("0. Back to message list");
                System.out.print("➤ Select an action: ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> {
                        if (!isChannel || isOwnerOrAdmin)
                            replyToMessage(messageId);
                        else
                            System.out.println("❌ You are not allowed to reply.");
                    }
                    case "2" -> forwardMessage(messageId);
                    case "3" -> {
                        if (!isChannel || isOwnerOrAdmin)
                            reactToMessage(messageId);
                        else
                            System.out.println("❌ You are not allowed to react.");
                    }
                    case "4" -> {
                        if (isSender)
                            editMessage(messageId);
                        else
                            System.out.println("❌ You can only edit your own messages.");
                    }
                    case "5" -> {
                        if (!isChannel || isOwnerOrAdmin || isSender)
                            deleteMessage(messageId);
                        else
                            System.out.println("❌ You are not allowed to delete this message.");
                    }
                    case "0" -> {}
                    default -> System.out.println("❌ Invalid option.");
                }

            } catch (NumberFormatException e) {
                System.out.println("❌ Please enter a valid number or command.");
            }
        }
    }

    private void editMessage(UUID messageId) {
        System.out.print("📝 Enter new content: ");
        String newContent = scanner.nextLine().trim();

        if (newContent.isEmpty()) {
            System.out.println("❌ Content cannot be empty.");
            return;
        }

        JSONObject req = new JSONObject();
        req.put("action", "edit_message");
        req.put("message_id", messageId.toString());
        req.put("new_content", newContent);

        JSONObject res = sendWithResponse(req);
        if (res == null || !res.getString("status").equals("success")) {
            System.out.println("❌ Failed to edit message.");
            return;
        }

        System.out.println("✅ Message updated successfully.");
    }


    private String reactToMessage(UUID messageId) {
        return "not ready";

    }

    private String forwardMessage(UUID messageId) {
        return "not ready";
    }

    private String replyToMessage(UUID messageId) {
        return "not ready";
    }

    private void deleteMessage(UUID messageId) {
        System.out.println("\n🗑️ Delete Message Options:");
        System.out.println("1. Delete for yourself (one-sided)");
        System.out.println("2. Delete for everyone (global) [only if allowed]");
        System.out.println("0. Cancel");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> {
                JSONObject req = new JSONObject();
                req.put("action", "delete_message");
                req.put("message_id", messageId.toString());
                req.put("delete_type", "one-sided");

                JSONObject res = sendWithResponse(req);
                if (res != null && res.getString("status").equals("success")) {
                    System.out.println("✅ Message deleted for you.");
                } else {
                    System.out.println("❌ Failed to delete message.");
                }
            }

            case "2" -> {
                JSONObject req = new JSONObject();
                req.put("action", "delete_message");
                req.put("message_id", messageId.toString());
                req.put("delete_type", "global");

                JSONObject res = sendWithResponse(req);
                if (res != null && res.getString("status").equals("success")) {
                    System.out.println("✅ Message deleted for everyone.");
                } else {
                    System.out.println("❌ Failed to delete message globally.");
                    if (res != null) System.out.println("⚠️ " + res.optString("message"));
                }
            }

            case "0" -> {
                System.out.println("❎ Delete canceled.");
            }

            default -> {
                System.out.println("❌ Invalid option.");
            }
        }
    }



}


