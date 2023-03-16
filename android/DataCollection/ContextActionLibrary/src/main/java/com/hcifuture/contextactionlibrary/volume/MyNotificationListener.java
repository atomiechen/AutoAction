package com.hcifuture.contextactionlibrary.volume;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.Collector;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyNotificationListener extends TriggerManager {

    public static final String TAG = "MyNotificationListener";
    private AppManager appManager;
    private Message latest_message;
    private Message last_removed_message;

    public static class Message {
        public String sender;
        public String source_app;
        public String title;
        public String content;
        public String type;

        public Message(String sender, String source_app, String title, String content, String type) {
            this.sender = sender;
            this.source_app = source_app;
            this.title = title;
            this.content = content;
            this.type = type;
        }

        public Message() { this("", "", "", "", ""); }
    }

    public MyNotificationListener(VolEventListener volEventListener, AppManager appManager) {
        super(volEventListener);
        this.appManager = appManager;
        this.latest_message = new Message();
    }

    public void onNotificationPosted(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap) {
        Log.e(TAG, "onNotificationPosted: rankingMap" + rankingMap);
        recordNotification(sbn, 0);
    }

    public void onNotificationRemoved(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, int reason) {
        Log.e(TAG, "onNotificationRemoved reason " + reason);
        recordNotification(sbn, 1);
    }

    public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
        Log.e(TAG, "onNotificationRankingUpdate: rankingMap" + rankingMap);
    }

    private void recordNotification(StatusBarNotification sbn, int posted_or_removed) {
        String source_app = appManager.getNameByPackageName(sbn.getPackageName());
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE); //通知title
        String content = extras.getString(Notification.EXTRA_TEXT); //通知内容
        String sender = "";
        if (source_app.equals("微信") || source_app.equals("QQ"))
            sender = title;
        String type = appManager.getAppType(sbn.getPackageName());
        if (posted_or_removed == 0)
            this.latest_message = new Message(sender, source_app, title, content, type);
        else
            this.last_removed_message = new Message(sender, source_app, title, content, type);

        Log.i(TAG, "package: " + sbn.getPackageName());
        Log.i(TAG, "time: " + new Date(sbn.getPostTime()));
        Log.i(TAG, "title: " + title);
        Log.i(TAG, "content: " + content);
        Log.i(TAG, "sender: " + sender);
        Log.i(TAG, "type: " + type);
        Log.i(TAG, "source_app: " + source_app);
    }

    public List<String> getMessageBehavior() {
        List<String> result = new ArrayList<>();
        if (last_removed_message != null) {
            result.add("remove a message: " + Collector.gson.toJson(last_removed_message));
            last_removed_message = null;
        }
        return result;
    }

    public Message getLatestMessage() {
        return latest_message;
    }
}
