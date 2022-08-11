package com.hcifuture.contextactionlibrary.volume;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VolumeManager {

    // 每个函数有id
    Map<String, VolumeFunction> functions = new HashMap<>();  // fid, function

    class VolumeFunction {
        String fid;
        double paramA, paramB;
        List<Pair<Double, Double>> historyRecords = new ArrayList<>();
        VolumeFunction(String fid) {
            this.fid = fid;
        }
        VolumeFunction(String fid, double paramA, double paramB) {
            this.fid = fid;
            this.paramA = paramA;
            this.paramB = paramB;
        }
        String getFid() {
            return fid;
        }
        void train() {
            double xSum = 0;
            double ySum = 0;
            for (Pair<Double, Double> record: historyRecords){
                xSum += record.first;
                ySum += record.second;
            }
            double xMean = xSum / historyRecords.size();
            double yMean = ySum / historyRecords.size();
            double numerator = 0d;
            double denominator = 0d;
            for (Pair<Double, Double> record: historyRecords) {
                double x = record.first;
                double y = record.second;
                numerator = numerator + (x - xMean) * (y - yMean);
                denominator = denominator + (x - xMean) * (x - xMean);
            }
            if (Math.abs(denominator) < 1e-8) {  // 结果不可靠，暂时用默认值
                paramB = 0.5;
                paramA = 20;
            } else {
                paramB = numerator / denominator;
                paramA = yMean - paramB * xMean;
            }
        }

        double predict(double noise) {
            double y = paramB * noise + paramA;
            return Math.max(Math.min(y, 100), 0); // [0, 100]
        }
    }

    private int fid_count = 0;
    public String newFunction() {
        String fid = String.format("%d_%08d", new Date().getTime(), fid_count++);
        Log.e("NewFid", fid);
        functions.put(fid, new VolumeFunction(fid));  // TODO 完善初始化
        return fid;
    }
    public boolean addRecord(String fid, double noise, double volume) {
        return addRecord(fid, noise, volume, true);
    }

    public boolean addRecord(String fid, double noise, double volume, boolean retrain) {
        if (!functions.containsKey(fid)) return false;

        functions.get(fid).historyRecords.add(new Pair<>(noise, volume));
        Log.e("historyRecords", functions.get(fid).historyRecords.toString());
        if (retrain) functions.get(fid).train();
        return true;
    }

    public boolean train(String fid) {
        if (!functions.containsKey(fid)) return false;
        functions.get(fid).train();
        return true;
    }

    public double predict(String fid, double noise) {
        return functions.containsKey(fid) ? functions.get(fid).predict(noise) : -1;
    }

    static public double correlation(double[] x, double[] y) {  // 计算相关系数，备用
        double xSum = 0;
        double ySum = 0;
        double xP2Sum = 0;
        double yP2Sum = 0;
        double xySum = 0;
        int len = x.length;
        for (int i = 0; i < y.length; i++) {
            xSum += x[i];
            ySum += y[i];
            xP2Sum += Math.pow(x[i], 2);
            yP2Sum += Math.pow(y[i], 2);
            xySum += x[i] * y[i];

        }
        double Rxy = (len * xySum - xSum * ySum) / (Math.sqrt((len * xP2Sum - Math.pow(xSum, 2)) * (len * yP2Sum - Math.pow(ySum, 2))));
        return Rxy;

    }
}
