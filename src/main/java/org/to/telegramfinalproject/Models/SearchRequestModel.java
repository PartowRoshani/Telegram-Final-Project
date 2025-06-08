package org.to.telegramfinalproject.Models;

public class SearchRequestModel {
    private String action;
    private String keyword;

    public SearchRequestModel(String action, String keyword) {
        this.action = action;
        this.keyword = keyword;
    }

    public String getAction() {
        return action;
    }

    public String getKeyword() {
        return keyword;
    }
}
