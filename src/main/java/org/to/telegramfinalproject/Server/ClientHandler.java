package org.to.telegramfinalproject.Server;

import org.json.JSONObject;
import org.to.telegramfinalproject.Models.RequestModel;
import org.to.telegramfinalproject.Models.ResponseModel;
import org.to.telegramfinalproject.Models.User;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService = new AuthService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                JSONObject requestJson = new JSONObject(inputLine);

                RequestModel request = new RequestModel(
                        requestJson.optString("action"),
                        requestJson.optString("user_id"),
                        requestJson.optString("username"),
                        requestJson.optString("password"),
                        requestJson.optString("profile_name")
                );

                ResponseModel response;

                switch (request.getAction()) {
                    case "register":
                        boolean registered = authService.register(
                                request.getUser_id(),
                                request.getUsername(),
                                request.getPassword(),
                                request.getProfile_name()
                        );
                        response = registered
                                ? new ResponseModel("success", "Registration successful.")
                                : new ResponseModel("error", "Registration failed.");
                        break;

                    case "login":
                        User user = authService.login(request.getUsername(), request.getPassword());
                        response = (user != null)
                                ? new ResponseModel("success", "Welcome " + user.getProfile_name())
                                : new ResponseModel("error", "Login failed.");
                        break;

                    case "logout":
                        response = new ResponseModel("success", "Logged out.");
                        break;

                    default:
                        response = new ResponseModel("error", "Unknown action: " + request.getAction());
                }

                JSONObject responseJson = new JSONObject();
                responseJson.put("status", response.getStatus());
                responseJson.put("message", response.getMessage());
                out.println(responseJson.toString());
            }

        } catch (IOException e) {
            System.out.println("Connection with client lost.");
        }
    }
}
