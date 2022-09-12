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
import android.os.Bundle;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import org.json.JSONObject;

public class SoundManager extends TriggerManager {

    static final String TAG = "SoundManager";

    private final double NORMALIZED_MAX_VOLUME = 100.0;

    private final AudioManager audioManager;

    private final MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private ScheduledFuture<?> recordingFt;
    private ScheduledFuture<?> countdownStopFt;

    private boolean hasCapturePermission = false;
    private int resultCode;
    private Intent data;
    private AtomicBoolean isCollecting = new AtomicBoolean(false);
    private AtomicBoolean audioCaptureThreadOn = new AtomicBoolean(false);
    private final int SAMPLE_RATE = 44100;
    private final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO;
    private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
    private final String mPcmFilePath;
    public static double SYSTEM_VOLUME;

    private final Context mContext;
    private final ScheduledExecutorService scheduledExecutorService;
    private final List<ScheduledFuture<?>> futureList;

    private final int MAX_VOLUME_MUSIC;

    public static Integer latest_audioLevel;

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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void start() {
        super.start();
        startAudioCapture(0);
    }

    @Override
    public void stop() {
        if (countdownStopFt != null) {
            countdownStopFt.cancel(true);
        }
        stopAudioCapture();
        super.stop();
    }

    public boolean isAudioOn() {
        return audioManager.isMusicActive();
    }

    public int getAudioMode() {
        // Value is MODE_NORMAL, MODE_RINGTONE, MODE_IN_CALL, MODE_IN_COMMUNICATION, MODE_CALL_SCREENING,
        // MODE_CALL_REDIRECT, or MODE_COMMUNICATION_REDIRECT
        return audioManager.getMode();
    }

    public Integer getAudioLevel(double db) {
        if (db <= 0)
            return 0;
        else if (db < 15)
            return 1;
        else if (db < 40)
            return 2;
        else
            return 3;
    }

    public double getVolume() {
        return ((double) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public static Integer volume2level(double volume) {
        if (volume == 0)
            return 0;
        return ((int) volume) / 20 + 1;
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
                    try {
                        AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration =
                                new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                                        .addMatchingUsage(AudioAttributes.USAGE_ALARM)
                                        .addMatchingUsage(AudioAttributes.USAGE_ASSISTANT)
                                        .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION)
                                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                                        .addMatchingUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                        .addMatchingUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                                        .addMatchingUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
//                                    .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
//                                    .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
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
//                        futureList.add(countdownStopFt = scheduledExecutorService.schedule(() -> {
//                            stopAudioCapture();
//                        }, milliseconds, TimeUnit.MILLISECONDS));

                        Log.e(TAG, "startAudioCapture: start success");
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "startAudioCapture: start fail because error happens");
                        e.printStackTrace();
                        if (countdownStopFt != null) {
                            countdownStopFt.cancel(true);
                        }
                        stopAudioCapture();
                        isCollecting.set(false);
                        return false;
                    }
                }
            } else {
                Log.e(TAG, "startAudioCapture: start fail because no permission");
                isCollecting.set(false);
                return false;
            }
        } else {
            Log.e(TAG, "startAudioCapture: start fail because concurrent audio capture");
            return false;
        }
    }

    public int getBytesAsWord(byte[] bytes, int start) {
        // 注意字节序：PCM是小尾端存储，但Java中的整数是大尾端存储
        return (short) ((bytes[start+1] & 0xff) << 8) | (bytes[start] & 0xff);
    }

    private void startLoopToSaveAudioFile(String mPcmFilePath) {
        audioCaptureThreadOn.set(true);
        int interval = 2000; // detect every 2s
        futureList.add(recordingFt = scheduledExecutorService.schedule(() -> {
            FileOutputStream fos = null;
            double loudness_sum = 0;
            int sum_cnt = 0;
            try {
//                Log.i(TAG, "文件地址: " + mPcmFilePath);
//                fos = new FileOutputStream(mPcmFilePath);
                byte[] bytes = new byte[BUFFER_SIZE];

                long last_time = System.currentTimeMillis();
                while (audioCaptureThreadOn.get()) {
                    // 这里是小尾端存储，2个字节为一次sample
                    int size = audioRecord.read(bytes, 0, bytes.length);
                    if (size != bytes.length) {
                        Log.e(TAG, "startLoopToSaveAudioFile: read fail " + size + " need " + bytes.length);
                        audioCaptureThreadOn.set(false);
                        break;
                    }
                    for (int i = 0; i < bytes.length; i += 2) {
                        int val = getBytesAsWord(bytes, i);
//                        loudness_cnt += Math.abs(val);
                        loudness_sum += val * val;
                        sum_cnt++;
                    }
                    long cur_time = System.currentTimeMillis();
                    if (cur_time - last_time >= interval) {
                        // RMS dBFS，均方根计算dBFS
                        double rms = Math.sqrt(loudness_sum / sum_cnt);
                        double newDB = Math.max(0, 20 * Math.log10(rms));
//                        double newDB = Math.max(0, 20 * Math.log10(loudness_sum / sum_cnt));
                        double diff = newDB - SYSTEM_VOLUME;
                        if (diff != 0.0) {
                            JSONObject json = new JSONObject();
                            JSONUtils.jsonPut(json, "audio_db", newDB);
                            JSONUtils.jsonPut(json, "old_audio_db", SYSTEM_VOLUME);
                            JSONUtils.jsonPut(json, "diff", diff);
                            volEventListener.recordEvent(VolEventListener.EventType.Audio, "system_audio_db", json.toString());
                            Log.e(TAG, "startLoopToSaveAudioFile: rms = " + rms);
                            Log.e(TAG, "startLoopToSaveAudioFile: audio db = " + newDB);

                            SYSTEM_VOLUME = newDB;
                            if (!Objects.equals(latest_audioLevel, getAudioLevel(SYSTEM_VOLUME))) {
                                latest_audioLevel = getAudioLevel(SYSTEM_VOLUME);
                                Bundle bundle = new Bundle();
                                bundle.putInt("AudioLevel", latest_audioLevel);
                                volEventListener.onVolEvent(VolEventListener.EventType.Audio, bundle);
                            }
                        }
                        loudness_sum = 0;
                        sum_cnt = 0;
                        last_time = cur_time;
                    }
                    if (fos != null) {
                        fos.write(bytes, 0, bytes.length);
                        fos.flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Log.i(TAG, "停止录制");
                try {
                    double rms = Math.sqrt(loudness_sum / sum_cnt);
                    SYSTEM_VOLUME = Math.max(0, 20 * Math.log10(rms));
                    latest_audioLevel = getAudioLevel(SYSTEM_VOLUME);
                    Log.e(TAG, "System Volume rms = " + rms);
                    Log.e(TAG, "System Volume: " + SYSTEM_VOLUME + "dB");
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, TimeUnit.MILLISECONDS));
    }

    synchronized public void stopAudioCapture() {
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
