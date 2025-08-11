package org.to.telegramfinalproject.Server;

import javafx.geometry.Side;
import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Database.*;
import org.to.telegramfinalproject.Models.*;
import org.to.telegramfinalproject.Utils.ChannelPermissionUtil;
import org.to.telegramfinalproject.Utils.GroupPermissionUtil;

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
                            if (user == null) {
                                response = new ResponseModel("error", "Login failed.");
                                break;
                            }
                            this.currentUser = user;

                            SessionManager.addUser(user.getInternal_uuid(), this.socket);
                            userDatabase.updateUserStatus(user.getInternal_uuid(), "online");
                            //RealTime
                            List<UUID> contactIds = ContactDatabase.getContactUUIDs(user.getInternal_uuid());
                            RealTimeEventDispatcher.notifyUserStatusChanged(user.getInternal_uuid(), "online", contactIds);

                            List<Contact> contacts = ContactDatabase.getContacts(user.getInternal_uuid());
                            List<Group> groups = GroupDatabase.getGroupsByUser(user.getInternal_uuid());
                            List<Channel> channels = ChannelDatabase.getChannelsByUser(user.getInternal_uuid());
                            List<Message> unreadMessages = MessageDatabase.getUnreadMessages(user.getInternal_uuid());
                            List<UUID> archivedChatIds = ArchivedChatDatabase.getArchivedChats(user.getInternal_uuid());



                            user.setContactList(contacts);
                            user.setGroupList(groups);
                            user.setChannelList(channels);
                            user.setUnreadMessages(unreadMessages);



                            List<ChatEntry> chatList = new ArrayList<>();
                            List<ChatEntry> archivedChatList = new ArrayList<>();
                            List<ChatEntry> activeChatList = new ArrayList<>();


                            JSONArray contactList = new JSONArray();
                            for (Contact contact : contacts) {
                                User target = userDatabase.findByInternalUUID(contact.getContact_id());
                                if (target == null) continue;

                                JSONObject c = new JSONObject();
                                c.put("user_id", contact.getUser_id().toString());
                                c.put("contact_id", contact.getContact_id().toString());
                                User Contact = userDatabase.findByInternalUUID(contact.getContact_id());
                                c.put("contact_displayId", Contact.getUser_id());
                                c.put("is_blocked", contact.getIs_blocked());

                                c.put("profile_name", target.getProfile_name());
                                c.put("image_url", target.getImage_url());
                                c.put("last_seen", Contact.getLast_seen());

                                contactList.put(c);
                            }



//                            for (Contact contact : contacts) {
//                                User target = userDatabase.findByInternalUUID(contact.getContact_id());
//                                if (target == null) continue;
//                                LocalDateTime last = MessageDatabase.getLastMessageTimeBetween(user.getInternal_uuid(), target.getInternal_uuid(), "private");
//
////                                chatList.add(new ChatEntry(
////                                        target.getInternal_uuid(),      // internal UUID
////                                        target.getUser_id(),            // public display ID
////                                        target.getProfile_name(),
////                                        target.getImage_url(),
////                                        "private",
////                                        last,
////                                        false,
////                                        false
////                                ));
//
//                                UUID targetId = target.getInternal_uuid();
//
//                                ChatEntry entry = new ChatEntry(
//                                        targetId,
//                                        target.getUser_id(),
//                                        target.getProfile_name(),
//                                        target.getImage_url(),
//                                        "private",
//                                        last,
//                                        false,
//                                        false
//                                );
//
//
//
//                            }



                            List<PrivateChat> privateChats = PrivateChatDatabase.findChatsOfUser(currentUser.getInternal_uuid());
                            for (PrivateChat chat : privateChats) {
                                UUID otherId = chat.getUser1_id().equals(currentUser.getInternal_uuid()) ?
                                        chat.getUser2_id() : chat.getUser1_id();

                                User otherUser = userDatabase.findByInternalUUID(otherId);
                                if (otherUser == null) continue;

                                LocalDateTime lastMessageTime = MessageDatabase.getLastMessageTime(chat.getChat_id(), "private");


                                ChatEntry entry = new ChatEntry(
                                        chat.getChat_id(),
                                        otherUser.getUser_id(),
                                        otherUser.getProfile_name(),
                                        otherUser.getImage_url(),
                                        "private",
                                        lastMessageTime,
                                        false,
                                        false
                                );
                                entry.setOtherUserId(otherId);

                                if (currentUser.getInternal_uuid() == otherId) {
                                    entry.setSavedMessages(true);
                                }

                                if (archivedChatIds.contains(chat.getChat_id())) {
                                    archivedChatList.add(entry);
                                    chatList.add(entry);
                                } else {
                                    activeChatList.add(entry);
                                    chatList.add(entry);
                                }
                            }



                            for (Group group : groups) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(group.getInternal_uuid(), "group");
                                boolean isOwner = GroupDatabase.isOwner(group.getInternal_uuid(), user.getInternal_uuid());
                                boolean isAdmin = GroupDatabase.isAdmin(group.getInternal_uuid(), user.getInternal_uuid());

//                                chatList.add(new ChatEntry(
//                                        group.getInternal_uuid(),
//                                        group.getGroup_id(),
//                                        group.getGroup_name(),
//                                        group.getImage_url(),
//                                        "group",
//                                        last,
//                                        isOwner,
//                                        isAdmin
//                                ));

                                ChatEntry entry = new ChatEntry(
                                        group.getInternal_uuid(),
                                        group.getGroup_id(),
                                        group.getGroup_name(),
                                        group.getImage_url(),
                                        "group",
                                        last,
                                        isOwner,
                                        isAdmin
                                );

                                if (archivedChatIds.contains(group.getInternal_uuid())) {
                                    archivedChatList.add(entry);
                                    chatList.add(entry);
                                } else {
                                    activeChatList.add(entry);
                                    chatList.add(entry);
                                }


                            }

                            for (Channel channel : channels) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(channel.getInternal_uuid(), "channel");
                                boolean isOwner = ChannelDatabase.isOwner(channel.getInternal_uuid(), user.getInternal_uuid());
                                boolean isAdmin = ChannelDatabase.isAdmin(channel.getInternal_uuid(), user.getInternal_uuid());

