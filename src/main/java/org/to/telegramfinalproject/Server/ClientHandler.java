package org.to.telegramfinalproject.Server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Database.*;
import org.to.telegramfinalproject.Models.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService = new AuthService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                JSONObject requestJson = new JSONObject(inputLine);

                RequestModel request = new RequestModel(
                        requestJson.optString("action"),
                        requestJson.optString("user_id"),
                        requestJson.optString("username"),
                        requestJson.optString("password"),
                        requestJson.optString("profile_name")
                );

                ResponseModel response = null;

                String action = requestJson.optString("action");

                switch (action) {
                    case "register":
                        boolean registered = authService.register(
                                request.getUser_id(),
                                request.getUsername(),
                                request.getPassword(),
                                request.getProfile_name()
                        );
                        response = registered
                                ? new ResponseModel("success", "Registration successful.")
                                : new ResponseModel("error", "Registration failed.");
                        break;

                    case "login":
                        User user = authService.login(request.getUsername(), request.getPassword());
                        if (user == null) {
                            response = new ResponseModel("error", "Login failed.");
                        }
                        else {

                            SessionManager.addUser(user.getInternal_uuid(), this.socket);
                            userDatabase.updateUserStatus(user.getInternal_uuid(), "online");



                            List<Contact> contacts = ContactDatabase.getContacts(user.getInternal_uuid());
                            List<Group> groups = GroupDatabase.getGroupsByUser(user.getInternal_uuid());
                            List<Channel> channels = ChannelDatabase.getChannelsByUser(user.getInternal_uuid());
                            List<Message> unreadMessages = MessageDatabase.getUnreadMessages(user.getInternal_uuid());

                            user.setContactList(contacts);
                            user.setGroupList(groups);
                            user.setChannelList(channels);
                            user.setUnreadMessages(unreadMessages);


                            List<ChatEntry> chatList = new ArrayList<>();

                            for (Contact contact : contacts) {
                                User target = userDatabase.findByInternalUUID(contact.getContact_id());
                                if (target == null) continue;

                                LocalDateTime last = MessageDatabase.getLastMessageTimeBetween(user.getInternal_uuid(), target.getInternal_uuid(), "private");

                                chatList.add(new ChatEntry(
                                        target.getUser_id(),
                                        target.getProfile_name(),
                                        target.getImage_url(),
                                        "private",
                                        last
                                ));
                            }

                            for (Group group : groups) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(group.getInternal_uuid(), "group");
                                chatList.add(new ChatEntry(
                                        group.getGroup_id(),
                                        group.getGroup_name(),
                                        group.getImage_url(),
                                        "group",
                                        last
                                ));
                            }

                            for (Channel channel : channels) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(channel.getInternal_uuid(), "channel");
                                chatList.add(new ChatEntry(
                                        channel.getChannel_id(),
                                        channel.getChannel_name(),
                                        channel.getImage_url(),
                                        "channel",
                                        last
                                ));
                            }

                            chatList.sort((a, b) -> {
                                if (a.getLastMessageTime() == null) return 1;
                                if (b.getLastMessageTime() == null) return -1;
                                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                            });



                            JSONObject userData = JsonUtil.userToJson(user);
                            JSONArray chatListJson = JsonUtil.chatListToJson(chatList);
                            userData.put("chat_list", chatListJson);


                            response =  new ResponseModel("success", "Welcome " + user.getProfile_name(), userData);
                        }

                        break;

                            case "logout":

                                UUID uuid = UUID.fromString(request.getUser_id());
                                userDatabase.updateUserStatus(uuid, "offline");
                                userDatabase.updateLastSeen(uuid);
                                SessionManager.removeUser(uuid);

                                response = new ResponseModel("success", "Logged out.");
                                break;

                    case "search":
                        String keyword = requestJson.optString("keyword");

                        List<JSONObject> results = new ArrayList<>();

                        // User search
                        List<User> users = new userDatabase().searchUsers(keyword);
                        for (User u : users) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "user");
                            obj.put("id", u.getUser_id());
                            obj.put("name", u.getProfile_name());
                            results.add(obj);
                        }

                        // Group search
                        List<Group> groups = GroupDatabase.searchGroups(keyword);
                        for (Group g : groups) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "group");
                            obj.put("id", g.getGroup_id());
                            obj.put("name", g.getGroup_name());
                            results.add(obj);
                        }

                        // Channel search
                        List<Channel> channels = ChannelDatabase.searchChannels(keyword);
                        for (Channel c : channels) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "channel");
                            obj.put("id", c.getChannel_id());
                            obj.put("name", c.getChannel_name());
                            results.add(obj);
                        }

                        JSONArray dataArray = new JSONArray(results);
                        JSONObject data = new JSONObject();
                        data.put("results", dataArray);
                        response = new ResponseModel("success", "Search results found", data);
                        break;

                    default:
                        response = new ResponseModel("error", "Unknown action: " + action);
                        }

                        JSONObject responseJson = new JSONObject();
                        responseJson.put("status", response.getStatus());
                        responseJson.put("message", response.getMessage());
                        responseJson.put("data",response.getData() != null ?response.getData() :JSONObject.NULL);
                        out.println(responseJson.toString());
                }

            } catch(IOException e){
                System.out.println("Connection with client lost.");

            UUID userId = SessionManager.getUserIdBySocket(this.socket);
            if (userId != null) {
                userDatabase.updateUserStatus(userId, "offline");
                userDatabase.updateLastSeen(userId);
                SessionManager.removeUser(userId);
            }
            }
        }



}

