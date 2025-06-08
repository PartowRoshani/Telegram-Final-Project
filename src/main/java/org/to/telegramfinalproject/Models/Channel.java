package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Channel {
    private UUID internal_uuid;
    private String channel_id;
    private String channel_name;
    private UUID creator_id;
    private String image_url;
    private String description;
    private LocalDateTime created_at;
    private List<ChannelSubscribe> members;

    public Channel(UUID internal_uuid, String channel_name, UUID creator_id, LocalDateTime created_at){
        this.internal_uuid = internal_uuid;
        this.channel_name = channel_name;
        this.creator_id = creator_id;
        this.created_at = created_at;
    }

    public Channel() {

    }

    public void setChannel_id(String Channel_id){this.channel_id = Channel_id;}
    public void setCreator_id(UUID creator_id){this.creator_id = creator_id;}
    public void setChannel_name(String channel_name){this.channel_name = channel_name;}
    public void setImage_url(String image_url){this.image_url = image_url;}
    public void setCreated_at(LocalDateTime created_at){this.created_at = created_at;}
    public void setDescription(String description){this.description =description;}
    public void setMembers(List<ChannelSubscribe> members){this.members = members;}

    public String getChannel_id(){return  this.channel_id;}
    public UUID getCreator_id(){return   this.creator_id;}
    public String getChannel_name(){return  this.channel_name;}
    public String getImage_url(){return  this.image_url;}
    public LocalDateTime getCreated_at(){return  this.created_at;}
    public String getDescription(){return  this.description;}
    public List<ChannelSubscribe> getMembers(){return  this.members;}
    public UUID getInternal_uuid() {
        return this.internal_uuid;
    }

    public void setInternal_uuid(UUID internalUuid) { this.internal_uuid = internalUuid;
    }
}
