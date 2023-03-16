package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.media.AudioManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VolumeDetector {
    private AudioManager audioManager;
    private List<Integer> last_volumes;
    private Integer maxVolume;

    public VolumeDetector(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public List<String> getVolumeBehaviors() {
        if (last_volumes == null) {
            last_volumes = getVolumes();
            return new ArrayList<>();
        }

        List<Integer> current_volumes = getVolumes();
        List<String> result = new ArrayList<>();
        if (!current_volumes.get(0).equals(last_volumes.get(0)))
            result.add("adjust media volume from " + last_volumes.get(0) + " to " + current_volumes.get(0));
        if (!current_volumes.get(1).equals(last_volumes.get(1)))
            result.add("adjust notification volume from " + last_volumes.get(1) + " to " + current_volumes.get(1));
        if (!current_volumes.get(2).equals(last_volumes.get(2)))
            result.add("adjust alarm volume from " + last_volumes.get(2) + " to " + current_volumes.get(2));
        if (!current_volumes.get(3).equals(last_volumes.get(3)))
            result.add("adjust call volume from " + last_volumes.get(3) + " to " + current_volumes.get(3));
        return result;
    }

    public List<Integer> getVolumes() {
        Integer media = (int)(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume);
        Integer notification = (int)(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) / maxVolume);
        Integer alarm = (int)(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_ALARM) / maxVolume);
        Integer call = (int)(15.0 * audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) / maxVolume);
        return new ArrayList<>(Arrays.asList(media, notification, alarm, call));
    }
}
