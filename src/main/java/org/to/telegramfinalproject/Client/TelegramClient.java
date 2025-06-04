package org.to.telegramfinalproject.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TelegramClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Scanner scanner;
    ActionHandler handler = null;

    public TelegramClient() {
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            this.socket = new Socket(SERVER_HOST, SERVER_PORT);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            System.out.println(" Connected to Telegram Server");
            this.handler = new ActionHandler(this.out, this.in, this.scanner);
            this.showMainMenu();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }

    }

    private void showMainMenu() {
        while(true) {
            System.out.println("Main Menu:");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            switch (this.scanner.nextLine()) {
                case "1":
                    this.handler.register();
                    break;
                case "2":
                    this.handler.loginHandler();
                    break;
                case "3":
                    System.out.println(" Disconnecting...");

                    try {
                        if (this.socket != null) {
                            this.socket.close();
                        }

                        if (this.in != null) {
                            this.in.close();
                        }

                        if (this.out != null) {
                            this.out.close();
                        }

                        System.out.println("Disconnected.");
                    } catch (IOException e) {
                        System.err.println(" Error closing connection: " + e.getMessage());
                    }

                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    public static void main(String[] args) {
        TelegramClient client = new TelegramClient();
        client.start();
    }
}

