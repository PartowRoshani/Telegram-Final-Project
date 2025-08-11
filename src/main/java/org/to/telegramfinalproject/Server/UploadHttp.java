package org.to.telegramfinalproject.Server;

import static spark.Spark.*;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

import javax.sound.sampled.*; // برای WAV

import org.json.JSONObject;
import com.mpatric.mp3agic.Mp3File;

public class UploadHttp {

    public static void start(int httpPort, String baseDir) throws IOException {
        port(httpPort);

        Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
        staticFiles.externalLocation(basePath.toString());

        post("/upload", (req, res) -> {
            res.type("application/json");
            try {
                long MAX_FILE = 25L * 1024 * 1024; // 25MB
                req.attribute("org.eclipse.jetty.multipartConfig",
                        new MultipartConfigElement("/tmp", MAX_FILE, MAX_FILE, 0));

                Part filePart = req.raw().getPart("file");
                if (filePart == null || filePart.getSize() == 0) {
                    res.status(400);
                    return jsonError("empty file");
                }
                if (filePart.getSize() > MAX_FILE) {
                    res.status(413);
                    return jsonError("file too large");
                }

                String mime = filePart.getContentType();
                if (mime == null) {
                    res.status(415);
                    return jsonError("unknown mime");
                }

                String original = filePart.getSubmittedFileName();
                String ext = guessExt(original, mime);
                String day = LocalDate.now().toString();
                String typeDir = subdirFor(mime); // images/audios/files
                String subdir = typeDir + "/" + day;
                String name = java.util.UUID.randomUUID() + ext;

                Path dir = basePath.resolve(subdir).normalize();
                Files.createDirectories(dir);
                Path target = dir.resolve(name).normalize();

                try (InputStream in = filePart.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    filePart.delete();
                }

                String fileUrl = "/" + subdir.replace('\\', '/') + "/" + name;
                String fileType = mapToFileType(mime);

               //Meta deta only for audio and image
                Integer width = null, height = null, durationSeconds = null;
                String thumbnailUrl = null;

                if ("IMAGE".equals(fileType) || "GIF".equals(fileType)) {
                    int[] wh = imageSize(target);
                    if (wh != null) { width = wh[0]; height = wh[1]; }
                } else if ("AUDIO".equals(fileType)) {
                    durationSeconds = audioDurationSeconds(target, mime, ext);
                }

                res.status(200);
                return new JSONObject()
                        .put("file_url", fileUrl)
                        .put("file_type", fileType)
                        .put("file_name", original == null ? "" : safeName(original))
                        .put("file_size", Files.size(target))
                        .put("mime_type", mime)
                        .put("width", width == null ? JSONObject.NULL : width)
                        .put("height", height == null ? JSONObject.NULL : height)
                        .put("duration_seconds", durationSeconds == null ? JSONObject.NULL : durationSeconds)
                        .put("thumbnail_url", JSONObject.NULL)
                        .toString();

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return jsonError("internal error");
            }
        });

        init();
        awaitInitialization();
        System.out.println("Upload HTTP server on http://localhost:" + httpPort + " baseDir=" + basePath);
    }

    // ---------- Helpers ----------

    private static String jsonError(String msg) {
        return new JSONObject().put("error", msg).toString();
    }

    private static String subdirFor(String mime) {
        String m = mime.toLowerCase();
        if (m.startsWith("image/")) return "images";
        if (m.startsWith("audio/")) return "audios";
        return "files";
    }

    private static String mapToFileType(String mime) {
        String m = mime.toLowerCase();
        if (m.startsWith("image/")) {
            if (m.contains("gif")) return "GIF";
            return "IMAGE";
        }
        if (m.startsWith("audio/")) return "AUDIO";
        return "FILE";
    }

    private static String guessExt(String original, String mime) {
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.'));
            if (ext.length() <= 10) return ext;
        }
        if ("image/png".equalsIgnoreCase(mime))  return ".png";
        if ("image/jpeg".equalsIgnoreCase(mime)) return ".jpg";
        if ("image/gif".equalsIgnoreCase(mime))  return ".gif";
        if ("audio/mpeg".equalsIgnoreCase(mime)) return ".mp3";
        if ("audio/wav".equalsIgnoreCase(mime) || "audio/x-wav".equalsIgnoreCase(mime)) return ".wav";
        if ("application/pdf".equalsIgnoreCase(mime)) return ".pdf";
        return "";
    }

    private static String safeName(String name) {
        return name.replace("\"", "").replace("\n", "").replace("\r", "");
    }

    private static int[] imageSize(Path file) {
        try {
            BufferedImage bi = ImageIO.read(file.toFile());
            if (bi != null) return new int[]{bi.getWidth(), bi.getHeight()};
        } catch (Exception ignore) {}
        return null;
    }

    //only audio
    private static Integer audioDurationSeconds(Path file, String mime, String ext) {
        try {
            if ("audio/mpeg".equalsIgnoreCase(mime) || ".mp3".equalsIgnoreCase(ext)) {
                Mp3File mp3 = new Mp3File(file.toFile());
                return (int) mp3.getLengthInSeconds();
            }

            // WAV با javax.sound.sampled
            if ("audio/wav".equalsIgnoreCase(mime) || "audio/x-wav".equalsIgnoreCase(mime) || ".wav".equalsIgnoreCase(ext)) {
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(file.toFile())) {
                    AudioFormat format = ais.getFormat();
                    long frames = ais.getFrameLength();
                    if (frames > 0 && format.getFrameRate() > 0) {
                        double seconds = frames / format.getFrameRate();
                        return (int)Math.round(seconds);
                    }
                }
            }
        } catch (UnsupportedAudioFileException | IOException ignore) {
            // فرمت صوتی پشتیبانی نشده برای AudioSystem
        } catch (Exception ignore) {
            // mp3agic یا سایر استثناها
        }
        return null;
    }
}
