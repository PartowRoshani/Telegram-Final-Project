package org.to.telegramfinalproject.Models;

public class RequestModel {
    private String action;
    private String user_id;
    private String username;
    private String password;
    private String profile_name;

    public RequestModel(String action, String user_id, String username, String password, String profile_name) {
        this.action = action;
        this.user_id = user_id;
        this.username = username;
        this.password = password;
        this.profile_name = profile_name;
    }

    public String getAction() {
        return this.action;
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
}


