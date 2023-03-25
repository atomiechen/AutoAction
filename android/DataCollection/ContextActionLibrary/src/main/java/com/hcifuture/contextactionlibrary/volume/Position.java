package com.hcifuture.contextactionlibrary.volume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Position {
    private String name;
    private double latitude;
    private double longitude;
    private List<String> wifiIds;

    public static List<Position> tsinghua_positions = new ArrayList<>(Arrays.asList(
        new Position("桃李园食堂", 40.010988, 116.326144),
        new Position("FIT楼", 39.99720217393699, 116.33134402255038),
        new Position("FIT楼", 39.99728042716742, 116.33216585135744),
        new Position("FIT楼", 39.99680201397061, 116.33178279555754),
        new Position("紫荆公寓", 40.01007471832654, 116.32629384140003),
        new Position("紫荆公寓", 40.010976059721095, 116.32763929282127),
        new Position("紫荆公寓", 40.01183092398141, 116.32735492785623),
        new Position("紫荆公寓", 40.01191804330896, 116.33014170450211),
        new Position("紫荆公寓", 40.01264766328225, 116.3290611176194),
        new Position("紫荆操场", 40.00997290827983, 116.32950605795057),
        new Position("紫荆园食堂", 40.011822, 116.328661),
        new Position("清芬园食堂", 40.005776, 116.328095),
        new Position("澜园食堂", 39.998399, 116.325046),
        new Position("南园食堂", 39.995646, 116.326995),
        new Position("听涛园食堂", 40.00635039307844, 116.32693762637197),
        new Position("东大操场", 40.00580600112954, 116.33229336550423),
        new Position("西大操场", 40.00509150103414, 116.32235349880905),
        new Position("第三教室楼", 40.002766693484865, 116.32855984558233),
        new Position("第六教学楼", 40.00274326196632, 116.32983067576762),
        new Position("第四教室楼", 40.002466343400606, 116.3273168234982),
        new Position("第五教室楼", 40.00254089850771, 116.32666889475816),
        new Position("第一教室楼", 40.00144405740559, 116.32408297023568),
        new Position("第二教室楼", 40.00237604756882, 116.32402758318),
        new Position("清华学堂", 40.00232354345792, 116.32516711623293),
        new Position("清华学堂", 40.00232354345792, 116.32516711623293),
        new Position("紫荆网球场", 40.010537147150494, 116.3308172320525),
        new Position("紫荆篮球场", 40.00956473551514, 116.33083583395972),
        new Position("人文社科图书馆", 40.004283207661516, 116.328434052897),
        new Position("图书馆北馆", 40.00519134670305, 116.32415012891892),
        new Position("图书馆西馆", 40.00487421763812, 116.32350446560002),
        new Position("观畴园食堂", 40.00688362096663, 116.32225547553632),
        new Position("玉树园食堂", 40.01177663560526, 116.33182512244312),
        new Position("芝兰园食堂", 40.01033658769939, 116.33345848990906),
        new Position("综合体育馆", 40.0041742333691, 116.33240947182703),
        new Position("东大操场网球场", 40.00424877673055, 116.33384705185233),
        new Position("西体育馆", 40.00502604942537, 116.32150002395629),
        new Position("新清华学堂", 40.0015905517878, 116.32953453779986),
        new Position("蒙民伟音乐厅", 40.00199812503571, 116.32896046973477),
        new Position("东主楼", 40.001481859683025, 116.33367057734802),
        new Position("东主楼", 40.0020586597806, 116.33350104485591),
        new Position("西主楼", 40.00144835212949, 116.33132247659809),
        new Position("西主楼", 40.00203022514113, 116.33129892308988),
        new Position("主楼", 40.00157749157738, 116.33246969760378)
    ));

    public Position(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
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

//    public double getGpsScore(Position position) {
//        double score = 0;
//        double distance = getDistance(position);
//        if (distance > 200)
//            return -1;
//        else
//            score += 50 * (200 - distance) / 200;
//        return score;
//    }

    public double getWifiScore(Position position) {
        double score = 0;
        List<String> wifiList = position.getWifiIds();
        double count = 0;
        for (String wifiId: wifiIds) {
            if (wifiList.contains(wifiId)) {
                count += 1;
            }
        }
        score += 50 * (count / wifiIds.size());
        return score;
    }

    public static double getScore(Position x, Position y) {
//        double wifi_score = 0;
        double gps_score = 0;
//        boolean wifi_valid = false;
        boolean gps_valid = false;
        if (!(y.getLatitude() < -90 && y.getLongitude() < -180) && !(x.latitude < -90 && x.longitude < -180) && !(y.getLatitude() == 0 && y.getLongitude() == 0) && !(x.latitude == 0 && x.longitude == 0)) {
            gps_valid = true;
            double distance = getDistance(x, y);
            if (distance > 200)
                return -1;
            else
                gps_score = 100 * (200 - distance) / 200;
        }
//        List<String> wifiList = position.getWifiIds();
//        if (wifiList != null && wifiList.size() > 0 && wifiIds != null && wifiIds.size() > 0) {
//            wifi_valid = true;
//            double count = 0;
//            for (String wifiId : wifiIds) {
//                if (wifiList.contains(wifiId)) {
//                    count += 1;
//                }
//            }
//            wifi_score = 50 * Math.sqrt(count / wifiIds.size());
//        }
//        double score = 0;
//        if(!wifi_valid && !gps_valid) score = -1;
//        if(wifi_valid && !gps_valid) score = 2 * wifi_score;
//        if(!wifi_valid && gps_valid) score = 2 * gps_score;
//        if(wifi_valid && gps_valid) score = wifi_score + gps_score;
        return gps_score;
    }

    public static boolean isSame(Position x, Position y) {
        return getScore(x, y) > 40;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public static double getDistance(Position x, Position y) {
        double lat1 = rad(y.getLatitude());
        double lat2 = rad(x.latitude);
        double lon1 = rad(y.getLongitude());
        double lon2 = rad(x.longitude);

        lat1 = Math.PI / 2 - lat1;
        lat2 = Math.PI / 2 - lat2;
        if (lon1 < 0)
            lon1 = Math.PI * 2 + lon1;
        if (lon2 < 0)
            lon2 = Math.PI * 2 + lon2;

        double EARTH_RADIUS = 6378137;
        double x1 = EARTH_RADIUS * Math.cos(lon1) * Math.sin(lat1);
        double y1 = EARTH_RADIUS * Math.sin(lon1) * Math.sin(lat1);
        double z1 = EARTH_RADIUS * Math.cos(lat1);
        double x2 = EARTH_RADIUS * Math.cos(lon2) * Math.sin(lat2);
        double y2 = EARTH_RADIUS * Math.sin(lon2) * Math.sin(lat2);
        double z2 = EARTH_RADIUS * Math.cos(lat2);

        double d = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2));
        double theta = Math.acos((2 * EARTH_RADIUS * EARTH_RADIUS - d * d) / (2 * EARTH_RADIUS * EARTH_RADIUS));
        return theta * EARTH_RADIUS;
    }

    public static String getLocation(double latitude, double longitude, String _default) {
        Position present = new Position("", latitude, longitude);
        String result = (_default != null) ? _default : "";
        double max_score = 0;
        for (Position position: tsinghua_positions) {
            double score = getScore(position, present);
            if (score > max_score)
                result = position.name;
        }
        return result;
    }
}
