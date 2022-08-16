package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.media.AudioManager;

public class SoundManager {
    private AudioManager audioManager;
    private Context mContext;

    public SoundManager(Context context) {
        mContext = context;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);;
    }

    public boolean isAudioOn() {
        return audioManager.isMusicActive();
    }

    public int getAudioMode() {
        // Value is MODE_NORMAL, MODE_RINGTONE, MODE_IN_CALL, MODE_IN_COMMUNICATION, MODE_CALL_SCREENING,
        // MODE_CALL_REDIRECT, or MODE_COMMUNICATION_REDIRECT
        return audioManager.getMode();
    }
}
