package com.hcifuture.contextactionlibrary.volume;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.async.AudioCollector;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;

public class NoiseManager {

    private static final String TAG = "VolumeController";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private AudioCollector audioCollector;
    private VolEventListener volEventListener;

    private ScheduledFuture<?> scheduledNoiseDetection;
    public double lastNoise = 0;
    private final double threshold = 20;

    public NoiseManager(ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, AudioCollector audioCollector, VolEventListener volEventListener) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.audioCollector = audioCollector;
        this.volEventListener = volEventListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void start() {
        long period = 30000;  // detect noise every 30s
        Log.e(TAG, "schedule periodic noise detection");
        scheduledNoiseDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                Log.e(TAG, "start to detect noise level");
                double noise = audioCollector.getNoiseLevel(5000, 10).get(5020, TimeUnit.MILLISECONDS);
                Log.e(TAG, "noise level detected: " + lastNoise);
                if (Math.abs(noise - lastNoise) > threshold) {
                    // signal to adjust volume according to noise
                    Bundle bundle = new Bundle();
                    bundle.putDouble("noise", noise);
                    volEventListener.onVolEvent(VolEventListener.EventType.Noise, bundle);
                }
            } catch (Exception e) {
                Log.e(TAG, "error during noise detection: " + e);
            }
        }, 5000, period, TimeUnit.MILLISECONDS);
        futureList.add(scheduledNoiseDetection);
    }

}
