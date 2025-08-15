package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TelegramClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Scanner scanner;
    private ActionHandler handler;
    public static BlockingQueue<JSONObject> responseQueue = new LinkedBlockingQueue<>();
    public static UUID loggedInUserId = null;
    public static final Map<String, BlockingQueue<JSONObject>> pendingResponses = new ConcurrentHashMap<>();
    private DataInputStream inBin;                     // NEW
    private static SocketMediaDownloader downloader;   // NEW
    public static final AtomicBoolean mediaBusy = new AtomicBoolean(false); //
    private DownloadsIndex downloadIndex;

    private static TelegramClient instance;

    public TelegramClient() {
        this.scanner = new Scanner(System.in);
        instance = this;
    }

    public static SocketMediaDownloader getDownloader() {
        return downloader;
    }

    public static TelegramClient getInstance() {
        return instance;
    }
    private DataOutputStream outBin;

    public void start() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            InputStream rawIn  = socket.getInputStream();
            OutputStream rawOut = socket.getOutputStream();
            in  = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8), true);

            inBin  = new DataInputStream(rawIn);
            outBin = new DataOutputStream(rawOut);
            downloader = new SocketMediaDownloader(out, inBin, outBin);

            System.out.println("✅ Connected to Telegram Server");
            handler = new ActionHandler(out, in, outBin, scanner);

            Thread listenerThread = new Thread(new IncomingMessageListener(in));
            listenerThread.setDaemon(true);
            listenerThread.start();

            showMainMenu();

        } catch (IOException e) {
            System.err.println("❌ Error connecting to server: " + e.getMessage());
        }
    }

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
//                        new Thread(new ActionHandler.CurrentChatMenuRefresher(this.handler)).start();


                        UUID internalId = UUID.fromString(Session.currentUser.getString("internal_uuid"));
                        loggedInUserId = internalId;
                        this.downloadIndex = DownloadIndexRegistry.forAccount(internalId);
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
            responseQueue.clear();  // optional: clear old responses
            getInstance().out.println(req.toString());
            System.out.println("📤 [SEND] " + req.toString(2));

        } catch (Exception e) {
            System.err.println("❌ Error sending request: " + e.getMessage());
        }
    }




    public static Socket getSocket() {
        return socket;
    }

    public static void main(String[] args) {
        new TelegramClient().start();
    }

    public PrintWriter getOut() {
        return out;
    }

}

