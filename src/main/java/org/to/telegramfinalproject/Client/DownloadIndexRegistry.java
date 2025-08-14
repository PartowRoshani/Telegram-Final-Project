// DownloadIndexRegistry.java
package org.to.telegramfinalproject.Client;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DownloadIndexRegistry {
    private static final ConcurrentHashMap<UUID, DownloadsIndex> INSTANCES = new ConcurrentHashMap<>();
    private static volatile boolean HOOK_REGISTERED = false;

    private DownloadIndexRegistry() {}

    public static DownloadsIndex forAccount(UUID accountId) {
        registerHookOnce();
        return INSTANCES.computeIfAbsent(accountId, DownloadsIndex::new);
    }

    public static void closeAccount(UUID accountId) {
        DownloadsIndex idx = INSTANCES.remove(accountId);
        if (idx != null) idx.saveQuietly();
    }

    private static synchronized void registerHookOnce() {
        if (HOOK_REGISTERED) return;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (DownloadsIndex idx : INSTANCES.values()) idx.saveQuietly();
        }));
        HOOK_REGISTERED = true;
    }
}
