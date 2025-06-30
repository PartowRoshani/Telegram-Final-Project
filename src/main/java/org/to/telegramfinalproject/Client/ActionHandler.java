package org.to.telegramfinalproject.Client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Models.SearchRequestModel;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;

public class ActionHandler {
    private final PrintWriter out;
    private final BufferedReader in;
    private final Scanner scanner;

    public ActionHandler(PrintWriter out, BufferedReader in, Scanner scanner) {
        this.out = out;
        this.in = in;
        this.scanner = scanner;
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
        System.out.print("Username: ");
        String username = this.scanner.nextLine();
        System.out.print("User id: ");
        String user_id = this.scanner.nextLine();
        System.out.print("Password: ");
        String password = this.scanner.nextLine();
        System.out.print("Profile name: ");
        String profile_name = this.scanner.nextLine();

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

        // اگر شکست خورد، internalId نامعتبر می‌سازیم (برای جلوگیری از null)
        return new ChatEntry(
                UUID.randomUUID(),                    // ساخت یک UUID موقت (ولی اشتباه)
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
            if (response != null) {
                if (response.getString("status").equals("success")) {
                    JSONArray chatListJson = response.getJSONObject("data").getJSONArray("chat_list");
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

                        chatList.add(entry);
                    }

                    Session.chatList = chatList;
                    System.out.println("✅ Chat list updated.");
                } else {
                    System.out.println("❌ Failed to refresh chat list: " + response.getString("message"));
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error during refreshChatList: " + e.getMessage());
            e.printStackTrace();
        }
    }




    public void createGroup() {
        System.out.print("Enter group ID: ");
        String groupId = scanner.nextLine();
        System.out.print("Enter group name: ");
        String groupName = scanner.nextLine();
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
        System.out.print("Enter channel ID: ");
        String channelId = scanner.nextLine();
        System.out.print("Enter channel name: ");
        String channelName = scanner.nextLine();
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

            JSONObject response = TelegramClient.responseQueue.take();

            if (response == null) {
                System.out.println("⚠️ No response received.");
                return;
            }

            System.out.println("✅ Server Response: " + response.getString("message"));

            String status = response.getString("status");
            if (!"success".equals(status) || !response.has("data") || response.isNull("data"))
                return;

            switch (action) {
                case "login":
                case "register":
                    Session.currentUser = response.getJSONObject("data");

                    JSONArray chatListJson = Session.currentUser.getJSONArray("chat_list");
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



                        chatList.add(entry);
                    }


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
                                System.out.println((i + 1) + ". [message] \"" + item.getString("content") + "\""
                                        + " (from: " + item.optString("sender", "N/A") + ", at: " + item.getString("time") + ")");
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
                                    System.out.println("ℹ Trying to add contact...");
                                    addContact(uuid);

                                    refreshChatList();
                                    System.out.println("🔄 Rechecking chat list...");

                                    existing = Session.chatList.stream()
                                            .filter(c -> c.getId().equals(uuid) && c.getType().equals("private"))
                                            .findFirst()
                                            .orElse(null);

                                    if (existing != null) {
                                        openChat(existing);
                                    } else {
                                        System.out.println("❌ Failed to open chat. Try again later.");
                                    }
                                }
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
                                } else {
                                    System.out.println("ℹ Trying to join " + type + "...");
                                    joinGroupOrChannel(type, uuidStr);

                                    refreshChatList();
                                    System.out.println("🔄 Rechecking chat list...");

                                    existing = Session.chatList.stream()
                                            .filter(c -> c.getId().equals(uuid) && c.getType().equals(type))
                                            .findFirst()
                                            .orElse(null);

                                    if (existing != null) {
                                        openChat(existing);
                                    } else {
                                        System.out.println("❌ Failed to open " + type + ". Try again later.");
                                    }
                                }
                            }

                            case "message" -> {
                                String receiverId = selected.getString("receiver_id");
                                String receiverType = selected.getString("receiver_type");

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
                        String content = m.getString("content");
                        String time = m.getString("send_at");

                        String label = senderId.equals(Session.currentUser.getString("internal_uuid")) ? "You" : "Other";
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


    public void userMenu(UUID internal_uuid) {
        while (true) {
            System.out.println("\nUser Menu:");
            System.out.println("1. Show chat list");
            System.out.println("2. Search");
            System.out.println("3. Create Channel");
            System.out.println("4. Create group");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> showChatListAndSelect();
                case "2" -> search();
                case "3" -> createChannel();
                case "4" -> createGroup();
                case "5" -> {
                    logout();
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public void showChatListAndSelect() {
        if (Session.chatList == null || Session.chatList.isEmpty()) {
            System.out.println("No chats available.");
            return;
        }

        System.out.println("\nYour Chats:");
        for (int i = 0; i < Session.chatList.size(); i++) {
            ChatEntry entry = Session.chatList.get(i);
            String time = (entry.getLastMessageTime() == null)
                    ? "No messages yet"
                    : entry.getLastMessageTime().toString();
            System.out.println((i + 1) + ". [" + entry.getType() + "] " +
                    entry.getName() + " - Last: " + time);
        }

        System.out.print("Select a chat by number: ");
        int choice = Integer.parseInt(scanner.nextLine()) - 1;

        if(choice == -1){
            System.out.println("Exit...");
            return;
        }
        if (choice < -1 || choice >= Session.chatList.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        ChatEntry selected = Session.chatList.get(choice);
        openChat(selected);
    }

    private void openChat(ChatEntry chat) {
        JSONObject req = new JSONObject();
        req.put("action", "get_messages");
        req.put("receiver_id", chat.getId());
        req.put("receiver_type", chat.getType());
        send(req);

        boolean stayInChat = true;

        while (stayInChat) {
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
    }





    private boolean showPrivateChatMenu(ChatEntry chat) {
        System.out.println("1. Send message");
        System.out.println("2. Block/Unblock");
        System.out.println("3. Delete chat (one-sided)");
        System.out.println("4. Delete chat (both sides)");
        System.out.println("5. Back");


        String input = scanner.nextLine();
        switch (input) {
            case "1" -> sendMessageTo(chat.getId(), "private");
            case "2" -> toggleBlock(chat.getId());
            case "3" -> {
                deleteChat(chat.getId(), false);
                return false;
            }
            case "4" -> {
                deleteChat(chat.getId(), true);
                return false;
            }
            case "5" -> {
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
            return new JSONObject(); // پیش‌فرض خالی یعنی هیچ پرمیشنی
        }
        return res.getJSONObject("data");
    }



    private boolean showGroupChatMenu(ChatEntry chat) {
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
        if (isOwner) {
            System.out.println("7. Delete Group");
        }
        System.out.println("8. Leave Group");
        System.out.println("0. Back to Chat List");

        String input = scanner.nextLine();
        switch (input) {
            case "1" -> sendMessageTo(chat.getId(), "group");
            case "2" -> viewGroupMembers(chat.getId());
            case "3" -> {
                if (isOwner || (isAdmin && perms.optBoolean("can_add_members", false))) addMemberToGroup(chat.getId());
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
            case "7" -> {
                if (isOwner) deleteGroup(chat.getId());
                else System.out.println("❌ You don't have permission.");
                return false;
            }
            case "8" -> {
                leaveChat(chat.getId(), "group");
                return false;
            }
            case "0" -> {
                return false;
            }
            default -> System.out.println("Invalid choice.");
        }
        return true;
    }




    private boolean showChannelChatMenu(ChatEntry chat) {
        boolean isAdmin = chat.isAdmin();
        boolean isOwner = chat.isOwner();

        System.out.println("\n--- Channel Menu ---");

        if (isOwner || isAdmin)
            System.out.println("1. Send Post");

        System.out.println("2. View Subscribers");

        if (isOwner || isAdmin) {
            System.out.println("3. Add Admin");
            System.out.println("4. Edit Channel Info");
        }

        System.out.println("5. Unsubscribe");
        System.out.println("0. Back to Chat List");

        String input = scanner.nextLine();
        switch (input) {
            case "1" -> {
                if (isOwner || isAdmin) sendMessageTo(chat.getId(), "channel");
                else System.out.println("❌ You are not allowed to post.");
            }
            case "2" -> viewChannelSubscribers(chat.getId());
            case "3" -> {
                if (isOwner) addAdminToChannel(chat.getId());
                else System.out.println("❌ Only owner can add admins.");
            }
            case "4" -> {
                if (isOwner || isAdmin) editChannelInfo(chat.getId());
                else System.out.println("❌ You don't have permission.");
            }
            case "5" -> {
                leaveChat(chat.getId(), "channel");
                return false; // خروج کامل از چت
            }
            case "0" -> {
                return false; // بازگشت به لیست چت‌ها
            }
            default -> System.out.println("Invalid choice.");
        }
        return true; // همچنان در منو باقی بمان
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
        String targetInternalUUID = selected.getString("internal_uuid"); // دقت کن internal_uuid

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
        promoteReq.put("user_id", targetInternalUUID); // ارسال internal_uuid واقعی
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



    private void removeAdminFromGroup(UUID groupId) {
        System.out.print("Enter user_id to remove from admin: ");
        String userId = scanner.nextLine().trim();

        JSONObject req = new JSONObject();
        req.put("action", "remove_admin_from_group");
        req.put("group_id", groupId.toString());
        req.put("user_id", userId);

        JSONObject res = sendWithResponse(req);
        if (res != null)
            System.out.println(res.getString("message"));
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
        req.put("target_id", targetId.toString());
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

        if (res.getBoolean("success")) {
            System.out.println("✅ You left the chat.");
        } else {
            System.out.println("❌ " + res.getString("message"));
        }
    }

    private void sendMessageTo(UUID id, String type) {
        System.out.print("Enter message: ");
        String text = scanner.nextLine().trim();

        if (text.isEmpty()) {
            System.out.println("Message cannot be empty.");
            return;
        }

        JSONObject req = new JSONObject();
        req.put("action", "send_message");
        req.put("sender_id", Session.getUserUUID());
        req.put("receiver_id", id.toString());
        req.put("receiver_type", type);
        req.put("text", text);

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        if (res.getBoolean("success")) {
            System.out.println("✅ Message sent.");
        } else {
            System.out.println("❌ Failed to send message.");
        }
    }

    private void addMemberToGroup(UUID groupId) {
        System.out.print("Enter user_id to add: ");
        String userId = scanner.nextLine().trim();

        JSONObject req = new JSONObject();
        req.put("action", "add_member_to_group");
        req.put("group_id", groupId.toString());
        req.put("user_id", userId);

        JSONObject res = sendWithResponse(req);
        if (res != null)
            System.out.println(res.getString("message"));
    }

    private void editGroupInfo(UUID groupId) {
        System.out.print("Enter new group name: ");
        String newName = scanner.nextLine().trim();

        System.out.print("Enter new group description: ");
        String newDesc = scanner.nextLine().trim();

        JSONObject req = new JSONObject();
        req.put("action", "edit_group_info");
        req.put("group_id", groupId.toString());
        req.put("name", newName);
        req.put("description", newDesc);

        JSONObject res = sendWithResponse(req);
        if (res != null)
            System.out.println(res.getString("message"));
    }


    private void viewChannelSubscribers(UUID channelId) {
        JSONObject req = new JSONObject();
        req.put("action", "view_channel_subscribers");
        req.put("channel_id", channelId.toString());

        JSONObject res = sendWithResponse(req);
        if (res == null) return;

        if (res.getBoolean("success")) {
            JSONArray subs = res.getJSONArray("subscribers");
            System.out.println("--- Subscribers ---");
            for (int i = 0; i < subs.length(); i++) {
                JSONObject s = subs.getJSONObject(i);
                System.out.println("- " + s.getString("profile_name"));
            }
        } else {
            System.out.println("❌ Failed to load subscribers.");
        }
    }

    private void addAdminToChannel(UUID channelId) {
        System.out.print("Enter user_id to promote: ");
        String userId = scanner.nextLine().trim();

        JSONObject req = new JSONObject();
        req.put("action", "add_admin_to_channel");
        req.put("channel_id", channelId.toString());
        req.put("user_id", userId);

        JSONObject res = sendWithResponse(req);
        if (res != null)
            System.out.println(res.getString("message"));
    }

    private void editChannelInfo(UUID channelId) {
        System.out.print("Enter new channel name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter new description: ");
        String desc = scanner.nextLine().trim();

        JSONObject req = new JSONObject();
        req.put("action", "edit_channel_info");
        req.put("channel_id", channelId.toString());
        req.put("name", name);
        req.put("description", desc);

        JSONObject res = sendWithResponse(req);
        if (res != null)
            System.out.println(res.getString("message"));
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



    public void processIncomingEvents() {
        try {
            while (in.ready()) {
                String line = in.readLine();
                if (line == null) continue;

                JSONObject response = new JSONObject(line);
                if (!response.has("action")) continue;

                String action = response.getString("action");

                switch (action) {
                    case "new_message" -> {
                        JSONObject msg = response.getJSONObject("data");
                        System.out.println("\n🔔 New Message:");
                        System.out.println("From: " + msg.getString("sender"));
                        System.out.println("Time: " + msg.getString("time"));
                        System.out.println("Content: " + msg.getString("content"));
                        System.out.print(">> ");
                    }

                    case "user_status_changed" -> {
                        JSONObject msg = response.getJSONObject("data");
                        System.out.println("\n🔄 User Status Changed:");
                        System.out.println("User: " + msg.getString("user_id"));
                        System.out.println("Status: " + msg.getString("status"));
                        System.out.print(">> ");
                    }

                    case "update_group_or_channel" -> {
                        JSONObject data = response.getJSONObject("data");
                        System.out.println("\n📢 " + data.getString("chat_type") + " updated: " + data.getString("new_name"));
                        System.out.print(">> ");
                    }


                    default -> {
                        if (!action.equals("search")) {
                            System.out.println("\n❓ Unknown action received: " + action);
                            System.out.print(">> ");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("🔴 Failed to process event: " + e.getMessage());
        }
    }


    public void addAdminToEntity(String type, UUID entityId) {
        System.out.print("Enter user ID to promote to admin: ");
        String targetUserId = scanner.nextLine().trim();

        JSONObject permissions = new JSONObject();
        System.out.print("Can send messages? (true/false): ");
        permissions.put("can_send", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can edit info? (true/false): ");
        permissions.put("can_edit", Boolean.parseBoolean(scanner.nextLine()));

        JSONObject req = new JSONObject();
        req.put("action", type.equals("group") ? "add_admin_to_group" : "add_admin_to_channel");
        req.put(type + "_id", entityId.toString());
        req.put("target_user_id", targetUserId);
        req.put("permissions", permissions);

        send(req);
        JSONObject res = getResponse();
        System.out.println(res.getString("message"));
    }


    public void editAdminPermissions(String type, UUID entityId) {
        System.out.print("Enter user ID of the admin to edit: ");
        String targetUserId = scanner.nextLine().trim();

        JSONObject permissions = new JSONObject();
        System.out.print("Can send messages? (true/false): ");
        permissions.put("can_send", Boolean.parseBoolean(scanner.nextLine()));
        System.out.print("Can edit info? (true/false): ");
        permissions.put("can_edit", Boolean.parseBoolean(scanner.nextLine()));

        JSONObject req = new JSONObject();
        req.put("action", type.equals("group") ? "edit_group_admin_permissions" : "edit_channel_admin_permissions");
        req.put(type + "_id", entityId.toString());
        req.put("target_user_id", targetUserId);
        req.put("permissions", permissions);

        send(req);
        JSONObject res = getResponse();
        System.out.println(res.getString("message"));
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






    private JSONObject sendWithResponse(JSONObject request) {
        try {
            if (!request.has("action") || request.isNull("action")) {
                System.err.println("❌ Invalid request: missing action.");
                return null;
            }

            String action = request.getString("action");
            this.out.println(request.toString());

            JSONObject response = TelegramClient.responseQueue.take();

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


}
