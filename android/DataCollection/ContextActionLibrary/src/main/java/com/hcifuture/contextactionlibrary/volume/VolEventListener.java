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
        FrontEnd
    }

    void onVolEvent(EventType eventType, Bundle bundle);
    void recordEvent(EventType type, String action, String other);
}
