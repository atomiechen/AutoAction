package com.hcifuture.contextactionlibrary.volume;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VolumeRuleManager {

    List<VolumeRule> mRuleList;  // 暂时用列表存放，之后应建立持久化存储乃至数据库
    public static List<Location> locations;

    public VolumeRuleManager() {
        mRuleList = new ArrayList<VolumeRule>();
        locations = new ArrayList<>();
        fillLocations();
//        fillContextList(); // 手动生成一些数据，用于测试
        fillManualRuleList();
    }

//    class RecordItem {
//        public RecordItem(VolumeContext volumeContext, int volume) {
//            this.volumeContext = volumeContext;
//            this.volume = volume;
//        }
//        //       public Date time;
//        public VolumeContext volumeContext;
//        public int volume;
//    }

    // 以下方案未必合理，因为并未将数据集严格离散化，也没有形成条目可数的规则
    boolean closeInTime(Date date1, Date date2){
        // 暂时将间隔不超三小时视为一个时段
        return Math.abs(date1.getTime() - date2.getTime()) % (3600 * 24) < 3600 * 3;
    }

    boolean closeInPlace(double latitudeA, double longitudeA, double latitudeB, double longitudeB){
        double BLOCK_LENGTH = 0.0002; //经纬度分块粒度，先用0.0002
        return (Math.abs(latitudeA - latitudeB) < BLOCK_LENGTH) &&
                (Math.abs(longitudeA - longitudeB) < BLOCK_LENGTH);
    }

    boolean closeInNoise(double noise1, double noise2) {
        // 暂时将相差不超过20的视为同一噪声等级
        return Math.abs(noise1 - noise2) < 20;
    }

//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public List<Integer> getVolumes(VolumeRule.Type type, VolumeContext volumeContext) {  // 先用方差代替
//
//        switch (type) {
//            case TIME:
//                return mContextList.stream().filter(item -> closeInTime(item.volumeContext.getDate(), volumeContext.getDate()))
//                        .map(item -> item.volume).collect(Collectors.toList());
//            case PLACE:
//                return mContextList.stream().filter(item -> closeInPlace(item.volumeContext.getLatitude(),
//                        item.volumeContext.getLongitude(), volumeContext.getLatitude(), volumeContext.getLongitude()))
//                        .map(item -> item.volume).collect(Collectors.toList());
//            case NOISE:
//                return mContextList.stream().filter(item -> closeInNoise(item.volumeContext.getNoise(), volumeContext.getNoise()))
//                        .map(item -> item.volume).collect(Collectors.toList());
//            case APP:
//                return mContextList.stream().filter(item -> item.volumeContext.getApp().equals(volumeContext.getApp()))
//                        .map(item -> item.volume).collect(Collectors.toList());
//            case DEVICE:
//                return mContextList.stream().filter(item -> item.volumeContext.getDeviceType().equals(volumeContext.getDeviceType()))
//                        .map(item -> item.volume).collect(Collectors.toList());
//            default:
//                return mContextList.stream().map(item -> item.volume).collect(Collectors.toList());
//        }
//    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Bundle getRecommendation(VolumeContext volumeContext){
        ArrayList<VolumeRule> rules = getRules(volumeContext);
        rules.sort(Comparator.comparing(VolumeRule::getPriority).reversed());
//        ArrayList<VolumeRule> rules = new ArrayList<>(_rules.stream().sorted(Comparator.comparing(VolumeRule::getPriority).reversed())
//                .collect(Collectors.toList()));
        ArrayList<Bundle> rules_bundle = new ArrayList<>();
        ArrayList<String> rules_string = new ArrayList<>();
//        for (VolumeRule rule: rules) {
//            rules_bundle.add(rule.toBundle());
//            rules_string.add(rule.toString());
//        }
        Bundle context = getContext(volumeContext);
        Bundle valueRange = getValueRange();
        Bundle result = new Bundle();
        result.putParcelableArrayList("rules", rules_bundle);
        result.putStringArrayList("rulesString", rules_string);
        result.putBundle("context", context);
        result.putBundle("valueRange", valueRange);
        return result;
    }

    public ArrayList<VolumeRule> getRules(VolumeContext volumeContext) {
        //naive version
        List<String> keys = Arrays.asList("noise", "device", "place", "time", "app");
        ArrayList<VolumeRule> result = new ArrayList<>();
//        Bundle context = volumeContext.toBundle();
//        for (VolumeRule rule: mRuleList) {
//            Bundle mRule = rule.toBundle();
//            boolean bundles_value_equal = true;
//            for (String key: keys) {
//                if (!equalValue(context, mRule, key)) {
//                    bundles_value_equal = false;
//                    break;
//                }
//            }
//            if (bundles_value_equal) {
//                result.add(rule);
//            }
//        }
        return result;
    }

    public boolean equalValue(Bundle context, Bundle rule, String key) {
        if (!rule.containsKey(key) || !context.containsKey(key))
            return true;
        if (key.equals("noise")) {
            return context.getInt("noise") == rule.getInt("noise");
        } else if (key.equals("device") || key.equals("place") || key.equals("app")) {
            return context.getString(key).equals(rule.getString(key));
        } else if (key.equals("time")) {
            if (context.getInt("time") < 4)
                return context.getInt("time") == rule.getInt("time");
            else {
                int startTime = 2500;
                int endtime = -1000;
                if (rule.getInt("time") == 0) {
                    startTime = 0;
                    endtime = 1100;
                } else if (rule.getInt("time") == 1) {
                    startTime = 1100;
                    endtime = 1400;
                } else if (rule.getInt("time") == 2) {
                    startTime = 1400;
                    endtime = 1800;
                } else if (rule.getInt("time") == 3) {
                    startTime = 1800;
                    endtime = 2400;
                } else if (rule.getInt("time") == 4) {
                    startTime = rule.getInt("startTime");
                    endtime = rule.getInt("endTime");
                }
                return context.getInt("startTime") >= startTime
                        && context.getInt("endTime") <= endtime;
            }
        }
        return false;
    }

    public Bundle getContext(VolumeContext volumeContext) {
//        return new VolumeRule(volumeContext, 0).toBundle();
        return new Bundle();
    }

    public Bundle getValueRange() {
        // device
        ArrayList<String> device = new ArrayList<>(Arrays.asList("扬声器", "耳机", "未知设备"));
        // place
        ArrayList<String> place = new ArrayList<>();
        for (Location location: locations) {
            place.add(location.getName());
        }
        // app
        ArrayList<String> app = new ArrayList<>();
        app.addAll(AppList.video_appNames);
        app.addAll(AppList.information_appNames);
        app.addAll(AppList.meeting_appNames);
        app.addAll(AppList.social_appNames);
        app.addAll(AppList.music_appNames);
        app.addAll(AppList.others_appNames);

        Bundle result = new Bundle();
        result.putStringArrayList("device", device);
        result.putStringArrayList("place", place);
        result.putStringArrayList("app", app);
        return result;
    }

    public void addLocation(Location location) {
        if (locations == null)
            locations = new ArrayList<>();
        locations.add(location);
    }

    public void addRecord(VolumeContext volumeContext, int volume) {
        if (mRuleList == null) {
            mRuleList = new ArrayList<>();
        }
        mRuleList.add(new VolumeRule(volumeContext, volume));
    }

