package com.hcifuture.contextactionlibrary.volume;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class SoundManager extends TriggerManager {

    private final double NORMALIZED_MAX_VOLUME = 100.0;

    private final AudioManager audioManager;

    private final MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;

    private final Context mContext;
    private final int MAX_VOLUME_MUSIC;

    public SoundManager(VolEventListener volEventListener, Context context) {
        super(volEventListener);
        mContext = context;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        MAX_VOLUME_MUSIC = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mediaProjectionManager = (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
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

    public int percent2int(double vol_percent) {
        return (int)(vol_percent * MAX_VOLUME_MUSIC / NORMALIZED_MAX_VOLUME + 0.5d);
    }

    public double int2percent(int vol) {
        return vol * NORMALIZED_MAX_VOLUME / MAX_VOLUME_MUSIC;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    public void startAudioCapture(int resultCode, Intent data) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection != null) {
            // TODO
            AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration =
                    new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .build();

            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_AAC_LC)
                    .setSampleRate(8000)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build();

            audioRecord = new AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration)
                    .build();

            audioRecord.startRecording();
        }
    }

    public void stopAudioCapture() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
