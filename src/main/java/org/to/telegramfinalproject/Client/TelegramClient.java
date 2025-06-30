package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TelegramClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8000;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Scanner scanner;
    ActionHandler handler = null;
    public static BlockingQueue<JSONObject> responseQueue = new LinkedBlockingQueue<>();

    public TelegramClient() {
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            this.socket = new Socket(SERVER_HOST, SERVER_PORT);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            System.out.println("✅ Connected to Telegram Server");
            this.handler = new ActionHandler(this.out, this.in, this.scanner);

            // 👂 فقط این ترد مجاز به خواندن از in است
            Thread listenerThread = new Thread(new IncomingMessageListener(in));
            listenerThread.setDaemon(true);
            listenerThread.start();

            showMainMenu();

        } catch (IOException e) {
            System.err.println("❌ Error connecting to server: " + e.getMessage());
        }
    }

    private void showMainMenu() {
        while (true) {
            System.out.println("Main Menu:");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> handler.register();
                case "2" -> {
                    handler.loginHandler();
                    if (Session.currentUser != null) {
                        System.out.println("✅ Login successful.");
                        UUID internalId = UUID.fromString(Session.currentUser.getString("internal_uuid"));
                        handler.userMenu(internalId);
                    } else {
                        System.out.println("❌ Login failed.");
                    }
                }
                case "3" -> {
                    System.out.println("Exiting...");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    public static void main(String[] args) {
        new TelegramClient().start();
    }
}
