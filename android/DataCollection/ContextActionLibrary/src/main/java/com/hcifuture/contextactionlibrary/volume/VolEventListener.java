package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

public interface VolEventListener {
    enum EventType {
        Noise,
        Device,
        Position,
        App,
        Motion,
        Bluetooth,
        Audio,
        Time,
        Crowd,
        FrontEnd,
        Step
    }

    void onVolEvent(EventType eventType, Bundle bundle);
    void recordEvent(EventType type, String action, String other);
    boolean upload(String filename, long startTimestamp, long endTimestamp, String name, String commit);
    boolean upload(String filename, long startTimestamp, long endTimestamp, String name, String commit, Bundle extras);
}
