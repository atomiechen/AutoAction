package com.hcifuture.contextactionlibrary.sensor.collector.async;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.sensor.collector.CollectorException;
import com.hcifuture.contextactionlibrary.sensor.collector.CollectorManager;
import com.hcifuture.contextactionlibrary.sensor.collector.CollectorResult;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.contextactionlibrary.status.Heart;
import com.hcifuture.contextactionlibrary.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioCollector extends AsynchronousCollector {
    private static final String TAG = "AudioCollector";

    private MediaRecorder mMediaRecorder;
    private final AtomicBoolean isCollecting;
    private final AudioManager audioManager;
    private List<Double> noiseCheckpoints;
    public double lastest_noise;

    private ScheduledFuture<?> repeatedSampleFt;

    /*
      Error code:
        0: No error
        1: Invalid audio length
        2: Null audio filename
        3: Concurrent task of audio recording
        4: Unknown audio recording exception
        5: Unknown exception when stopping recording
        6: Mic not available
     */

    public AudioCollector(Context context, CollectorManager.CollectorType type, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList) {
        super(context, type, scheduledExecutorService, futureList);
        isCollecting = new AtomicBoolean(false);
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        noiseCheckpoints = new ArrayList<>();
        lastest_noise = 0.0;
    }

    @Override
    public void initialize() {

    }

    @Override
    public void close() {
        stopRecording();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public CompletableFuture<CollectorResult> getData(TriggerConfig config) {
        Heart.getInstance().newSensorGetEvent(getName(), System.currentTimeMillis());
        CompletableFuture<CollectorResult> ft = new CompletableFuture<>();
        CollectorResult result = new CollectorResult();
        File saveFile = new File(config.getAudioFilename());
        result.setSavePath(saveFile.getAbsolutePath());

        if (config.getAudioLength() <= 0) {
            ft.completeExceptionally(new CollectorException(1, "Invalid audio length: " + config.getAudioLength()));
        } else if (config.getAudioFilename() == null) {
            ft.completeExceptionally(new CollectorException(2, "Null audio filename"));
        } else if (isCollecting.compareAndSet(false, true)) {
            try {
                // check mic availability
                // ref: https://stackoverflow.com/a/67458025/11854304
//                MODE_NORMAL -> You good to go. Mic not in use
//                MODE_RINGTONE -> Incoming call. The phone is ringing
//                MODE_IN_CALL -> A phone call is in progress
//                MODE_IN_COMMUNICATION -> The Mic is being used by another application
                int micMode = audioManager.getMode();
                if (micMode != AudioManager.MODE_NORMAL) {
                    ft.completeExceptionally(new CollectorException(6, "Mic not available: " + micMode));
                    isCollecting.set(false);
                } else {
                    FileUtils.makeDir(saveFile.getParent());
                    startRecording(saveFile);
                    futureList.add(scheduledExecutorService.schedule(() -> {
                        try {
                            stopRecording();
                            ft.complete(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                            ft.completeExceptionally(new CollectorException(5, e));
                        } finally {
//                            ft.complete(result);
                            isCollecting.set(false);
                        }
                    }, config.getAudioLength(), TimeUnit.MILLISECONDS));
                }
            } catch (Exception e) {
                e.printStackTrace();
                stopRecording();
                ft.completeExceptionally(new CollectorException(4, e));
                isCollecting.set(false);
            }
        } else {
            ft.completeExceptionally(new CollectorException(3, "Concurrent task of audio recording"));
        }

        return ft;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startRecording(File file) throws IOException {
        mMediaRecorder = new MediaRecorder();
        // may throw IllegalStateException due to lack of permission
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setAudioChannels(2);
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setAudioEncodingBitRate(16 * 44100);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOutputFile(file);
        mMediaRecorder.prepare();
        mMediaRecorder.start();
        noiseCheckpoints = new ArrayList<>();
        updateNoise();
    }

    private void updateNoise() {
        double BASE = 1.0;
        int SPACE = 100;
        if (mMediaRecorder != null) {
            double ratio = mMediaRecorder.getMaxAmplitude() / BASE;
            double db = 0;// 分贝
            if (ratio > 1)
                db = 20 * Math.log10(ratio);
            if (noiseCheckpoints != null)
                noiseCheckpoints.add(db);
            futureList.add(scheduledExecutorService.schedule(() -> {
                try {
                    updateNoise();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, SPACE, TimeUnit.MILLISECONDS));
        }
    }

    private void stopRecording() {
        if (mMediaRecorder != null) {
            try {
                // may throw IllegalStateException because no valid audio data has been received
                mMediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mMediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMediaRecorder = null;
            if (noiseCheckpoints != null) {
                double count = 0;
                for (Double noiseCheckpoint: noiseCheckpoints) {
                    count += noiseCheckpoint;
                }
                lastest_noise = count / noiseCheckpoints.size();
            }
        }
    }

    @Override
    public void pause() {
        stopRecording();
    }

    @Override
    public void resume() {

    }

    @Override
    public String getName() {
        return "Audio";
    }

    @Override
    public String getExt() {
        return ".mp3";
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CompletableFuture<List<Double>> detectNoiseLevel(long period, int number) {
        CompletableFuture<List<Double>> ft = new CompletableFuture<>();
        if (isCollecting.compareAndSet(false, true)) {
            try {
                // check mic availability
                // ref: https://stackoverflow.com/a/67458025/11854304
//                MODE_NORMAL -> You good to go. Mic not in use
//                MODE_RINGTONE -> Incoming call. The phone is ringing
//                MODE_IN_CALL -> A phone call is in progress
//                MODE_IN_COMMUNICATION -> The Mic is being used by another application
                int micMode = audioManager.getMode();
                if (micMode != AudioManager.MODE_NORMAL) {
                    ft.completeExceptionally(new CollectorException(6, "Mic not available: " + micMode));
                    isCollecting.set(false);
                } else {
                    try {
                        mMediaRecorder = new MediaRecorder();
                        // may throw IllegalStateException due to lack of permission
                        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mMediaRecorder.setAudioChannels(2);
                        mMediaRecorder.setAudioSamplingRate(44100);
                        mMediaRecorder.setAudioEncodingBitRate(16 * 44100);
                        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mMediaRecorder.setOutputFile("/dev/null");
                        mMediaRecorder.prepare();
                        mMediaRecorder.start();
                        Log.e(TAG, String.format("detectNoiseLevel: MediaRecorder started, period: %dms number: %d", period, number));

                        // first call returns 0
                        mMediaRecorder.getMaxAmplitude();

                        List<Double> sampledNoise = new ArrayList<>();
                        repeatedSampleFt = scheduledExecutorService.scheduleAtFixedRate(() -> {
                            try {
                                double BASE = 1.0;
                                // Returns the maximum absolute amplitude that was sampled since the last call to this method
                                int maxAmplitude = mMediaRecorder.getMaxAmplitude();
                                double ratio = maxAmplitude / BASE;
                                double db = 0;// 分贝
                                if (ratio > 1)
                                    db = 20 * Math.log10(ratio);
                                Log.e(TAG, "detectNoiseLevel: " + String.format("maxAmplitude: %d ratio: %f db: %f", maxAmplitude, ratio, db));

                                lastest_noise = db;
                                sampledNoise.add(db);
                                if (sampledNoise.size() == number) {
                                    try {
                                        // may throw IllegalStateException because no valid audio data has been received
                                        mMediaRecorder.stop();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        mMediaRecorder.release();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    mMediaRecorder = null;
                                    ft.complete(sampledNoise);
                                    repeatedSampleFt.cancel(false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                ft.completeExceptionally(new CollectorException(5, e));
                            } finally {
                                isCollecting.set(false);
                            }
                        }, period, period, TimeUnit.MILLISECONDS);
                        futureList.add(repeatedSampleFt);

                    } catch (Exception e) {
                        ft.completeExceptionally(new CollectorException(7, e));
                        isCollecting.set(false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                stopRecording();
                ft.completeExceptionally(new CollectorException(4, e));
                isCollecting.set(false);
            }
        } else {
            ft.completeExceptionally(new CollectorException(3, "Concurrent task of audio recording"));
        }

        return ft;
    }
}
