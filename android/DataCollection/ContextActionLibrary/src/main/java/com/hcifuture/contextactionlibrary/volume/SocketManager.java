package com.hcifuture.contextactionlibrary.volume;


import android.util.Log;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class SocketManager extends TriggerManager {
    static final String TAG = "SocketManager";

    private Socket socket = null;

    public SocketManager(VolEventListener volEventListener) {
        super(volEventListener);

        // get server URL
        URI serverUrl;
        try {
            serverUrl = new URI(volEventListener.getServerUrl());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // ref: https://socketio.github.io/socket.io-client-java/initialization.html
        // on connection, send id information as handshake auth
        // 发送用户ID和设备ID
        HashMap<String, String> map = new HashMap<>();
        map.put("user_id", volEventListener.getUserId());
        map.put("device_id", volEventListener.getDeviceId());
        IO.Options options = IO.Options.builder().setAuth(map).build();

        // instantiate socket
        socket = IO.socket(serverUrl, options);

        // 连接成功回调
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.e(TAG, "connected to socket.io server " + serverUrl);
            Log.e(TAG, "userid: " + volEventListener.getUserId());
            Log.e(TAG, "deviceid: " + volEventListener.getDeviceId());
        });
        // 断开连接回调
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.e(TAG, "disconnected from socket.io server " + serverUrl);
            Log.e(TAG, "userid: " + volEventListener.getUserId());
            Log.e(TAG, "deviceid: " + volEventListener.getDeviceId());
        });

//        socket.on("llm_query", args -> {
//            Log.e(TAG, "LLM query: " + (String) args[0]);
//        });
//        socket.on("llm_response", args -> {
//            Log.e(TAG, "LLM response: " + (String) args[0]);
//        });

        // 情境query
        socket.on("context_query", args -> {
            try {
                // 服务器需要acknowledge，此处返回该事件的response
                // ref: https://socketio.github.io/socket.io-client-java/emitting_events.html
                int lastIndex = args.length - 1;
                if (args.length > 0 && args[lastIndex] instanceof Ack) {
                    String response = volEventListener.getCurrentContext();
                    Log.i(TAG, response);
                    ((Ack) args[lastIndex]).call(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void start() {
        if (socket != null) {
            try {
                socket.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if (socket != null) {
            try {
                socket.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
