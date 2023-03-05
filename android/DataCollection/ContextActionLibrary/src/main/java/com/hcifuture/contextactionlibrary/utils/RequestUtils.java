package com.hcifuture.contextactionlibrary.utils;

import com.hcifuture.shared.communicate.config.RequestConfig;
import com.hcifuture.shared.communicate.listener.RequestListener;

public class RequestUtils {

    static String mUserId = String.valueOf(System.currentTimeMillis());

    static public String getUserId(RequestListener requestListener) {
        // get unique user ID
        RequestConfig request = new RequestConfig();
        request.putString("getUserId", "");
        String userId = (String) requestListener.onRequest(request).getObject("getUserId");
        if (userId == null || "test_userid".equals(userId)) {
            String deviceId = getDeviceId(requestListener);
            if (deviceId != null && !"Unknown".equals(deviceId) && !deviceId.isEmpty()) {
                // use unique device ID
                userId = "dev_" + deviceId;
            } else {
                // use last known user ID
                userId = "unknown_" + mUserId;
            }
        } else {
            mUserId = userId;
        }
        return "user_" + userId;
    }

    static public String getDeviceId(RequestListener requestListener) {
        RequestConfig request = new RequestConfig();
        request.putString("getDeviceId", "");
        return (String) requestListener.onRequest(request).getObject("getDeviceId");
    }

    static public String getServerUrl(RequestListener requestListener) {
        // get socket server URL
        RequestConfig request = new RequestConfig();
        request.putString("getSocketUrl", "");
        return (String) requestListener.onRequest(request).getObject("getSocketUrl");
    }
}