//                                chatList.add(new ChatEntry(
//                                        channel.getInternal_uuid(),
//                                        channel.getChannel_id(),
//                                        channel.getChannel_name(),
//                                        channel.getImage_url(),
//                                        "channel",
//                                        last,
//                                        isOwner,
//                                        isAdmin
//                                ));

                                ChatEntry entry = new ChatEntry(
                                        channel.getInternal_uuid(),
                                        channel.getChannel_id(),
                                        channel.getChannel_name(),
                                        channel.getImage_url(),
                                        "channel",
                                        last,
                                        isOwner,
                                        isAdmin
                                );

                                if (archivedChatIds.contains(channel.getInternal_uuid())) {
                                    archivedChatList.add(entry);
                                    chatList.add(entry);
                                } else {
                                    activeChatList.add(entry);
                                    chatList.add(entry);
                                }
                            }

                            activeChatList.sort((a, b) -> {
                                if (a.getLastMessageTime() == null) return 1;
                                if (b.getLastMessageTime() == null) return -1;
                                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                            });

                            archivedChatList.sort((a, b) -> {
                                if (a.getLastMessageTime() == null) return 1;
                                if (b.getLastMessageTime() == null) return -1;
                                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                            });


                            chatList.sort((a, b) -> {
                                if (a.getLastMessageTime() == null) return 1;
                                if (b.getLastMessageTime() == null) return -1;
                                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                            });

                            JSONObject userData = JsonUtil.userToJson(user);
                            userData.put("chat_list", JsonUtil.chatListToJson(chatList));
                            userData.put("archived_chat_list", JsonUtil.chatListToJson(archivedChatList));
                            userData.put("active_chat_list", JsonUtil.chatListToJson(activeChatList));
                            userData.put("contact_list", contactList);
                            response = new ResponseModel("success", "Welcome " + user.getProfile_name(), userData);
                        }
                        break;
                    }
                    case "logout": {
                        String user_Id = requestJson.optString("user_id");
                        if (user_Id != null && !user_Id.isEmpty()) {
                            try {
                                userId = UUID.fromString(user_Id);

                                userDatabase.updateUserStatus(userId, "offline");

                                // Real-Time
                                List<UUID> contacts = ContactDatabase.getContactUUIDs(userId);
                                RealTimeEventDispatcher.notifyUserStatusChanged(userId, "offline", contacts);

                                userDatabase.updateLastSeen(userId);
                                SessionManager.removeUser(userId);

                                response = new ResponseModel("success", "Logged out.");
                            } catch (IllegalArgumentException e) {
                                response = new ResponseModel("error", "Invalid UUID format.");
                            }
                        } else {
                            response = new ResponseModel("error", "Invalid user_id for logout.");
                        }
                        break;
                    }


                    case "searchInUsers":{
                        String keyword = requestJson.optString("keyword");
                        List<JSONObject> results = new ArrayList<>();
                        String user_Id = requestJson.getString("user_id");
                        User currentUser = new userDatabase().findByUserId(user_Id);
                        if (currentUser == null) {
                            response = new ResponseModel("error", "User not found or not logged in.");
                            break;
                        }
                        UUID currentUserUUID = currentUser.getInternal_uuid();

                        for (User u : new userDatabase().searchUsers(keyword, currentUserUUID)) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "user");
                            obj.put("id", u.getUser_id());
                            obj.put("uuid", u.getInternal_uuid().toString());
                            obj.put("name", u.getProfile_name());
                            results.add(obj);
                        }
                        JSONObject data = new JSONObject();
                        data.put("results", new JSONArray(results));
                        response = new ResponseModel("success", "Search results found", data);
                        break;
                    }



                    case "searchEligibleUsers": {
                        String keyword = requestJson.optString("keyword");
                        UUID entityId = UUID.fromString(requestJson.getString("entity_id"));
                        String entityType = requestJson.getString("entity_type");  // group یا channel

                        String user_Id = requestJson.getString("user_id");
                        User currentUser = new userDatabase().findByUserId(user_Id);

                        List<JSONObject> results = new ArrayList<>();
                        for (User u : new userDatabase().searchUsers(keyword, currentUser.getInternal_uuid())) {

                            boolean isMember = switch (entityType) {
                                case "group" -> GroupDatabase.isUserInGroup(u.getInternal_uuid(), entityId);
                                case "channel" -> ChannelDatabase.isUserInChannel(u.getInternal_uuid(), entityId);
                                default -> true;
                            };

                            if (isMember || u.getInternal_uuid().equals(currentUser.getInternal_uuid())) continue;

                            JSONObject obj = new JSONObject();
                            obj.put("id", u.getUser_id());
                            obj.put("uuid", u.getInternal_uuid().toString());
                            obj.put("name", u.getProfile_name());
                            results.add(obj);
                        }


                        JSONObject data = new JSONObject();
                        data.put("results", new JSONArray(results));
                        response = new ResponseModel("success", "Eligible users found", data);
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
                            obj.put("uuid", u.getInternal_uuid().toString());
                            obj.put("name", u.getProfile_name());
                            results.add(obj);
                        }

                        for (Group g : GroupDatabase.searchGroups(keyword)) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "group");
                            obj.put("id", g.getGroup_id());
                            obj.put("uuid", g.getInternal_uuid().toString());
                            obj.put("name", g.getGroup_name());
                            results.add(obj);
                        }

                        for (Channel c : ChannelDatabase.searchChannels(keyword)) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "channel");
                            obj.put("id", c.getChannel_id());
                            obj.put("uuid", c.getInternal_uuid().toString());
                            obj.put("name", c.getChannel_name());
                            results.add(obj);
                        }

                        List<Message> matchedMessages = MessageDatabase.searchMessagesForUser(currentUser.getInternal_uuid(), keyword);
                        for (Message m : matchedMessages) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "message");
                            obj.put("content", m.getContent());
                            obj.put("time", m.getSend_at().toString());
                            obj.put("sender", m.getSender_id() != null ? m.getSender_id().toString() : "N/A");
                            obj.put("receiver_id", m.getReceiver_id().toString());
                            obj.put("receiver_type", m.getReceiver_type());
                            User senderUser = userDatabase.findByInternalUUID(m.getSender_id());
                            String senderName = senderUser != null ? senderUser.getProfile_name() : "Unknown";
                            obj.put("sender_name", senderName);

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


                        List<Message> groupMessages = MessageDatabase.searchMessagesInGroups(groupIds, keyword, currentUser.getInternal_uuid());
                        for (Message m : groupMessages) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "message");
                            obj.put("from", "group");
                            obj.put("content", m.getContent());
                            obj.put("time", m.getSend_at().toString());
                            obj.put("sender", m.getSender_id() != null ? m.getSender_id().toString() : "N/A");
                            obj.put("receiver_id", m.getReceiver_id().toString());
                            obj.put("receiver_type", "group");
                            Group senderGroup = GroupDatabase.findByInternalUUID(m.getReceiver_id());
                            String groupName = senderGroup != null ? senderGroup.getGroup_name() : "Unknown";
                            obj.put("group_name", groupName);
                            User senderUser = userDatabase.findByInternalUUID(m.getSender_id());
                            String senderName = senderUser != null ? senderUser.getProfile_name() : "Unknown";
                            obj.put("sender_name", senderName);

                            results.add(obj);
                        }

                        List<Message> channelMessages = MessageDatabase.searchMessagesInChannels(channelIds, keyword);
                        for (Message m : channelMessages) {
                            JSONObject obj = new JSONObject();
                            obj.put("type", "message");
                            obj.put("from", "channel");
                            obj.put("content", m.getContent());
                            obj.put("time", m.getSend_at().toString());
                            obj.put("sender", m.getSender_id() != null ? m.getSender_id().toString() : "N/A");
                            obj.put("receiver_id", m.getReceiver_id().toString());
                            obj.put("receiver_type", "channel");
                            Channel SenderChannel = ChannelDatabase.findByInternalUUID(m.getReceiver_id());
                            String channelName = SenderChannel != null ? SenderChannel.getChannel_name() : "Unknown";
                            obj.put("channel_name", channelName);
                            User senderUser = userDatabase.findByInternalUUID(m.getSender_id());
                            String senderName = senderUser != null ? senderUser.getProfile_name() : "Unknown";
                            obj.put("sender_name", senderName);
                            results.add(obj);
                        }


                        JSONObject data = new JSONObject();
                        data.put("results", new JSONArray(results));
                        response = new ResponseModel("success", "Search results found", data);
                        break;
                    }

                    case "add_contact": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        UUID userUUID = new userDatabase().findByUserId(requestJson.getString("user_id")).getInternal_uuid();
                        UUID contactUUID = UUID.fromString(requestJson.getString("contact_id"));

                        boolean success = ContactDatabase.addContact(userUUID, contactUUID);

                        //RealTime
                        if (success) {
                            RealTimeEventDispatcher.notifyAddedToChat(
                                    "private",
                                    userUUID,
                                    currentUser.getProfile_name(),
                                    currentUser.getImage_url(),
                                    contactUUID
                            );

                        }

                        response = success
                                ? new ResponseModel("success", "Contact added successfully.")
                                : new ResponseModel("error", "Failed to add contact. Maybe already exists.");
                        break;
                    }

                    case "join_group": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID userUUID = UUID.fromString(requestJson.getString("user_id"));
                        Group group = GroupDatabase.findByInternalUUID(UUID.fromString(requestJson.getString("id")));
                        if (group == null) {
                            response = new ResponseModel("error", "Group not found.");
                            break;
                        }


                        boolean joined = GroupDatabase.addMemberToGroup(userUUID, group.getInternal_uuid());

                        //RealTime
                        if (joined) {
                            List<UUID> members = GroupDatabase.getMemberUUIDs(group.getInternal_uuid());
                            RealTimeEventDispatcher.sendGroupOrChannelUpdate("group", group.getInternal_uuid(), group.getGroup_name(), group.getImage_url(), group.getDescription(), members);
                        }

                        response = joined
                                ? new ResponseModel("success", "Joined group.")
                                : new ResponseModel("error", "Failed to join group.");
                        break;
                    }

                    case "join_channel": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID userUUID = UUID.fromString(requestJson.getString("user_id"));
                        Channel channel = ChannelDatabase.findByInternalUUID(UUID.fromString(requestJson.getString("id")));
                        if (channel == null) {
                            response = new ResponseModel("error", "Channel not found.");
                            break;
                        }


                        boolean joined = ChannelDatabase.addSubscriberToChannel(userUUID, channel.getInternal_uuid());

                        //RealTime
                        if (joined) {
                            List<UUID> members = ChannelDatabase.getChannelSubscriberUUIDs(channel.getInternal_uuid());
                            RealTimeEventDispatcher.sendGroupOrChannelUpdate("channel", channel.getInternal_uuid(), channel.getChannel_name(), channel.getImage_url(), channel.getDescription(), members);
                        }

                        response = joined
                                ? new ResponseModel("success", "Joined channel.")
                                : new ResponseModel("error", "Failed to join channel.");
                        break;
                    }


                    case "get_chat_info": {


                        try {
                            String id = requestJson.getString("receiver_id");
                            String type = requestJson.getString("receiver_type");
                            JSONObject data = new JSONObject();

                            switch (type) {
                                case "private" -> {
                                    userDatabase userDatabase = new userDatabase();
                                    User u = userDatabase.findByUserId(id);
                                    if (u == null) {
                                        try {
                                            UUID uuid = UUID.fromString(id);
                                            u = userDatabase.findByInternalUUID(uuid);
                                        } catch (IllegalArgumentException ignored) {}
                                    }

                                    if (u != null) {
                                        data.put("internal_id", u.getInternal_uuid().toString());
                                        data.put("name", u.getProfile_name());
                                        data.put("image_url", u.getImage_url());
                                        data.put("type", "private");
                                        data.put("id", u.getUser_id());
                                    } else {
                                        response = new ResponseModel("error", "User not found.");
                                        break;
                                    }
                                }

                                case "group" -> {
                                    Group group = GroupDatabase.findByInternalUUID(UUID.fromString(id));
                                    if (group != null) {
                                        data.put("internal_id", group.getInternal_uuid().toString());
                                        data.put("name", group.getGroup_name());
                                        data.put("image_url", group.getImage_url());
                                        data.put("description", group.getDescription() != null ? group.getDescription() : "");
                                        data.put("type", "group");
                                        data.put("id", group.getGroup_id());

                                        boolean isOwner = GroupDatabase.isOwner(group.getInternal_uuid(), currentUser.getInternal_uuid());
                                        boolean isAdmin = GroupDatabase.isAdmin(group.getInternal_uuid(), currentUser.getInternal_uuid());

                                        data.put("is_owner", isOwner);
                                        data.put("is_admin", isAdmin);
                                    } else {
                                        response = new ResponseModel("error", "Group not found.");
                                        break;
                                    }
                                }



                                case "channel" -> {
                                    Channel channel = ChannelDatabase.findByInternalUUID(UUID.fromString(id));
                                    if (channel != null) {
                                        data.put("internal_id", channel.getInternal_uuid().toString());
                                        data.put("name", channel.getChannel_name());
                                        data.put("image_url", channel.getImage_url());
                                        data.put("description", channel.getDescription() != null ? channel.getDescription() : "");
                                        data.put("type", "channel");
                                        data.put("id", channel.getChannel_id());

                                        boolean isOwner = ChannelDatabase.isOwner(channel.getInternal_uuid(), currentUser.getInternal_uuid());
                                        boolean isAdmin = ChannelDatabase.isAdmin(channel.getInternal_uuid(), currentUser.getInternal_uuid());

                                        data.put("is_owner", isOwner);
                                        data.put("is_admin", isAdmin);
                                    } else {
                                        response = new ResponseModel("error", "Channel not found.");
                                        break;
                                    }
                                }


                                default -> {
                                    response = new ResponseModel("error", "Unknown type.");
                                    break;
                                }
                            }

                            if (response == null) {
                                response = new ResponseModel("success", "Chat info fetched", data);
                            }

                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error fetching chat info: " + e.getMessage());
                        }
                        break;
                    }



                    case "get_chat_list": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        List<Contact> contacts = ContactDatabase.getContacts(currentUser.getInternal_uuid());
                        List<Group> groups = GroupDatabase.getGroupsByUser(currentUser.getInternal_uuid());
                        List<Channel> channels = ChannelDatabase.getChannelsByUser(currentUser.getInternal_uuid());
                        List<UUID> archivedChatIds = ArchivedChatDatabase.getArchivedChats(currentUser.getInternal_uuid());
                        List<ChatEntry> chatList = new ArrayList<>();
                        List<ChatEntry> archivedChatList = new ArrayList<>();
                        List<ChatEntry> activeChatList = new ArrayList<>();






