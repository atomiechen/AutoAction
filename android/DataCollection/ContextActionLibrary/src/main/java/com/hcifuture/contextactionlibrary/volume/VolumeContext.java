package com.hcifuture.contextactionlibrary.volume;

import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VolumeContext {
    private String context_exact_time;
    private String context_time;
    private String context_day_of_week;
    private String context_gps_position;
    private String context_activity;
    private String context_wifi_name;
    private String context_environment_sound;
    private int context_noise_db;
    private String context_playback_device;
    private String context_app;
    private String context_network;
    private String message_sender;
    private String message_source_app;
    private String message_title;
    private String message_content;
    private String message_type;

    private List<Event> events;
    private List<String> behavior;

    public static class Event {
        long timestamp;
        String event;
        Bundle extras;

        public Event(long time, String event, Bundle extras) {
            this.timestamp = time;
            this.event = event;
            this.extras = extras;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public VolumeContext(String exact_time, String time, String week, String gps_position, String activity, String wifi_name, String environment_sound, int noise_db, String playback_device, String app,
                         String network, String message_sender, String message_source_app, String message_title, String message_content, String message_type,
                         List<Event> eventList, List<String> message_behavior, List<String> volume_behavior) {
        this.context_exact_time = exact_time;
        this.context_time = time;
        this.context_day_of_week = week;
        this.context_gps_position = gps_position;
        this.context_activity = activity;
        this.context_wifi_name = wifi_name;
        this.context_environment_sound = environment_sound;
        this.context_noise_db = noise_db;
        this.context_playback_device = playback_device;
        this.context_app = app;
        this.context_network = network;
        this.message_sender = message_sender;
        this.message_source_app = message_source_app;
        this.message_title = message_title;
        this.message_content = message_content;
        this.message_type = message_type;

        this.events = eventList;

        this.behavior = new ArrayList<>();
        this.behavior.addAll(message_behavior);
        this.behavior.addAll(volume_behavior);
    }


//    private List<TempEvent> fillEvent(List<Event> eventList) {
//        List<TempEvent> result = new ArrayList<>();
//        for (Event event: eventList) {
//            String event_name = event.event;
//            boolean already_in_list = false;
//            for (TempEvent tempEvent: result) {
//                if (tempEvent.event.equals(event_name)) {
//                    tempEvent.count += 1;
//                    already_in_list = true;
//                }
//            }
//            if (!already_in_list) {
//                result.add(new TempEvent(event_name, 1));
//            }
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            result.sort((o1, o2) -> o2.count - o1.count);
//        }
//        return result;
//    }

    public String getContextTime() {
        return context_time;
    }

    public void setContextTime(String context_time) {
        this.context_time = context_time;
    }

    public String getContextWeek() {
        return context_day_of_week;
    }

    public void setContextWeek(String context_week) {
        this.context_day_of_week = context_week;
    }

    public String getContextGpsPosition() {
        return context_gps_position;
    }

    public void setContextGpsPosition(String context_gps_position) {
        this.context_gps_position = context_gps_position;
    }

    public String getContextActivity() {
        return context_activity;
    }

    public void setContextActivity(String context_activity) {
        this.context_activity = context_activity;
    }

    public String getContextWifiName() {
        return context_wifi_name;
    }

    public void setContextWifiName(String context_wifi_name) {
        this.context_wifi_name = context_wifi_name;
    }

    public String getContextEnvironmentSound() {
        return context_environment_sound;
    }

    public void setContextEnvironmentSound(String context_environment_sound) {
        this.context_environment_sound = context_environment_sound;
    }

    public String getContextPlaybackDevice() {
        return context_playback_device;
    }

    public void setContextPlaybackDevice(String context_playback_device) {
        this.context_playback_device = context_playback_device;
    }

    public String getContextApp() {
        return context_app;
    }

    public void setContextApp(String context_app) {
        this.context_app = context_app;
    }

    public String getContextNetwork() {
        return context_network;
    }

    public void setContextNetwork(String context_network) {
        this.context_network = context_network;
    }

    public String getMessageSender() {
        return message_sender;
    }

    public void setMessageSender(String messageSender) {
        this.message_sender = messageSender;
    }

    public String getMessageSourceApp() {
        return message_source_app;
    }

    public void setMessageSourceApp(String messageSourceApp) {
        this.message_source_app = messageSourceApp;
    }

    public String getMessageTitle() {
        return message_title;
    }

    public void setMessageTitle(String message_title) {
        this.message_title = message_title;
    }

    public String getMessageContent() {
        return message_content;
    }

    public void setMessageContent(String message_content) {
        this.message_content = message_content;
    }

    public String getMessageType() {
        return message_type;
    }

    public void setMessageType(String message_type) {
        this.message_type = message_type;
    }

}
