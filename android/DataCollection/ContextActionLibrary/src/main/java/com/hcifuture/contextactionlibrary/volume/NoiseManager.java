package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.CollectorException;
import com.hcifuture.contextactionlibrary.sensor.collector.async.AudioCollector;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NoiseManager extends TriggerManager {

    private static final String TAG = "NoiseManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private AudioCollector audioCollector;

    private ScheduledFuture<?> scheduledNoiseDetection;
    private ScheduledFuture<?> scheduledSaveFile;
    private final long initialDelay = 5000;
    private final long samplePeriod = 10;  // sample max amplitude every 10ms
    private final int intervalDetection = 2000; // detect db every 2s

    private double lastNoise = 0;
    private double lastTriggerNoise = 0;
    private boolean hasFirstDetection = false;
    private long lastTimestamp = 0;
    private final double threshold = 20;

    private final String FILE_DIR;
    private final AtomicInteger mFileIDCounter = new AtomicInteger(0);
    private String mCurrentFilename;
    private int mCurrentFileID;
    private final int intervalFile = 30000; // save aac file every 30s

    public NoiseManager(VolEventListener volEventListener, Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, AudioCollector audioCollector) {
        super(volEventListener);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.audioCollector = audioCollector;

        // 放在Data/Click/下，Uploader会监听文件夹、自动重传
        FILE_DIR = context.getExternalMediaDirs()[0].getAbsolutePath() + "/Data/Click/MicAudio/";
    }

    @Override
    public void start() {
        // periodically save audio files
        Log.e(TAG, "schedule periodic noise detection");
        if (scheduledSaveFile == null) {
            scheduledSaveFile = scheduledExecutorService.schedule(() -> {

                if (scheduledNoiseDetection == null) {
                    // periodically sample max amplitude values every "samplePeriod"
                    // and detect noise every "intervalDetection"
                    List<Integer> sampledNoise_mic = new ArrayList<>();
                    AtomicLong start_time = new AtomicLong(System.currentTimeMillis());
                    scheduledNoiseDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
                        long cur_time = System.currentTimeMillis();
                        int maxAmplitude = audioCollector.getMaxAmplitude();
                        if (maxAmplitude >= 0) {
                            sampledNoise_mic.add(maxAmplitude);
                        }
                        if (cur_time - start_time.get() >= intervalDetection) {
                            try {
                                double noise = getAvgNoiseFromSeq(sampledNoise_mic);
                                updateNoise(noise, true);
                            } catch (Exception e) {
                            } finally {
                                sampledNoise_mic.clear();
                                start_time.set(cur_time);
                            }
                        }
                    }, samplePeriod, samplePeriod, TimeUnit.MILLISECONDS);
                    futureList.add(scheduledNoiseDetection);
                }

                while (true) {
                    try {
                        mCurrentFileID = mFileIDCounter.get();
                        String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                        mCurrentFilename = FILE_DIR + "Mic_" + dateTime + "_" + mCurrentFileID + ".aac";
                        long start_file_time = System.currentTimeMillis();

                        Log.e(TAG, "start recording to " + mCurrentFilename);

                        // record audio to file
                        audioCollector.getData(new TriggerConfig()
                                .setAudioLength(intervalFile).setAudioFilename(mCurrentFilename)).get();

                        // upload current file
                        long cur_time = System.currentTimeMillis();
                        volEventListener.upload(mCurrentFilename, start_file_time, cur_time, "Volume_MicAudio", "");
                        // update file ID
                        mFileIDCounter.getAndIncrement();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        // wait 2s to try again
                        Thread.sleep(2000);
                    }
                }
            }, initialDelay, TimeUnit.MILLISECONDS);
            futureList.add(scheduledSaveFile);
        }
    }

    @Override
    public void stop() {
        if (scheduledNoiseDetection != null) {
            scheduledNoiseDetection.cancel(true);
            scheduledNoiseDetection = null;
        }
        if (scheduledSaveFile != null) {
            scheduledSaveFile.cancel(true);
            scheduledSaveFile = null;
        }
    }

    public String getNoiseLevel() {
        if (lastNoise <= 0)
            return "error";
        else if (lastNoise <= 35)
            return "quiet";
        else if (lastNoise <= 60)
            return "moderate";
        else
            return "noisy";
    }

    public CompletableFuture<Double> detectNoise(long length, long period) {
        return detectNoise(length, period, false);
    }

    // detect noise level using mic max amplitude values
    private CompletableFuture<Double> detectNoise(long length, long period, boolean checkEvent) {
        return getMaxAmplitudeSequence(length, period).thenApply(seq -> {
            double noise = getAvgNoiseFromSeq(seq);
            updateNoise(noise, checkEvent);
            return noise;
        });
    }

    private void updateNoise(double noise, boolean checkEvent) {
        Log.e(TAG, "noise level detected: " + noise);
        if (checkEvent && hasFirstDetection) {
            checkEvent(noise);
        }
        if (!hasFirstDetection) {
            lastTriggerNoise = noise;
            hasFirstDetection = true;
        }
        double diff = noise - getPresentNoise();
        if (diff != 0.0) {
            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "noise", noise);
            JSONUtils.jsonPut(json, "old_noise", getPresentNoise());
            JSONUtils.jsonPut(json, "diff", diff);
            volEventListener.recordEvent(VolEventListener.EventType.Noise, "periodic_detect", json.toString());
            setPresentNoise(noise);
        }
    }

    public void checkEvent(double noise) {
        if (Math.abs(noise - lastTriggerNoise) >= threshold) {
            // signal to adjust volume according to noise
            Bundle bundle = new Bundle();
            bundle.putDouble("noise", noise);
            bundle.putDouble("lastTriggerNoise", lastTriggerNoise);
            volEventListener.onVolEvent(VolEventListener.EventType.Noise, bundle);
            lastTriggerNoise = noise;
//            setPresentNoise(noise);
        }
    }

    public double getPresentNoise() {
        return lastNoise;
    }

    private void setPresentNoise(double noise) {
        lastNoise = noise;
        lastTimestamp = System.currentTimeMillis();
    }

    private CompletableFuture<List<Integer>> getMaxAmplitudeSequence(long length, long period) {
        CompletableFuture<List<Integer>> ft = new CompletableFuture<>();
        // periodically get max amplitude values
        List<Integer> sampledNoise_mic = new ArrayList<>();
        ScheduledFuture<?> repeatedSampleFt = scheduledExecutorService.scheduleAtFixedRate(() -> {
            int maxAmplitude = audioCollector.getMaxAmplitude();
            if (maxAmplitude >= 0) {
                sampledNoise_mic.add(maxAmplitude);
            }
        }, period, period, TimeUnit.MILLISECONDS);
        futureList.add(repeatedSampleFt);
        audioCollector.getData(new TriggerConfig().setAudioLength((int) length)).whenComplete((result, ex) -> {
            if (repeatedSampleFt != null) {
                repeatedSampleFt.cancel(true);
            }
            if (ex == null) {
                ft.complete(sampledNoise_mic);
            } else {
                ft.completeExceptionally(ex);
            }
        });
        return ft;
    }

    private double getAvgNoiseFromSeq(List<Integer> seq) throws CollectorException {
        double BASE = 1.0; // 32768
        double sum = 0.0;
        double sum_squared = 0.0;
        int count = 0;

        int idx = 0;
        double db;
        int next_idx;
        double next_db;
        int maxAmplitude = 0;
        int current_maxAmplitude;

        // 找到第一个非零值
        while (idx < seq.size() && (maxAmplitude = seq.get(idx)) == 0) {
            idx++;
        }
        if (idx >= seq.size()) {
            // 没有非零值
            throw new CollectorException(8, "No MaxAmplitude > 0");
        }
        db = 20 * Math.log10(maxAmplitude / BASE);
        current_maxAmplitude = maxAmplitude;
        next_idx = idx + 1;
//            Log.e(TAG, "getNoiseLevel: " + String.format("idx: %d maxAmplitude: %d db: %f", idx, maxAmplitude, db));
        // 采样为0时使用两边非零值线性插值
        while (true) {
            while (next_idx < seq.size() && (maxAmplitude = seq.get(next_idx)) == 0) {
                next_idx++;
            }
            if (next_idx >= seq.size()) {
                sum += db;
                sum_squared += current_maxAmplitude * current_maxAmplitude;
                count += 1;
                break;
            }
            next_db = 20 * Math.log10(maxAmplitude / BASE);
            sum += db + (db + next_db) * 0.5 * (next_idx - idx - 1);
            double interp = (current_maxAmplitude + maxAmplitude) * 0.5;
            sum_squared += current_maxAmplitude * current_maxAmplitude + interp * interp * (next_idx - idx - 1);
            count += next_idx - idx;

            idx = next_idx++;
            db = next_db;
            current_maxAmplitude = maxAmplitude;
        }

        double rms = Math.sqrt(sum_squared / count);
        double average_noise_rms = 20 * Math.log10(rms / BASE);
        double average_noise = (count > 0)? (sum / count) : 0.0;

        Log.e(TAG, String.format("getAvgNoiseFromSeq: %d sampled, average %fdb", count, average_noise));
        return average_noise;
    }
}
