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
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.utils.FileUtils;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import org.json.JSONObject;

public class SoundManager extends TriggerManager {

    static final String TAG = "SoundManager";

    private final double NORMALIZED_MAX_VOLUME = 100.0;
    private final String AUDIO_DIR;

    private final AudioManager audioManager;

    private final MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private ScheduledFuture<?> recordingFt;
    private ScheduledFuture<?> countdownStopFt;

    private final AtomicInteger mFileIDCounter = new AtomicInteger(0);
    private String mCurrentFilename;
    private int mCurrentFileID;
    private final int intervalFile = 30000; // save aac file every 30s
    private final int intervalDetection = 2000; // detect db every 2s

    private boolean hasCapturePermission = false;
    private int resultCode;
    private Intent data;
    private final AtomicBoolean isCollecting = new AtomicBoolean(false);
    private final AtomicBoolean audioCaptureThreadOn = new AtomicBoolean(false);
    private final int SAMPLE_RATE = 44100;
    private final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_STEREO;
    private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
    private final String mPcmFilePath;
    public static double SYSTEM_VOLUME;
    private final AacEncoder aacEncoder;

    private final Context mContext;
    private final ScheduledExecutorService scheduledExecutorService;
    private final List<ScheduledFuture<?>> futureList;

    private final int MAX_VOLUME_MUSIC;

    public static Integer latest_audioLevel;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public SoundManager(VolEventListener volEventListener, Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList) {
        super(volEventListener);
        mContext = context;
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        // 放在Data/Click/下，Uploader会监听文件夹、自动重传
        AUDIO_DIR = context.getExternalMediaDirs()[0].getAbsolutePath() + "/Data/Click/SystemAudio/";

        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        MAX_VOLUME_MUSIC = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        mediaProjectionManager = (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mPcmFilePath = mContext.getExternalMediaDirs()[0].getAbsolutePath() + "/tmp/system_audio.pcm";
        aacEncoder = new AacEncoder();
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

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

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
        futureList.add(recordingFt = scheduledExecutorService.schedule(() -> {
            FileOutputStream fos = null;
            double loudness_sum = 0;
            int sum_cnt = 0;
            double db_sum = 0;
            int db_cnt = 0;
            final double BASE = 1.0; // 32768
            try {
//                Log.i(TAG, "文件地址: " + mPcmFilePath);
//                fos = new FileOutputStream(mPcmFilePath);
                byte[] bytes = new byte[BUFFER_SIZE];

                long last_time = System.currentTimeMillis();
                long last_file_time = 0;
                int maxValInFile = 0;
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
                            db_sum += 20 * Math.log10(Math.abs(val) / BASE);
                            db_cnt++;
                        }
                        if (val > maxValInFile) {
                            maxValInFile = val;
                        }
                    }
                    long cur_time = System.currentTimeMillis();
                    if (cur_time - last_file_time >= intervalFile) {
                        if (fos != null) {
                            fos.close();
                            if (maxValInFile == 0) {
                                // no data in file, no need to upload
                                FileUtils.deleteFile(new File(mCurrentFilename), "");
                                // reset file ID
                                mFileIDCounter.getAndDecrement();
                            } else {
                                // upload current file
                                volEventListener.upload(mCurrentFilename, last_file_time, cur_time, "Volume_SystemAudio", "");
                            }
                            maxValInFile = 0;
                        }
                        // create a new file
                        String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        // update file ID
                        mCurrentFileID = mFileIDCounter.getAndIncrement();
                        mCurrentFilename = AUDIO_DIR + "SystemAudio_" + dateTime + "_" + mCurrentFileID + ".aac";
                        FileUtils.makeFile(new File(mCurrentFilename));
                        fos = new FileOutputStream(mCurrentFilename);
                        last_file_time = cur_time;
                    }
                    if (fos != null)
                        fos.write(aacEncoder.offerEncoder(bytes));
                    if (cur_time - last_time >= intervalDetection) {
                        // （未使用）RMS dBFS，均方根计算dBFS
                        double rms = Math.sqrt(loudness_sum / sum_cnt);
                        double newDB_rms = Math.max(0, 20 * Math.log10(rms / BASE));
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

    public static class AacEncoder {

        private MediaCodec mediaCodec;
        private String mediaType = "OMX.google.aac.encoder";

        ByteBuffer[] inputBuffers = null;
        ByteBuffer[] outputBuffers = null;
        MediaCodec.BufferInfo bufferInfo;

        //pts时间基数
        long presentationTimeUs = 0;

        //创建一个输入流用来输出转换的数据
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        public AacEncoder() {

            try {
                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                //mediaCodec = MediaCodec.createByCodecName(mediaType);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
            final int kSampleRates[] = {8000, 11025, 22050, 44100, 48000};
            //比特率 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
            final int kBitRates[] = {64000, 96000, 128000};

            //初始化   此格式使用的音频编码技术、音频采样率、使用此格式的音频信道数（单声道为 1，立体声为 2）
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC, kSampleRates[3], 2);

            mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //比特率 声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[1]);

            //传入的数据大小
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024);// It will
            //设置相关参数
            mediaCodec.configure(mediaFormat, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            //开始
            mediaCodec.start();

            inputBuffers = mediaCodec.getInputBuffers();
            outputBuffers = mediaCodec.getOutputBuffers();
            bufferInfo = new MediaCodec.BufferInfo();
        }

        public void close() {
            try {
                mediaCodec.stop();
                mediaCodec.release();
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public byte[] offerEncoder(byte[] input) throws Exception {
//            Log.e("offerEncoder", input.length + " is coming");

            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);//其中需要注意的有dequeueInputBuffer（-1），参数表示需要得到的毫秒数，-1表示一直等，0表示不需要等，传0的话程序不会等待，但是有可能会丢帧。
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                inputBuffer.limit(input.length);

                //计算pts
                long pts = computePresentationTime(presentationTimeUs);

                mediaCodec
                        .queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                presentationTimeUs += 1;
            }


            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            while (outputBufferIndex >= 0) {
                int outBitsSize = bufferInfo.size;
                int outPacketSize = outBitsSize + 7; // 7 is ADTS size
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + outBitsSize);

                //添加ADTS头
                byte[] outData = new byte[outPacketSize];
                addADTStoPacket(outData, outPacketSize);

                outputBuffer.get(outData, 7, outBitsSize);
                outputBuffer.position(bufferInfo.offset);

                //写到输出流里
                outputStream.write(outData);

                // Log.e("AudioEncoder", outData.length + " bytes written");

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }

            //输出流的数据转成byte[]
            byte[] out = outputStream.toByteArray();

            //写完以后重置输出流，否则数据会重复
            outputStream.flush();
            outputStream.reset();

            //返回
            return out;
        }

        /**
         * 给编码出的aac裸流添加adts头字段
         *
         * @param packet    要空出前7个字节，否则会搞乱数据
         * @param packetLen
         */
        private void addADTStoPacket(byte[] packet, int packetLen) {
            int profile = 2;  //AAC LC
            int freqIdx = 4;  //44.1KHz
            int chanCfg = 2;  //CPE
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
        }


        //计算PTS，实际上这个pts对应音频来说作用并不大，设置成0也是没有问题的
        private long computePresentationTime(long frameIndex) {
            return frameIndex * 90000 * 1024 / 44100;
        }
    }

}
