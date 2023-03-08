package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

import com.hcifuture.contextactionlibrary.utils.TimeUtils;

public class VolumeContext {
    private String context_time;
    private String context_week;
    private String context_gps_position;
    private String context_activity;
    private String context_wifi_name;
    private String context_environment_sound;
    private String context_playback_device;
    private String context_app;
    private String context_network;
    private String message_sender;
    private String message_source_app;
    private String message_title;
    private String message_content;
    private String message_type;

    private String[] events;

    public VolumeContext(String context_gps_position, String context_activity, String context_wifi_name,
                         String context_environment_sound, String context_playback_device, String context_app, String context_network,
                         String message_sender, String message_source_app, String message_title, String message_content, String message_type) {
        this.context_time = TimeUtils.getCurrentTimePeriod();
        this.context_week = TimeUtils.getCurrentDayOfWeek();
        this.context_gps_position = context_gps_position;
        this.context_activity = context_activity;
        this.context_wifi_name = context_wifi_name;
        this.context_environment_sound = context_environment_sound;
        this.context_playback_device = context_playback_device;
        this.context_app = context_app;
        this.context_network = context_network;
        this.message_sender = message_sender;
        this.message_source_app = message_source_app;
        this.message_title = message_title;
        this.message_content = message_content;
        this.message_type = message_type;
    }

    public Bundle toBundle() {
        Bundle result = new Bundle();
//        if (soundVolume >= 0) result.putInt("soundVolume", soundVolume);
//        if (device != null) result.putString("device", device);
//        if (device != null) {
//            if (device.endsWith("speaker")) result.putString("device", "扬声器");
//            else if (device.endsWith("earpiece") || device.endsWith("headset") || device.contains("bt") || device.contains("sco") || device.endsWith("headphone"))
//                result.putString("device", "耳机");
//            else result.putString("device", "未知类型");
//        }
//        if (time >= 0) {
//            result.putInt("time", time);
//            result.putInt("startTime", startTime);
//            result.putInt("endTime", endTime);
//        }
//        if (app != null) result.putString("app", app);
//        if (activity >= 0) result.putInt("activity", activity);
//        if (noise >= 0) {
//            if (noise < 50) result.putInt("noise", 0);
//            else if (noise > 70) result.putInt("noise", 2);
//            else result.putInt("noise", 1);
//        }
//        if (manAround >= 0) {
//            if (manAround == 1) result.putBoolean("manAround", true);
//            else if (manAround == 0) result.putBoolean("manAround", false);
//        }
//        if (share >= 0) {
//            if (share == 1) result.putBoolean("share", true);
//            else if (share == 0) result.putBoolean("share", false);
//        }
//        if (!(wifiId == null && (latitude < -90 || longitude < -180))) {
//            String place = ContextRuleManager.findPlace(this);
//            result.putString("place", place);
//        }
        return result;
    }
//    @Override
//    public String toString() {
//        DateFormat df = new SimpleDateFormat("HH:mm:ss");
//        return "VolumeContext{" +
//                df.format(date) +
//                ", (" + latitude +
//                ", " + longitude +
//                "), noise=" + noise +
//                ", app='" + app + '\'' +
//                ", " + device + '\'' +
//                '}';
//    }

    // Getters and setters for all member variables

    public String getContextTime() {
        return context_time;
    }

    public void setContextTime(String context_time) {
        this.context_time = context_time;
    }

    public String getContextWeek() {
        return context_week;
    }

    public void setContextWeek(String context_week) {
        this.context_week = context_week;
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

    public void setEvents(String[] events) {
        this.events = events;
    }

    public String[] getEvents() {
        return events;
    }

}
