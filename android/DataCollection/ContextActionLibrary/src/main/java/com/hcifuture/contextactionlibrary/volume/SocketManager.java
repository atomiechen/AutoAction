package com.hcifuture.contextactionlibrary.volume;


import android.util.Log;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URISyntaxException;

public class SocketManager {
    static String TAG = "SocketManager";

    private Socket socket;

    public SocketManager(String serverUrl) {
        try {
            socket = IO.socket(serverUrl);
            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            socket.on("llm_query", args -> {
                Log.e(TAG, "LLM query: " + (String) args[0]);
            });
            socket.on("llm_response", args -> {
                Log.e(TAG, "LLM response: " + (String) args[0]);
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        socket.connect();
    }

    public void disconnect() {
        socket.disconnect();
    }

    public void sendMessage(String message) {
        socket.emit("message", message);
    }

    public void addMessageListener(final OnMessageReceivedListener listener) {
        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String message = (String) args[0];
                listener.onMessageReceived(message);
            }
        });
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // 连接成功回调
            Log.e(TAG, "connected to socket io server");
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // 断开连接回调
            Log.e(TAG, "disconnected from socket io server");
        }
    };

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }
}
