package com.hcifuture.contextactionlibrary.volume;

import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class TriggerManager {

    VolEventListener volEventListener;

    LogCollector logCollector;
    AtomicInteger mLogID = new AtomicInteger(0);

    public TriggerManager(VolEventListener volEventListener) {
        this.volEventListener = volEventListener;
    }

    int incLogID() {
        return mLogID.getAndIncrement();
    }

    void record(long timestamp, int logID, String type, String action, String tag, String other) {
        if (logCollector == null) {
            return;
        }

        String line = timestamp + "\t" + logID + "\t" + type + "\t" + action + "\t" + tag + "\t" + other;
        logCollector.addLog(line);
        Log.e(type, "record: " + line);
    }

    public void start() {}
    public void stop() {}
}
