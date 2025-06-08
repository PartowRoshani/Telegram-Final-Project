package org.to.telegramfinalproject.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

public class ActionHandler {
    private final PrintWriter out;
    private final BufferedReader in;
    private final Scanner scanner;

    public ActionHandler(PrintWriter out, BufferedReader in, Scanner scanner) {
        this.out = out;
        this.in = in;
        this.scanner = scanner;
    }

    public void loginHandler() {
        System.out.println("Login form: \n");
        System.out.println("Username: ");
        String username = this.scanner.nextLine();
        System.out.println("Password: ");
        String password = this.scanner.nextLine();
        JSONObject request = new JSONObject();
        request.put("action", "login");
        request.put("user_id", JSONObject.NULL);
        request.put("username", username);
        request.put("password", password);
        request.put("profile_name", JSONObject.NULL);
        this.send(request);
    }

    public void register() {
        System.out.println("Register form: \n");
        System.out.println("Username: ");
        String username = this.scanner.nextLine();
        System.out.println("User id: ");
        String user_id = this.scanner.nextLine();
        System.out.println("Password: ");
        String password = this.scanner.nextLine();
        System.out.println("Profile name: ");
        String profile_name = this.scanner.nextLine();
        JSONObject request = new JSONObject();
        request.put("action", "register");
        request.put("user_id", user_id);
        request.put("username", username);
        request.put("password", password);
        request.put("profile_name", profile_name);
        this.send(request);
    }


    public void search(){

        System.out.print("Enter keyword to search: ");
        String keyword = scanner.nextLine();

        JSONObject request = new JSONObject();
        request.put("action", "search");
        request.put("keyword", keyword);

        send(request);

    }

    private void send(JSONObject request) {
        try {
            if (!request.has("action") || request.isNull("action")) {
                System.err.println("Error: Request does not contain 'action'.");
                return;
            }

            String action = request.getString("action");

            this.out.println(request.toString());

            String responseText = this.in.readLine();

            if (responseText != null) {
                JSONObject response = new JSONObject(responseText);
                System.out.println("Server response: " + response.getString("message"));
                String status = response.getString("status");

                if (status.equals("success") && response.has("data") && !response.isNull("data")) {
                    switch (action) {
                        case "login":
                        case "register":
                            Session.currentUser = response.getJSONObject("data");
                            JSONArray chatListJson = Session.currentUser.getJSONArray("chat_list");
                            List<ChatEntry> chatList = new ArrayList<>();

                            for (Object obj : chatListJson) {
                                JSONObject chat = (JSONObject) obj;
                                ChatEntry entry = new ChatEntry(
                                        chat.getString("id"),
                                        chat.getString("name"),
                                        chat.getString("image_url"),
                                        chat.getString("type"),
                                        chat.isNull("last_message_time") ? null : LocalDateTime.parse(chat.getString("last_message_time"))
                                );
                                chatList.add(entry);
                            }

                            Session.chatList = chatList;
                            break;

                        case "search":
                            JSONArray results = response.getJSONObject("data").getJSONArray("results");

                            if (results.isEmpty()) {
                                System.out.println("No results found.");
                            } else {
                                System.out.println("\nSearch Results:");
                                for (Object obj : results) {
                                    JSONObject item = (JSONObject) obj;
                                    System.out.println("- [" + item.getString("type") + "] " + item.getString("name") + " (ID: " + item.getString("id") + ")");
                                }
                            }
                            break;

                        case "get_messages":
                            break;
                    }
                }

            } else {
                System.out.println("No response from server.");
            }

        } catch (IOException e) {
            System.err.println("Error while communicating with server: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }




    public void userMenu() {
        while (true) {
            System.out.println("\nUser Menu:");
            System.out.println("1. Show chat list");
            System.out.println("2. Search");
            System.out.println("3. Logout");

            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {

                case "1":
                    showChatListAndSelect();
                    break;
                case "2" :
                    search();
                    break;

                case "3":
                    Session.currentUser = null;
                    Session.chatList = null;
                    System.out.println("Logged out.");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }




    public void showChatListAndSelect() {
        if (Session.chatList == null || Session.chatList.isEmpty()) {
            System.out.println("No chats available.");
            return;
        }

        System.out.println("\nYour Chats:");
        for (int i = 0; i < Session.chatList.size(); i++) {
            ChatEntry entry = Session.chatList.get(i);
            String time = entry.getLastMessageTime() == null ? "No messages yet" : entry.getLastMessageTime().toString();
            System.out.println((i + 1) + ". [" + entry.getType() + "] " + entry.getName() + " - Last: " + time);
        }

        System.out.print("Select a chat by number: ");
        int choice = Integer.parseInt(scanner.nextLine()) - 1;

        if (choice < 0 || choice >= Session.chatList.size()) {
            System.out.println("Invalid selection.");
            return;
        }

        ChatEntry selected = Session.chatList.get(choice);
        openChat(selected);
    }


    private void openChat(ChatEntry chat) {
        JSONObject request = new JSONObject();
        request.put("action", "get_messages");
        request.put("receiver_id", chat.getId());
        request.put("receiver_type", chat.getType());

        send(request);
    }

}

