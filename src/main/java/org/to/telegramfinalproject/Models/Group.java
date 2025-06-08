package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Group {
    private UUID internal_uuid;
    private String group_id;
    private String group_name;
    private UUID creator_id;
    private String image_url;
    private String description;
    private LocalDateTime created_at;
    private List<GroupMember> members;

    public Group(UUID internal_uuid, String group_name, UUID creator_id, LocalDateTime created_at){
        this.internal_uuid = internal_uuid;
        this.group_name = group_name;
        this.creator_id = creator_id;
        this.created_at = created_at;
    }

    public Group() {

    }

    public void setGroup_id(String group_id){this.group_id = group_id;}
    public void  setCreator_id(UUID creator_id){this.creator_id = creator_id;}
    public void setGroup_name(String group_name){this.group_name = group_name;}
    public void setImage_url(String image_url){this.image_url = image_url;}
    public void setCreated_at(LocalDateTime created_at){this.created_at = created_at;}
    public void setDescription(String description){this.description =description;}
    public void setMembers(List<GroupMember> members){this.members = members;}

    public String getGroup_id(){return this.group_id;}
    public UUID getCreator_id(){return this.creator_id;}
    public String getGroup_name(){return this.group_name;}
    public String getImage_url(){return this.image_url;}
    public LocalDateTime getCreated_at(){return this.created_at;}
    public String getDescription(){return  this.description;}
    public List<GroupMember> getMembers(){return this.members;}
    public UUID getInternal_uuid() {
        return this.internal_uuid;
    }

    public void setInternal_uuid(UUID internalUuid) { this.internal_uuid = internalUuid;
    }
}
