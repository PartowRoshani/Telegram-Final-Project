package org.to.telegramfinalproject.Client;


import org.json.JSONObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RealTimeBuffer {
    public static final BlockingQueue<JSONObject> incomingEvents = new LinkedBlockingQueue<>();
}

