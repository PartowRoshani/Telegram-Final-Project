package org.to.telegramfinalproject.Server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Database.*;
import org.to.telegramfinalproject.Models.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService = new AuthService();
    private User currentUser;


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        UUID userId = null;

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                JSONObject requestJson = new JSONObject(inputLine);
                String action = requestJson.getString("action");
                ResponseModel response = null;
                System.out.println("Received action: '" + action + "'");
                System.out.println("Full request: " + requestJson.toString());
                if (!requestJson.has("action") || requestJson.isNull("action")) {
                    ResponseModel errorResponse = new ResponseModel("error", "Missing 'action' in request.");
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("status", errorResponse.getStatus());
                    responseJson.put("message", errorResponse.getMessage());
                    responseJson.put("data", JSONObject.NULL);
                    out.println(responseJson.toString());
                    continue;
                }

                switch (action) {
                    case "register":
                    case "login": {
                        RequestModel request = new RequestModel(
                                action,
                                requestJson.optString("user_id"),
                                requestJson.optString("username"),
                                requestJson.optString("password"),
                                requestJson.optString("profile_name")
                        );

                        if (action.equals("register")) {
                            boolean registered = authService.register(
                                    request.getUser_id(),
                                    request.getUsername(),
                                    request.getPassword(),
                                    request.getProfile_name()
                            );
                            response = registered
                                    ? new ResponseModel("success", "Registration successful.")
                                    : new ResponseModel("error", "Registration failed.");
                        } else {

                            User user = authService.login(request.getUsername(), request.getPassword());

                            if (user == null){
                                response = new ResponseModel("error", "Login failed.");
                                break;
                            }
                            this.currentUser = user;

                            if (SessionManager.contains(user.getInternal_uuid())) {
                                response = new ResponseModel("error", "You are already logged in from another device.");
                                break;

                            }


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
                                chatList.add(new ChatEntry(target.getUser_id(), target.getProfile_name(), target.getImage_url(), "private", last));
                            }
                            for (Group group : groups) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(group.getInternal_uuid(), "group");
                                chatList.add(new ChatEntry(group.getGroup_id(), group.getGroup_name(), group.getImage_url(), "group", last));
                            }
                            for (Channel channel : channels) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(channel.getInternal_uuid(), "channel");
                                chatList.add(new ChatEntry(channel.getChannel_id(), channel.getChannel_name(), channel.getImage_url(), "channel", last));
                            }

                            chatList.sort((a, b) -> {
                                if (a.getLastMessageTime() == null) return 1;
                                if (b.getLastMessageTime() == null) return -1;
                                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                            });

                            JSONObject userData = JsonUtil.userToJson(user);
                            userData.put("chat_list", JsonUtil.chatListToJson(chatList));
                            response = new ResponseModel("success", "Welcome " + user.getProfile_name(), userData);
                        }
                        break;
                    }

                    case "logout": {
                        String user_Id = requestJson.optString("user_id");
                        if (user_Id != null && !user_Id.isEmpty()) {
                            try {
                                UUID uuid = UUID.fromString(user_Id);
                                userDatabase.updateUserStatus(uuid, "offline");
                                userDatabase.updateLastSeen(uuid);
                                SessionManager.removeUser(uuid);
                                response = new ResponseModel("success", "Logged out.");
                            } catch (IllegalArgumentException ex) {
                                response = new ResponseModel("error", "Invalid UUID format for user_id.");
                            }
                        } else {
                            response = new ResponseModel("error", "Invalid user_id for logout.");
                        }
                        break;

                    }

                    case "search": {
                        String keyword = requestJson.optString("keyword");
                        List<JSONObject> results = new ArrayList<>();
                        String user_Id = requestJson.getString("user_id");
                        User currentUser = new userDatabase().findByUserId(user_Id);
                        UUID currentUserUUID = currentUser.getInternal_uuid();

                        for (User u : new userDatabase().searchUsers(keyword, currentUserUUID)) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "user");
                            obj.put("id", u.getUser_id());
                            obj.put("name", u.getProfile_name());
                            results.add(obj);
                        }

                        for (Group g : GroupDatabase.searchGroups(keyword)) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "group");
                            obj.put("id", g.getGroup_id());
                            obj.put("name", g.getGroup_name());
                            results.add(obj);
                        }

                        for (Channel c : ChannelDatabase.searchChannels(keyword)) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "channel");
                            obj.put("id", c.getChannel_id());
                            obj.put("name", c.getChannel_name());
                            results.add(obj);
                        }

                        List<Message> matchedMessages = MessageDatabase.searchMessagesForUser(currentUser.getInternal_uuid(), keyword);
                        for (Message m : matchedMessages) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "message");
                            obj.put("content", m.getContent());
                            obj.put("sender", m.getSender_id().toString());
                            obj.put("time", m.getSend_at().toString());
                            results.add(obj);
                        }



                        List<UUID> groupIds = new ArrayList<>();
                        for (Group g : GroupDatabase.getGroupsByUser(currentUser.getInternal_uuid())) {
                            groupIds.add(g.getInternal_uuid());
                        }

                        List<UUID> channelIds = new ArrayList<>();
                        for (Channel c : ChannelDatabase.getChannelsByUser(currentUser.getInternal_uuid())) {
                            channelIds.add(c.getInternal_uuid());
                        }


                        List<Message> groupMessages = MessageDatabase.searchMessagesInGroups(groupIds, keyword);
                        for (Message m : groupMessages) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "message");
                            obj.put("from", "group");
                            obj.put("content", m.getContent());
                            obj.put("time", m.getSend_at().toString());
                            results.add(obj);
                        }

                        List<Message> channelMessages = MessageDatabase.searchMessagesInChannels(channelIds, keyword);
                        for (Message m : channelMessages) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "message");
                            obj.put("from", "channel");
                            obj.put("content", m.getContent());
                            obj.put("time", m.getSend_at().toString());
                            results.add(obj);
                        }


                        JSONObject data = new JSONObject();
                        data.put("results", new JSONArray(results));
                        response = new ResponseModel("success", "Search results found", data);
                        break;
                    }

                    default:
                        response = new ResponseModel("error", "Unknown action: " + action);
                }

                JSONObject responseJson = new JSONObject();
                responseJson.put("status", response.getStatus());
                responseJson.put("message", response.getMessage());
                responseJson.put("data", response.getData() != null ? response.getData() : JSONObject.NULL);
                out.println(responseJson.toString());
            }
        } catch (IOException e) {
            System.out.println("Connection with client lost.");
             userId = (currentUser != null) ? currentUser.getInternal_uuid() : SessionManager.getUserIdBySocket(this.socket);
            if (userId != null) {
                userDatabase.updateUserStatus(userId, "offline");
                userDatabase.updateLastSeen(userId);
                SessionManager.removeUser(userId);
            }
        }   finally {
            try {
                if (currentUser != null) {
                    userId = currentUser.getInternal_uuid();
                    System.out.println("🔚 Client disconnected. Cleaning up user " + userId);
                    userDatabase.updateUserStatus(userId, "offline");
                    userDatabase.updateLastSeen(userId);
                    SessionManager.removeUser(userId);
                } else {
                    System.out.println("❗ currentUser is null, couldn't set offline.");
                }
                socket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


    }
}
