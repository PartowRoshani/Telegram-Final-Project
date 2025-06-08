package org.to.telegramfinalproject.Models;

import org.json.JSONObject;

public class ResponseModel {
    private String status;
    private String message;
    private JSONObject data;


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
}
