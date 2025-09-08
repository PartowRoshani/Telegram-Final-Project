package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.UUID;

public class GroupMember {
    private UUID group_id;
    private UUID user_id;
    private LocalDateTime join_at;
    private String role;

    public GroupMember(UUID group_id, UUID user_id, String role){
        this.group_id = group_id;
        this.user_id = user_id;
        this.join_at = LocalDateTime.now();
        this.role = role;
    }

    public  void  setGroup_id(UUID group_id){this.group_id = group_id;}
    public void  setUser_id(UUID user_id){this.user_id = user_id;}
    public void  setJoin_at(LocalDateTime join_at){this.join_at = join_at;}
    public void setRole(String role){this.role = role;}



    public  UUID  getGroup_id(){return this.group_id;}
    public UUID  getUser_id(){return this.user_id;}
    public LocalDateTime  getJoin_at(){return this.join_at;}
    public String getRole(){return this.role;}


}
