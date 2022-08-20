package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.media.AudioManager;

public class SoundManager extends TriggerManager {

    private double NORMALIZED_MAX_VOLUME = 100.0;

    private AudioManager audioManager;
    private Context mContext;
    private int VOLUME_MAX;

    public SoundManager(VolEventListener volEventListener, Context context) {
        super(volEventListener);
        mContext = context;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        VOLUME_MAX = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public boolean isAudioOn() {
        return audioManager.isMusicActive();
    }

    public int getAudioMode() {
        // Value is MODE_NORMAL, MODE_RINGTONE, MODE_IN_CALL, MODE_IN_COMMUNICATION, MODE_CALL_SCREENING,
        // MODE_CALL_REDIRECT, or MODE_COMMUNICATION_REDIRECT
        return audioManager.getMode();
    }

    public int getVolume() {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public double getNormalizedVolume() {
        return getVolume() * NORMALIZED_MAX_VOLUME / VOLUME_MAX;
    }
}
