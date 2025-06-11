package org.to.telegramfinalproject.Client;


import org.json.JSONObject;

import java.io.BufferedReader;

public class IncomingMessageListener implements Runnable {
    private final BufferedReader in;

    public IncomingMessageListener(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                JSONObject response = new JSONObject(line);

                if (!response.has("action")) continue;
                String action = response.getString("action");

                switch (action) {
                    case "new_message" -> {
                        JSONObject msg = response.getJSONObject("data");
                        System.out.println("\n🔔 New Message:");
                        System.out.println("From: " + msg.getString("sender"));
                        System.out.println("Time: " + msg.getString("time"));
                        System.out.println("Content: " + msg.getString("content"));
                        System.out.print(">> ");
                    }

                    case "message_edited" -> {
                        JSONObject msg = response.getJSONObject("data");
                        System.out.println("\n✏️ Message Edited:");
                        System.out.println("ID: " + msg.getString("message_id"));
                        System.out.println("New Content: " + msg.getString("new_content"));
                        System.out.println("Edit Time: " + msg.getString("edit_time"));
                        System.out.print(">> ");
                    }

                    case "message_deleted" -> {
                        JSONObject msg = response.getJSONObject("data");
                        System.out.println("\n🗑️ Message Deleted:");
                        System.out.println("Message ID: " + msg.getString("message_id"));
                        System.out.print(">> ");
                    }

                    case "status_change" -> {
                        JSONObject msg = response.getJSONObject("data");
                        System.out.println("\n🔄 User Status Changed:");
                        System.out.println("User: " + msg.getString("user_id"));
                        System.out.println("Status: " + msg.getString("status"));
                        System.out.print(">> ");
                    }

                    case "system_notification" -> {
                        JSONObject msg = response.getJSONObject("data");
                        System.out.println("\n⚠️ System Notification:");
                        System.out.println(msg.getString("content"));
                        System.out.print(">> ");
                    }

                    case "contact_added" ->{
                        System.out.println("\n🔔 You were added by a new contact: " + response.getString("user_id"));
                        System.out.print(">> ");
                    }



                    default -> {
                        if (!action.equals("search")) {  // ignore action: search
                            System.out.println("\n❓ Unknown action received: " + action);
                            System.out.print(">> ");
                        }
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("🔴 Listener stopped: " + e.getMessage());
        }
    }
}
