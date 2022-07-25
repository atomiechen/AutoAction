package com.hcifuture.contextactionlibrary.position.utility;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class NotificationUtility {
    private static final String TAG = "NotificationUtility";

    private static boolean allowToast = true;

    public static void setAllowToast(boolean allowToast) {
        NotificationUtility.allowToast = allowToast;
    }

    public static void sendShortToast(Context context, String information) {
        if (!allowToast) {
            Log.i(TAG, "sendShortToast: Not allowed. " + information);
            return;
        }
        Log.i(TAG, "sendShortToast: " + information);
        Toast.makeText(context, information, Toast.LENGTH_SHORT).show();
    }
}
