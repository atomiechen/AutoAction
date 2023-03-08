package com.hcifuture.contextactionlibrary.volume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VolumeContext {
    String context_time;
    String context_week;
    String context_gps_position;
    String context_activity;
    String context_wifi_name;
    String context_environment_sound;
    String context_playback_device;
    String context_app;
    String context_network;
    String message_sender;
    String message_source_app;
    String message_title;
    String message_content;
    String message_type;

    public VolumeContext(String time, String week, String gps_position, String activity, String wifi_name, String environment_sound, String playback_device, String app,
                         String network, String message_sender, String message_source_app, String message_title, String message_content, String message_type) {
        this.context_time = time;
        this.context_week = week;
        this.context_gps_position = gps_position;
        this.context_activity = activity;
        this.context_wifi_name = wifi_name;
        this.context_environment_sound = environment_sound;
        this.context_playback_device = playback_device;
        this.context_app = app;
        this.context_network = network;
        this.message_sender = message_sender;
        this.message_source_app = message_source_app;
        this.message_title = message_title;
        this.message_content = message_content;
        this.message_type = message_type;
    }
}
