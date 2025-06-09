package org.to.telegramfinalproject.Models;

import org.json.JSONObject;

public class SearchRequestModel {
    private String action;
    private String keyword;
    private String user_id;

    public SearchRequestModel(String action, String keyword, String user_id) {
        this.action = action;
        this.keyword = keyword;
        this.user_id = user_id;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("action", action);
        json.put("keyword", keyword);
        json.put("user_id", user_id);
        return json;
    }
}

