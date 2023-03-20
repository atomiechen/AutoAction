package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VolumeDetector {
    private AudioManager audioManager;
    private int maxVolume;

    public VolumeDetector(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public String getVolumes() {
        int media = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume);
        int notification = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) / maxVolume);
        int alarm = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_ALARM) / maxVolume);
        int call = (int) Math.round(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) / maxVolume);
        return String.format("{'media': %d, 'notification': %d, 'alarm': %d, 'call': %d}", media, notification, alarm, call);
    }

    public int getMaxVolume() {
        return maxVolume;
    }
}
