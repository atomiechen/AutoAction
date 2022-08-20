package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

public interface VolEventListener {
    enum EventType {
        Noise,
        Device,
        Position,
        App,
        Motion
    }

    void onVolEvent(EventType eventType, Bundle bundle);
}
