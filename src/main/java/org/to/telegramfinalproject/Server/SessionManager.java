package org.to.telegramfinalproject.Server;

import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Map<UUID, Socket> onlineUsers = new ConcurrentHashMap<>(); //catch UUID for find the user socket

    public static void addUser(UUID userId, Socket socket) {
        onlineUsers.put(userId, socket);
    }

    public static void removeUser(UUID userId) {
        onlineUsers.remove(userId);
    }

    public static boolean isOnline(UUID userId) {
        return onlineUsers.containsKey(userId);
    }

    public static Socket getUserSocket(UUID userId) {
        return onlineUsers.get(userId);
    }

    public static UUID getUserIdBySocket(Socket socket) {
        for (Map.Entry<UUID, Socket> entry : onlineUsers.entrySet()) {
            if (entry.getValue().equals(socket)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean contains(UUID userId) {
        return onlineUsers.containsKey(userId);
    }




}
