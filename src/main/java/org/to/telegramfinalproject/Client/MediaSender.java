package org.to.telegramfinalproject.Client;

import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;



public class MediaSender {

    public static void sendImageOrAudio(Socket socket,
                                        UUID senderId,
                                        String receiverType,
                                        UUID receiverId,
                                        File file,
                                        String messageType,   // "IMAGE" یا "AUDIO"
                                        String captionOrEmpty) throws Exception {

        if (!"IMAGE".equals(messageType) && !"AUDIO".equals(messageType))
            throw new IllegalArgumentException("Only IMAGE/AUDIO");

        // 1) اعلام سوییچ به باینری
        PrintWriter textOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        textOut.println("MEDIA");

        // 2) متادیتا
        String mime = Files.probeContentType(file.toPath());
        if (mime == null) mime = "application/octet-stream";

        Integer width = null, height = null;
        if ("IMAGE".equals(messageType)) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) { width = img.getWidth(); height = img.getHeight(); }
            } catch (Exception ignore) {}
        }

        UUID messageId = UUID.randomUUID();
        JSONObject header = new JSONObject()
                .put("message_id", messageId.toString())
                .put("sender_id", senderId.toString())
                .put("receiver_type", receiverType)
                .put("receiver_id", receiverId.toString())
                .put("message_type", messageType)
                .put("file_name", file.getName())
                .put("mime_type", mime)
                .put("file_size", file.length())
                .put("text", captionOrEmpty == null ? "" : captionOrEmpty);

        if (width != null)  header.put("width", width);
        if (height != null) header.put("height", height);

        byte[] headerBytes = header.toString().getBytes("UTF-8");

        // 3) ارسال فریم باینری
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        dos.writeInt(0x4D444D31);               // MAGIC
        dos.writeInt(headerBytes.length);       // headerLen
        dos.write(headerBytes);                 // header
        dos.writeLong(file.length());           // contentLen

        try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) {
                dos.write(buf, 0, n);
            }
        }
        dos.flush();

        // (اختیاری) Ack متنی
        BufferedReader textIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        String ack = textIn.readLine();
        if (!"OK".equalsIgnoreCase(ack)) {
            throw new IOException("Server did not ACK: " + ack);
        }
    }
}
