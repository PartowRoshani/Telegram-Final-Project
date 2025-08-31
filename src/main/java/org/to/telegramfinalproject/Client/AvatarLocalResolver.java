package org.to.telegramfinalproject.Client;

import javafx.scene.image.Image;
import java.nio.file.*;

public final class AvatarLocalResolver {
    private static final Path UPLOADS_ROOT = Paths.get(
            System.getProperty("app.uploads.root", "uploads")
    ).toAbsolutePath().normalize();

    private static boolean isHttp(String s) {
        return s.startsWith("http://") || s.startsWith("https://");
    }

    public static String resolve(String serverValue) {
        if (serverValue == null || serverValue.isBlank()) return null;
        if (isHttp(serverValue) || serverValue.startsWith("file:")) return serverValue;

        String rel = serverValue.startsWith("/") ? serverValue.substring(1) : serverValue; // "avatars/..."
        Path p = UPLOADS_ROOT.resolve(rel).normalize();
        if (!p.startsWith(UPLOADS_ROOT)) return null;        // امنیت مسیر
        if (!Files.exists(p)) {                               // مهم: واقعاً وجود دارد؟
            System.err.println("Avatar resolve: NOT FOUND -> " + p);
            return null;
        }
        String url = p.toUri().toString();                   // file:///.../uploads/avatars/....
        System.out.println("Avatar resolve: " + serverValue + " -> " + url);
        return url;
    }

    public static Image load(String serverValue) {
        String url = resolve(serverValue);
        if (url == null) return null;

        // فقط برای http/https کش‌بریکر
        if (isHttp(url)) {
            url += (url.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis();
        }

        Image img = new Image(url, false); // sync load تا خطا را همان‌جا بفهمیم
        if (img.isError()) {
            System.err.println("Avatar load failed: " + url + " -> " + img.getException());
            return null;
        }
        return img;
    }
}
