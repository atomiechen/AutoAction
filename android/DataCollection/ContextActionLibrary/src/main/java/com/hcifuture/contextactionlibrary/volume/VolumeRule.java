package com.hcifuture.contextactionlibrary.volume;

public class VolumeRule {
    Type type;
    String description;
    int volume;
    double priority;

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

    VolumeRule(Type type, String description, int volume, double priority){
        this.type = type;
        this.description = description;
        this.volume = volume;
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

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "VolumeRule{" +
                "type=" + type +
                ", description='" + description + '\'' +
                ", volume=" + volume +
                ", priority=" + priority +
                '}';
    }
}
