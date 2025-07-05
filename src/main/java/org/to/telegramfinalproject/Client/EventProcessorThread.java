package org.to.telegramfinalproject.Client;

public class EventProcessorThread extends Thread {
    private final ActionHandler handler;

    public EventProcessorThread(ActionHandler handler) {
        this.handler = handler;
        setDaemon(true);
    }

//    @Override
//    public void run() {
//        while (true) {
//            try {
//                Thread.sleep(2000);
//                handler.processIncomingEvents();
//            } catch (InterruptedException ignored) {}
//        }
//    }
}
