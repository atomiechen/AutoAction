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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.utils.FileUtils;
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
        Log.e(TAG, "start capture");
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

    public int getVolume() {
        return getVolume(AudioManager.STREAM_MUSIC);
    }

    public int getVolume(int streamType) {
        return audioManager.getStreamVolume(streamType);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startLoopToSaveAudioFile(String mPcmFilePath) {
        audioCaptureThreadOn.set(true);
        int interval = 2000; // detect every 2s
        int cycle_per_file = 80;
        futureList.add(recordingFt = scheduledExecutorService.schedule(() -> {
            FileOutputStream fos = null;
            double loudness_sum = 0;
            int sum_cnt = 0;
            double db_sum = 0;
            int db_cnt = 0;
            try {
//                Log.i(TAG, "文件地址: " + mPcmFilePath);
//                fos = new FileOutputStream(mPcmFilePath);
                byte[] bytes = new byte[BUFFER_SIZE];

                long last_time = System.currentTimeMillis();
                int count = 0;
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
                        loudness_sum += val * val;
                        sum_cnt++;
                        if (val != 0) {
                            db_sum += 20 * Math.log10(Math.abs(val));
                            db_cnt++;
                        }
                    }
                    long cur_time = System.currentTimeMillis();
                    if (count == 0) {
                        if (fos != null) {
                            fos.close();
                        }
                        FileUtils.writeStringToFile("", new File(ConfigContext.VOLUME_SAVE_FOLDER + "System Audio/" + "SystemAudio.mp3"));
                        fos = new FileOutputStream(ConfigContext.VOLUME_SAVE_FOLDER + "System Audio/" + "SystemAudio.mp3");
                        writeMP3Header(fos, bytes.length * cycle_per_file);
                        count = cycle_per_file;
                    }
                    if (fos != null) {
                        fos.write(bytes, 0, bytes.length);
                        count--;
                    }
                    if (cur_time - last_time >= interval) {
                        // （未使用）RMS dBFS，均方根计算dBFS
                        double rms = Math.sqrt(loudness_sum / sum_cnt);
                        double newDB_rms = Math.max(0, 20 * Math.log10(rms));
                        // （使用）直接对db值进行平均
                        double newDB = 0;
                        if (db_cnt != 0) {
                            newDB = db_sum / db_cnt;
                        }
                        double diff = newDB - SYSTEM_VOLUME;
                        if (diff != 0.0) {
                            JSONObject json = new JSONObject();
                            JSONUtils.jsonPut(json, "audio_db", newDB);
                            JSONUtils.jsonPut(json, "old_audio_db", SYSTEM_VOLUME);
                            JSONUtils.jsonPut(json, "diff", diff);
                            JSONUtils.jsonPut(json, "musicVolume", getVolume());
                            JSONUtils.jsonPut(json, "musicVolumeMax", MAX_VOLUME_MUSIC);
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
                        db_sum = 0;
                        db_cnt = 0;
                        last_time = cur_time;
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
            try {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            } catch (Exception ignored) {}
        }
        if (mediaProjection != null) {
            try {
                mediaProjection.stop();
                mediaProjection = null;
            } catch (Exception ignored) {}
        }
        isCollecting.set(false);
        Log.e(TAG, "stopAudioCapture: stop capture");
    }

    public void writeMP3Header(FileOutputStream fos, int length) throws IOException {
//        Log.e(TAG, "write mp3 header");
        //计算长度
        int PCMSize = length;

        //填入参数，比特率等等。这里用的是16位单声道 8000 hz
        WaveHeader header = new WaveHeader();
        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = PCMSize + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 2;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 44100;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = PCMSize;

        byte[] h = header.getHeader();

        assert h.length == 44; //WAV标准，头部应该是44字节
        //write header
        fos.write(h, 0, h.length);
//        //write data stream
//        fos.write(bytes, 0, byteLength);
//        fos.close();
    }

    public static class WaveHeader {

        public final char fileID[] = {'R', 'I', 'F', 'F'};
        public int fileLength;
        public char wavTag[] = {'W', 'A', 'V', 'E'};;
        public char FmtHdrID[] = {'f', 'm', 't', ' '};
        public int FmtHdrLeth;
        public short FormatTag;
        public short Channels;
        public int SamplesPerSec;
        public int AvgBytesPerSec;
        public short BlockAlign;
        public short BitsPerSample;
        public char DataHdrID[] = {'d','a','t','a'};
        public int DataHdrLeth;

        public byte[] getHeader() throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            WriteChar(bos, fileID);
            WriteInt(bos, fileLength);
            WriteChar(bos, wavTag);
            WriteChar(bos, FmtHdrID);
            WriteInt(bos,FmtHdrLeth);
            WriteShort(bos,FormatTag);
            WriteShort(bos,Channels);
            WriteInt(bos,SamplesPerSec);
            WriteInt(bos,AvgBytesPerSec);
            WriteShort(bos,BlockAlign);
            WriteShort(bos,BitsPerSample);
            WriteChar(bos,DataHdrID);
            WriteInt(bos,DataHdrLeth);
            bos.flush();
            byte[] r = bos.toByteArray();
            bos.close();
            return r;
        }

        private void WriteShort(ByteArrayOutputStream bos, int s) throws IOException {
            byte[] mybyte = new byte[2];
            mybyte[1] =(byte)( (s << 16) >> 24 );
            mybyte[0] =(byte)( (s << 24) >> 24 );
            bos.write(mybyte);
        }


        private void WriteInt(ByteArrayOutputStream bos, int n) throws IOException {
            byte[] buf = new byte[4];
            buf[3] =(byte)( n >> 24 );
            buf[2] =(byte)( (n << 8) >> 24 );
            buf[1] =(byte)( (n << 16) >> 24 );
            buf[0] =(byte)( (n << 24) >> 24 );
            bos.write(buf);
        }

        private void WriteChar(ByteArrayOutputStream bos, char[] id) {
            for (int i=0; i<id.length; i++) {
                char c = id[i];
                bos.write(c);
            }
        }
    }
}
