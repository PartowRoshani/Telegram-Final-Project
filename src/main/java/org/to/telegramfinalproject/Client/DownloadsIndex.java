package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DownloadsIndex {
    private final UUID accountId;
    private final Path indexFile;
    private final Map<UUID, Entry> map = new ConcurrentHashMap<>();

    public DownloadsIndex(UUID accountId) {
        this.accountId = accountId;
        this.indexFile = resolveIndexPath(accountId.toString());
        load();
    }


    /** مسیر فایل ایندکس مخصوص هر اکانت */
    private static Path resolveIndexPath(String accountId) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");

        Path dir;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            dir = (appData != null)
                    ? Paths.get(appData, "TeleSock")
                    : Paths.get(home, "AppData", "Roaming", "TeleSock");
        } else {
            dir = Paths.get(home, ".telesock");
        }
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve("downloads-index-" + accountId + ".json");
    }

    /** لود از دیسک (اگر فایل وجود داشته باشد) */
    private synchronized void load() {
        map.clear();
        try {
            if (!Files.exists(indexFile)) return;
            String json = Files.readString(indexFile, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) return;

            JSONObject root  = new JSONObject(json);
            JSONObject items = root.optJSONObject("items");
            if (items == null) return;

            for (String key : items.keySet()) {
                JSONObject e = items.getJSONObject(key);
                map.put(UUID.fromString(key), new Entry(
                        e.getString("path"),
                        e.optLong("size", 0L),
                        e.optLong("ts", System.currentTimeMillis())
                ));
            }
        } catch (Exception e) {
            System.err.println("⚠️ DownloadsIndex load failed: " + e.getMessage());
        }
    }

    /** ذخیره اتمیک روی دیسک */
    private synchronized void save() throws IOException {
        JSONObject items = new JSONObject();
        for (Map.Entry<UUID, Entry> it : map.entrySet()) {
            JSONObject e = new JSONObject();
            e.put("path", it.getValue().path);
            e.put("size", it.getValue().size);
            e.put("ts",   it.getValue().ts);
            items.put(it.getKey().toString(), e);
        }
        byte[] data = new JSONObject().put("items", items).toString(2).getBytes(StandardCharsets.UTF_8);

        Path tmp = indexFile.resolveSibling(indexFile.getFileName() + ".tmp");
        Files.write(tmp, data);
        try {
            Files.move(tmp, indexFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, indexFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** ذخیره‌ی بی‌سر‌وصدا */
    public void saveQuietly() {
        try { save(); } catch (Exception ignored) {}
    }

    /** اگر قبلاً دانلود شده و فایلش هست، مسیر را برمی‌گرداند؛ وگرنه رکورد کهنه پاک می‌شود. */
    public Path find(UUID mediaKey) {
        Entry e = map.get(mediaKey);
        if (e == null) return null;
        Path p = Paths.get(e.path);
        if (Files.exists(p)) return p;
        map.remove(mediaKey);
        saveQuietly();
        return null;
    }

    /** بعد از دانلود موفق */
    public void put(UUID mediaKey, Path path, long size) {
        map.put(mediaKey, new Entry(path.toString(), size, System.currentTimeMillis()));
        saveQuietly();
    }

    /** اختیاری */
    public void remove(UUID mediaKey) {
        map.remove(mediaKey);
        saveQuietly();
    }

    private static final class Entry {
        final String path; final long size; final long ts;
        Entry(String path, long size, long ts) {
            this.path = path; this.size = size; this.ts = ts;
        }
    }
}
