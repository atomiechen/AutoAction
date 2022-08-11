package com.hcifuture.contextactionlibrary.volume;

import java.util.Objects;

import androidx.annotation.NonNull;

public class Device {
    public String deviceID;
    public String name;
    public String description;
    public int deviceType;

    Device(String deviceID, String name, String description, int deviceType) {
        this.deviceID = deviceID;
        this.name = name;
        this.description = description;
        this.deviceType = deviceType;
    }

    @NonNull
    @Override
    public String toString() {
        return deviceID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return deviceID.equals(device.deviceID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceID);
    }
}
