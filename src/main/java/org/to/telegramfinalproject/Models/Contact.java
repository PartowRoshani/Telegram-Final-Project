package org.to.telegramfinalproject.Models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Contact {
    private UUID user_id;
    private UUID contact_id;
    private LocalDateTime added_at;
    private Boolean is_blocked;

    public Contact(UUID user_id, UUID contact_id){
        this.user_id = user_id;
        this.contact_id = contact_id;
        this.added_at = LocalDateTime.now();
    }

    public void setUser_id(UUID user_id){this.user_id = user_id;}
    public void setContact_id(UUID contact_id){this.contact_id = contact_id;}
    public void setAdd_at(LocalDateTime add_at){this.added_at =add_at;}
    public  void setIs_blocked(Boolean is_blocked){this.is_blocked =is_blocked;}

    public UUID getUser_id(){
        return this.user_id;
    }

    public UUID getContact_id(){return this.contact_id;}

    public LocalDateTime getAdd_at() {
        return added_at;
    }
    public Boolean getIs_blocked(){
        return is_blocked;
    }





}
