package org.to.telegramfinalproject.Models;

public class ResponseModel {
    private String status;
    private String message;

    public ResponseModel(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }
}