//                        for (Contact contact : contacts) {
//                            User target = userDatabase.findByInternalUUID(contact.getContact_id());
//                            if (target == null) continue;
//
//                            LocalDateTime last = MessageDatabase.getLastMessageTimeBetween(currentUser.getInternal_uuid(), target.getInternal_uuid(), "private");
//
//                            chatList.add(new ChatEntry(
//                                    target.getInternal_uuid(),
//                                    target.getUser_id(),
//                                    target.getProfile_name(),
//                                    target.getImage_url(),
//                                    "private",
//                                    last,
//                                    false,
//                                    false
//                            ));
//                        }
//

                        List<PrivateChat> privateChats = PrivateChatDatabase.findChatsOfUser(currentUser.getInternal_uuid());
                        for (PrivateChat chat : privateChats) {
                            UUID otherId = chat.getUser1_id().equals(currentUser.getInternal_uuid()) ?
                                    chat.getUser2_id() : chat.getUser1_id();

                            User otherUser = userDatabase.findByInternalUUID(otherId);
                            if (otherUser == null) continue;

                            LocalDateTime lastMessageTime = MessageDatabase.getLastMessageTime(chat.getChat_id(), "private");

                            boolean isSavedMessages = chat.getUser1_id().equals(currentUser.getInternal_uuid())
                                    && chat.getUser2_id().equals(currentUser.getInternal_uuid());

                            ChatEntry entry = new ChatEntry(
                                    chat.getChat_id(),
                                    otherUser.getUser_id(),
                                    otherUser.getProfile_name(),
                                    otherUser.getImage_url(),
                                    "private",
                                    lastMessageTime,
                                    false,
                                    false
                            );
                            entry.setOtherUserId(otherId);

                            // Mark it as saved messages if it's the special self-chat
                            if (isSavedMessages) {
                                entry.setSavedMessages(true);
                            }

                            if (archivedChatIds.contains(chat.getChat_id())) {
                                archivedChatList.add(entry);
                                chatList.add(entry);
                            } else {
                                activeChatList.add(entry);
                                chatList.add(entry);
                            }
                        }


                        for (Group group : groups) {
                            LocalDateTime last = MessageDatabase.getLastMessageTime(group.getInternal_uuid(), "group");
                            boolean isOwner = GroupDatabase.isOwner(group.getInternal_uuid(), currentUser.getInternal_uuid());
                            boolean isAdmin = GroupDatabase.isAdmin(group.getInternal_uuid(), currentUser.getInternal_uuid());

//                                chatList.add(new ChatEntry(
//                                        group.getInternal_uuid(),
//                                        group.getGroup_id(),
//                                        group.getGroup_name(),
//                                        group.getImage_url(),
//                                        "group",
//                                        last,
//                                        isOwner,
//                                        isAdmin
//                                ));

                            ChatEntry entry = new ChatEntry(
                                    group.getInternal_uuid(),
                                    group.getGroup_id(),
                                    group.getGroup_name(),
                                    group.getImage_url(),
                                    "group",
                                    last,
                                    isOwner,
                                    isAdmin
                            );

                            if (archivedChatIds.contains(group.getInternal_uuid())) {
                                archivedChatList.add(entry);
                                chatList.add(entry);
                            } else {
                                activeChatList.add(entry);
                                chatList.add(entry);
                            }


                        }

                        for (Channel channel : channels) {
                            LocalDateTime last = MessageDatabase.getLastMessageTime(channel.getInternal_uuid(), "channel");
                            boolean isOwner = ChannelDatabase.isOwner(channel.getInternal_uuid(), currentUser.getInternal_uuid());
                            boolean isAdmin = ChannelDatabase.isAdmin(channel.getInternal_uuid(), currentUser.getInternal_uuid());

//                                chatList.add(new ChatEntry(
//                                        channel.getInternal_uuid(),
//                                        channel.getChannel_id(),
//                                        channel.getChannel_name(),
//                                        channel.getImage_url(),
//                                        "channel",
//                                        last,
//                                        isOwner,
//                                        isAdmin
//                                ));

                            ChatEntry entry = new ChatEntry(
                                    channel.getInternal_uuid(),
                                    channel.getChannel_id(),
                                    channel.getChannel_name(),
                                    channel.getImage_url(),
                                    "channel",
                                    last,
                                    isOwner,
                                    isAdmin
                            );

                            if (archivedChatIds.contains(channel.getInternal_uuid())) {
                                archivedChatList.add(entry);
                                chatList.add(entry);
                            } else {
                                activeChatList.add(entry);
                                chatList.add(entry);
                            }
                        }

                        activeChatList.sort((a, b) -> {
                            if (a.getLastMessageTime() == null) return 1;
                            if (b.getLastMessageTime() == null) return -1;
                            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                        });

                        archivedChatList.sort((a, b) -> {
                            if (a.getLastMessageTime() == null) return 1;
                            if (b.getLastMessageTime() == null) return -1;
                            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                        });


                        chatList.sort((a, b) -> {
                            if (a.getLastMessageTime() == null) return 1;
                            if (b.getLastMessageTime() == null) return -1;
                            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                        });
                        JSONObject data = new JSONObject();
                        data.put("chat_list", JsonUtil.chatListToJson(chatList));
                        data.put("archived_chat_list", JsonUtil.chatListToJson(archivedChatList));
                        data.put("active_chat_list", JsonUtil.chatListToJson(activeChatList));
                        response = new ResponseModel("success", "Chat list updated.", data);

                        break;
                    }




                    case "create_group": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            String groupId = requestJson.getString("group_id");
                            String groupName = requestJson.getString("group_name");
                            String userIdStr = requestJson.getString("user_id");
                            String imageUrl = requestJson.optString("image_url", null);

                            UUID creatorUUID = UUID.fromString(userIdStr);

                            boolean created = GroupService.createGroup(groupId, groupName, creatorUUID, imageUrl);

                            if (created) {
                                Group createdGroup = GroupDatabase.findByGroupId(groupId);
                                if (createdGroup != null) {
                                    JSONObject data = new JSONObject();
                                    data.put("internal_id", createdGroup.getInternal_uuid().toString());
                                    data.put("id", createdGroup.getGroup_id());
                                    data.put("name", createdGroup.getGroup_name());
                                    data.put("image_url", createdGroup.getImage_url());
                                    data.put("type", "group");

                                    response = new ResponseModel("success", "Group created.", data);
                                } else {
                                    response = new ResponseModel("error", "Group created but not found.");
                                }
                            } else {
                                response = new ResponseModel("error", "Group creation failed.");
                            }
                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error creating group: " + e.getMessage());
                        }
                        break;
                    }


                    case "create_channel": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            String channelId = requestJson.getString("channel_id");
                            String channelName = requestJson.getString("channel_name");
                            String userIdStr = requestJson.getString("user_id");
                            String imageUrl = requestJson.optString("image_url", null);

                            UUID creatorUUID = UUID.fromString(userIdStr);

                            boolean created = ChannelService.createChannel(channelId, channelName, creatorUUID, imageUrl);

                            if (created) {
                                Channel createdChannel = ChannelDatabase.findByChannelId(channelId);
                                if (createdChannel != null) {
                                    JSONObject data = new JSONObject();
                                    data.put("internal_id", createdChannel.getInternal_uuid().toString());
                                    data.put("id", createdChannel.getChannel_id());
                                    data.put("name", createdChannel.getChannel_name());
                                    data.put("image_url", createdChannel.getImage_url());
                                    data.put("type", "channel");

                                    response = new ResponseModel("success", "Channel created.", data);
                                } else {
                                    response = new ResponseModel("error", "Channel created but not found.");
                                }
                            } else {
                                response = new ResponseModel("error", "Channel creation failed.");
                            }

                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error creating channel: " + e.getMessage());
                        }
                        break;
                    }

                    case "add_admin_to_channel": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            UUID channelId = UUID.fromString(requestJson.getString("channel_id"));
                            UUID targetUserId = UUID.fromString(requestJson.getString("target_user_id"));
                            JSONObject permissions = requestJson.optJSONObject("permissions");

                            if (!ChannelPermissionUtil.canAddAdmins(channelId, currentUser.getInternal_uuid())) {
                                response = new ResponseModel("error", "You are not allowed to add admins to the channel.");
                                break;
                            }

                            String targetRole = ChannelDatabase.getChannelRole(channelId, targetUserId);
                            if (targetRole == null) {
                                response = new ResponseModel("error", "User is not a subscriber of the channel.");
                                break;
                            }

                            if (targetRole.equals("owner") || targetRole.equals("admin")) {
                                response = new ResponseModel("error", "User is already an admin or owner.");
                                break;
                            }

                            boolean success = ChannelDatabase.addAdminToChannel(channelId, targetUserId, permissions);

                            //RealTime
                            if (success) {
                                Channel channel = ChannelDatabase.findByInternalUUID(channelId);
                                if (channel != null) {
                                    RealTimeEventDispatcher.notifyBecameAdmin(
                                            "channel",
                                            channel.getInternal_uuid(),
                                            channel.getChannel_name(),
                                            channel.getImage_url(),
                                            targetUserId
                                    );
                                }
                            }
                            response = success
                                    ? new ResponseModel("success", "Admin added to channel.")
                                    : new ResponseModel("error", "Failed to add admin.");
                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error adding admin to channel: " + e.getMessage());
                        }
                        break;
                    }

                    case "edit_channel_admin_permissions": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("target_user_id"));
                        JSONObject permissions = requestJson.optJSONObject("permissions");

                        String role = ChannelDatabase.getChannelRole(channelId, currentUser.getInternal_uuid());
                        if (!role.equals("owner")) {
                            response = new ResponseModel("error", "Only owner can update admin permissions.");
                            break;
                        }

                        boolean success = ChannelDatabase.updateChannelAdminPermissions(channelId, targetUserId, permissions);
                        response = success
                                ? new ResponseModel("success", "Permissions updated.")
                                : new ResponseModel("error", "Failed to update permissions.");
                        break;
                    }


                    case "edit_group_info": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            UUID groupUUID = UUID.fromString(requestJson.getString("group_id")); // internal_uuid
                            String newGroupId = requestJson.getString("new_group_id").trim(); // شناسه نمایشی جدید
                            String name = requestJson.optString("name");
                            String description = requestJson.optString("description", null);
                            String imageUrl = requestJson.has("image_url") && !requestJson.isNull("image_url")
                                    ? requestJson.getString("image_url") : null;

                            boolean isOwner = GroupDatabase.isOwner(groupUUID, currentUser.getInternal_uuid());
                            if (!isOwner && !GroupPermissionUtil.canEditGroup(groupUUID, currentUser.getInternal_uuid())) {
                                response = new ResponseModel("error", "You don't have permission to edit this group.");
                                break;
                            }

                            if (isOwner && !GroupDatabase.isGroupIdUnique(newGroupId, groupUUID)) {
                                response = new ResponseModel("error", "Group ID is already taken by another group.");
                                break;
                            }

                            boolean updated = GroupDatabase.updateGroupInfo(groupUUID, newGroupId, name, description, imageUrl);
                            response = updated
                                    ? new ResponseModel("success", "Group info updated successfully.")
                                    : new ResponseModel("error", "Failed to update group info.");

                            //RealTime
                            if (updated) {
                                List<UUID> members = GroupDatabase.getMemberUUIDs(groupUUID);
                                RealTimeEventDispatcher.sendGroupOrChannelUpdate("group", groupUUID, name, imageUrl, description, members);
                            }

                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error updating group: " + e.getMessage());
                        }
                        break;
                    }




                    case "view_channel_admins": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));



                        List<JSONObject> admins = ChannelDatabase.getChannelAdminsAndOwner(channelId);
                        JSONObject data = new JSONObject();
                        data.put("admins", new JSONArray(admins));
                        response = new ResponseModel("success", "Admins fetched.", data);
                        break;
                    }



                    case "add_admin_to_group": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("user_id"));
                        JSONObject permissions = requestJson.optJSONObject("permissions");

                        if (!GroupPermissionUtil.canAddAdmins(groupId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to add admins.");
                            break;
                        }


                        boolean success = GroupDatabase.addAdminToGroup(groupId, targetUserId, permissions);

                        //RealTime
                        if (success) {
                            Group group = GroupDatabase.findByInternalUUID(groupId);
                            if (group != null) {
                                RealTimeEventDispatcher.notifyBecameAdmin(
                                        "group",
                                        group.getInternal_uuid(),
                                        group.getGroup_name(),
                                        group.getImage_url(),
                                        targetUserId
                                );
                            }
                        }
                        response = success
                                ? new ResponseModel("success", "Admin added to group.")
                                : new ResponseModel("error", "Failed to add admin.");
                        break;
                    }


                    case "edit_group_admin_permissions": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("target_user_id"));
                        JSONObject permissions = requestJson.optJSONObject("permissions");

                        if (!GroupPermissionUtil.canAddAdmins(groupId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to edit admin permissions.");
                            break;
                        }

                        boolean success = GroupDatabase.updateGroupAdminPermissions(groupId, targetUserId, permissions);
                        response = success
                                ? new ResponseModel("success", "Permissions updated.")
                                : new ResponseModel("error", "Failed to update permissions.");
                        break;
                    }





                    case "remove_admin_from_channel": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));
                        UUID targetUserUUID = UUID.fromString(requestJson.getString("target_user_id"));

                        if (!ChannelPermissionUtil.canRemoveAdmins(channelId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to remove admins.");
                            break;
                        }

                        String targetRole = ChannelDatabase.getChannelRole(channelId, targetUserUUID);
                        if (targetRole.equals("owner")) {
                            response = new ResponseModel("error", "You cannot remove the owner.");
                            break;
                        }
                        if (!targetRole.equals("admin")) {
                            response = new ResponseModel("error", "Target user is not an admin.");
                            break;
                        }

                        boolean success = ChannelDatabase.demoteAdminToSubscriber(channelId, targetUserUUID);

                        //RealTime
                        if (success) {
                            Channel channel = ChannelDatabase.findByInternalUUID(channelId);
                            if (channel != null) {
                                RealTimeEventDispatcher.notifyRemovedAdminFromChat(
                                        "channel",
                                        channel.getInternal_uuid(),
                                        channel.getChannel_name(),
                                        channel.getImage_url(),
                                        targetUserUUID
                                );
                            }
                        }

                        response = success
                                ? new ResponseModel("success", "Admin removed successfully.")
                                : new ResponseModel("error", "Failed to remove admin.");
                        break;
                    }


                    case "add_member_to_group": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("user_id"));

                        if (!GroupPermissionUtil.canAddMembers(groupId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to add members.");
                            break;
                        }

                        if (GroupDatabase.isUserInGroup(targetUserId, groupId)) {
                            response = new ResponseModel("error", "User is already a member.");
                            break;
                        }

                        boolean success = GroupDatabase.addMemberToGroup(targetUserId, groupId);

                        Group group = GroupDatabase.findByInternalUUID(groupId);

                        //RealTime
                        if (success && group != null) {
                            RealTimeEventDispatcher.notifyAddedToChat(
                                    "group",
                                    group.getInternal_uuid(),
                                    group.getGroup_name(),
                                    group.getImage_url(),
                                    targetUserId
                            );
                        }

                        response = success
                                ? new ResponseModel("success", "Member added to group.")
                                : new ResponseModel("error", "Failed to add member.");
                        break;

                    }


                    case "remove_member_from_group": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("user_id"));

                        if (!GroupPermissionUtil.canRemoveMembers(groupId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to remove members.");
                            break;
                        }

                        String targetRole = GroupDatabase.getGroupRole(groupId, targetUserId);
                        if (targetRole == null) {
                            response = new ResponseModel("error", "User is not a member of the group.");
                            break;
                        }

                        if (targetRole.equals("owner")) {
                            response = new ResponseModel("error", "You cannot remove the owner.");
                            break;
                        }

                        //RealTime
                        boolean success = GroupDatabase.removeMemberFromGroup(groupId, targetUserId);
                        if (success) {
                            Group group = GroupDatabase.findByInternalUUID(groupId);
                            if (group != null) {
                                RealTimeEventDispatcher.notifyRemovedFromChat(
                                        "group",
                                        group.getInternal_uuid(),
                                        targetUserId
                                );
                            }
                        }
                        response = success
                                ? new ResponseModel("success", "Member removed from group.")
                                : new ResponseModel("error", "Failed to remove member.");
                        break;
                    }



                    case "view_group_admins": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));

                        String role = GroupDatabase.getGroupRole(groupId, currentUser.getInternal_uuid());
                        if (!role.equals("owner") && !role.equals("admin")) {
                            response = new ResponseModel("error", "You are not authorized to view admins.");
                            break;
                        }

                        List<JSONObject> admins = GroupDatabase.getGroupAdminsAndOwner(groupId);
                        JSONObject data = new JSONObject();
                        data.put("admins", new JSONArray(admins));
                        response = new ResponseModel("success", "Admins fetched.", data);
                        break;
                    }


                    case "get_messages": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            String receiverId = requestJson.getString("receiver_id");
                            String receiverType = requestJson.getString("receiver_type");

                            List<Message> messages = new ArrayList<>();

                            switch (receiverType) {
                                case "private" -> {
                                    UUID chatId = UUID.fromString(receiverId);
                                    List<UUID> members = PrivateChatDatabase.getMembers(chatId);
                                    if (!members.contains(currentUser.getInternal_uuid())) {
                                        response = new ResponseModel("error", "You're not a member of this private chat.");
                                        break;
                                    }

                                    messages = MessageDatabase.privateChatHistory(chatId, currentUser.getInternal_uuid());
                                }


                                case "group" -> {
                                    Group group = GroupDatabase.findByInternalUUID(UUID.fromString(receiverId));
                                    if (group == null) {
                                        response = new ResponseModel("error", "Group not found.");
                                        break;
                                    }
                                    messages = MessageDatabase.groupChatHistory(group.getInternal_uuid(),currentUser.getInternal_uuid());
                                }


                                case "channel" -> {
                                    Channel channel = ChannelDatabase.findByInternalUUID(UUID.fromString(receiverId));
                                    if (channel == null) {
                                        response = new ResponseModel("error", "Channel not found.");
                                        break;
                                    }
                                    messages = MessageDatabase.channelChatHistory(channel.getInternal_uuid(),currentUser.getInternal_uuid());
                                }
                                default -> {
                                    response = new ResponseModel("error", "Invalid receiver type.");
                                    break;
                                }
                            }

                            if (response == null) {
                                JSONArray messageArray = new JSONArray();
                                for (Message m : messages) {
                                    JSONObject obj = new JSONObject();
                                    obj.put("id", m.getMessage_id().toString());
                                    obj.put("sender_id", m.getSender_id().toString());
                                    obj.put("receiver_id", m.getReceiver_id().toString());
                                    obj.put("receiver_type", m.getReceiver_type());
                                    obj.put("content", m.getContent());
                                    obj.put("send_at", m.getSend_at().toString());
                                    User senderUser = userDatabase.findByInternalUUID(m.getSender_id());
                                    String senderName = senderUser != null ? senderUser.getProfile_name() : "Unknown";
                                    obj.put("sender_name", senderName);
                                    messageArray.put(obj);
                                }

                                JSONObject data = new JSONObject();
                                data.put("messages", messageArray);

                                response = new ResponseModel("success", "Messages fetched.", data);
                            }

                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error fetching messages: " + e.getMessage());
                        }
                        break;
                    }

                    case "toggle_block": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            UUID userUUID = UUID.fromString(requestJson.getString("user_id"));
                            UUID targetUUID = UUID.fromString(requestJson.getString("target_id"));

                            boolean isBlocked = ContactDatabase.toggleBlock(userUUID, targetUUID);

                            //RealTime
                            if (isBlocked) {
                                RealTimeEventDispatcher.notifyBlocked(userUUID, targetUUID);
                            } else {
                                RealTimeEventDispatcher.notifyUnblocked(userUUID, targetUUID);
                            }


                            String message = isBlocked ? "🔒 User blocked successfully." : "🔓 User unblocked successfully.";
                            response = new ResponseModel("success", message);

                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error processing block/unblock: " + e.getMessage());
                        }
                        break;
                    }



                    case "transfer_group_ownership": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));
                        UUID newOwnerUserId = UUID.fromString(requestJson.getString("new_owner_user_id"));
                        User newOwner = userDatabase.findByInternalUUID(newOwnerUserId);
                        if (newOwner == null) {
                            response = new ResponseModel("error", "New owner not found.");
                            break;
                        }

                        if (!GroupDatabase.isOwner(groupId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "Only current owner can transfer ownership.");
                            break;
                        }

                        if (!GroupDatabase.isAdmin(groupId, newOwner.getInternal_uuid())) {
                            response = new ResponseModel("error", "Selected user is not an admin.");
                            break;
                        }

                        boolean success = GroupDatabase.transferOwnership(groupId, newOwner.getInternal_uuid());

                        //RealTime
                        if (success) {
                            List<UUID> members = GroupDatabase.getMemberUUIDs(groupId);
                            Group group = GroupDatabase.findByInternalUUID(groupId);

                            //update for new owner
                            RealTimeEventDispatcher.sendOwnershipTransferred(
                                    "group",
                                    groupId,
                                    group.getGroup_name(),
                                    List.of(newOwner.getInternal_uuid())
                            );

                            //Update for everyone
                            RealTimeEventDispatcher.sendGroupOrChannelUpdate(
                                    "group",
                                    groupId,
                                    group.getGroup_name(),
                                    group.getImage_url(),
                                    group.getDescription(),
                                    members
                            );
                        }




                        response = success
                                ? new ResponseModel("success", "Ownership transferred.")
                                : new ResponseModel("error", "Failed to transfer ownership.");

                        break;
                    }


                    case "view_group_members" : {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));

                        JSONArray members = GroupDatabase.getGroupMembers(groupId);

                        if (members != null) {
                            JSONObject data = new JSONObject();
                            data.put("members", members);
                            response = new ResponseModel("success", "Members fetched successfully.", data);
                        } else {
                            response = new ResponseModel("error", "Failed to fetch members.");
                        }
                        break;
                    }



                    case "delete_private_chat": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        // دریافت شناسه چت و نوع حذف (یک‌طرفه یا دوطرفه)
                        UUID targetId = UUID.fromString(requestJson.getString("chat_id"));
                        boolean both = requestJson.getBoolean("both");

                        // Real-Time Event Dispatch (اطلاع‌رسانی ریل تایم)
                        if (both) {
                            // حذف دوطرفه
                            RealTimeEventDispatcher.notifyChatDeleted("private", targetId, List.of(currentUser.getInternal_uuid()));
                            RealTimeEventDispatcher.notifyChatDeleted("private", currentUser.getInternal_uuid(), List.of(targetId));
                        } else {
                            // حذف یک‌طرفه
                            RealTimeEventDispatcher.notifyChatDeleted("private", targetId, List.of(currentUser.getInternal_uuid()));
                        }

                        // فراخوانی متد حذف چت
                        response = handleDeleteChat(requestJson);
                        break;
                    }



                    case "get_group_permissions": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));
                        userId = currentUser.getInternal_uuid();

                        JSONObject permissions = GroupDatabase.getGroupPermissions(groupId, userId);

                        response = new ResponseModel("success", "Permissions fetched.", permissions);
                        break;
                    }


                    case "leave_chat": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        String chatType = requestJson.getString("chat_type");
                        UUID chatId = UUID.fromString(requestJson.getString("chat_id"));
                         userId = UUID.fromString(requestJson.getString("user_id"));

                        boolean success = false;




                        switch (chatType) {
                            case "group" -> success = GroupDatabase.removeMemberFromGroup(chatId, userId);
                            case "channel" -> success = ChannelDatabase.removeSubscriberFromChannel(chatId, userId);
                            default -> {
                                response = new ResponseModel("error", "Unsupported chat type.");
                                break;
                            }
                        }

                        //RealTime
                        if (success) {
                            if (chatType.equals("group")) {
                                RealTimeEventDispatcher.notifyRemovedFromChat("group", chatId, userId);
                            } else if (chatType.equals("channel")) {
                                RealTimeEventDispatcher.notifyRemovedFromChat("channel", chatId, userId);
                            }
                        }

                        if (response == null) {
                            response = success
                                    ? new ResponseModel("success", "Left the " + chatType + " successfully.")
                                    : new ResponseModel("error", "Failed to leave the " + chatType + ".");
                        }

                        break;
                    }


                    case "delete_group": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));

                        if (!GroupDatabase.isOwner(groupId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "Only the group owner can delete the group.");
                            break;
                        }

                        List<UUID> memberIds = GroupDatabase.getGroupMemberUUIDs(groupId);

                        boolean success = GroupDatabase.deleteGroup(groupId);

                        //RealTime
                        if (success) {
                            RealTimeEventDispatcher.notifyChatDeleted("group", groupId, memberIds);
                            response = new ResponseModel("success", "Group deleted successfully.");
                        } else {
                            response = new ResponseModel("error", "Failed to delete group.");
                        }
                        break;
                    }


                    case "get_channel_permissions": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            UUID channelId = UUID.fromString(requestJson.getString("channel_id"));
                             userId = currentUser.getInternal_uuid();

                            JSONObject permissions = ChannelDatabase.getChannelPermissions(channelId, userId);

                            response = new ResponseModel("success", "Permissions fetched.", permissions);
                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error fetching channel permissions: " + e.getMessage());
                        }
                        break;
                    }


                    case "view_channel_subscribers": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));

                        boolean isOwner = ChannelDatabase.isOwner(channelId, currentUser.getInternal_uuid());
                        boolean isAdmin = ChannelDatabase.isAdmin(channelId, currentUser.getInternal_uuid());

                        if (!isOwner && !isAdmin) {
                            response = new ResponseModel("error", "You are not authorized to view subscribers.");
                            break;
                        }

                        JSONArray subscribers = ChannelDatabase.getChannelSubscribers(channelId);

                        if (subscribers != null) {
                            JSONObject data = new JSONObject();
                            data.put("subscribers", subscribers);
                            response = new ResponseModel("success", "Subscribers fetched successfully.", data);
                        } else {
                            response = new ResponseModel("error", "Failed to fetch subscribers.");
                        }
                        break;
                    }


                    case "add_subscriber_to_channel": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("user_id"));

                        if (!ChannelPermissionUtil.canAddSubscribers(channelId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to add subscribers.");
                            break;
                        }

                        if (ChannelDatabase.isUserInChannel(targetUserId, channelId)) {
                            response = new ResponseModel("error", "User is already a subscriber.");
                            break;
                        }

                        boolean success = ChannelDatabase.addSubscriberToChannel(targetUserId, channelId);

                        //RealTime
                        if (success) {
                            Channel channel = ChannelDatabase.findByInternalUUID(channelId);
                            if (channel != null) {
                                RealTimeEventDispatcher.notifyAddedToChat(
                                        "channel",
                                        channel.getInternal_uuid(),
                                        channel.getChannel_name(),
                                        channel.getImage_url(),
                                        targetUserId
                                );
                            }
                        }
                        response = success
                                ? new ResponseModel("success", "Subscriber added to channel.")
                                : new ResponseModel("error", "Failed to add subscriber.");
                        break;
                    }

                    case "remove_subscriber_from_channel": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("user_id"));

                        if (!ChannelPermissionUtil.canRemoveSubscribers(channelId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to remove subscribers.");
                            break;
                        }

                        String targetRole = ChannelDatabase.getChannelRole(channelId, targetUserId);
                        if (targetRole == null) {
                            response = new ResponseModel("error", "User is not a subscriber of the channel.");
                            break;
                        }

                        if (targetRole.equals("owner")) {
                            response = new ResponseModel("error", "You cannot remove the owner.");
                            break;
                        }

                        boolean success = ChannelDatabase.removeSubscriberFromChannel(channelId, targetUserId);

                        //RealTime
                        if (success) {
                            Channel channel = ChannelDatabase.findByInternalUUID(channelId);
                            if (channel != null) {
                                RealTimeEventDispatcher.notifyRemovedFromChat(
                                        "channel",
                                        channel.getInternal_uuid(),
                                        targetUserId
                                );
                            }
                        }
                        response = success
                                ? new ResponseModel("success", "Subscriber removed from channel.")
                                : new ResponseModel("error", "Failed to remove subscriber.");
                        break;
                    }


                    case "edit_channel_info": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        try {
                            UUID channelUUID = UUID.fromString(requestJson.getString("channel_id")); // internal_uuid
                            String newChannelId = requestJson.getString("new_channel_id").trim();
                            String name = requestJson.optString("name");
                            String description = requestJson.optString("description", null);
                            String imageUrl = requestJson.has("image_url") && !requestJson.isNull("image_url")
                                    ? requestJson.getString("image_url") : null;

                            boolean isOwner = ChannelDatabase.isOwner(channelUUID, currentUser.getInternal_uuid());
                            if (!isOwner && !ChannelPermissionUtil.canEditChannel(channelUUID, currentUser.getInternal_uuid())) {
                                response = new ResponseModel("error", "You don't have permission to edit this channel.");
                                break;
                            }

                            if (isOwner && !ChannelDatabase.isChannelIdUnique(newChannelId, channelUUID)) {
                                response = new ResponseModel("error", "Channel ID is already taken by another channel.");
                                break;
                            }

                            boolean updated = ChannelDatabase.updateChannelInfo(channelUUID, newChannelId, name, description, imageUrl);

                            //RealTime
                            if (updated) {
                                List<UUID> subscribers = ChannelDatabase.getChannelSubscriberUUIDs(channelUUID);
                                RealTimeEventDispatcher.sendGroupOrChannelUpdate("channel", channelUUID, name, imageUrl, description, subscribers);
                            }

                            response = updated
                                    ? new ResponseModel("success", "Channel info updated successfully.")
                                    : new ResponseModel("error", "Failed to update channel info.");

                        } catch (Exception e) {
                            response = new ResponseModel("error", "Error updating channel: " + e.getMessage());
                        }
                        break;
                    }


                    case "delete_channel": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));

                        if (!ChannelDatabase.isOwner(channelId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "Only the owner can delete the channel.");
                            break;
                        }

                        List<UUID> subscriberIds = ChannelDatabase.getChannelSubscriberUUIDs(channelId);

                        boolean success = ChannelDatabase.deleteChannel(channelId);

                        //RealTime
                        if (success) {
                            RealTimeEventDispatcher.notifyChatDeleted("channel", channelId, subscriberIds);
                            response = new ResponseModel("success", "Channel deleted successfully.");
                        } else {
                            response = new ResponseModel("error", "Failed to delete channel.");
                        }

                        break;
                    }

                    case "remove_admin_from_group": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID groupId = UUID.fromString(requestJson.getString("group_id"));
                        UUID targetUserId = UUID.fromString(requestJson.getString("target_user_id"));

                        if (!GroupPermissionUtil.canRemoveAdmins(groupId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You are not allowed to remove admins.");
                            break;
                        }

                        String targetRole = GroupDatabase.getGroupRole(groupId, targetUserId);
                        if (targetRole == null) {
                            response = new ResponseModel("error", "User is not a member of the group.");
                            break;
                        }

                        if (targetRole.equals("owner")) {
                            response = new ResponseModel("error", "You cannot remove the owner.");
                            break;
                        }

                        if (!targetRole.equals("admin")) {
                            response = new ResponseModel("error", "Target user is not an admin.");
                            break;
                        }

                        boolean success = GroupDatabase.demoteAdminToMember(groupId, targetUserId);

                        // RealTime
                        if (success) {
                            Group group = GroupDatabase.findByInternalUUID(groupId);
                            if (group != null) {
                                RealTimeEventDispatcher.notifyRemovedAdminFromChat(
                                        "group",
                                        group.getInternal_uuid(),
                                        group.getGroup_name(),
                                        group.getImage_url(),
                                        targetUserId
                                );
                            }
                        }

                        response = success
                                ? new ResponseModel("success", "Admin removed successfully.")
                                : new ResponseModel("error", "Failed to remove admin.");

                        break;
                    }



                    case "transfer_channel_ownership": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID channelId = UUID.fromString(requestJson.getString("channel_id"));
                        String newOwnerUserIdStr = requestJson.getString("new_owner_user_id");

                        User newOwner = new userDatabase().findByUserId(newOwnerUserIdStr);
                        if (newOwner == null) {
                            response = new ResponseModel("error", "User not found.");
                            break;
                        }

                        if (!ChannelDatabase.isOwner(channelId, currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "Only the owner can transfer ownership.");
                            break;
                        }

                        boolean success = ChannelDatabase.transferOwnership(channelId, newOwner.getInternal_uuid());

                        //RealTime
                        if (success) {
                            List<UUID> subscribers = ChannelDatabase.getChannelSubscriberUUIDs(channelId);
                            Channel channel = ChannelDatabase.findByInternalUUID(channelId);  // 🟢 گرفتن اطلاعات کامل

                            // 1. ارسال ownership_transferred فقط به owner جدید
                            RealTimeEventDispatcher.sendOwnershipTransferred(
                                    "channel",
                                    channelId,
                                    channel.getChannel_name(), // ✅ اسم واقعی کانال
                                    List.of(newOwner.getInternal_uuid())
                            );

                            // 2. ارسال update_group_or_channel برای بروزرسانی UI تمام افراد
                            RealTimeEventDispatcher.sendGroupOrChannelUpdate(
                                    "channel",
                                    channelId,
                                    channel.getChannel_name(),     // ✅ name واقعی
                                    channel.getImage_url(),        // ✅ image
                                    channel.getDescription(),     // ✅ description
                                    subscribers
                            );
                        }



                        response = success

                                ? new ResponseModel("success", "Ownership transferred successfully.")
                                : new ResponseModel("error", "Failed to transfer ownership.");
                        break;
                    }

                    case "view_profile": {
                        UUID targetId = UUID.fromString(requestJson.getString("target_id"));
                        User user = userDatabase.findByInternalUUID(targetId);
                        if (user == null) {
                            response = new ResponseModel("error", "User not found.");
                            break;
                        }

                        JSONObject data = new JSONObject();
                        data.put("profile_name", user.getProfile_name());
                        data.put("user_id", user.getUser_id());
                        data.put("bio", user.getBio());
                        data.put("image_url", user.getImage_url());
                        data.put("is_online", userDatabase.isUserOnline(user.getInternal_uuid()));
                        data.put("last_seen", userDatabase.getLastSeen(user.getInternal_uuid()));

                        response = new ResponseModel("success", "Profile data", data);
                        break;
                    }

                    case "edit_admin_permissions": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        UUID chatId = UUID.fromString(requestJson.getString("chat_id"));
                        String chatType = requestJson.getString("chat_type");
                        UUID adminId = UUID.fromString(requestJson.getString("admin_id"));
                        JSONObject newPermissions = requestJson.getJSONObject("permissions");

                        boolean success = false;

                        if (chatType.equals("group")) {
                            success = GroupDatabase.updateAdminPermissions(chatId, adminId, newPermissions);
                        } else if (chatType.equals("channel")) {
                            success = ChannelDatabase.updateAdminPermissions(chatId, adminId, newPermissions);
                        } else {
                            response = new ResponseModel("error", "Invalid chat type.");
                            break;
                        }

                        if (success) {
                            JSONObject data = new JSONObject();
                            data.put("chat_id", chatId.toString());
                            data.put("chat_type", chatType);
                            data.put("permissions", newPermissions);

                            JSONObject event = new JSONObject();
                            event.put("action", "admin_permissions_updated");
                            event.put("data", data);

                            RealTimeEventDispatcher.sendToUser(adminId, event);
                        }


                        response = success
                                ? new ResponseModel("success", "Permissions updated successfully.")
                                : new ResponseModel("error", "Failed to update permissions.");
                        break;
                    }



