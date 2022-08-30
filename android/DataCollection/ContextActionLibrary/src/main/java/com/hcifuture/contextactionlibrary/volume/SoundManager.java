package com.hcifuture.contextactionlibrary.volume;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.RequiresApi;

public class SoundManager extends TriggerManager {

    static final String TAG = "SoundManager";

    private final double NORMALIZED_MAX_VOLUME = 100.0;

    private final AudioManager audioManager;

    private final MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private ScheduledFuture<?> recordingFt;

    private boolean hasCapturePermission = false;
    private int resultCode;
    private Intent data;
    private AtomicBoolean isCollecting = new AtomicBoolean(false);
    private AtomicBoolean audioCaptureThreadOn = new AtomicBoolean(false);
    private final int SAMPLE_RATE = 44100;
    private final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
    private final String mPcmFilePath;

    private final Context mContext;
    private final ScheduledExecutorService scheduledExecutorService;
    private final List<ScheduledFuture<?>> futureList;

    private final int MAX_VOLUME_MUSIC;

    public SoundManager(VolEventListener volEventListener, Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList) {
        super(volEventListener);
        mContext = context;
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;

        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        MAX_VOLUME_MUSIC = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        mediaProjectionManager = (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mPcmFilePath = mContext.getExternalMediaDirs()[0].getAbsolutePath() + "/tmp/system_audio.pcm";
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

    public void saveAudioCaptureToken(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            this.resultCode = resultCode;
            this.data = data;
            hasCapturePermission = true;
        }
    }

    public boolean hasCapturePermission() {
        return hasCapturePermission;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    public boolean startAudioCapture(long milliseconds) {
        if (isCollecting.compareAndSet(false, true)) {
            if (hasCapturePermission) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {
                    hasCapturePermission = false;
                    isCollecting.set(false);
                    Log.e(TAG, "startAudioCapture: start fail because no permission null mediaProject");
                    return false;
                } else {
                    AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration =
                            new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                                    .build();

                    AudioFormat audioFormat = new AudioFormat.Builder()
                            .setEncoding(ENCODING)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_MASK)
                            .build();

                    audioRecord = new AudioRecord.Builder()
                            .setAudioFormat(audioFormat)
                            .setAudioPlaybackCaptureConfig(audioPlaybackCaptureConfiguration)
                            .build();

                    audioRecord.startRecording();

                    startLoopToSaveAudioFile(mPcmFilePath);

                    // stop after certain duration
                    futureList.add(scheduledExecutorService.schedule(() -> {
                        stopAudioCapture();
                    }, milliseconds, TimeUnit.MILLISECONDS));

                    Log.e(TAG, "startAudioCapture: start success");
                    return true;
                }
            } else {
                Log.e(TAG, "startAudioCapture: start fail because no permission 2");
                isCollecting.set(false);
                return false;
            }
        } else {
            Log.e(TAG, "startAudioCapture: start fail because concurrent audio capture");
            return false;
        }
    }

    private void startLoopToSaveAudioFile(String mPcmFilePath) {
        audioCaptureThreadOn.set(true);
        futureList.add(recordingFt = scheduledExecutorService.schedule(() -> {
            FileOutputStream fos = null;
            try {
                Log.i(TAG, "文件地址: " + mPcmFilePath);
                fos = new FileOutputStream(mPcmFilePath);
                byte[] bytes = new byte[BUFFER_SIZE];

                while (audioCaptureThreadOn.get()) {
                    audioRecord.read(bytes, 0, bytes.length);
                    fos.write(bytes, 0, bytes.length);
                    fos.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Log.i(TAG, "停止录制");
                if (fos != null) {
                    try {
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, TimeUnit.MILLISECONDS));
    }

    public void stopAudioCapture() {
        if (recordingFt != null) {
            // do not use cancel to interrupt, may break audioRecord
//            recordingFt.cancel(true);
            audioCaptureThreadOn.set(false);
            while (!recordingFt.isDone()) {}
            recordingFt = null;
        }
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        isCollecting.set(false);
        Log.e(TAG, "stopAudioCapture: stop capture");
    }
}
