package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

public class ContextRule {
    Type type;
    String description;
    double volume;
    int priority;
    VolumeContext context;

    public enum Type {
        TIME("您以往在本时段使用的音量"),
        PLACE("您以往在此地点时使用的音量"),
        NOISE("当前环境音量"),
        APP("您以往使用本APP的音量"),
        DEVICE("您使用的播放设备");
        String text;
        Type(String text) { this.text = text; }

        public String getText() {
            return text;
        }
    }

    ContextRule(Type type, String description, int volume, int priority){
        this.type = type;
        this.description = description;
        this.volume = volume;
        this.priority = priority;
    }

    public ContextRule(VolumeContext volumeContext, double volume) {
        this.volume = volume;
        this.context = volumeContext;
        this.priority = 0;
    }

    public ContextRule(VolumeContext volumeContext, double volume, int priority) {
        this.volume = volume;
        this.context = volumeContext;
        this.priority = priority;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

//    @Override
//    public String toString() {
//        return "ContextRule{" +
//                "type=" + type +
//                ", description='" + description + '\'' +
//                ", volume=" + volume +
//                ", priority=" + priority +
//                '}';
//    }

    @Override
    public String toString() {
        String result = "";
//        if (context.time >= 0) {
//            String[] time = {"早上", "中午", "下午", "晚间"};
//            if (context.time <= 3) result += time[context.time];
//            else if (context.time == 4)
//                result += context.startTime / 100 + ":" + (context.startTime % 100) + "-" + context.endTime / 100 + ":" + (context.endTime % 100);
//        }
//        if (context.app != null) {
//            if (!result.equals("")) result += "·";
//            result += context.app;
//        }
//        if (context.device != null) {
//            if (!result.equals("")) result += "·";
//            String dev;
//            if (context.device.endsWith("speaker")) dev = "扬声器";
//            else if (context.device.endsWith("earpiece") || context.device.endsWith("headset") || context.device.contains("bt") || context.device.endsWith("headphone"))
//                dev = "耳机";
//            else dev = "未知类型";
//            result += dev;
//        }
//        if (context.noise >= 0) {
//            if (!result.equals("")) result += "·";
//            result += "噪音";
//            if (context.noise < 50) result += "小";
//            else if (context.noise > 70) result += "大";
//            else result += "中";
//        }
//        if (!(context.wifiId == null && context.latitude <= 0 && context.longitude <= 0)) {
//            if (!result.equals("")) result += "·";
//            String place = ContextRuleManager.findPlace(context);
//            if (place.equals("unknown"))
//                result += "未知地点";
//            else
//                result += place;
//        }
//        if (context.activity >= 0) {
//            if (!result.equals("")) result += "·";
//            String[] activity = {"静止", "走路", "跑步", "骑车", "开车", "其他"};
//            result += activity[context.activity];
//        }
//        if (context.soundVolume >= 0) {
//            if (!result.equals("")) result += "·";
//            String[] soundVolume = {"小", "中", "大"};
//            result += "音频音量" + soundVolume[context.soundVolume];
//        }
//        if (context.manAround >= 0) {
//            if (!result.equals("")) result += "·";
//            if (context.manAround == 1) result += "周围有人";
//            else if (context.manAround == 0) result += "周围无人";
//        }
//        if (context.share >= 0) {
//            if (!result.equals("")) result += "·";
//            if (context.share == 1) result += "与人共享";
//            else if (context.share == 0) result += "非与人共享";
//        }
//        if (result.equals("")) result += "任意条件";
//        result += "·音量" + volume + "%";
        return result;
    }

    public Bundle toBundle() {
        Bundle result = context.toBundle();
        result.putDouble("volume", volume);
        result.putInt("priority", priority);
        return result;
    }
}
