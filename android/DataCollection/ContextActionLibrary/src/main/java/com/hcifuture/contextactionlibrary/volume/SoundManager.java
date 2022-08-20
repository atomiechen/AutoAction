package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.media.AudioManager;

public class SoundManager extends TriggerManager {

    private double NORMALIZED_MAX_VOLUME = 100.0;

    private AudioManager audioManager;
    private Context mContext;
    private int MAX_VOLUME_MUSIC;

    public SoundManager(VolEventListener volEventListener, Context context) {
        super(volEventListener);
        mContext = context;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        MAX_VOLUME_MUSIC = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
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
        return getVolume() * NORMALIZED_MAX_VOLUME / MAX_VOLUME_MUSIC;
    }

    public int percent2int(double vol_percent) {
        return (int)(vol_percent * MAX_VOLUME_MUSIC / 100 + 0.5d);
    }

    public double int2percent(int vol) {
        return vol * 100.0 / MAX_VOLUME_MUSIC;
    }

}
