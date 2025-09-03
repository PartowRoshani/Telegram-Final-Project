package org.to.telegramfinalproject.Server;

import org.to.telegramfinalproject.Database.userDatabase;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    private static final int PORT = 8080;


    public static void main(String[] args) {

        try {
            int uploadPort = 8081;
            String uploadBaseDir = "C:\\telegram-data\\profile-pics";

            // start upload HTTP
            try {
                UploadHttp.start(uploadPort, uploadBaseDir);
            } catch (IOException e) {
                System.err.println("Failed to start UploadHttp: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server started on port " + PORT);
                userDatabase.setAllUsersOffline();


                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                }

            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Upload server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
