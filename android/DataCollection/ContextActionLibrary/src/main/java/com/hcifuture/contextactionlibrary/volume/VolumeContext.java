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
    private String context_network_delay;
    private String context_screen_orientation;
    private int context_nearby_PC;
    private String context_volume;
    private String message_sender;
    private String message_source_app;
    private String message_title;
    private String message_content;
    private String message_type;

    private List<Event> events;

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
                         String network_delay, String screen_orientation, int nearby_PC, String volume, List<Event> eventList) {
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
        this.context_network_delay = network_delay;
        this.context_screen_orientation = screen_orientation;
        this.context_nearby_PC = nearby_PC;
        this.context_volume = volume;
        this.message_sender = message_sender;
        this.message_source_app = message_source_app;
        this.message_title = message_title;
        this.message_content = message_content;
        this.message_type = message_type;

        this.events = new ArrayList<>();
        this.events.addAll(eventList);
    }

    public String getContextTime() {
        return context_time;
    }

    public String getContextWeek() {
        return context_day_of_week;
    }

    public String getContextGpsPosition() {
        return context_gps_position;
    }

    public String getContextActivity() {
        return context_activity;
    }

    public String getContextWifiName() {
        return context_wifi_name;
    }

    public String getContextEnvironmentSound() {
        return context_environment_sound;
    }

    public String getContextPlaybackDevice() {
        return context_playback_device;
    }

    public String getContextApp() {
        return context_app;
    }

    public String getContextNetwork() {
        return context_network;
    }

    public String getMessageSender() {
        return message_sender;
    }

    public String getMessageSourceApp() {
        return message_source_app;
    }

    public String getMessageTitle() {
        return message_title;
    }

    public String getMessageContent() {
        return message_content;
    }

    public String getMessageType() {
        return message_type;
    }

}
