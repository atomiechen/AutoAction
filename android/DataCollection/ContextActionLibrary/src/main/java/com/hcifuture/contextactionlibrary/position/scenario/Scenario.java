package com.hcifuture.contextactionlibrary.position.scenario;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import com.hcifuture.contextactionlibrary.position.sensor.BluetoothScanner;
import com.hcifuture.contextactionlibrary.position.sensor.LocalNetworkSearcher;
import com.hcifuture.contextactionlibrary.position.sensor.WIFIScanner;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Scenario {
    public static final int STATUS_UNFINISHED = 100; // 各个模块开始分析的初始状态
    public static final int STATUS_SUCCEED = 101;
    public static final int STATUS_FAILED = 102;

    public static final int RESULT_READY = 200;
    public static final int RESULT_LATEST = 201;// WIFI 扫描成功 获取到最新的无线局域网络信息
    public static final int RESULT_CACHED = 202; // WIFI 扫描失败 获取到缓存的无线局域网络信息
    public static final int RESULT_FOUND = 203; // Recognize Position 找到相似位置
    public static final int RESULT_NOT_FOUND = 204; // Recognize Position 未能找到相似位置
    public static final int RESULT_INVALID = 205; // Recognize Position 结果不可用

    public static final int PROBLEM_DETECTED = 300;
    public static final int PROBLEM_MISSING_INFORMATION = 301; // Recognize Position 未能获取到历史位置信息文件列表 或者其为空

    public String name = "DEFAULT_SCENARIO";

    public JSONObject when = new JSONObject();
    public JSONObject where = new JSONObject();
    public JSONObject who = new JSONObject();

    public Set<BluetoothDevice> deviceSet = new HashSet<>();
    public Set<ScanResult> beaconSet = new TreeSet<>((o1, o2) -> o2.getRssi() - o1.getRssi());
    public List<BluetoothScanner.BluetoothInformation> bluetoothList = new ArrayList<>();

    public String internetIP = "";
    public List<LocalNetworkSearcher.DeviceUnderNetwork> deviceUnderNetworkList = new ArrayList<>();

    public float[] orientationAngles = new float[3];

    public boolean tookOneStep = false;

    public int wifiScanResultStatus = Scenario.STATUS_UNFINISHED;
    public List<android.net.wifi.ScanResult> wifiScanResultList = new ArrayList<>();
    public List<WIFIScanner.WIFIInformation> wifiList = new ArrayList<>();

    public int recognizedPositionStatus = Scenario.STATUS_UNFINISHED;
    public JSONObject recognizedPositionProfile = new JSONObject();
    public double recognizedPositionConfidence = 0.0;

    public void clear() {
        when = new JSONObject();
        where = new JSONObject();
        who = new JSONObject();

        deviceSet = new HashSet<>();
        beaconSet = new TreeSet<>((o1, o2) -> o2.getRssi() - o1.getRssi());
        bluetoothList = new ArrayList<>();

        internetIP = "";
        deviceUnderNetworkList = new ArrayList<>();

        orientationAngles = new float[3];

        tookOneStep = false;

        wifiScanResultStatus = Scenario.STATUS_UNFINISHED;
        wifiScanResultList = new ArrayList<>();
        wifiList = new ArrayList<>();

        recognizedPositionStatus = Scenario.STATUS_UNFINISHED;
        recognizedPositionProfile = new JSONObject();
        recognizedPositionConfidence = 0.0;
    }

}
