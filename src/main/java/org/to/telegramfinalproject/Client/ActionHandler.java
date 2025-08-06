package org.to.telegramfinalproject.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;
import org.to.telegramfinalproject.Models.ChatEntry;

public class ActionHandler {
    private final PrintWriter out;
    private final BufferedReader in;
    private final Scanner scanner;
    private Map<Integer, JSONObject> messageMap = new HashMap<>();

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

                            if (Session.receiverThread == null || !Session.receiverThread.isAlive()) {
                                Session.receiverThread = new ReceiverThread(in);
                                Session.receiverThread.start();
                            }
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
                            JSONArray messages = response.getJSONObject("data").getJSONArray("messages");
                            System.out.println("\n--- Chat with " + response.getJSONObject("data").getString("chat_name") + " ---");

                            this.messageMap.clear();
                            int index = 1;

                            for (Object obj : messages) {
                                JSONObject message = (JSONObject) obj;
                                String sender = message.getString("sender");
                                String content = message.getString("content");
                                String timestamp = message.getString("timestamp");
                                String statusIcon = "";
                                if (sender.equals(Session.currentUser.getString("username")) && message.has("status")) {
                                    status = message.getString("status");
                                    if (status.equals("seen")) {
                                        statusIcon = "✅✅";
                                    } else if (status.equals("sent")) {
                                        statusIcon = "✅";
                                    }
                                }

                                // Reply preview display
                                if (message.has("reply_to")) {
                                    System.out.print("Reply to: ");
                                    if (message.has("reply_preview")) {
                                        JSONObject preview = message.getJSONObject("reply_preview");
                                        String type = preview.getString("content_type");
                                        if (type.equals("text")) {
                                            System.out.print(preview.getString("content"));
                                        } else {
                                            System.out.print("[ " + type + " - " + preview.optString("file_name", "unnamed") + " ]");
                                        }
                                    }
                                    System.out.println();
                                }

                                // Forward display
                                if (message.has("forward_from")) {
                                    System.out.println("📤 Forwarded message");
                                }

                                //Main message
                                System.out.println(index + ". " + sender + " [" + timestamp + "]: " + content + " " + statusIcon);

                                this.messageMap.put(index, message);
                                index++;
                            }

                            this.chatLoop(response.getJSONObject("data").getString("receiver_id"),
                                    response.getJSONObject("data").getString("receiver_type"));
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
                    if (Session.receiverThread != null) {
                        Session.receiverThread.interrupt(); // Thread stop request
                        Session.receiverThread = null;      // clear reference for next time
                    }

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

