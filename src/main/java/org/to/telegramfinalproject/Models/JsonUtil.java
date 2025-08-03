package org.to.telegramfinalproject.Models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class JsonUtil {
    public static JSONArray contactListToJson(List<Contact> contacts) {


        JSONArray array = new JSONArray();
        for (Contact contact : contacts) {
            JSONObject obj = new JSONObject();
            obj.put("user_id", contact.getUser_id().toString());
            obj.put("contact_id", contact.getContact_id().toString());
            obj.put("is_blocked", contact.getIs_blocked());
            obj.put("added_at", contact.getAdd_at().toString());
            array.put(obj);
        }
        return array;
    }




    public static JSONObject userToJson(User user) {
        JSONObject obj = new JSONObject();
        obj.put("internal_uuid", user.getInternal_uuid().toString());
        obj.put("user_id", user.getUser_id() != null ?user.getUser_id().toString() :JSONObject.NULL);
        obj.put("username", user.getUsername());
        obj.put("profile_name", user.getProfile_name());
        obj.put("bio", user.getBio()!= null ?user.getBio().toString() :JSONObject.NULL);
        obj.put("image_url", user.getImage_url() != null ?user.getImage_url().toString() :JSONObject.NULL);
        obj.put("status", user.getStatus());
        obj.put("last_seen", user.getLast_seen() != null ? user.getLast_seen().toString() : JSONObject.NULL);
        obj.put("contactList", JsonUtil.contactListToJson(user.getContactList()));
        obj.put("channelList", JsonUtil.channelListToJson(user.getChannelList()));
        obj.put("groupList", JsonUtil.groupListToJson(user.getGroupList()));
        obj.put("unreadMessages", JsonUtil.messageListToJson(user.getUnreadMessages()));
        return obj;
    }


    public static JSONArray messageListToJson(List<Message> messages) {
        JSONArray array = new JSONArray();
        for (Message message : messages) {
            JSONObject obj = new JSONObject();
            obj.put("message_id", message.getMessage_id().toString());
            obj.put("sender_id", message.getSender_id() != null ? message.getSender_id().toString() : JSONObject.NULL);
            obj.put("receiver_type", message.getReceiver_type());
            obj.put("receiver_id", message.getReceiver_id().toString());
            obj.put("content", message.getContent());
            obj.put("message_type", message.getMessage_type());
            obj.put("send_at", message.getSend_at().toString());
            obj.put("status", message.getStatus());
            obj.put("reply_to_id", message.getReply_to_id() != null ? message.getReply_to_id().toString() : JSONObject.NULL);
            obj.put("is_edited", message.isIs_edited());
            obj.put("original_message_id", message.getOriginal_message_id() != null ? message.getOriginal_message_id().toString() : JSONObject.NULL);
            obj.put("forwarded_by", message.getForwarded_by() != null ? message.getForwarded_by().toString() : JSONObject.NULL);
            obj.put("forwarded_from", message.getForwarded_from() != null ? message.getForwarded_from().toString() : JSONObject.NULL);
            array.put(obj);
        }
        return array;
    }




    public static JSONArray groupListToJson(List<Group> groups) {
        JSONArray array = new JSONArray();
        for (Group group : groups) {
            JSONObject obj = new JSONObject();
            obj.put("internal_uuid", group.getInternal_uuid().toString());
            obj.put("group_id", group.getGroup_id() != null ?group.getGroup_id().toString() :JSONObject.NULL);
            obj.put("group_name", group.getGroup_name());
            obj.put("creator_id", group.getCreator_id().toString());
            obj.put("image_url",group.getImage_url()!= null ?group.getImage_url().toString() : JSONObject.NULL );
            obj.put("description", group.getDescription() != null ?group.getDescription().toString() : JSONObject.NULL );
            obj.put("created_at", group.getCreated_at().toString());
            obj.put("members", JsonUtil.groupMemberListToJson(group.getMembers()));
            array.put(obj);
        }
        return array;
    }




    public static JSONArray channelListToJson(List<Channel> channels) {
        JSONArray array = new JSONArray();
        for (Channel channel : channels) {
            JSONObject obj = new JSONObject();
            obj.put("internal_uuid",channel.getInternal_uuid().toString());
            obj.put("channel_id", channel.getChannel_id() != null ?channel.getChannel_id().toString() :JSONObject.NULL);
            obj.put("channel_name", channel.getChannel_name());
            obj.put("creator_id", channel.getCreator_id().toString());
            obj.put("image_url",channel.getImage_url()!= null ?channel.getImage_url().toString() : JSONObject.NULL );
            obj.put("description",channel.getDescription() != null ?channel.getDescription().toString() : JSONObject.NULL );
            obj.put("created_at",channel.getCreated_at().toString());
            obj.put("members", JsonUtil.channelSubscribeToJson(channel.getMembers()));
            array.put(obj);
        }
        return array;
    }



    public static JSONArray groupMemberListToJson(List<GroupMember> members) {
        JSONArray array = new JSONArray();

        if (members == null) {
            return array;
        }

        for (GroupMember m : members) {
            JSONObject obj = new JSONObject();
            obj.put("group_id", m.getGroup_id().toString());
            obj.put("user_id", m.getUser_id().toString());
            obj.put("joined_at", m.getJoin_at().toString());
            obj.put("role", m.getRole());
            array.put(obj);
        }

        return array;
    }


    public static JSONArray channelSubscribeToJson(List<ChannelSubscribe> subscribes) {
        JSONArray array = new JSONArray();

        if (subscribes == null) {
            return array;
        }

        for (ChannelSubscribe s : subscribes) {
            JSONObject obj = new JSONObject();
            obj.put("channel_id", s.getChannel_id().toString());
            obj.put("user_id", s.getUser_id().toString());
            obj.put("Subscribed_at", s.getJoin_at().toString());
            array.put(obj);
        }

        return array;
    }



    public static JSONArray chatListToJson(List<ChatEntry> chatList) {
        JSONArray jsonArray = new JSONArray();

        for (ChatEntry entry : chatList) {
            JSONObject obj = new JSONObject();
            obj.put("internal_id", entry.getId().toString());
            obj.put("id", entry.getDisplayId());
            obj.put("name", entry.getName());
            obj.put("image_url", entry.getImageUrl());
            obj.put("type", entry.getType());
            obj.put("last_message_time", entry.getLastMessageTime() == null ? JSONObject.NULL : entry.getLastMessageTime().toString());
            obj.put("is_owner", entry.isOwner());
            obj.put("is_admin", entry.isAdmin());


            jsonArray.put(obj);
        }

        return jsonArray;
    }


    public static JSONObject chatToJson(ChatEntry chat) {
        JSONObject obj = new JSONObject();
        obj.put("id", chat.getId());
        obj.put("name", chat.getName());
        obj.put("image_url", chat.getImageUrl() != null ? chat.getImageUrl() : JSONObject.NULL);
        obj.put("type", chat.getType());
        obj.put("last_message_time", chat.getLastMessageTime() != null ? chat.getLastMessageTime().toString() : JSONObject.NULL);
        return obj;
    }



}
