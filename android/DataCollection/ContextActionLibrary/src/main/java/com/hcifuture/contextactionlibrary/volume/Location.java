package com.hcifuture.contextactionlibrary.volume;

import java.util.List;

public class Location {
    private String name;
    private double latitude;
    private double longitude;
    private List<String> wifiIds;
    private static final double EARTH_RADIUS = 6378137;

    public Location(String name, double latitude, double longitude, List<String> wifiIds) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wifiIds = wifiIds;
    }

    public Location(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wifiIds = null;
    }

    public Location(String name, List<String> wifiIds) {
        this.name = name;
        this.latitude = -200;
        this.longitude = -200;
        this.wifiIds = wifiIds;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<String> getWifiIds() {
        return wifiIds;
    }

    public String getName() {
        return name;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWifiIds(List<String> wifiIds) {
        this.wifiIds = wifiIds;
    }

//    public double getGpsScore(VolumeContext volumeContext) {
//        double score = 0;
//        double distance = getDistance(volumeContext);
//        if (distance > 200)
//            return -1;
//        else
//            score += 50 * (200 - distance) / 200;
//        return score;
//    }

//    public double getWifiScore(VolumeContext volumeContext) {
//        double score = 0;
//        List<String> wifiList = volumeContext.getWifiId();
//        double count = 0;
//        for (String wifiId: wifiIds) {
//            if (wifiList.contains(wifiId)) {
//                count += 1;
//            }
//        }
//        score += 50 * (count / wifiIds.size());
//        return score;
//    }
//
//    public double getScore(VolumeContext volumeContext) {
//        double wifi_score = 0;
//        double gps_score = 0;
//        boolean wifi_valid = false;
//        boolean gps_valid = false;
//        if (!(volumeContext.latitude < -90 && volumeContext.longitude < -180) && !(latitude < -90 && longitude < -180) ) {
//            gps_valid = true;
//            double distance = getDistance(volumeContext);
//            if (distance > 200)
//                return -1;
//            else
//                gps_score = 50 * (200 - distance) / 200;
//        }
//        List<String> wifiList = volumeContext.getWifiId();
//        if (wifiList != null && wifiList.size() > 0 && wifiIds != null && wifiIds.size() > 0) {
//            wifi_valid = true;
//            double count = 0;
//            for (String wifiId : wifiIds) {
//                if (wifiList.contains(wifiId)) {
//                    count += 1;
//                }
//            }
//            wifi_score = 50 * (count / wifiIds.size());
//        }
//        double score = 0;
//        if(!wifi_valid && !gps_valid) score = -1;
//        if(wifi_valid && !gps_valid) score = 2 * wifi_score;
//        if(!wifi_valid && gps_valid) score = 2 * gps_score;
//        if(wifi_valid && gps_valid) score = wifi_score + gps_score;
//        return score;
//    }
//
//    private static double rad(double d) {
//        return d * Math.PI / 180.0;
//    }
//
//    public double getDistance(VolumeContext volumeContext) {
//        double lat1 = rad(volumeContext.latitude);
//        double lat2 = rad(latitude);
//        double lon1 = rad(volumeContext.longitude);
//        double lon2 = rad(longitude);
//
//        lat1 = Math.PI / 2 - lat1;
//        lat2 = Math.PI / 2 - lat2;
//        if (lon1 < 0)
//            lon1 = Math.PI * 2 + lon1;
//        if (lon2 < 0)
//            lon2 = Math.PI * 2 + lon2;
//
//        double x1 = EARTH_RADIUS * Math.cos(lon1) * Math.sin(lat1);
//        double y1 = EARTH_RADIUS * Math.sin(lon1) * Math.sin(lat1);
//        double z1 = EARTH_RADIUS * Math.cos(lat1);
//        double x2 = EARTH_RADIUS * Math.cos(lon2) * Math.sin(lat2);
//        double y2 = EARTH_RADIUS * Math.sin(lon2) * Math.sin(lat2);
//        double z2 = EARTH_RADIUS * Math.cos(lat2);
//
//        double d = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2));
//        double theta = Math.acos((2 * EARTH_RADIUS * EARTH_RADIUS - d * d) / (2 * EARTH_RADIUS * EARTH_RADIUS));
//        return theta * EARTH_RADIUS;
//    }
}
