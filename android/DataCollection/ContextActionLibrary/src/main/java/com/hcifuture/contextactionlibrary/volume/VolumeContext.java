package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VolumeContext {
    public long context_timestamp;
    public String context_exact_time;
    public String context_time;
    public String context_day_of_week;
    public String context_gps_position;
    public String context_activity;
    public String context_wifi_name;
    public String context_noise_level;
    public int context_noise_db;
    public String context_audio_device;
    public String context_app;
    public String context_network;
    public int context_network_delay;
    public String context_screen_orientation;
    public int context_nearby_PC;
    public HashMap<String, Integer> context_volume;
    public String context_volume_stream_type;
    public boolean context_audio_playing;
    public String message_sender;
    public String message_source_app;
    public String message_title;
    public String message_content;
    public String message_type;

    public List<Event> events;

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

    public VolumeContext(long timestamp, String exact_time, String time, String week, String gps_position, String activity, String wifi_name, String context_noise_level, int noise_db, String context_audio_device, String app,
                         String network, String message_sender, String message_source_app, String message_title, String message_content, String message_type,
                         int network_delay, String screen_orientation, int nearby_PC, HashMap<String, Integer> volume,
                         String streamType, boolean audio_playing, List<Event> eventList) {
        this.context_timestamp = timestamp;
        this.context_exact_time = exact_time;
        this.context_time = time;
        this.context_day_of_week = week;
        this.context_gps_position = gps_position;
        this.context_activity = activity;
        this.context_wifi_name = wifi_name;
        this.context_noise_level = context_noise_level;
        this.context_noise_db = noise_db;
        this.context_audio_device = context_audio_device;
        this.context_app = app;
        this.context_network = network;
        this.context_network_delay = network_delay;
        this.context_screen_orientation = screen_orientation;
        this.context_nearby_PC = nearby_PC;
        this.context_volume = volume;
        this.context_volume_stream_type = streamType;
        this.context_audio_playing = audio_playing;
        this.message_sender = message_sender;
        this.message_source_app = message_source_app;
        this.message_title = message_title;
        this.message_content = message_content;
        this.message_type = message_type;

        this.events = new ArrayList<>();
        this.events.addAll(eventList);
    }

}
