package com.hcifuture.contextactionlibrary.volume;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.Collector;

import java.util.ArrayList;
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
        recordNotification(sbn, 0, 0);
    }

    public void onNotificationRemoved(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, int reason) {
        Log.e(TAG, "onNotificationRemoved reason " + reason);
        recordNotification(sbn, 1, reason);
    }

    public void onNotificationRankingUpdate(NotificationListenerService.RankingMap rankingMap) {
        Log.e(TAG, "onNotificationRankingUpdate: rankingMap" + rankingMap);
    }

    private void recordNotification(StatusBarNotification sbn, int posted_or_removed, int reason) {
        String source_app = appManager.getNameByPackageName(sbn.getPackageName());
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE); //通知title
        String content = extras.getString(Notification.EXTRA_TEXT); //通知内容
        String sender = "";
        if (source_app.equals("微信") || source_app.equals("QQ"))
            sender = title;
        String type = appManager.getAppType(sbn.getPackageName());
        if (type.equals("system"))
            return;
        if (posted_or_removed == 0)
            this.latest_message = new Message(sender, source_app, title, content, type);
        else
            this.last_removed_message = new Message(sender, source_app, title, content, type);

        Bundle bundle = new Bundle();
        bundle.putString("source_app", source_app);
        bundle.putString("title", title);
        bundle.putString("content", content);
        bundle.putString("sender", sender);
        bundle.putString("type", type);
        bundle.putString("key", sbn.getKey());

        if (posted_or_removed == 0)
            volEventListener.onVolEvent(VolEventListener.EventType.NewMessagePosted, bundle);
        else {
            String reasonString = "";
            switch (reason) {
                case NotificationListenerService.REASON_CLICK:
                    reasonString = "click";
                    break;
                case NotificationListenerService.REASON_CANCEL:
                    reasonString = "cancel";
                    break;
                case NotificationListenerService.REASON_CANCEL_ALL:
                    reasonString = "cancel all";
                    break;
                case NotificationListenerService.REASON_ERROR:
                    reasonString = "error";
                    break;
                case NotificationListenerService.REASON_PACKAGE_CHANGED:
                    reasonString = "package changed";
                    break;
                case NotificationListenerService.REASON_USER_STOPPED:
                    reasonString = "user stopped";
                    break;
                case NotificationListenerService.REASON_PACKAGE_BANNED:
                    reasonString = "package banned";
                    break;
                case NotificationListenerService.REASON_APP_CANCEL:
                    reasonString = "app cancel";
                    break;
                case NotificationListenerService.REASON_APP_CANCEL_ALL:
                    reasonString = "app cancel all";
                    break;
                case NotificationListenerService.REASON_LISTENER_CANCEL:
                    reasonString = "listener cancel";
                    break;
                case NotificationListenerService.REASON_LISTENER_CANCEL_ALL:
                    reasonString = "listener cancel all";
                    break;
                case NotificationListenerService.REASON_GROUP_SUMMARY_CANCELED:
                    reasonString = "group summary canceled";
                    break;
                case NotificationListenerService.REASON_GROUP_OPTIMIZATION:
                    reasonString = "group optimization";
                    break;
                case NotificationListenerService.REASON_PACKAGE_SUSPENDED:
                    reasonString = "package suspended";
                    break;
                case NotificationListenerService.REASON_PROFILE_TURNED_OFF:
                    reasonString = "profile turned off";
                    break;
                case NotificationListenerService.REASON_UNAUTOBUNDLED:
                    reasonString = "unautobundled";
                    break;
                case NotificationListenerService.REASON_CHANNEL_BANNED:
                    reasonString = "channel banned";
                    break;
                case NotificationListenerService.REASON_SNOOZED:
                    reasonString = "snoozed";
                    break;
                case NotificationListenerService.REASON_TIMEOUT:
                    reasonString = "timeout";
                    break;
                case NotificationListenerService.REASON_CHANNEL_REMOVED:
                    reasonString = "channel removed";
                    break;
                case NotificationListenerService.REASON_CLEAR_DATA:
                    reasonString = "clear data";
                    break;
                default:
                    reasonString = "unknown: " + reason;

            }
            bundle.putString("reason", reasonString);
            volEventListener.onVolEvent(VolEventListener.EventType.MessageRemoved, bundle);
        }
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
