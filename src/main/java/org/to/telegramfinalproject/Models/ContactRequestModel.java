package org.to.telegramfinalproject.Models;

import org.json.JSONObject;

public class ContactRequestModel {
    private String event;
    private String contactId;
    private String userId;

    public ContactRequestModel(String event, String contactId, String userId) {
        this.event = event;
        this.contactId = contactId;
        this.userId = userId;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("event", event);
        json.put("contact_id", contactId);
        json.put("user_id", userId);
        return json;
    }
}