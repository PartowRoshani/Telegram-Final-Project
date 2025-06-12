package org.to.telegramfinalproject.Client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;
import org.to.telegramfinalproject.Models.ContactRequestModel;
import org.to.telegramfinalproject.Models.SearchRequestModel;
import org.to.telegramfinalproject.Models.SearchResultModel;

import java.io.BufferedReader;
import java.io.IOException;
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
        req.put("user_id", Session.currentUser.getString("internalUUID")); // ✅ internal_uuid لازم است
        req.put("id", uuid);  // ✅ این هم internal_uuid است
        send(req);
    }



    private ChatEntry fetchChatInfo(String receiverId, String receiverType) {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_info");
        req.put("receiver_id", receiverId);
        req.put("receiver_type", receiverType);
        out.println(req.toString());

        try {
            String responseText = in.readLine();
            if (responseText != null) {
                JSONObject response = new JSONObject(responseText);
                if (response.getString("status").equals("success")) {
                    JSONObject data = response.getJSONObject("data");
                    return new ChatEntry(
                            receiverId,
                            data.getString("name"),
                            data.optString("image_url", ""),
                            receiverType,
                            null
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching chat info: " + e.getMessage());
        }

        return new ChatEntry(receiverId, "[Unknown " + receiverType + "]", "", receiverType, null);
    }


    private void refreshChatList() {
        JSONObject req = new JSONObject();
        req.put("action", "get_chat_list");
        req.put("user_id", Session.currentUser.getString("user_id"));
        out.println(req.toString());

        try {
            String responseText = in.readLine();
            if (responseText != null) {
                JSONObject response = new JSONObject(responseText);
                if (response.getString("status").equals("success")) {
                    JSONArray chatListJson = response.getJSONObject("data").getJSONArray("chat_list");
                    List<ChatEntry> chatList = new ArrayList<>();

                    for (Object obj : chatListJson) {
                        JSONObject chat = (JSONObject) obj;
                        ChatEntry entry = new ChatEntry(
                                chat.getString("id"),
                                chat.getString("name"),
                                chat.optString("image_url", ""),
                                chat.getString("type"),
                                chat.isNull("last_message_time")
                                        ? null
                                        : LocalDateTime.parse(chat.getString("last_message_time"))
                        );
                        chatList.add(entry);
                    }

                    Session.chatList = chatList;
                    System.out.println("✅ Chat list updated.");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to refresh chat list: " + e.getMessage());
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
        req.put("group_id", groupId);                               // ID قابل نمایش
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






    private void send(JSONObject request) {
        try {
            if (!request.has("action") || request.isNull("action")) {
                System.err.println("Error: Request does not contain 'action'.");
                return;
            }

            String action = request.getString("action");
            this.out.println(request.toString());

            String responseText = this.in.readLine();
            if (responseText == null) {
                System.out.println("No response from server.");
                return;
            }

            JSONObject response = new JSONObject(responseText);
            System.out.println("Server response: " + response.getString("message"));

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
                                chat.getString("id"),
                                chat.getString("name"),
                                chat.optString("image_url", ""),
                                chat.getString("type"),
                                chat.isNull("last_message_time") ? null :
                                        LocalDateTime.parse(chat.getString("last_message_time"))
                        );
                        chatList.add(entry);
                    }

                    Session.chatList = chatList;
                    break;

                case "search":
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

                        System.out.print("Select a result number to interact: ");
                        int index = Integer.parseInt(scanner.nextLine()) - 1;
                        if (index < 0 || index >= results.length()) return;

                        JSONObject selected = results.getJSONObject(index);
                        String type = selected.getString("type");

                        switch (type) {
                            case "user" -> {
                                String userId = selected.getString("id");
                                String uuid = selected.getString("uuid");

                                ChatEntry existing = Session.chatList.stream()
                                        .filter(c -> c.getId().equals(userId) && c.getType().equals("private"))
                                        .findFirst()
                                        .orElse(null);

                                if (existing != null) {
                                    openChat(existing);
                                } else {
                                    UUID contactId = UUID.fromString(uuid);
                                    addContact(contactId);
                                    ChatEntry newChat = fetchChatInfo(contactId.toString(), "private");
                                    refreshChatList();
                                    openChat(newChat);
                                }
                            }

                            case "group", "channel" -> {
                                String id = selected.getString("id");
                                String uuid = selected.getString("uuid");

                                ChatEntry existing = Session.chatList.stream()
                                        .filter(c -> c.getId().equals(id) && c.getType().equals(type))
                                        .findFirst()
                                        .orElse(null);

                                if (existing != null) {
                                    openChat(existing);
                                } else {
                                    joinGroupOrChannel(type, uuid);

                                    ChatEntry newChat = fetchChatInfo(id, type);
                                    refreshChatList();
                                    openChat(newChat);
                                }
                            }


                            case "message" -> {
                                String receiverId = selected.getString("receiver_id");
                                String receiverType = selected.getString("receiver_type");

                                ChatEntry chat = Session.chatList.stream()
                                        .filter(c -> c.getId().equals(receiverId) && c.getType().equals(receiverType))
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
                    if (status.equals("success") && response.has("data")) {
                        JSONObject chatJson = response.getJSONObject("data");
                        ChatEntry chat = new ChatEntry(
                                chatJson.getString("id"),
                                chatJson.getString("name"),
                                chatJson.optString("image_url", ""),
                                chatJson.getString("type"),
                                null
                        );
                        refreshChatList();
                        System.out.println("✅ Created and opening chat...");
                        openChat(chat);
                    }
                    break;




                case "get_messages":
                    // Optional: handle later
                    break;
            }

        } catch (IOException e) {
            System.err.println("Error communicating with server: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
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

        if (choice < 0 || choice >= Session.chatList.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        ChatEntry selected = Session.chatList.get(choice);
        openChat(selected);
    }

    private void openChat(ChatEntry chat) {
        JSONObject request = new JSONObject();
        request.put("action", "get_messages");
        request.put("receiver_id", chat.getId());
        request.put("receiver_type", chat.getType());
        send(request);
    }

    public void logout() {
        if (Session.currentUser != null && Session.currentUser.has("user_id")) {
            JSONObject request = new JSONObject();
            request.put("action", "logout");
            request.put("user_id", Session.currentUser.getString("internalUUID"));

            send(request);
            Session.currentUser = null;
            Session.chatList = null;
        } else {
            System.out.println("Not logged in.");
        }
    }



}