//    public static String findPlace(VolumeContext volumeContext) {
//        String atWhere = "";
//        double max_score = -1;
//        if (locations != null) {
//            for (Location location : locations) {
//                double score = location.getScore(volumeContext);
//                if (score > max_score && score > 0) {
//                    atWhere = location.getName();
//                    max_score = score;
//                }
//            }
//        }
//        if (max_score > 40)
//            return atWhere;
//        else
//            return "unknown";
//    }

    void fillManualRuleList() {
//        double noise, String device, double latitude, double longitude, List<String> wifiId, int time, int startTime, int endTime, String app
        // 全部任意，默认30%

    }

    public int getRuleListSize() {
        return mRuleList.size();
    }

    public void fillLocations() {
        double latitude = 40.00826611;
        double longitude = 116.31997283;
        String name = "宿舍";
        List<String> wifiList = Arrays.asList(
                "\"Tsinghua-Secure\"a8:58:40:d7:13:b2",
                "Tsinghuaa8:58:40:d7:13:a0",
                "Tsinghua-Securea8:58:40:d7:13:a2",
                "Tsinghua-IPv6-SAVAa8:58:40:d7:13:a3",
                "Tsinghua-IPv6-SAVAa8:58:40:d7:13:b3",
                "Tsinghua-5Ga8:58:40:d7:13:b5",
                "Tsinghuaa8:58:40:d7:13:b0",
                "Tsinghua-Securea8:58:40:d7:13:b2",
                "Tsinghuaa8:58:40:d5:d5:40",
                "Tsinghua-Securea8:58:40:d5:d5:42",
                "Tsinghua-IPv6-SAVAa8:58:40:d5:d5:43",
                "THU-Internet-Exchange74:59:09:f2:87:c4",
                "Tsinghua-Securea8:58:40:d7:12:82",
                "Tsinghua-Securea8:58:40:d6:d4:a2",
                "Tsinghua-IPv6-SAVAa8:58:40:d7:07:a3",
                "Tsinghuaa8:58:40:d5:ca:a0",
                "Tsinghua-Securea8:58:40:d5:ca:a2",
                "Tsinghua-IPv6-SAVAa8:58:40:d6:d1:b3",
                "Tsinghua-5Ga8:58:40:d6:d1:b5",
                "Tsinghuaa8:58:40:d6:d1:b0",
                "Tsinghua-Securea8:58:40:d6:d1:b2",
                "Tsinghua_unSecured8:32:14:74:ed:71",
                "THU-Internet-Exchange74:59:09:f2:87:c8",
                "Tsinghua-IPv6-SAVAa8:58:40:d7:07:b3",
                "Tsinghua-5Ga8:58:40:d7:07:b5",
                "Tsinghua-5Ga8:58:40:d7:12:95",
                "Tsinghua-Securea8:58:40:d6:d4:b2",
                "Tsinghuaa8:58:40:d7:12:90",
                "Tsinghua-Securea8:58:40:d7:12:92",
                "Tsinghua-IPv6-SAVAa8:58:40:d7:12:93",
                "Tsinghua-IPv6-SAVAa8:58:40:d6:97:93",
                "Tsinghuaa8:58:40:d6:97:90",
                "Tsinghua-5Ga8:58:40:d6:97:95",
                "Tsinghua-5Ga8:58:40:d5:d5:55",
                "Tsinghua-IPv6-SAVAa8:58:40:d6:03:f3",
                "Tsinghuaa8:58:40:d6:03:f0",
                "Tsinghua-Securea8:58:40:d6:03:f2",
                "Tsinghua-5Ga8:58:40:d6:03:f5",
                "Tsinghua-Securea8:58:40:d0:6e:f2",
                "Tsinghua-IPv6-SAVAa8:58:40:d0:6e:f3",
                "Tsinghua-IPv6-SAVAa8:58:40:d0:f9:d3",
                "Tsinghua-5Ga8:58:40:d0:6e:f5",
                "Tsinghuaa8:58:40:d0:6e:f0"
        );
        addLocation(new Location(name, latitude, longitude, wifiList));
        addLocation(new Location("FIT楼", 39.9957763, 116.3255634, null));
        addLocation(new Location("紫荆操场", 40.0086677, 116.3233307, null));
        addLocation(new Location("文科图书馆", 40.0029605, 116.3224965, null));
        addLocation(new Location("李文正馆", 40.0043092, 116.3178216, null));
        addLocation(new Location("六教", 40.0016146, 116.3238986, null));
        addLocation(new Location("桃李园食堂", 40.0104485, 116.3223971, null));
    }
}