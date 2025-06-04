package org.to.telegramfinalproject.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import org.json.JSONObject;

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

    private void send(JSONObject request) {
        try {
            this.out.println(request.toString());
            String responseText = this.in.readLine();
            if (responseText != null) {
                JSONObject response = new JSONObject(responseText);
                System.out.println("Server response: " + response.getString("message"));
            } else {
                System.out.println("No response from server.");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }

    }
}

