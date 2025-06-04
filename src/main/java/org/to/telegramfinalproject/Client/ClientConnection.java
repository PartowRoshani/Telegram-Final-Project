package org.to.telegramfinalproject.Client;

import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(String request) {
        out.println(request);
    }

    public String receive() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        socket.close();
        in.close();
        out.close();
    }
}