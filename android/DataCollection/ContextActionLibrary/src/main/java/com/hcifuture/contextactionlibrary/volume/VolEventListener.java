package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

public interface VolEventListener {
    enum EventType {
        TimeChange,
        DayOfWeekChange,
        PositionChange,
        ActivityChange,
        WifiChange,
        NoiseUp,
        NoiseDown,
        DeviceChange,
        AppChange,
        NetworkChange,
        // TODO
        NewMessagePosted,
        MessageRemoved,
        NetworkDelayUp,
        NetworkDelayDown,
        ScreenOrientationChange,
        NearbyPCIncrease,
        NearbyPCDecrease,
        VolumeUp,
        VolumeDown,
        // unused
        FrontEnd
    }

    void onVolEvent(EventType eventType, Bundle bundle);
    void recordEvent(EventType type, String action, String other);
    boolean upload(String filename, long startTimestamp, long endTimestamp, String name, String commit);
    boolean upload(String filename, long startTimestamp, long endTimestamp, String name, String commit, Bundle extras);
    String getCurrentContext();
    String getUserId();
    String getDeviceId();
    String getServerUrl();
}
