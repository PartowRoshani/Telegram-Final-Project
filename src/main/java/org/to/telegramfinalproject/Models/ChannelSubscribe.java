package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChannelSubscribe{
    private UUID channel_id;
    private UUID user_id;
    private LocalDateTime Subscribed_at;


    public ChannelSubscribe(UUID channel_id, UUID user_id, String role){
        this.channel_id = channel_id;
        this.user_id = user_id;
        this.Subscribed_at = LocalDateTime.now();
    }

    public  void  setChannel_id(UUID group_id){this.channel_id = group_id;}
    public void  setUser_id(UUID user_id){this.user_id = user_id;}
    public void  setJoin_at(LocalDateTime join_at){this.Subscribed_at = join_at;}



    public  UUID  getChannel_id(){return  this.channel_id;}
    public UUID  getUser_id(){return  this.user_id;}
    public LocalDateTime  getJoin_at(){return  this.Subscribed_at;}


}
