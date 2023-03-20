package com.hcifuture.contextactionlibrary.volume;

import java.util.ArrayList;

public class Condition {
    private final ArrayList<String> context_time;
    private final ArrayList<String> context_week;
    private final ArrayList<String> context_gps_position;
    private final ArrayList<String> context_activity;
    private final ArrayList<String> context_wifi_name;
    private final ArrayList<String> context_environment_sound;
    private final ArrayList<String> context_playback_device;
    private final ArrayList<String> context_app;
    private final ArrayList<String> context_network;
    private final ArrayList<String> message_sender;
    private final ArrayList<String> message_source_app;
    private final ArrayList<String> message_title;
    private final ArrayList<String> message_content;
    private final ArrayList<String> message_type;

    // Constructor
    public Condition(ArrayList<String> context_time, ArrayList<String> context_week, ArrayList<String> context_gps_position,
                     ArrayList<String> context_activity, ArrayList<String> context_wifi_name, ArrayList<String> noise,
                     ArrayList<String> context_playback_device, ArrayList<String> context_app,
                     ArrayList<String> context_network, ArrayList<String> message_sender, ArrayList<String> message_source_app,
                     ArrayList<String> message_title, ArrayList<String> message_content, ArrayList<String> message_type) {
        this.context_time = context_time;
        this.context_week = context_week;
        this.context_gps_position = context_gps_position;
        this.context_activity = context_activity;
        this.context_wifi_name = context_wifi_name;
        this.context_environment_sound = noise;
        this.context_playback_device = context_playback_device;
        this.context_app = context_app;
        this.context_network = context_network;
        this.message_sender = message_sender;
        this.message_source_app = message_source_app;
        this.message_title = message_title;
        this.message_content = message_content;
        this.message_type = message_type;
    }

    // Check if the condition is satisfied given a VolumeContext
    public boolean isSatisfied(VolumeContext context) {
        // Check context_time
        boolean timeCheck = true;
        if (!context_time.isEmpty()) {
            timeCheck = context_time.contains(context.context_time);
        }

        // Check context_week
        boolean weekCheck = true;
        if (!context_week.isEmpty()) {
            weekCheck = context_week.contains(context.context_day_of_week);
        }

        // Check context_gps_position
        boolean gpsCheck = true;
        if (!context_gps_position.isEmpty()) {
            gpsCheck = context_gps_position.contains(context.context_gps_position);
        }

        // Check context_activity
        boolean activityCheck = true;
        if (!context_activity.isEmpty()) {
            activityCheck = context_activity.contains(context.context_activity);
        }

        // Check context_wifi_name
        boolean wifiCheck = true;
        if (!context_wifi_name.isEmpty()) {
            wifiCheck = context_wifi_name.contains(context.context_wifi_name);
        }

        // Check noise_upperbound and noise_lowerbound
        boolean noiseCheck = true;
        if (!context_environment_sound.isEmpty()) {
            noiseCheck = context_environment_sound.contains(context.context_noise_level);
        }

        // Check context_playback_device
        boolean playbackCheck = true;
        if (!context_playback_device.isEmpty()) {
            playbackCheck = context_playback_device.contains(context.context_audio_device);
        }

        // Check context_app
        boolean appCheck = true;
        if (!context_app.isEmpty()) {
            appCheck = context_app.contains(context.context_app);
        }

        // Check context_network
        boolean networkCheck = true;
        if (!context_network.isEmpty()) {
            networkCheck = context_network.contains(context.context_network);
        }

        // Check if message_sender is in the list of senders
        boolean senderCheck = true;
        if (!message_sender.isEmpty()) {
            senderCheck = message_sender.contains(context.message_sender);
        }

        // Check if message_source_app is in the list of source apps
        boolean sourceAppCheck = true;
        if (!message_source_app.isEmpty()) {
            sourceAppCheck = message_source_app.contains(context.message_source_app);
        }

        // Check if message_title is in the list of titles
        boolean titleCheck = true;
        if (!message_title.isEmpty()) {
            titleCheck = message_title.contains(context.message_title);
        }

        // Check if message_content is in the list of contents
        boolean contentCheck = true;
        if (!message_content.isEmpty()) {
            contentCheck = message_content.contains(context.message_content);
        }

        // Check if message_type is in the list of types
        boolean typeCheck = true;
        if (!message_type.isEmpty()) {
            typeCheck = message_type.contains(context.message_type);
        }

        // Return true if all conditions are satisfied
        return timeCheck && weekCheck && gpsCheck && activityCheck && wifiCheck && noiseCheck &&
                playbackCheck && appCheck && networkCheck && senderCheck && sourceAppCheck &&
                titleCheck && contentCheck && typeCheck;
    }
}



