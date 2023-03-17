package com.hcifuture.contextactionlibrary.volume;

import java.util.ArrayList;
import java.util.List;

public class VolumeContext {
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

    private List<String> events;
    private List<String> behavior;

//    public static class m_Event {
//        long timestamp;
//        String event;
//
//        public m_Event(long time, String event) {
//            this.timestamp = time;
//            this.event = event;
//        }
//    }

    public VolumeContext(String time, String week, String gps_position, String activity, String wifi_name, String environment_sound, int noise_db, String playback_device, String app,
                         String network, String message_sender, String message_source_app, String message_title, String message_content, String message_type, List<String> message_behavior, List<String> volume_behavior) {
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

        this.events = new ArrayList<>();

        this.behavior = new ArrayList<>();
        this.behavior.addAll(message_behavior);
        this.behavior.addAll(volume_behavior);
    }

    public static VolumeContext fillEvent(VolumeContext old, VolumeContext present) {
        if (!present.context_time.equals(old.context_time)) {
            present.events.add("TimeChange");
        }
        if (!present.context_day_of_week.equals(old.context_day_of_week)) {
            present.events.add("DayOfWeekChange");
        }
        if (!present.context_gps_position.equals(old.context_gps_position)) {
            present.events.add("PositionChange");
        }
        if (!present.context_activity.equals(old.context_activity)) {
            present.events.add("ActivityChange");
        }
        if (!present.context_wifi_name.equals(old.context_wifi_name)) {
            present.events.add("WifiChange");
        }
        if (!present.context_environment_sound.equals(old.context_environment_sound)) {
            if (present.context_noise_db - old.context_noise_db > 5)
                present.events.add("NoiseUp");
            else if (old.context_noise_db - present.context_noise_db > 5)
                present.events.add("NoiseDown");
        }
        if (!present.context_playback_device.equals(old.context_playback_device)) {
            present.events.add("DeviceChange");
        }
        if (!present.context_app.equals(old.context_app)) {
            present.events.add("AppChange");
        }
        if (!present.context_network.equals(old.context_network)) {
            present.events.add("NetworkChange");
        }
        if (!present.message_content.equals(old.message_content)) {
            present.events.add("NewMessageCome");
        }
        return present;
    }

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
