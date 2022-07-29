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
//        fillContextList(); // 手动生成一些数据，用于测试
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
//        List<VolumeRule> ruleList = new ArrayList<>();
//
//        for (VolumeRule.Type type : VolumeRule.Type.values()) {
//            List<Integer> volumes = getVolumes(type, volumeContext);
////            Log.i(type + " volumes", volumes.toString());
//            int targetVolume = (int) volumes.stream().mapToDouble(x->x).average().orElse(0D);
//
//            // 计算样本标准差
//            double rval = 0;
//            if (volumes.size() == 0) rval = 500;  // largest
//            else if (volumes.size() == 1) rval = 200;  // second largest
//            else {
//                for (Integer volume : volumes) {
//                    rval += Math.pow((volume - targetVolume), 2);
//                }
//                rval = Math.sqrt(rval / (volumes.size() - 1));
//            }
//            // 暂时用样本标准差作为priority
//            ruleList.add(new VolumeRule(type, String.format("根据%s，已为您推荐目前的最适音量。", type.text), targetVolume, rval));
//
//        }
//
//        // 方差作为优先级时，优先级小者在前
//        return ruleList.stream().sorted(Comparator.comparing(VolumeRule::getPriority)).collect(Collectors.toList());
        ArrayList<VolumeRule> rules = getRules(volumeContext);
        ArrayList<Bundle> rules_bundle = new ArrayList<>();
        ArrayList<String> rules_string = new ArrayList<>();
        for (VolumeRule rule: rules) {
            rules_bundle.add(rule.toBundle());
            rules_string.add(rule.toString());
        }
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
        ArrayList<VolumeRule> result = new ArrayList<>();
        result.add(new VolumeRule(volumeContext, 10));
        return result;
    }

    public Bundle getContext(VolumeContext volumeContext) {
        return new VolumeRule(volumeContext, 10).toBundle();
    }

    public Bundle getValueRange() {
        ArrayList<String> device = (ArrayList<String>) Arrays.asList("扬声器", "耳机");
        ArrayList<String> place = (ArrayList<String>) Arrays.asList("家", "宿舍");
        ArrayList<String> app = (ArrayList<String>) Arrays.asList("微信", "QQ");
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

    public static String findPlace(VolumeContext volumeContext) {
        String atWhere = "";
        double max_score = -1;
        if (locations != null) {
            for (Location location : locations) {
                double score = location.getScore(volumeContext);
                if (score > max_score && score > 0) {
                    atWhere = location.getName();
                    max_score = score;
                }
            }
        }
        if (max_score > 40)
            return atWhere;
        else
            return "unknown";
    }

//    void fillContextList() {
//        // time: night, around 2
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 0, 0, 0),
//                22.5301, 114.01, 40, "com.xingin.xhs", "speaker"), 3));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 1, 0, 0),
//                22.43, 114.2, 60, "com.netease.cloudmusic", "headset"), 2));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 0, 20, 0),
//                22.0, 114, 17, "com.netease.cloudmusic", "headset"), 1));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 2, 0, 0),
//                22.77, 114.015794, 35, "com.eusoft.ting.en", "bt_a2dp"), 2));
//        // place: near(22.541364, 114.009766), around 6
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 8, 0, 0),
//                22.541364, 114.009766, 30, "com.tencent.wemeet.app", "bt_a2dp"), 6));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 15, 20, 0),
//                22.541334, 114.009756, 50, "com.tencent.mm", "bt_a2dp"), 6));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 16, 20, 0),
//                22.541363, 114.009764, 27, "com.tencent.karaoke", "earpiece"), 7));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 20, 0, 0),
//                22.541354, 114.009768, 40, "com.gotokeep.keep", "headset"), 5));
//        // noise: loud, around 12
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 8, 0, 0),
//                22.53, 114.0, 80, "com.tencent.karaoke", "bt_a2dp"), 12));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 15, 20, 0),
//                22.541334, 114.01, 85, "com.tencent.mm", "speaker"), 13));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 7, 20, 0),
//                22.541363, 114.1, 90, "com.tencent.karaoke", "earpiece"), 11));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 20, 0, 0),
//                22.541354, 114.2, 82, "com.gotokeep.keep", "speaker"), 12));
//        // app: bilibli, around 8
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 3, 0, 0),
//                22.52, 114.015794, 30, "tv.danmaku.bili", "speaker"), 7));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 15, 0, 0),
//                22.538565, 114.015794, 12, "tv.danmaku.bili", "bt_a2dp"), 8));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 9, 20, 0),
//                22.53, 114.015794, 27, "tv.danmaku.bili", "speaker"), 8));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 21, 0, 0),
//                22.54, 114.015794, 40, "tv.danmaku.bili", "bt_a2dp"), 8));
//        // device: speaker, around 10.5
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 8, 0, 0),
//                22.53, 114.01001, 30, "com.gotokeep.keep", "speaker"), 11));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 15, 20, 0),
//                22.534, 114.015794, 72, "com.youku.phone", "speaker"), 10));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 16, 20, 0),
//                22.536, 114.1, 27, "com.gotokeep.keep", "speaker"), 10));
//        mContextList.add(new RecordItem(new VolumeContext(new Date(2022, 7, 20, 20, 0, 0),
//                22.538565, 114.01, 40, "com.tencent.mm", "speaker"), 11));
//    }

    public int getRuleListSize() {
        return mRuleList.size();
    }
}