package org.to.telegramfinalproject.Utils;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

public class FileUtil {

    public static JSONObject uploadProfilePicture(
            File file,
            UUID userUuid,
            PrintWriter textOut,
            BufferedReader textIn,
            DataOutputStream binOut
    ) throws IOException {

        if (file == null || !file.exists() || !file.isFile()) {
            throw new FileNotFoundException("Selected file is invalid.");
        }

        String mime = null;
        try {
            mime = Files.probeContentType(file.toPath());
        } catch (IOException ignore) {}
        if (mime == null) mime = "application/octet-stream";

        long size = file.length();
        System.out.println("[FileUtil] Preparing to upload: " + file.getName() +
                " (size=" + size + ", mime=" + mime + ")"); //log 1


        // sending the metadata
        JSONObject meta = new JSONObject();
        meta.put("action", "upload_profile_picture");
        meta.put("user_uuid", userUuid.toString());
        meta.put("file_name", file.getName());
        meta.put("file_size", size);
        meta.put("mime", mime);

        textOut.println(meta.toString());
        textOut.flush();

        System.out.println("[FileUtil] Metadata sent to server.");//log 2

        // sending the file bytes
        try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buf = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int toRead = (int) Math.min(buf.length, remaining);
                int r = fis.read(buf, 0, toRead);
                if (r == -1) break;
                binOut.write(buf, 0, r);
                remaining -= r;
            }
            binOut.flush();
            System.out.println("[FileUtil] File bytes sent successfully."); // log 3
        }

        String respLine = textIn.readLine();
        if (respLine == null) {
            throw new EOFException("No response from server after upload.");
        }
        System.out.println("[FileUtil] Server response received: " + respLine); // log 4
        return new JSONObject(respLine);
    }
}
