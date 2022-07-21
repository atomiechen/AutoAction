package com.example.volumerecommendation;

import java.util.Date;

public class VolumeContext {
    // Time
    Date date;
    // Place
    double latitude;
    double longitude;
    String wifiId;
    String bluetoothId;
    // Noise
    double noise;
    // App
    String app;
    // Device
    String volumeMode;
    String deviceType;
    String deviceId;


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getWifiId() {
        return wifiId;
    }

    public void setWifiId(String wifiId) {
        this.wifiId = wifiId;
    }

    public String getBluetoothId() {
        return bluetoothId;
    }

    public void setBluetoothId(String bluetoothId) {
        this.bluetoothId = bluetoothId;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getVolumeMode() {
        return volumeMode;
    }

    public void setVolumeMode(String volumeMode) {
        this.volumeMode = volumeMode;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public VolumeContext(Date date, double latitude, double longitude, double noise, String app, String deviceType) {
        this(date, latitude, longitude, null, null, noise, app, null, deviceType, null);
    }

    public VolumeContext(Date date, double latitude, double longitude, String wifiId,
                         String bluetoothId, double noise, String app, String volumeMode,
                         String deviceType, String deviceId) {
        this.date = date;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wifiId = wifiId;
        this.bluetoothId = bluetoothId;
        this.noise = noise;
        this.app = app;
        this.volumeMode = volumeMode;
        this.deviceType = deviceType;
        this.deviceId = deviceId;
    }
}
