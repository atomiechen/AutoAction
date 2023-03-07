package com.hcifuture.contextactionlibrary.volume;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.Date;

public class MyNotificationListener extends TriggerManager {

    static final String TAG = "MyNotificationListener";

    public MyNotificationListener(VolEventListener volEventListener) {
        super(volEventListener);
    }

    public void onNotificationPosted(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap) {
        Log.e(TAG, "onNotificationPosted: rankingMap" + rankingMap);
        showNotification(sbn);
    }

    public void onNotificationRemoved(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, int reason) {
        Log.e(TAG, "onNotificationRemoved reason " + reason);
        showNotification(sbn);
    }

    public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
        Log.e(TAG, "onNotificationRankingUpdate: rankingMap" + rankingMap);
    }

    private void showNotification(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE); //通知title
        String content = extras.getString(Notification.EXTRA_TEXT); //通知内容
//        int smallIconId = extras.getInt(Notification.EXTRA_SMALL_ICON); //通知小图标id
//        Bitmap largeIcon = extras.getParcelable(Notification.EXTRA_LARGE_ICON); //通知的大图标，注意和获取小图标的区别
//        PendingIntent pendingIntent = sbn.getNotification().contentIntent; //获取通知的PendingIntent
//        sbn.getNotification().getLargeIcon();
        Log.i(TAG, "package: " + sbn.getPackageName());
        Log.i(TAG, "time: " + new Date(sbn.getPostTime()));
        Log.i(TAG, "title: " + title);
        Log.i(TAG, "content: " + content);
    }

}
