package com.hcifuture.contextactionlibrary.volume;

public abstract class TriggerManager {

    VolEventListener volEventListener;

    public TriggerManager(VolEventListener volEventListener) {
        this.volEventListener = volEventListener;
    }

    public void start() {}
    public void stop() {}
}
