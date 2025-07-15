package org.to.telegramfinalproject.Client;

import org.json.JSONObject;

import java.io.BufferedReader;

//public class EventProcessorThread extends Thread {
//    private final ActionHandler handler;
//    private final BufferedReader in;
//
//    public EventProcessorThread(ActionHandler handler, BufferedReader in) {
//        this.handler = handler;
//        this.in = in;
//        setDaemon(true);
//    }
//
//    @Override
//    public void run() {
//        try {
//            System.out.println("👂 Real-Time Listener started.");
//            String line;
//            while ((line = in.readLine()) != null) {
//                JSONObject json = new JSONObject(line);
//                System.out.println("📥 Received raw line: " + line);
//
//                if (json.has("action")) {
//                    // پیام real-time
//                    handler.processIncomingEvent(json);
//                } else {
//                    // پیام پاسخ معمولی
//                    TelegramClient.responseQueue.put(json);
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("❌ Error in EventProcessorThread: " + e.getMessage());
//        }
//    }
//}
