package com.hcifuture.contextactionlibrary.volume;


import android.util.Log;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class SocketManager extends TriggerManager {
    static final String TAG = "SocketManager";

    private Socket socket;

    public SocketManager(VolEventListener volEventListener, String serverUrl) {
        super(volEventListener);
        try {
            socket = IO.socket(serverUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // 连接成功回调
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.e(TAG, "connected to socket.io server " + serverUrl);
        });
        // 断开连接回调
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.e(TAG, "disconnected from socket.io server " + serverUrl);
        });

        socket.on("llm_query", args -> {
            Log.e(TAG, "LLM query: " + (String) args[0]);
        });
        socket.on("llm_response", args -> {
            Log.e(TAG, "LLM response: " + (String) args[0]);
        });

        // 情境query
        socket.on("context_query", args -> {
            // 服务器需要acknowledge，此处返回该事件的response
            if (args.length > 1 && args[1] instanceof Ack) {
                String response = volEventListener.getCurrentContext();
                ((Ack) args[1]).call(response);
            }
        });
    }

    public void connect() {
        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }

    public void sendMessage(String message) {
        socket.emit("message", message, new Ack() {
            @Override
            public void call(Object... args) {

            }
        });
    }

    public void addMessageListener(final OnMessageReceivedListener listener) {
        socket.on("message", args -> {
            String message = (String) args[0];
            listener.onMessageReceived(message);
        });
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }
}
