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
        if (serverValue == null) return null;
        serverValue = serverValue.trim();
        if (serverValue.isEmpty()) return null;

        // URL کامل یا file: → همون رو برگردون
        if (isHttp(serverValue) || serverValue.startsWith("file:")) {
            return serverValue;
        }

        // ⬅️ مهم: برای مسیرهای لوکال، هر چیزی بعد از ? یا # را حذف کن
        serverValue = stripQueryAndHash(serverValue);

        // حذف اسلش ابتدایی (اگر بود)
        String rel = serverValue.startsWith("/") ? serverValue.substring(1) : serverValue;

        // ساخت مسیر امن داخل uploads
        Path p = UPLOADS_ROOT.resolve(rel).normalize();

        // جلوگیری از خروج از دایرکتوری uploads (path traversal)
        if (!p.startsWith(UPLOADS_ROOT)) return null;

        // وجود واقعی فایل
        if (!Files.exists(p)) {
            System.err.println("Avatar resolve: NOT FOUND -> " + p);
            return null;
        }

        // خروجی به صورت file:// URI که JavaFX Image می‌فهمد
        String url = p.toUri().toString();
        System.out.println("Avatar resolve: " + serverValue + " -> " + url);
        return url;
    }

    private static String stripQueryAndHash(String s) {
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        int h = s.indexOf('#');
        if (h >= 0) s = s.substring(0, h);
        return s;
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
