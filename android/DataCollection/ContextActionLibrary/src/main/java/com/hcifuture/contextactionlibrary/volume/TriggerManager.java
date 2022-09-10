package com.hcifuture.contextactionlibrary.volume;

import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class TriggerManager {

    VolEventListener volEventListener;

    public TriggerManager(VolEventListener volEventListener) {
        this.volEventListener = volEventListener;
    }

    public void start() {}
    public void stop() {}
}
