package org.to.telegramfinalproject.Models;

import java.util.List;

public class LoginBootstrapData {
    private User user;
    private List<Contact> contacts;
    private List<Group> groups;
    private List<Channel> channels;
    private List<Message> unreadMessages;

    public LoginBootstrapData(User user, List<Contact> contacts, List<Group> groups,
                              List<Channel> channels, List<Message> unreadMessages) {
        this.user = user;
        this.contacts = contacts;
        this.groups = groups;
        this.channels = channels;
        this.unreadMessages = unreadMessages;
    }

    public void  setUnreadMessages(List<Message> unreadMessages){this.unreadMessages = unreadMessages;}
    public void  setContacts(List<Contact> contacts){this.contacts =contacts;}
    public void setGroups(List<Group> groups){this.groups = groups;}
    public void setChannels(List<Channel> channels){this.channels = channels;}
    public void setUser(User user){this.user = user;}

    public List<Message> getUnreadMessages(){return unreadMessages;}
    public List<Group> getGroups(){return groups;}
    public List<Contact> getContacts(){return  contacts;}
    public List<Channel> getChannels(){return  channels;}
    public User getUser(User user){return  user;}
}
