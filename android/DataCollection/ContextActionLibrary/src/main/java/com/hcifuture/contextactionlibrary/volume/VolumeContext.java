package com.hcifuture.contextactionlibrary.volume;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class VolumeContext {
    // Time
    Date date;
    // Place
    double latitude;
    double longitude;
    List<String> wifiId;
    List<String> bluetoothId;
    String place;
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

    public List<String> getWifiId() {
        return wifiId;
    }

    public void setWifiId(List<String> wifiId) {
        this.wifiId = wifiId;
    }

    public List<String> getBluetoothId() {
        return bluetoothId;
    }

    public void setBluetoothId(List<String> bluetoothId) {
        this.bluetoothId = bluetoothId;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
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

    public VolumeContext(Date date, double latitude, double longitude, List<String> wifiId,double noise, String app, String deviceType) {
        this(date, latitude, longitude, wifiId, null, noise, app, null, deviceType, null);
    }

    public VolumeContext(Date date, double latitude, double longitude, List<String> wifiId,
                         List<String> bluetoothId, double noise, String app, String volumeMode,
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

    @Override
    public String toString() {
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        return "VolumeContext{" +
                df.format(date) +
                ", (" + latitude +
                ", " + longitude +
                "), noise=" + noise +
                ", app='" + app + '\'' +
                ", " + deviceType + '\'' +
                '}';
    }
}
