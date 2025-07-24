package org.to.telegramfinalproject.Server;

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
                                        target.getInternal_uuid(),      // internal UUID
                                        target.getUser_id(),            // public display ID
                                        target.getProfile_name(),
                                        target.getImage_url(),
                                        "private",
                                        last,
                                        false,
                                        false
                                ));
                            }

                            for (Group group : groups) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(group.getInternal_uuid(), "group");
                                boolean isOwner = GroupDatabase.isOwner(group.getInternal_uuid(), user.getInternal_uuid());
                                boolean isAdmin = GroupDatabase.isAdmin(group.getInternal_uuid(), user.getInternal_uuid());

                                chatList.add(new ChatEntry(
                                        group.getInternal_uuid(),
                                        group.getGroup_id(),
                                        group.getGroup_name(),
                                        group.getImage_url(),
                                        "group",
                                        last,
                                        isOwner,
                                        isAdmin
                                ));
                            }

                            for (Channel channel : channels) {
                                LocalDateTime last = MessageDatabase.getLastMessageTime(channel.getInternal_uuid(), "channel");
                                boolean isOwner = ChannelDatabase.isOwner(channel.getInternal_uuid(), user.getInternal_uuid());
                                boolean isAdmin = ChannelDatabase.isAdmin(channel.getInternal_uuid(), user.getInternal_uuid());

                                chatList.add(new ChatEntry(
                                        channel.getInternal_uuid(),
                                        channel.getChannel_id(),
                                        channel.getChannel_name(),
                                        channel.getImage_url(),
                                        "channel",
                                        last,
                                        isOwner,
                                        isAdmin
                                ));
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
                            obj.put("sender", m.getSender_id().toString());
                            obj.put("time", m.getSend_at().toString());
                            obj.put("receiver_id", m.getReceiver_id().toString());
                            obj.put("receiver_type", m.getReceiver_type());
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

                        List<ChatEntry> chatList = new ArrayList<>();

                        for (Contact contact : contacts) {
                            User target = userDatabase.findByInternalUUID(contact.getContact_id());
                            if (target == null) continue;

                            LocalDateTime last = MessageDatabase.getLastMessageTimeBetween(currentUser.getInternal_uuid(), target.getInternal_uuid(), "private");

                            chatList.add(new ChatEntry(
                                    target.getInternal_uuid(),
                                    target.getUser_id(),
                                    target.getProfile_name(),
                                    target.getImage_url(),
                                    "private",
                                    last,
                                    false,
                                    false
                            ));
                        }

                        for (Group group : groups) {
                            LocalDateTime last = MessageDatabase.getLastMessageTime(group.getInternal_uuid(), "group");
                            boolean isOwner = GroupDatabase.isOwner(group.getInternal_uuid(), currentUser.getInternal_uuid());
                            boolean isAdmin = GroupDatabase.isAdmin(group.getInternal_uuid(), currentUser.getInternal_uuid());

                            chatList.add(new ChatEntry(
                                    group.getInternal_uuid(),
                                    group.getGroup_id(),
                                    group.getGroup_name(),
                                    group.getImage_url(),
                                    "group",
                                    last,
                                    isOwner,
                                    isAdmin
                            ));
                        }

                        for (Channel channel : channels) {
                            LocalDateTime last = MessageDatabase.getLastMessageTime(channel.getInternal_uuid(), "channel");
                            boolean isOwner = ChannelDatabase.isOwner(channel.getInternal_uuid(), currentUser.getInternal_uuid());
                            boolean isAdmin = ChannelDatabase.isAdmin(channel.getInternal_uuid(), currentUser.getInternal_uuid());

                            chatList.add(new ChatEntry(
                                    channel.getInternal_uuid(),
                                    channel.getChannel_id(),
                                    channel.getChannel_name(),
                                    channel.getImage_url(),
                                    "channel",
                                    last,
                                    isOwner,
                                    isAdmin
                            ));
                        }

                        chatList.sort((a, b) -> {
                            if (a.getLastMessageTime() == null) return 1;
                            if (b.getLastMessageTime() == null) return -1;
                            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                        });

                        JSONObject data = new JSONObject();
                        data.put("chat_list", JsonUtil.chatListToJson(chatList));
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
                                    User otherUser = new userDatabase().findByInternalUUID(UUID.fromString(receiverId));
                                    if (otherUser == null) {
                                        response = new ResponseModel("error", "User not found.");
                                        break;
                                    }
                                    messages = MessageDatabase.privateChatHistory(currentUser.getInternal_uuid(), otherUser.getInternal_uuid());
                                }
                                case "group" -> {
                                    Group group = GroupDatabase.findByInternalUUID(UUID.fromString(receiverId));
                                    if (group == null) {
                                        response = new ResponseModel("error", "Group not found.");
                                        break;
                                    }
                                    messages = MessageDatabase.groupChatHistory(group.getInternal_uuid());
                                }


                                case "channel" -> {
                                    Channel channel = ChannelDatabase.findByInternalUUID(UUID.fromString(receiverId));
                                    if (channel == null) {
                                        response = new ResponseModel("error", "Channel not found.");
                                        break;
                                    }
                                    messages = MessageDatabase.channelChatHistory(channel.getInternal_uuid());
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



                    case "delete_private_chat" : {
                        if (currentUser == null) {
                            response = new ResponseModel("error", "Unauthorized. Please login first.");
                            break;
                        }
                        UUID targetId = UUID.fromString(requestJson.getString("target_id"));
                        boolean both = requestJson.getBoolean("both");

                        //RealTime
                        if (both) {
                            RealTimeEventDispatcher.notifyChatDeleted("private", targetId, List.of(currentUser.getInternal_uuid()));
                            RealTimeEventDispatcher.notifyChatDeleted("private", currentUser.getInternal_uuid(), List.of(targetId));
                        } else {
                            RealTimeEventDispatcher.notifyChatDeleted("private", targetId, List.of(currentUser.getInternal_uuid()));
                        }
                        response = PrivateChatService.deletePrivateChat(currentUser.getInternal_uuid(), targetId, both);
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
}