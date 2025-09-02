//package org.to.telegramfinalproject.Client;
//
//import org.json.JSONObject;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.Socket;
//import java.util.Map;
//import java.util.Scanner;
//import java.util.UUID;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.LinkedBlockingQueue;
//
//public class TelegramClient {
//    private static final String SERVER_HOST = "localhost";
//    private static final int SERVER_PORT = 8000;
//    private static Socket socket;
//    private BufferedReader in;
//    private PrintWriter out;
//    private final Scanner scanner;
//    private ActionHandler handler;
//    public static BlockingQueue<JSONObject> responseQueue = new LinkedBlockingQueue<>();
//    public static UUID loggedInUserId = null;
//    public static final Map<String, BlockingQueue<JSONObject>> pendingResponses = new ConcurrentHashMap<>();
//
//
//    private static TelegramClient instance;
//
//    public TelegramClient() {
//        this.scanner = new Scanner(System.in);
//        instance = this;
//    }
//
//    public static TelegramClient getInstance() {
//        return instance;
//    }
//
//    public void start() {
//        try {
//            socket = new Socket(SERVER_HOST, SERVER_PORT);
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//            System.out.println("✅ Connected to Telegram Server");
//            handler = new ActionHandler(out, in, scanner);
//
//            Thread listenerThread = new Thread(new IncomingMessageListener(in));
//            listenerThread.setDaemon(true);
//            listenerThread.start();
//
//            showMainMenu();
//
//        } catch (IOException e) {
//            System.err.println("❌ Error connecting to server: " + e.getMessage());
//        }
//    }
//
//    private void showMainMenu() throws IOException {
//        while (true) {
//            System.out.println("Main Menu:");
//            System.out.println("1. Register");
//            System.out.println("2. Login");
//            System.out.println("3. Exit");
//            System.out.print("Choose an option: ");
//            String choice = scanner.nextLine();
//
//            switch (choice) {
//                case "1" -> handler.register();
//                case "2" -> {
//                    handler.loginHandler();
//                    if (Session.currentUser != null) {
//                        System.out.println("✅ Login successful.");
//                        new Thread(new ActionHandler.ChatStateMonitor(out)).start();
////                        new Thread(new ActionHandler.CurrentChatMenuRefresher(this.handler)).start();
//
//
//                        UUID internalId = UUID.fromString(Session.currentUser.getString("internal_uuid"));
//                        loggedInUserId = internalId;
//
//                        handler.userMenu(internalId);
//                    } else {
//                        System.out.println("❌ Login failed.");
//                    }
//                }
//                case "3" -> {
//                    System.out.println("Exiting...");
//                    return;
//                }
//                default -> System.out.println("Invalid choice.");
//            }
//        }
//    }
//
//    public static void send(JSONObject req) {
//        try {
//            responseQueue.clear();  // optional: clear old responses
//            getInstance().out.println(req.toString());
//            System.out.println("📤 [SEND] " + req.toString(2));
//
//        } catch (Exception e) {
//            System.err.println("❌ Error sending request: " + e.getMessage());
//        }
//    }
//
//
//
//
//    public static Socket getSocket() {
//        return socket;
//    }
//
//    public static void main(String[] args) {
//        new TelegramClient().start();
//    }
//
//    public PrintWriter getOut() {
//        return out;
//    }
//
//
//}



package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class TelegramClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8000;

    private static TelegramClient instance;

    private static Socket socket;
    private BufferedReader in;
    private PrintWriter out;


    private final Scanner scanner;
    private ActionHandler handler;

    public static final BlockingQueue<JSONObject> responseQueue = new LinkedBlockingQueue<>();
    public static final Map<String, BlockingQueue<JSONObject>> pendingResponses = new ConcurrentHashMap<>();
    public static UUID loggedInUserId = null;

    private volatile boolean listenerStarted = false;

    public TelegramClient() {
        this.scanner = new Scanner(System.in);
        instance = this;
    }

    public static synchronized TelegramClient getInstance() {
        if (instance == null) instance = new TelegramClient();
        return instance;
    }

//    public void startConsole() {
//        try {
//            connectIfNeeded();
//            initHandlerIfNeeded();
//            startListenerOnce();
//            showMainMenu();
//        } catch (IOException e) {
//            System.err.println("❌ Error connecting to server: " + e.getMessage());
//        }
//    }

    public void startConsole() {
        try {
            connectIfNeeded();
            initHandlerIfNeeded();
            startListenerOnce(IncomingMessageListener.UIMode.CONSOLE);  // ← کنسول
            showMainMenu();
        } catch (IOException e) {
            System.err.println("❌ Error connecting to server: " + e.getMessage());
        }
    }

    public void start() { startConsole(); }

    //UI only
//    public static synchronized TelegramClient getOrInitForUI() throws IOException {
//        TelegramClient cli = getInstance();
//        cli.connectIfNeeded();
//        cli.initHandlerIfNeeded();
//        cli.startListenerOnce();
//        return cli;
//    }

    public static synchronized TelegramClient getOrInitForUI() throws IOException {
        TelegramClient cli = getInstance();
        cli.connectIfNeeded();
        cli.initHandlerIfNeeded();
        cli.startListenerOnce(IncomingMessageListener.UIMode.UI);       // ← UI
        return cli;
    }


    private synchronized void connectIfNeeded() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;

        socket = new Socket(SERVER_HOST, SERVER_PORT);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("✅ Connected to Telegram Server");
    }

    private synchronized void initHandlerIfNeeded() {
        if (handler == null) {
            handler = new ActionHandler(out, in, scanner);
        }
    }

//    private void startListenerOnce() {
//        if (listenerStarted) return;
//        listenerStarted = true;
//        Thread listenerThread = new Thread(new IncomingMessageListener(in), "socket-listener");
//        listenerThread.setDaemon(true);
//        listenerThread.start();
//    }


    private void startListenerOnce(IncomingMessageListener.UIMode mode) {
        if (listenerStarted) return;
        listenerStarted = true;

        Thread listenerThread = new Thread(
                new IncomingMessageListener(in, mode),
                "socket-listener"
        );
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    //console
    private void showMainMenu() throws IOException {
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
                        new Thread(new ActionHandler.ChatStateMonitor(out)).start();

                        UUID internalId = UUID.fromString(Session.currentUser.getString("internal_uuid"));
                        loggedInUserId = internalId;

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

    public static void send(JSONObject req) {
        try {
            responseQueue.clear();
            getInstance().out.println(req.toString());
            System.out.println("📤 [SEND] " + req.toString(2));
        } catch (Exception e) {
            System.err.println("❌ Error sending request: " + e.getMessage());
        }
    }

    public BufferedReader getIn()  { return in; }
    public PrintWriter   getOut()  { return out; }
    public ActionHandler getHandler() { return handler; }
    public static Socket getSocket() { return socket; }

    public static void main(String[] args) {
        new TelegramClient().startConsole();
    }

    // TelegramClient.java
    private IncomingMessageListener listener;

    public IncomingMessageListener getListener() {
        return listener;
    }

}