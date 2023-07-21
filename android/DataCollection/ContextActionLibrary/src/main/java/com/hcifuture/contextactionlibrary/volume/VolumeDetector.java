package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class VolumeDetector {
    private AudioManager audioManager;
    private int maxVolume;

    public VolumeDetector(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public HashMap<String, Integer> getVolumes() {
//        int media = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume);
//        int notification = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) / maxVolume);
//        int alarm = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_ALARM) / maxVolume);
//        int call = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) / maxVolume);
//        return String.format("{'media': %d, 'notification': %d, 'alarm': %d, 'call': %d}", media, notification, alarm, call);
        HashMap<String, Integer> volume_settings = new HashMap<>();
        volume_settings.put("media", audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volume_settings.put("notification", audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
        volume_settings.put("alarm", audioManager.getStreamVolume(AudioManager.STREAM_ALARM));
        volume_settings.put("voice_call", audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
        volume_settings.put("ring", audioManager.getStreamVolume(AudioManager.STREAM_RING));
        volume_settings.put("max_media", audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume_settings.put("max_notification", audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));
        volume_settings.put("max_alarm", audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM));
        volume_settings.put("max_voice_call", audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
        volume_settings.put("max_ring", audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
        return volume_settings;
    }

    public int getMaxVolume() {
        return maxVolume;
    }

    public String getStreamTypeByMode() {
        switch (audioManager.getMode()) {
            case AudioManager.MODE_CALL_SCREENING:
            case AudioManager.MODE_RINGTONE:
                return "ring";
//                return AudioManager.STREAM_RING;
            case AudioManager.MODE_IN_CALL:
            case AudioManager.MODE_IN_COMMUNICATION:
                return "voice_call";
//                return AudioManager.STREAM_VOICE_CALL;
            case AudioManager.MODE_NORMAL:
            default:
                return "media";
//                return AudioManager.STREAM_MUSIC;
        }
    }
}
