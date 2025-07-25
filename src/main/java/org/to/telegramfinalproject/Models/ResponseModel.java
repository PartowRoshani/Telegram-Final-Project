package org.to.telegramfinalproject.Models;

import org.json.JSONObject;

public class ResponseModel {
    private String status;
    private String message;
    private JSONObject data;
    private String requestId;



    public ResponseModel(String status, String message) {
        this.status = status;
        this.message = message;
        this.data = null;
    }

    public ResponseModel(String status, String message, JSONObject data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }

    public JSONObject getData() {return this.data;}

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }


    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("status", this.status);
        json.put("message", this.message);
        json.put("data", this.data != null ? this.data : JSONObject.NULL);
        if (this.requestId != null) {
            json.put("request_id", this.requestId);
        }
        return json;
    }
}