//                    case "search_chat_members": {
//                        if (currentUser == null) {
//                            response = new ResponseModel("error", "Unauthorized. Please login first.");
//                            break;
//                        }
//
//                        UUID chatId = UUID.fromString(requestJson.getString("chat_id"));
//                        String chatType = requestJson.getString("chat_type");
//                        String query = requestJson.getString("query").toLowerCase();
//
//                        List<User> matched = new ArrayList<>();
//
//                        if (chatType.equals("group")) {
//                            if (!GroupDatabase.isMember(chatId, currentUser.getInternal_uuid())) {
//                                response = new ResponseModel("error", "You are not a member of this group.");
//                                break;
//                            }
//                            matched = GroupDatabase.searchGroupMembers(chatId, query);
//                        } else if (chatType.equals("channel")) {
//                            if (!ChannelDatabase.isAdmin(chatId, currentUser.getInternal_uuid())||!ChannelDatabase.isOwner(chatId, currentUser.getInternal_uuid())) {
//                                response = new ResponseModel("error", "Only admins or owner can search subscribers.");
//                                break;
//                            }
//                            matched = ChannelDatabase.searchSubscribers(chatId, query);
//                        } else {
//                            response = new ResponseModel("error", "Invalid chat type.");
//                            break;
//                        }
//
//                        JSONArray arr = new JSONArray();
//                        for (User u : matched) arr.put(u.toJSON());
//                        response = new ResponseModel("success", "Results found", Map.of("results", arr));
//                        break;
//                    }
//

                    case "archive_chat": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        UUID chatId = UUID.fromString(requestJson.getString("chat_id"));
                        String chatType = requestJson.getString("chat_type");

                        boolean success = ArchivedChatDatabase.archiveChat(currentUser.getInternal_uuid(), chatId, chatType);
                        response = success
                                ? new ResponseModel("success", "Chat archived successfully.")
                                : new ResponseModel("error", "Failed to archive chat.");
                        break;
                    }


                    case "unarchive_chat": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        UUID chatId = UUID.fromString(requestJson.getString("chat_id"));

                        boolean success = ArchivedChatDatabase.unarchiveChat(currentUser.getInternal_uuid(), chatId);
                        response = success
                                ? new ResponseModel("success", "Chat unarchived successfully.")
                                : new ResponseModel("error", "Failed to unarchive chat.");
                        break;
                    }

                    case "send_message" : {
                         response = handleSendMessage(requestJson);
                    }
                    break;

                    case "get_or_create_private_chat": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        try {
                            UUID user1 = UUID.fromString(requestJson.getString("user1"));
                            UUID user2 = UUID.fromString(requestJson.getString("user2"));

                            UUID oldChat = PrivateChatDatabase.findChatBetween(user1, user2);
                            UUID chatId = PrivateChatDatabase.getOrCreateChat(user1, user2);
                            boolean isNew = oldChat == null;

                            if (isNew) {
                                JSONObject chatPayload = new JSONObject();
                                chatPayload.put("action", "created_private_chat");

                                JSONObject chatData = new JSONObject();
                                chatData.put("chat_id", chatId.toString());
                                chatData.put("chat_type", "private");
                                chatData.put("last_message_time", LocalDateTime.now().toString());
                                chatPayload.put("data", chatData);

                                RealTimeEventDispatcher.sendToUser(user1, chatPayload);
                                RealTimeEventDispatcher.sendToUser(user2, chatPayload);
                            }

                            JSONObject data = new JSONObject();
                            data.put("chat_id", chatId.toString());
                            response = new ResponseModel("success", "Private chat ID retrieved.", data);
                        } catch (Exception e) {
                            e.printStackTrace();
                            response = new ResponseModel("error", "Invalid data or internal error.");
                        }

                        break;
                    }




                    case "get_private_chat_target": {
                        UUID chatId = UUID.fromString(requestJson.getString("chat_id"));
                        userId = currentUser.getInternal_uuid();

                        UUID targetId = PrivateChatDatabase.getOtherUserInChat(chatId, userId);
                        if (targetId == null) {
                            response = new ResponseModel("error", "Could not find other user.");
                        } else {
                            JSONObject data = new JSONObject();
                            data.put("target_id", targetId.toString());
                            response = new ResponseModel("success", "Target fetched.", data);
                        }
                        break;
                    }

                    case "get_contact_list": {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }

                        List<Contact> contacts = ContactDatabase.getContacts(currentUser.getInternal_uuid());
                        List<ContactEntry> contactEntries = new ArrayList<>();

                        for (Contact contact : contacts) {
                            UUID contactId = contact.getContact_id();
                            User contactUser = userDatabase.findByInternalUUID(contactId);
                            if (contactUser == null) continue;

                            ContactEntry entry = new ContactEntry(
                                    contactId,
                                    contactUser.getUser_id(),
                                    contactUser.getProfile_name(),
                                    contactUser.getImage_url(),
                                    contact.getIs_blocked()
                            );

                            contactEntries.add(entry);
                        }

                        // Optional: sort alphabetically
                        contactEntries.sort(Comparator.comparing(ContactEntry::getProfileName, String.CASE_INSENSITIVE_ORDER));

                        JSONObject data = new JSONObject();
                        data.put("contact_list", JsonUtil.contactEntryListToJson(contactEntries));

                        response = new ResponseModel("success", "Contact list refreshed", data);
                        break;
                    }

                    case "get_chat_messages" : {
                        UUID chatId = UUID.fromString(requestJson.getString("chat_id"));
                        String chatType = requestJson.getString("chat_type");
                        int offset = requestJson.optInt("offset", 0);
                        int limit = requestJson.optInt("limit", 10);

                        List<Message> messages = MessageDatabase.getMessagesForChat(chatId, chatType, currentUser.getInternal_uuid(), offset, limit);

                        JSONArray result = new JSONArray();
                        for (Message m : messages) {
                            JSONObject obj = new JSONObject();
                            obj.put("message_id", m.getMessage_id().toString());
                            obj.put("sender_id", m.getSender_id().toString());

                            User sender = userDatabase.findByInternalUUID(m.getSender_id());
                            obj.put("sender_name", sender != null ? sender.getProfile_name() : "Unknown");

                            obj.put("receiver_id", m.getReceiver_id().toString());
                            obj.put("receiver_type", m.getReceiver_type());

                            String receiverName = switch (m.getReceiver_type()) {
                                case "group" -> {
                                    Group g = GroupDatabase.findByInternalUUID(m.getReceiver_id());
                                    yield g != null ? g.getGroup_name() : "Unknown group";
                                }
                                case "channel" -> {
                                    Channel c = ChannelDatabase.findByInternalUUID(m.getReceiver_id());
                                    yield c != null ? c.getChannel_name() : "Unknown channel";
                                }
                                case "private" -> {
                                    UUID otherId = PrivateChatDatabase.getOtherParticipant(
                                            m.getReceiver_id(),             // chat_id
                                            currentUser.getInternal_uuid()  // my user uuid
                                    );
                                    User other = userDatabase.findByInternalUUID(otherId);
                                    yield other != null ? other.getProfile_name() : "Unknown user";
                                }

                                default -> "Unknown";
                            };
                            obj.put("receiver_name", receiverName);

                            obj.put("content", m.getContent());
                            obj.put("message_type", m.getMessage_type());
                            obj.put("time", m.getSend_at().toString());
                            obj.put("is_edited", m.isIs_edited());
                            obj.put("is_deleted_globally", m.isIs_deleted_globally());
                            obj.put("edited_at", m.getEdited_at() != null ? m.getEdited_at().toString() : JSONObject.NULL);

                            //For reply messages
                            if (m.getReply_to_id() != null) {
                                Message replied = MessageDatabase.findById(m.getReply_to_id());
                                if (replied != null) {
                                    User repliedSender = userDatabase.findByInternalUUID(replied.getSender_id());
                                    obj.put("reply_to_id", replied.getMessage_id().toString());
                                    obj.put("reply_to_sender", repliedSender != null ? repliedSender.getProfile_name() : "Unknown");
                                    obj.put("reply_to_content", replied.getContent());
                                }
                            }

                            //  For forwarded messages
                            if (m.getOriginal_message_id() != null && m.getForwarded_from() != null) {
                                User originalSender = userDatabase.findByInternalUUID(m.getForwarded_from());
                                obj.put("is_forwarded", true);
                                obj.put("forwarded_from_id", m.getForwarded_from().toString());
                                obj.put("forwarded_from_name", originalSender != null ? originalSender.getProfile_name() : "Unknown");
                            } else {
                                obj.put("is_forwarded", false);
                            }

                            //For reactions
                            List<String> reactions = MessageReactionDatabase.getReactions(m.getMessage_id());
                            obj.put("reactions", new JSONArray(reactions));


                            result.put(obj);
                        }



                        JSONObject data = new JSONObject();
                        data.put("get_chat_messages", result);

                        response = new ResponseModel("success", "get messages ", data);
                        break;
                    }

                    case "delete_message" : {
                        UUID messageId = UUID.fromString(requestJson.getString("message_id"));
                        String deleteType = requestJson.getString("delete_type");

                        Message msg = MessageDatabase.findById(messageId);
                        if (msg == null) {
                            response = new ResponseModel("error", "Message not found.");
                            break;
                        }

                        UUID currentUserId = currentUser.getInternal_uuid();

                        if (deleteType.equals("one-sided")) {
                            boolean success = MessageDatabase.markAsDeletedForUser(messageId, currentUserId);
                            if (success)
                                response = new ResponseModel("success", "Message deleted for current user.");
                            else
                                response = new ResponseModel("error", "Failed to delete message for user.");
                            break;
                        }

                        //If allowed to delete the message
                        if (deleteType.equals("global")) {
                            boolean allowed = false;

                            String type = msg.getReceiver_type();
                            UUID chatId = msg.getReceiver_id();

                            if (type.equals("private") || type.equals("group")) {
                                allowed = msg.getSender_id().equals(currentUserId);
                            } else if (type.equals("channel")) {
                                //Only owner and admins can delete messages
                                allowed = ChannelPermissionUtil.canDeleteMessage(chatId, currentUserId);
                            }

                            if (!allowed) {
                                response = new ResponseModel("error", "You are not allowed to delete this message globally.");
                                break;
                            }

                            boolean success = MessageDatabase.markAsGloballyDeleted(messageId);
                            if (success) {
                                response = new ResponseModel("success", "Message deleted globally.");
                                Message updated = MessageDatabase.findById(messageId);
                                List<UUID> receivers = Receivers.resolveFor(updated.getReceiver_type(), updated.getReceiver_id(), null);
                                RealTimeEventDispatcher.notifyMessageDeletedGlobal(updated.getReceiver_id(), messageId, receivers);

                            }
                            else{
                                response = new ResponseModel("error", "Failed to delete message globally.");
                            }
                            break;
                        }

                        response = new ResponseModel("error", "Invalid delete_type.");
                        break;
                    }

                    case "edit_message" : {
                        UUID msgId = UUID.fromString(requestJson.getString("message_id"));
                        String newContent = requestJson.getString("new_content");

                        Message msg = MessageDatabase.findById(msgId);
                        if (msg == null) {
                            response = new ResponseModel("error", "Message not found.");
                            break;
                        }

                        //Only sender can edit
                        if (!msg.getSender_id().equals(currentUser.getInternal_uuid())) {
                            response = new ResponseModel("error", "You can only edit your own messages.");
                            break;
                        }

                        boolean success = MessageDatabase.updateContentAndMarkEdited(msgId, newContent);
                        if (!success) {
                            response = new ResponseModel("error", "Failed to update message.");
                        } else {
                            response = new ResponseModel("success", "Message edited.");
                            Message updated = MessageDatabase.findById(msgId);
                            List<UUID> receivers = Receivers.resolveFor(updated.getReceiver_type(), updated.getReceiver_id(), null);
                            RealTimeEventDispatcher.notifyMessageEdited(updated.getReceiver_id(), updated.getMessage_id(), newContent, LocalDateTime.now(), receivers);

                        }
                        break;
                    }

                    case "send_reply_message" : {
                        UUID senderId = currentUser.getInternal_uuid();
                        String content = requestJson.getString("content");
                        String receiverType = requestJson.getString("receiver_type");
                        UUID receiverId = UUID.fromString(requestJson.getString("receiver_id"));
                        //For block
                        if ("private".equals(receiverType) && !canSendToPrivate(receiverId, senderId)) {
                            response = new ResponseModel("error", "You can't message this user (blocked).");
                            break;
                        }

                        UUID replyToId = UUID.fromString(requestJson.getString("reply_to_id"));

                        Message message = new Message(
                                UUID.randomUUID(),
                                senderId,
                                receiverType,
                                receiverId,
                                content,
                                "TEXT",
                                LocalDateTime.now(),
                                "SEND",
                                replyToId
                        );

                        boolean saved = MessageDatabase.saveReplyMessage(message);

                        String excerpt = MessageDatabase.getExcerpt(replyToId);
                        JSONObject meta = new JSONObject().put("reply_to", new JSONObject()
                                .put("id", replyToId.toString())
                                .put("excerpt", excerpt));

                        List<UUID> receivers = Receivers.resolveFor(receiverType, receiverId, senderId);
                        //RealTimeEventDispatcher.sendNewMessage(message, receivers, "reply", meta);
                        RealTimeEventDispatcher.sendNewMessageFiltered(message, receivers, senderId, "reply", meta);


                        response = saved ?
                                new ResponseModel("success", "Reply sent") :
                                new ResponseModel("error", "Failed to send reply");
                        break;
                    }

                    case "forward_message" : {
                        UUID senderId = currentUser.getInternal_uuid();
                        UUID originalMessageId = UUID.fromString(requestJson.getString("original_message_id"));
                        UUID targetChatId = UUID.fromString(requestJson.getString("target_chat_id"));
                        String targetChatType = requestJson.getString("target_chat_type");

                        //For block
                        if ("private".equals(targetChatType) && !canSendToPrivate(targetChatId, senderId)) {
                            response = new ResponseModel("error", "You can't message this user (blocked).");
                            break;
                        }

                        // original message
                        Message original = MessageDatabase.findById(originalMessageId);
                        if (original == null) {
                            response = new ResponseModel("error", "Original message not found.");
                            break;
                        }

                        //make forwarded message
                        Message forwarded = new Message(
                                UUID.randomUUID(),
                                currentUser.getInternal_uuid(),
                                targetChatType,
                                targetChatId,
                                original.getContent(),
                                original.getMessage_type(),
                                LocalDateTime.now(),
                                "SEND",
                                null,           // reply_to_id
                                false,          // is_edited
                                false,          // is_deleted_globally
                                original.getMessage_id(), //original message id
                                currentUser.getInternal_uuid(),       // forwarded_by
                                original.getSender_id(),              // forwarded_from
                                null                                   // edited_at
                        );

                        boolean success = MessageDatabase.saveForwardedMessage(forwarded);

                        if (success) {
                            response = new ResponseModel("success", "Message forwarded.");
                            JSONObject meta = new JSONObject().put("forwarded_from", new JSONObject()
                                    .put("chat_id", original.getReceiver_id().toString())
                                    .put("message_id", original.getMessage_id().toString())
                                    .put("sender_id", original.getSender_id().toString())
                                    .put("sender_name", userDatabase.findByInternalUUID(original.getSender_id()).getProfile_name()));

                            List<UUID> receivers = Receivers.resolveFor(targetChatType, targetChatId, currentUser.getInternal_uuid());
                            //RealTimeEventDispatcher.sendNewMessage(forwarded, receivers, "forward", meta);
                            RealTimeEventDispatcher.sendNewMessageFiltered(forwarded, receivers, senderId, "forward", meta);

                        } else {
                            response = new ResponseModel("error", "Failed to forward message.");
                        }
                        break;
                    }

                    case "react_to_message": {
                        UUID messageId = UUID.fromString(requestJson.getString("message_id"));
                        String reaction = requestJson.getString("reaction");
                        boolean success = MessageReactionDatabase.saveOrUpdateReaction(messageId, currentUser.getInternal_uuid(), reaction);


                        if (success) {
                            Message msg = MessageDatabase.findById(messageId);

                            JSONObject counts = MessageReactionDatabase.getCountsAsJson(messageId);

                            List<UUID> receivers = Receivers.resolveFor(msg.getReceiver_type(), msg.getReceiver_id(), null);

                            RealTimeEventDispatcher.notifyReactionAdded(
                                    msg.getReceiver_id(),           // chatId
                                    messageId,                       // messageId
                                    reaction,                        // emoji
                                    counts.optInt(reaction, 0),
                                    counts,
                                    receivers
                            );

                            response = new ResponseModel("success", "Reaction saved.");
                        } else {
                            response = new ResponseModel("error", "Failed to save reaction.");
                        }

                        break;
                    }

                    case "get_user_profile": {
                        // Get the current user's UUID
                        UUID user_UUID = this.currentUser.getInternal_uuid();

                        // Get the profile JSON from SidebarService
                        JSONObject data = SidebarService.getUserProfile(user_UUID);

                        if (data == null) {
                            response = new ResponseModel("error", "Failed to retrieve user profile.");
                        } else {
                            response = new ResponseModel("success", "User profile retrieved successfully.", data);
                        }
                        break;
                    }

                    case "edit_profile_name": {
                        // Validate input
                        if (!requestJson.has("new_profile_name")) {
                            response = new ResponseModel("error", "Missing new profile name.");
                            break;
                        }

                        UUID user_UUID = this.currentUser.getInternal_uuid();
                        String newProfileName = requestJson.getString("new_profile_name");

                        response = SidebarService.updateProfileName(user_UUID, newProfileName);
                        break;
                    }

                    case "edit_user_id": {
                        // Validate input
                        if (!requestJson.has("new_user_id")) {
                            response = new ResponseModel("error", "Missing new user ID.");
                            break;
                        }

                        UUID user_UUID = this.currentUser.getInternal_uuid();
                        String newUserId = requestJson.getString("new_user_id");

                        response = SidebarService.updateUserId(user_UUID, newUserId);
                        break;
                    }

                    case "edit_bio": {
                        // Validate input
                        if (!requestJson.has("new_bio")) {
                            response = new ResponseModel("error", "Missing new bio.");
                            break;
                        }

                        UUID user_UUID = this.currentUser.getInternal_uuid();
                        String newBio = requestJson.getString("new_bio").trim();

                        response = SidebarService.updateBio(user_UUID, newBio);
                        break;
                    }

                    case "edit_profile_picture": {
                        // Validate input
                        if (!requestJson.has("new_image_url")) {
                            response = new ResponseModel("error", "Missing new image url.");
                            break;
                        }

                        UUID user_UUID = this.currentUser.getInternal_uuid();
                        String newImageUrl = requestJson.getString("new_image_url").trim();

                        response = SidebarService.updateProfilePicture(user_UUID, newImageUrl);
                        break;
                    }

                    case "get_saved_messages": {
                        UUID user_Id = UUID.fromString(requestJson.getString("user_id"));
//                        response = SidebarService.handleGetSavedMessages(user_Id);
                        break;
                    }

                    case "send_saved_messages": {
                        response = SidebarService.handleSendMessage(requestJson);
                        break;
                    }

                    case "search_contacts": {
                        String user_id = requestJson.getString("user_id");
                        String search_term = requestJson.getString("search_term");

                        response = SidebarService.handleSearchContacts(user_id, search_term);
                        break;
                    }

                    case "remove_contact": {
                        UUID user_id = UUID.fromString(requestJson.getString("user_id"));
                        UUID contactId = UUID.fromString(requestJson.getString("contact_id"));

                        response = SidebarService.handleRemoveContact(user_id, contactId);
                        break;
                    }

                    default:
                        response = new ResponseModel("error", "Unknown action: " + action);
                }

                if (response == null) {
                    response = new ResponseModel("error", "No response generated for action: " + action);
                }

                JSONObject responseJson = new JSONObject();
                responseJson.put("status", response.getStatus());
                responseJson.put("message", response.getMessage());
                responseJson.put("data", response.getData() != null ? response.getData() : JSONObject.NULL);

                if (requestJson.has("request_id")) {
                    String requestId = requestJson.getString("request_id");
                    response.setRequestId(requestId);
                    responseJson.put("request_id", requestId);
                }

                out.println(responseJson.toString());
                System.out.println("📤 Sent response: " + responseJson.toString(2));

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
                    //RealTime
                    userId = currentUser.getInternal_uuid();
                    List<UUID> contacts = ContactDatabase.getContactUUIDs(userId);
                    RealTimeEventDispatcher.notifyUserStatusChanged(userId, "offline", contacts);

                    System.out.println("🔚 Client disconnected. Cleaning up user " + userId);
                    userDatabase.updateUserStatus(userId, "offline");
                    userDatabase.updateLastSeen(userId);
                    SessionManager.removeUser(userId);
                }
                else {
                    System.out.println("❗ currentUser is null, couldn't set offline.");
                }
                socket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


    }


    private ResponseModel handleSendMessage(JSONObject json) {

        try {
            if (currentUser == null)
                return new ResponseModel("error", "Unauthorized. Please login first.");

            UUID messageId = UUID.randomUUID();
            UUID senderId = currentUser.getInternal_uuid();
            String receiverType = json.getString("receiver_type");
            UUID receiverId;
            receiverId = UUID.fromString(json.getString("receiver_id"));

            if(Objects.equals(receiverType, "private")){
                PrivateChatDatabase.clearDeletedFlag(senderId, receiverId);
                UUID other = PrivateChatDatabase.getOtherParticipant(receiverId, senderId);
                if (other == null) {
                    return new ResponseModel("error", "Invalid private chat.");
                }
                if (ContactDatabase.isBlocked(senderId, other) || ContactDatabase.isBlocked(other, senderId)) {
                    return new ResponseModel("error", "You can't message this user (blocked).");
                }
            }


            String content = json.optString("content", "");
            String messageType = json.optString("message_type", "TEXT");

            boolean inserted = MessageDatabase.insertMessage(messageId, senderId, receiverId, receiverType, content, messageType);
            if (!inserted)
                return new ResponseModel("error", "Failed to insert message.");

            if (json.has("attachments")) {
                JSONArray attachmentsArray = json.getJSONArray("attachments");
                List<FileAttachment> attachments = new ArrayList<>();

                for (int i = 0; i < attachmentsArray.length(); i++) {
                    JSONObject attJson = attachmentsArray.getJSONObject(i);
                    attachments.add(new FileAttachment(
                            attJson.getString("file_url"),
                            attJson.getString("file_type")
                    ));
                }

                boolean attInserted = MessageDatabase.insertAttachments(messageId, attachments);
                if (!attInserted)
                    return new ResponseModel("error", "Message inserted but failed to attach files.");
            }

            // Send real-time message
            Message msg = new Message(messageId, senderId, receiverId, receiverType, content, messageType, LocalDateTime.now());
            List<UUID> receivers = getReceiversForChat(receiverId, receiverType);
            receivers.remove(senderId);
            RealTimeEventDispatcher.sendNewMessage(msg, receivers);

            // Update chat list (last_message_time)
            JSONObject chatUpdate = new JSONObject();
            chatUpdate.put("chat_id", receiverId.toString());
            chatUpdate.put("chat_type", receiverType);
            chatUpdate.put("last_message_time", LocalDateTime.now().toString());

            JSONObject chatPayload = new JSONObject();
            chatPayload.put("action", "chat_updated");
            chatPayload.put("data", chatUpdate);

            for (UUID receiver : receivers) {
                RealTimeEventDispatcher.sendToUser(receiver, chatPayload);
            }

            JSONObject data = new JSONObject();
            data.put("message_id", messageId.toString());
            return new ResponseModel("success", "Message sent successfully.", data);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseModel("error", "Exception occurred while sending message.");
        }
    }


    private List<UUID> getReceiversForChat(UUID receiverId, String receiverType) {
        switch (receiverType) {
            case "private":
                return PrivateChatDatabase.getMembers(receiverId);
            case "group":
                return GroupDatabase.getMemberUUIDs(receiverId);
            case "channel":
                return ChannelDatabase.getSubscriberUUIDs(receiverId);
            default:
                return new ArrayList<>();
        }
    }


    private ResponseModel handleDeleteChat(JSONObject json) {
        try {
            if (currentUser == null)
                return new ResponseModel("error", "Unauthorized");

            UUID chatId = UUID.fromString(json.getString("chat_id"));
            boolean bothSides = json.optBoolean("both_sides", json.optBoolean("both", false));
            PrivateChat chat = PrivateChatDatabase.findById(chatId);
            if (chat == null) return new ResponseModel("error", "Chat not found.");

            UUID self = currentUser.getInternal_uuid();
            UUID other = chat.getUser1_id().equals(self) ? chat.getUser2_id() : chat.getUser1_id();

            if (bothSides) {
                PrivateChatDatabase.markBothDeleted(chatId);
                MessageDatabase.markGloballyDeleted(chatId);
                MessageDatabase.logDeletedMessagesFor(chatId, self);
                MessageDatabase.logDeletedMessagesFor(chatId, other);
                return new ResponseModel("success", "Chat deleted for both sides.");
            } else {
                if (chat.getUser1_id().equals(self))
                    PrivateChatDatabase.markUser1Deleted(chatId);
                else
                    PrivateChatDatabase.markUser2Deleted(chatId);

                MessageDatabase.logDeletedMessagesFor(chatId, self);
                return new ResponseModel("success", "Chat deleted (one-sided).");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseModel("error", "Exception while deleting chat.");
        }
    }

    // helper
    private boolean canSendToPrivate(UUID chatId, UUID senderId) {
        UUID other = PrivateChatDatabase.getOtherParticipant(chatId, senderId);
        if (other == null) return false;
        return !(ContactDatabase.isBlocked(senderId, other) || ContactDatabase.isBlocked(other, senderId));
    }




}