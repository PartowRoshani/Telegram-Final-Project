package org.to.telegramfinalproject.Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;

public final class SocketMediaDownloader {
    private static final int MAGIC_DL = 0x4D444D32;
    private final PrintWriter outText;         // NEW
    private final DataInputStream inBin;
    private final DataOutputStream outBin;

    public SocketMediaDownloader(PrintWriter outText, DataInputStream inBin, DataOutputStream outBin) {
        this.outText = outText;
        this.inBin = inBin;
        this.outBin = outBin;
    }


    public java.nio.file.Path download(java.util.UUID mediaKey, java.nio.file.Path saveDir, String fileNameHint) throws Exception {
        // 1) سوییچ مود با PrintWriter
        outText.print("MEDIA_DL\n");
        outText.flush();

        // 2) هدر باینری درخواست
        org.json.JSONObject req = new org.json.JSONObject()
                .put("op","download")
                .put("media_key", mediaKey.toString())
                .put("offset", 0);
        byte[] hb = req.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        outBin.writeInt(MAGIC_DL);
        outBin.writeInt(hb.length);
        outBin.write(hb);
        outBin.flush();

        // 3) پاسخ
        int magic = inBin.readInt();
        if (magic != MAGIC_DL) throw new java.io.IOException("bad magic");

        int hlen = inBin.readInt();
        byte[] hbytes = inBin.readNBytes(hlen);
        org.json.JSONObject hdr = new org.json.JSONObject(new String(hbytes, java.nio.charset.StandardCharsets.UTF_8));
        if (!"success".equalsIgnoreCase(hdr.optString("status"))) {
            throw new java.io.IOException("download error: " + hdr.optString("message"));
        }

        long contentLen = inBin.readLong();
        String serverName = hdr.optString("file_name", fileNameHint != null ? fileNameHint : mediaKey.toString());

        java.nio.file.Files.createDirectories(saveDir);
        java.nio.file.Path dest = uniquePath(saveDir, serverName);

        try (java.io.OutputStream os = java.nio.file.Files.newOutputStream(dest)) {
            byte[] buf = new byte[8192];
            long remain = contentLen;
            while (remain > 0) {
                int toRead = (int) Math.min(buf.length, remain);
                int n = inBin.read(buf, 0, toRead);
                if (n == -1) throw new java.io.EOFException("unexpected EOF");
                os.write(buf, 0, n);
                remain -= n;
            }
        }
        return dest;
    }

    private static java.nio.file.Path uniquePath(java.nio.file.Path dir, String name) throws java.io.IOException {
        java.nio.file.Path p = dir.resolve(name);
        if (!java.nio.file.Files.exists(p)) return p;
        String base = name, ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0) { base = name.substring(0, dot); ext = name.substring(dot); }
        int i = 1;
        while (java.nio.file.Files.exists(dir.resolve(base + " (" + i + ")" + ext))) i++;
        return dir.resolve(base + " (" + i + ")" + ext);
    }
}
