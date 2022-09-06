package com.hcifuture.contextactionlibrary.volume;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.async.AudioCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NoiseManager extends TriggerManager {

    private static final String TAG = "NoiseManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private AudioCollector audioCollector;

    private ScheduledFuture<?> scheduledNoiseDetection;
    private long initialDelay = 5000;
    private long period = 30000;  // detect noise every 30s
    private double lastNoise = 0;
    private double lastTriggerNoise = 0;
    private boolean hasFirstDetection = false;
    private long lastTimestamp = 0;
    private final double threshold = 20;
    public static Integer latest_noiseLevel;

    public NoiseManager(VolEventListener volEventListener, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, AudioCollector audioCollector, LogCollector logCollector) {
        super(volEventListener);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.audioCollector = audioCollector;
        this.logCollector = logCollector;
    }

    @Override
    public void start() {
        // detect noise periodically
        Log.e(TAG, "schedule periodic noise detection");
        scheduledNoiseDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                Log.e(TAG, "start to detect noise level");
                detectNoise(5000, 10, true);
            } catch (Exception e) {
                Log.e(TAG, "error during noise detection: " + e);
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS);
        futureList.add(scheduledNoiseDetection);
    }

    @Override
    public void stop() {
        if (scheduledNoiseDetection != null) {
            scheduledNoiseDetection.cancel(true);
        }
    }

    public Integer getNoiseLevel(double db) {
        if (db <= 0)
            return 0;
        else if (db <= 35)
            return 1;
        else if (db <= 60)
            return 2;
        else
            return 3;
    }

    public CompletableFuture<Double> detectNoise(long length, long period) {
        return detectNoise(length, period, false);
    }

    private CompletableFuture<Double> detectNoise(long length, long period, boolean checkEvent) {
        return audioCollector.getNoiseLevel(length, period).thenApply(noise -> {
            Log.e(TAG, "noise level detected: " + noise);
            if (checkEvent && hasFirstDetection) {
                checkEvent(noise);
            }
            if (!hasFirstDetection) {
                lastTriggerNoise = noise;
                hasFirstDetection = true;
            }
            double diff = noise - getPresentNoise();
            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "noise", noise);
            JSONUtils.jsonPut(json, "old_noise", getPresentNoise());
            JSONUtils.jsonPut(json, "diff", diff);
            record(System.currentTimeMillis(), incLogID(), TAG, "periodic_detect", "" + diff, json.toString());
            setPresentNoise(noise);
            return noise;
        });
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
        latest_noiseLevel = getNoiseLevel(noise);
    }

}
