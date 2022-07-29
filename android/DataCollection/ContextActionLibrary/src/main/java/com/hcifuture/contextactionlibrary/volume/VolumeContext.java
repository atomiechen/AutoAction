package com.hcifuture.contextactionlibrary.volume;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class VolumeContext {
    int soundVolume;
    // Noise
    double noise;
    int manAround;
    int share;
    int time;
    int startTime;
    int endTime;
    // Place
    double latitude;
    double longitude;
    List<String> wifiId;
    // App
    String app;
    // Device
    String device;
    int activity;

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

    public VolumeContext(int soundVolume, double noise, String device, int manAround, int share, double latitude, double longitude,
                         List<String> wifiId, int time, String app, int activity) {
        this(soundVolume, noise, device, manAround, share, latitude, longitude, wifiId, time, -1, -1, app, activity);
        if (time == 4) {
            Calendar calendar = Calendar.getInstance();
            startTime = calendar.get(Calendar.HOUR_OF_DAY) * 100 + calendar.get(Calendar.MINUTE);
            endTime = startTime + 100;
        } else if (time == 3) {
            startTime = 1800;
            endTime = 2400;
        } else if (time == 2) {
            startTime = 1400;
            endTime = 1800;
        } else if (time == 1) {
            startTime = 1200;
            endTime = 1400;
        } else if (time == 0) {
            startTime = 0;
            endTime = 1200;
        }
    }

    public VolumeContext(int soundVolume, double noise, String device, int manAround, int share, double latitude, double longitude,
                         List<String> wifiId, int time, int startTime, int endTime, String app, int activity) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.wifiId = wifiId;
        this.noise = noise;
        this.app = app;
        this.device = device;
        this.soundVolume = soundVolume;
        this.manAround = manAround;
        this.share = share;
        this.activity = activity;
        this.time = time;
        this.startTime = startTime;
        this.endTime = endTime;
    }

//    @Override
//    public String toString() {
//        DateFormat df = new SimpleDateFormat("HH:mm:ss");
//        return "VolumeContext{" +
//                df.format(date) +
//                ", (" + latitude +
//                ", " + longitude +
//                "), noise=" + noise +
//                ", app='" + app + '\'' +
//                ", " + device + '\'' +
//                '}';
//    }
}
