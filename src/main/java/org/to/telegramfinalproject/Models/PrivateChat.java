package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.UUID;

public class PrivateChat {
    private final UUID chat_id;
    private UUID user1_id;
    private UUID user2_id;
    private LocalDateTime created_at;

    public PrivateChat(UUID chat_id, UUID user1_id, UUID user2_id){
        this.chat_id = chat_id;
        this.user1_id =user1_id;
        this.user2_id =user2_id;
        this.created_at =created_at;
    }

    public void setUser1_id(UUID user1_id){this.user1_id =user1_id;}
    public void setUser2_id(UUID user2_id){this.user2_id =user2_id;}
    public void setCreated_at(LocalDateTime created_at){this.created_at = created_at;}

    public UUID getUser1_id(){return this.user1_id;}
    public UUID getChat_id(){return this.chat_id;}
    public UUID getUser2_id(){return this.user2_id;}
    public LocalDateTime getCreated_at(){return this.created_at;}



}
