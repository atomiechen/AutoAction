package com.hcifuture.contextactionlibrary.volume;


import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URISyntaxException;

public class SocketManager {
    private Socket socket;
    private static final String SERVER_URL = "http://220.249.18.254:25603";

    public SocketManager() {
        try {
            socket = IO.socket(SERVER_URL);
            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
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
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            // 断开连接回调
        }
    };

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }
}
