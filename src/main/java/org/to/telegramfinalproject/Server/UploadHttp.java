package org.to.telegramfinalproject.Server;

import static spark.Spark.*;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.*;
import java.io.InputStream;

import org.json.JSONObject;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;




import java.nio.file.*;
import java.time.LocalDate;


// برای ویدیو (MP4 و …)
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
//import org.jcodec.containers.mp4.MP4Demuxer;
//import org.jcodec.containers.mp4.MP4DemuxerTrack;

// برای MP3
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
                String typeDir = subdirFor(mime); // images/videos/audios/files
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

                // متادیتا
                Integer width = null, height = null, durationSeconds = null;
                String thumbnailUrl = null;

                if ("IMAGE".equals(fileType) || "GIF".equals(fileType)) {
                    int[] wh = imageSize(target);
                    if (wh != null) { width = wh[0]; height = wh[1]; }
                } else if ("VIDEO".equals(fileType)) {
                    // تلاش برای استخراج width/height/duration با JCodec
                    VideoMeta vm = videoMeta(target);
                    if (vm != null) {
                        width = vm.width;
                        height = vm.height;
                        durationSeconds = vm.durationSeconds;
                    }
                    // ساخت thumbnail (اختیاری)
                    try {
                        String thumbName = name.replace(ext, "") + "_thumb.jpg";
                        Path thumbDir = basePath.resolve("thumbs/" + day).normalize();
                        Files.createDirectories(thumbDir);
                        Path thumbTarget = thumbDir.resolve(thumbName).normalize();
                        if (makeVideoThumbnail(target, thumbTarget)) {
                            thumbnailUrl = "/thumbs/" + day + "/" + thumbName;
                        }
                    } catch (Exception ignore) {}
                } else if ("AUDIO".equals(fileType)) {
                    // اگر MP3 بود، مدت را با mp3agic بگیر
                    if ("audio/mpeg".equalsIgnoreCase(mime) || ext.equalsIgnoreCase(".mp3")) {
                        try {
                            Mp3File mp3 = new Mp3File(target.toFile());
                            durationSeconds = (int) mp3.getLengthInSeconds();
                        } catch (Exception ignore) {}
                    }
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
                        .put("thumbnail_url", thumbnailUrl == null ? JSONObject.NULL : thumbnailUrl)
                        .toString();

            } catch (Exception e) {
                e.printStackTrace(); // لوکال
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
        if (m.startsWith("video/")) return "videos";
        if (m.startsWith("audio/")) return "audios";
        return "files";
    }

    private static String mapToFileType(String mime) {
        String m = mime.toLowerCase();
        if (m.startsWith("image/")) {
            if (m.contains("gif")) return "GIF";
            return "IMAGE";
        }
        if (m.startsWith("video/")) return "VIDEO";
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
        if ("video/mp4".equalsIgnoreCase(mime))  return ".mp4";
        if ("audio/mpeg".equalsIgnoreCase(mime)) return ".mp3";
        if ("application/pdf".equalsIgnoreCase(mime)) return ".pdf";
        return "";
    }

    private static String safeName(String name) {
        // پاک‌سازی خیلی ساده برای خروجی
        return name.replace("\"", "").replace("\n", "").replace("\r", "");
    }

    private static int[] imageSize(Path file) {
        try {
            BufferedImage bi = ImageIO.read(file.toFile());
            if (bi != null) return new int[]{bi.getWidth(), bi.getHeight()};
        } catch (Exception ignore) {}
        return null;
    }

    // --- Video meta via JCodec ---
    private static class VideoMeta {
        final Integer width, height, durationSeconds;
        VideoMeta(Integer w, Integer h, Integer d) { this.width = w; this.height = h; this.durationSeconds = d; }
    }

    private static VideoMeta videoMeta(Path file) {
        try {
            // Width/Height از طریق اولین فریم
            BufferedImage first = null;
            try {
                FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file.toFile()));
                Picture p = grab.getNativeFrame();
                if (p != null) first = AWTUtil.toBufferedImage(p);
            } catch (Exception ignore) {}

            Integer w = null, h = null;
            if (first != null) { w = first.getWidth(); h = first.getHeight(); }

            // Duration از Demuxer (فقط MP4ها عالی جواب میده)
            Integer dur = null;
//            try {
//                MP4Demuxer demuxer = new MP4Demuxer(NIOUtils.readableChannel(file.toFile()));
//                MP4DemuxerTrack vt = (MP4DemuxerTrack) demuxer.getVideoTrack();
//                double seconds = vt.getMeta().getTotalDuration();
//                dur = (int) Math.round(seconds);
//            } catch (Exception ignore) {}

            if (w != null || h != null || dur != null) return new VideoMeta(w, h, dur);
        } catch (Exception ignore) {}
        return null;
    }

    private static boolean makeVideoThumbnail(Path videoFile, Path thumbTarget) {
        try {
            FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile.toFile()));
            Picture p = grab.getNativeFrame();
            if (p == null) return false;
            BufferedImage bi = AWTUtil.toBufferedImage(p);
            Files.createDirectories(thumbTarget.getParent());
            return ImageIO.write(bi, "jpg", thumbTarget.toFile());
        } catch (Exception e) {
            return false;
        }
    }
}