    private void chatLoop(String receiverId, String receiverType) {
        System.out.println("Type your messages below (type 'exit' to leave chat):");

        Session.activeChatId = receiverId;
        Integer replyToMessageIndex = null;
        JSONObject forwardFromMessage = null;

        while (true) {
            System.out.print("> ");
            String msg = scanner.nextLine();

            if (msg.startsWith("/reply ")) {
                try {
                    int msgNum = Integer.parseInt(msg.split(" ")[1]);
                    if (messageMap.containsKey(msgNum)) {
                        replyToMessageIndex = msgNum;
                        System.out.println("You are replying to message #" + msgNum);
                    } else {
                        System.out.println("Invalid message number.");
                    }
                } catch (Exception e) {
                    System.out.println("Invalid format. Use /reply <message_number>");
                }
                continue;
            }

            if (msg.startsWith("/forward ")) {
                try {
                    int msgNum = Integer.parseInt(msg.split(" ")[1]);
                    if (messageMap.containsKey(msgNum)) {
                        forwardFromMessage = messageMap.get(msgNum);
                        System.out.println("Message #" + msgNum + " selected for forwarding.");
                    } else {
                        System.out.println("Invalid message number.");
                    }
                } catch (Exception e) {
                    System.out.println("Invalid format. Use /forward <message_number>");
                }
                continue;
            }


            if (msg.equalsIgnoreCase("exit")) {
                System.out.println("Exiting chat...");

                Session.activeChatId = null;
                break;
            }

            if (msg.startsWith("/sendfile ")) {
                String filePath = msg.substring(10).trim();

                try {
                    Path path = Paths.get(filePath);
                    byte[] fileBytes = Files.readAllBytes(path);
                    String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                    String fileName = path.getFileName().toString();

                    String contentType = Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream"; // fallback
                    }

                    JSONObject fileMsg = new JSONObject();
                    fileMsg.put("action", "send_message");
                    fileMsg.put("receiver_id", receiverId);
                    fileMsg.put("receiver_type", receiverType);
                    fileMsg.put("content_type", "file");
                    fileMsg.put("file_name", fileName);
                    fileMsg.put("file_mime", contentType);
                    fileMsg.put("content", encodedFile);

                    send(fileMsg);
                    System.out.println(" File sent: " + fileName + " (" + contentType + ")");

                } catch (IOException e) {
                    System.out.println(" Failed to send file: " + e.getMessage());
                }
            } else {
                // text message with optional reply or forward
                JSONObject textMsg = new JSONObject();
                textMsg.put("action", "send_message");
                textMsg.put("receiver_id", receiverId);
                textMsg.put("receiver_type", receiverType);
                textMsg.put("content_type", "text");
                textMsg.put("content", msg);

                //If client is replying
                if (replyToMessageIndex != null && messageMap.containsKey(replyToMessageIndex)) {
                    JSONObject target = messageMap.get(replyToMessageIndex);
                    textMsg.put("reply_to", target.getString("id"));
                    replyToMessageIndex = null; //Reset after use
                }
                //If client is forwarding
                if (forwardFromMessage != null) {
                    textMsg.put("forward_from", forwardFromMessage.getString("id"));
                    forwardFromMessage = null; //Reset after use
            }
                send(textMsg);
                }
        }
    }

    private class ReceiverThread extends Thread {
        private final BufferedReader in;

        public ReceiverThread(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (in.ready()) {
                        String line = in.readLine();
                        if (line != null) {
                            JSONObject message = new JSONObject(line);

                            if (message.has("action") && message.getString("action").equals("new_message")) {
                                String sender = message.getString("sender");
                                String content = message.getString("content");
                                String timestamp = message.getString("timestamp");
                                String chatId = message.getString("chat_id");
                                String statusIcon = "";
                                if (sender.equals(Session.currentUser.getString("username")) && message.has("status")) {
                                    String status = message.getString("status");
                                    if (status.equals("seen")) {
                                        statusIcon = "✅✅";
                                    } else if (status.equals("sent")) {
                                        statusIcon = "✅";
                                    }
                                }


                                boolean isCurrentChat = Session.activeChatId != null && Session.activeChatId.equals(chatId);

                                if (isCurrentChat) {
                                    System.out.println();

                                    //Reply preview
                                    if (message.has("reply_to")) {
                                        System.out.print("Reply to: ");
                                        if (message.has("reply_preview")) {
                                            JSONObject preview = message.getJSONObject("reply_preview");
                                            String type = preview.getString("content_type");
                                            if (type.equals("text")) {
                                                System.out.print(preview.getString("content"));
                                            } else {
                                                System.out.print("[ " + type + " - " + preview.optString("file_name", "unnamed") + " ]");
                                            }
                                        }
                                        System.out.println();
                                    }

                                    // Forward preview
                                    if (message.has("forward_from")) {
                                        System.out.println(" Forwarded message");
                                    }

                                    // main message
                                    System.out.println(sender + " [" + timestamp + "]: " + content + " " + statusIcon);

                                    System.out.print("> ");
                                }

                                //Forward message from another
                                else {
                                    System.out.println();

                                    //ReplY preview
                                    if (message.has("reply_to")) {
                                        System.out.print(" Reply to: ");
                                        if (message.has("reply_preview")) {
                                            JSONObject preview = message.getJSONObject("reply_preview");
                                            String type = preview.getString("content_type");
                                            if (type.equals("text")) {
                                                System.out.print(preview.getString("content"));
                                            } else {
                                                System.out.print("[ " + type + " - " + preview.optString("file_name", "unnamed") + " ]");
                                            }
                                        }
                                        System.out.println();
                                    }

                                    // forward preview
                                    if (message.has("forward_from")) {
                                        System.out.println(" Forwarded message");
                                    }

                                    // main message
                                    System.out.println(" New message from " + sender + ": " + content + " (" + timestamp + ")");
                                    System.out.print("> ");

                                    // seen request to server
                                    JSONObject seenRequest = new JSONObject();
                                    seenRequest.put("action", "mark_seen");
                                    seenRequest.put("chat_id", chatId);
                                    seenRequest.put("message_id", message.getString("id"));

                                    out.println(seenRequest.toString());
                                }
                            }
                        }
                    }
                    Thread.sleep(200);
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Receiver error: " + e.getMessage());
            }
        }
    }
}
