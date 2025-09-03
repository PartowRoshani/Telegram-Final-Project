package org.to.telegramfinalproject.Server;

import org.to.telegramfinalproject.Database.userDatabase;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestServer {
    private static final int SOCKET_PORT = 8000;          // سرور سوکت
    private static final int HTTP_PORT   = 8080;          // سرور آپلود
    private static final String UPLOAD_BASE_DIR = "uploads"; // پوشه‌ی ذخیره فایل‌ها

    public static void main(String[] args) {
        // 1) استارت HTTP Upload در ترد جدا
        Thread httpThread = new Thread(() -> {
            try {
                UploadHttp.start(HTTP_PORT, UPLOAD_BASE_DIR);
            } catch (IOException e) {
                System.err.println("Upload HTTP failed to start: " + e.getMessage());
                e.printStackTrace();
            }
        }, "upload-http");
        httpThread.setDaemon(true);
        httpThread.start();

        // 2) سرور سوکت با Thread Pool
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(SOCKET_PORT)) {
            System.out.println("Socket server started on port " + SOCKET_PORT);
            userDatabase.setAllUsersOffline();

            // 3) Shutdown Hook برای خاموشی تمیز
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down...");
                try { serverSocket.close(); } catch (IOException ignore) {}
                pool.shutdownNow();
                userDatabase.setAllUsersOffline();
                System.out.println("Goodbye.");
            }));

            // 4) حلقه پذیرش اتصال‌ها
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                pool.submit(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("Socket server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
