package org.to.telegramfinalproject.Models;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private String user_id;
    private UUID internal_uuid;
    private String username;
    private String password;
    private String profile_name;
    private String bio;
    private String image_url;
    private String status;
    private LocalDateTime last_seen;

    public User(String user_id, UUID internal_uuid, String username, String password, String profile_name) {
        this.user_id = user_id;
        this.internal_uuid = internal_uuid;
        this.username = username;
        this.password = password;
        this.profile_name = profile_name;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setProfile_name(String profile_name) {
        this.profile_name = profile_name;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLast_seen(LocalDateTime last_seen) {
        this.last_seen = last_seen;
    }

    public UUID getInternal_uuid() {
        return this.internal_uuid;
    }

    public String getUser_id() {
        return this.user_id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getProfile_name() {
        return this.profile_name;
    }

    public String getBio() {
        return this.bio;
    }

    public String getImage_url() {
        return this.image_url;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getLast_seen() {
        return this.last_seen;
    }
}

