package com.hcifuture.contextactionlibrary.position.scenario;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.hcifuture.contextactionlibrary.position.recorder.NormalRecorder;
import com.hcifuture.contextactionlibrary.position.sensor.BluetoothScanner;
import com.hcifuture.contextactionlibrary.position.sensor.IMUSensor;
import com.hcifuture.contextactionlibrary.position.sensor.LocalNetworkSearcher;
import com.hcifuture.contextactionlibrary.position.sensor.OrientationAngleSensor;
import com.hcifuture.contextactionlibrary.position.sensor.WIFIScanner;
import com.hcifuture.contextactionlibrary.position.trigger.NormalTrigger;
import com.hcifuture.contextactionlibrary.position.utility.LoggerUtility;
import com.hcifuture.contextactionlibrary.position.utility.NotificationUtility;
import com.hcifuture.contextactionlibrary.position.utility.PositionProfileUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ScenarioAnalyzer {
    private static final String TAG = "ScenarioAnalyzer";

    private final Context context;

    private final List<AnalyzerCallback> analyzerCallbackList = new ArrayList<>();
    private final long defaultTimeout = 8 * 1000;
    private final long minCollectInterval = 30 * 1000; // 收集传感器数据的最小间隔时间
    private final long maxValidTime = 300 * 1000; // 缓存传感器数据的最大有效时间

    private NormalTrigger normalTrigger;
    private NormalRecorder normalRecorder;

    private BluetoothScanner bluetoothScanner;
    private WIFIScanner wifiScanner;
    private IMUSensor imuSensor;
    private OrientationAngleSensor orientationAngleSensor;
    private LocalNetworkSearcher localNetworkSearcher;

    private Scenario currentScenario;

    private Supervisor supervisor;

    private long cacheCreateTimeStamp;
    private long cacheVerifyTimeStamp;

    private long wifiUpdateTimeStamp;

    private boolean isRunning;

    private JSONObject configuration;

    public ScenarioAnalyzer(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");
        removeAllCallbacks();

        bluetoothScanner = new BluetoothScanner(context);
        bluetoothScanner.init();

        wifiScanner = new WIFIScanner(context);
        wifiScanner.init();

        imuSensor = new IMUSensor(context);
        imuSensor.init();

        orientationAngleSensor = new OrientationAngleSensor(context);
        orientationAngleSensor.init();

        localNetworkSearcher = new LocalNetworkSearcher(context);
        localNetworkSearcher.init();

        currentScenario = new Scenario();

        supervisor = new Supervisor(context);

        cacheCreateTimeStamp = 0;
        cacheVerifyTimeStamp = 0;
        wifiUpdateTimeStamp = 0;
        isRunning = false;

        initConfiguration();
    }

    private void initConfiguration() {
        configuration = new JSONObject();
        try {
            configuration.put("WIFIScannerEnable", true);
            configuration.put("BluetoothScannerEnable", true);
            configuration.put("IMUSensorEnable", true);
            configuration.put("OrientationAngleSensorEnable", true);
            configuration.put("LocalNetworkSearcherEnable", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setConfiguration(JSONObject configuration) {
        this.configuration = configuration;
    }

    public void setTrigger(NormalTrigger normalTrigger) {
        Log.i(TAG, "setTrigger: ");
        this.normalTrigger = normalTrigger;
        this.normalTrigger.addCallback((start, triggerType) -> {
            if (start) {
                if (triggerType == NormalTrigger.TRIGGER_TYPE_FORCE) {
                    Log.i(TAG, "setTrigger: Triggered by force.");
                    LoggerUtility.getInstance().addTriggerLog(context, "触发器 - 触发 - 强制");
                    start(defaultTimeout);
                }
                if (triggerType == NormalTrigger.TRIGGER_TYPE_MOTION) {
                    Log.i(TAG, "setTrigger: Triggered by motion.");
                    LoggerUtility.getInstance().addTriggerLog(context, "触发器 - 触发 - 运动");
                    if (System.currentTimeMillis() - cacheCreateTimeStamp > minCollectInterval / 2) {
                        Log.i(TAG, "setTrigger: Reached minCollectInterval/2.");
                        start(defaultTimeout);
                    } else {
                        Log.i(TAG, "setTrigger: Not reached minCollectInterval/2.");
                        LoggerUtility.getInstance().addTriggerLog(context, "受收集传感器数据的最小间隔时间限制 不收集");
                    }

                }
                if (triggerType == NormalTrigger.TRIGGER_TYPE_SCREEN) {
                    Log.i(TAG, "setTrigger: Triggered by screen.");
                    LoggerUtility.getInstance().addTriggerLog(context, "触发器 - 触发 - 屏幕");
                    if (System.currentTimeMillis() - cacheCreateTimeStamp > minCollectInterval) {
                        Log.i(TAG, "setTrigger: Reached minCollectInterval.");
                        start(defaultTimeout);
                    } else {
                        Log.i(TAG, "setTrigger: Not reached minCollectInterval.");
                        LoggerUtility.getInstance().addTriggerLog(context, "受收集传感器数据的最小间隔时间限制 不收集");
                    }
                }
            } else {
                Log.i(TAG, "setTrigger: Triggered by screen off.");
                LoggerUtility.getInstance().addTriggerLog(context, "触发器 - 触发 - 屏幕关闭 停止收集");
                stop();
            }
        });
    }

    public void setRecorder(NormalRecorder normalRecorder) {
        Log.i(TAG, "setRecorder: ");
        this.normalRecorder = normalRecorder;
    }

    /**
     * 开始分析情境 即 开始收集各类传感器数据 当传感器数据更新时 计算情境 调用已注册的回调函数
     *
     * @param timeout 分析超时时间 单位为毫秒
     */
    public void start(long timeout) {
        if (!isRunning) {
            Log.i(TAG, "start: ");
            LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "开始收集位置信息");

            if (!supervisor.needRecollectSensorData(currentScenario)) {
                Log.i(TAG, "start: Supervisor said no need recollect sensor data.");
                LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "管理员表示无需重新收集位置信息");
                cacheVerifyTimeStamp = System.currentTimeMillis();
                if (cacheVerifyTimeStamp - cacheCreateTimeStamp < maxValidTime) {
                    LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "数据经检测 处于缓存传感器数据的最大有效时间内 因此不收集");
                    return;
                } else {
                    LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "数据经检测 不处于缓存传感器数据的最大有效时间内 因此收集");
                }
            }

            isRunning = true;
            cacheCreateTimeStamp = System.currentTimeMillis();
            cacheVerifyTimeStamp = cacheCreateTimeStamp;

            bluetoothScanner.addCallback((deviceSet, beaconSet, bluetoothList) -> {
                Log.i(TAG, "start: Bluetooth Updated.");
                LoggerUtility.getInstance().addAnalyzeScenarioLog(context, String.format(Locale.CHINA, "蓝牙信息更新 - %d - %d - %d", deviceSet.size(), beaconSet.size(), bluetoothList.size()));
                currentScenario.deviceSet = deviceSet;
                currentScenario.beaconSet = beaconSet;
                currentScenario.bluetoothList = bluetoothList;
//                analyzerSpecialBluetooth(currentScenario);
                for (AnalyzerCallback callback : analyzerCallbackList) {
                    callback.onChanged(currentScenario);
                }
            });
            try {
                if (configuration.getBoolean("WIFIScannerEnable")) {
                    bluetoothScanner.start();
                }
            } catch (JSONException e) {
                Log.e(TAG, "start: ", e);
                e.printStackTrace();
            }

            wifiScanner.addCallback((isLatest, scanResultList, wifiList) -> {
                Log.i(TAG, "start: WIFI Updated.");
                wifiUpdateTimeStamp = System.currentTimeMillis();
                LoggerUtility.getInstance().addAnalyzeScenarioLog(context, String.format(Locale.CHINA, "无线局域网信息更新 - %d", scanResultList.size()));
                if (isLatest) {
                    currentScenario.wifiScanResultStatus = Scenario.RESULT_LATEST;
                    LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "无线局域网信息是最新的");
                } else {
                    currentScenario.wifiScanResultStatus = Scenario.RESULT_CACHED;
                    LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "无线局域网信息不是最新的");
                }
                currentScenario.wifiScanResultList = scanResultList;
                currentScenario.wifiList = wifiList;
//                analyzerSpecialWIFI(currentScenario);
                for (AnalyzerCallback callback : analyzerCallbackList) {
                    callback.onChanged(currentScenario);
                }
            });
            try {
                if (configuration.getBoolean("BluetoothScannerEnable")) {
                    wifiScanner.start();
                }
            } catch (JSONException e) {
                Log.e(TAG, "start: ", e);
                e.printStackTrace();
            }

            imuSensor.addCallback(() -> {
                Log.i(TAG, "start: IMU Updated.");
                currentScenario.tookOneStep = true;
                for (AnalyzerCallback callback : analyzerCallbackList) {
                    callback.onChanged(currentScenario);
                }
            });
            try {
                if (configuration.getBoolean("IMUSensorEnable")) {
                    imuSensor.start();
                }
            } catch (JSONException e) {
                Log.e(TAG, "start: ", e);
                e.printStackTrace();
            }

            try {
                if (configuration.getBoolean("OrientationAngleSensorEnable")) {
                    orientationAngleSensor.start();
                }
            } catch (JSONException e) {
                Log.e(TAG, "start: ", e);
                e.printStackTrace();
            }

            localNetworkSearcher.addCallback((deviceUnderNetworkList) -> {
                Log.i(TAG, "start: DeviceUnderNetwork Updated.");
                LoggerUtility.getInstance().addAnalyzeScenarioLog(context, String.format(Locale.CHINA, "无线局域网内设备信息更新 - %d", deviceUnderNetworkList.size()));
                currentScenario.deviceUnderNetworkList = deviceUnderNetworkList;
                currentScenario.internetIP = localNetworkSearcher.getHostIP();
                for (AnalyzerCallback callback : analyzerCallbackList) {
                    callback.onChanged(currentScenario);
                }
            });
            try {
                if (configuration.getBoolean("LocalNetworkSearcherEnable")) {
                    localNetworkSearcher.start();
                }
            } catch (JSONException e) {
                Log.e(TAG, "start: ", e);
                e.printStackTrace();
            }

            new Handler().postDelayed(this::stop, timeout);
        } else {
            Log.i(TAG, "start: Already started.");
        }
    }

    /**
     * 停止分析情境 移除向各类传感器注册的回调函数 避免下次开始分析情境时注册多个同类型回调函数
     * 注意 此时并没有移除其他类向自身注册的回调函数 便于下次开始继续分析情境
     */
    public void stop() {
        if (isRunning) {
            bluetoothScanner.stop();
            bluetoothScanner.removeAllCallbacks();

            wifiScanner.stop();
            wifiScanner.removeAllCallbacks();

            imuSensor.stop();
            imuSensor.removeAllCallbacks();

            orientationAngleSensor.stop();

            localNetworkSearcher.stop();
            localNetworkSearcher.removeAllCallbacks();

            readSensor();

            for (AnalyzerCallback callback : analyzerCallbackList) {
                callback.onFinished(currentScenario);
            }

            isRunning = false;
            Log.i(TAG, "stop: ");
            LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "结束收集位置信息");
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isWIFIUpdated() {
        return wifiUpdateTimeStamp > cacheCreateTimeStamp;
    }

    private void readSensor() {
        currentScenario.orientationAngles = orientationAngleSensor.read();
    }

    public void recognizePosition() {
        Log.i(TAG, "recognizePosition: ");
        LoggerUtility.getInstance().addRecognizePositionLog(context, "开始识别位置");
        currentScenario.recognizedPositionStatus = Scenario.STATUS_UNFINISHED;

        JSONArray positionProfileArray = PositionProfileUtility.getInstance().getPositionProfileArray(context);
        if (positionProfileArray.length() == 0) {
            currentScenario.recognizedPositionStatus = Scenario.PROBLEM_MISSING_INFORMATION;
            String message = "历史位置信息文件列表为空";
            NotificationUtility.sendShortToast(context, message);
            LoggerUtility.getInstance().addRecognizePositionLog(context, message);
            return;
        }

        if (currentScenario.wifiScanResultStatus == Scenario.STATUS_UNFINISHED) {
            String message = "未能完成无线局域网络信息的扫描过程";
            NotificationUtility.sendShortToast(context, message);
            LoggerUtility.getInstance().addRecognizePositionLog(context, message);
        }

        if (currentScenario.wifiScanResultStatus == Scenario.RESULT_CACHED) {
            String message = "未能获取最新无线局域网络信息";
            NotificationUtility.sendShortToast(context, message);
            LoggerUtility.getInstance().addRecognizePositionLog(context, message);
        }

        if (currentScenario.wifiList.isEmpty()) {
            String message = "未能获取任何无线局域网络信息";
            NotificationUtility.sendShortToast(context, message);
            LoggerUtility.getInstance().addRecognizePositionLog(context, message);
            currentScenario.recognizedPositionStatus = Scenario.RESULT_INVALID;
            return;
        }

        Set<String> currentWIFIBSSIDSet = new HashSet<>();
        LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "WIFI Detail (%d) is as followed.", currentScenario.wifiList.size()));
        for (WIFIScanner.WIFIInformation wi : currentScenario.wifiList) {
            currentWIFIBSSIDSet.add(wi.bssid);
            LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "%s - %s", wi.ssid, wi.bssid));
        }

        Set<String> currentBluetoothAddressSet = new HashSet<>();
        LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "Bluetooth Detail (%d) is as followed.", currentScenario.bluetoothList.size()));
        for (BluetoothScanner.BluetoothInformation bi : currentScenario.bluetoothList) {
            currentBluetoothAddressSet.add(bi.address);
            LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "%s - %s - %s", bi.name, bi.address, bi.type));
        }

        Set<String> currentOthersMarkSet = new HashSet<>();

        double maxSimilarity = 0.0;
        JSONObject mostLikelyPositionProfile = new JSONObject();

        for (int i = 0; i < positionProfileArray.length(); i++) {
            try {
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "开始与 %s 比对", positionProfileArray.getJSONObject(i).getString("name")));

                JSONArray wifiDetailArray = positionProfileArray.getJSONObject(i).getJSONArray("wifi");
                JSONArray bluetoothDetailArray = positionProfileArray.getJSONObject(i).getJSONArray("bluetooth");
                JSONArray othersDetailArray = positionProfileArray.getJSONObject(i).getJSONArray("others");

                double currentMatchPoint = 0.0;
                double currentTotalPoint = (double) wifiDetailArray.length() + (double) bluetoothDetailArray.length() / 2;
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "Total Point %f", currentTotalPoint));
                if (currentTotalPoint == 0) {
                    continue;
                }

                for (int j = 0; j < wifiDetailArray.length(); j++) {
                    String bssid = wifiDetailArray.getJSONObject(j).getString("bssid");
                    if (currentWIFIBSSIDSet.contains(bssid)) {
                        currentMatchPoint += 1.0;
                    }
                }
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "WIFI Match Point %f", currentMatchPoint));

                for (int k = 0; k < bluetoothDetailArray.length(); k++) {
                    String address = bluetoothDetailArray.getJSONObject(k).getString("address");
                    if (currentBluetoothAddressSet.contains(address)) {
                        currentMatchPoint += 0.5;
                    }
                }
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "WIFI & Bluetooth Match Point %f", currentMatchPoint));

                for (int l = 0; l < othersDetailArray.length(); l++) {
                    // TODO: Check others.
                }
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "WIFI & Bluetooth & Others Match Point %f", currentMatchPoint));

                double currentSimilarity = currentMatchPoint / currentTotalPoint;
                if (currentSimilarity > maxSimilarity) {
                    maxSimilarity = currentSimilarity;
                    mostLikelyPositionProfile = positionProfileArray.getJSONObject(i);
                }
                Log.i(TAG, "recognizePosition: " + positionProfileArray.getJSONObject(i).getString("name") + " - " + currentSimilarity);
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "Similarity %s - %f", positionProfileArray.getJSONObject(i).getString("name"), currentSimilarity));
            } catch (JSONException e) {
                Log.e(TAG, "recognizePosition: ", e);
                e.printStackTrace();
                LoggerUtility.getInstance().addRecognizePositionLog(context, e.toString().replace("\n", " - "));
            }
        }

        try {
            if (maxSimilarity >= 0.3) {
                currentScenario.recognizedPositionStatus = Scenario.RESULT_FOUND;
                currentScenario.recognizedPositionProfile = mostLikelyPositionProfile;
                currentScenario.recognizedPositionConfidence = maxSimilarity;
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "结束识别位置 - 找到 %s %f", mostLikelyPositionProfile.getString("name"), maxSimilarity));
            } else {
                currentScenario.recognizedPositionStatus = Scenario.RESULT_NOT_FOUND;
                LoggerUtility.getInstance().addRecognizePositionLog(context, String.format(Locale.CHINA, "结束识别位置 - 未找到 %s %f", mostLikelyPositionProfile.getString("name"), maxSimilarity));
            }
        } catch (JSONException e) {
            Log.e(TAG, "recognizePosition: ", e);
            e.printStackTrace();
        }
    }

    public void recordPosition() {
        normalRecorder.recordPosition(currentScenario);
    }

    public void setCurrentScenarioName(String name) {
        currentScenario.name = name;
    }

    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    public boolean addCallback(AnalyzerCallback callback) {
        return analyzerCallbackList.add(callback);
    }

    public boolean removeCallback(AnalyzerCallback callback) {
        return analyzerCallbackList.remove(callback);
    }

    public void removeAllCallbacks() {
        analyzerCallbackList.clear();
    }

    public void analyzerSpecialBluetooth(Scenario scenario) {
        for (int i = 0; i < scenario.bluetoothList.size(); i++) {
            BluetoothScanner.BluetoothInformation bi = scenario.bluetoothList.get(i);
            if (bi.deviceType.equals("default")) {
                JSONArray positionProfileArray = PositionProfileUtility.getInstance().getPositionProfileArray(context);
                for (int j = 0; j < positionProfileArray.length(); j++) {
                    boolean foundDeviceType = false;
                    try {
                        JSONArray bluetoothDetailArray = positionProfileArray.getJSONObject(j).getJSONArray("bluetooth");
                        for (int k = 0; k < bluetoothDetailArray.length(); k++) {
                            String address = bluetoothDetailArray.getJSONObject(k).getString("address");
                            String deviceType = bluetoothDetailArray.getJSONObject(k).getString("deviceType");
                            if (bi.address.equals(address) && !deviceType.equals("default")) {
                                scenario.bluetoothList.set(i, new BluetoothScanner.BluetoothInformation(bi, deviceType));
                                foundDeviceType = true;
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "analyzerSpecialBluetooth: ", e);
                        e.printStackTrace();
                    }
                    if (foundDeviceType) {
                        break;
                    }
                }
            }
        }
    }

    public void analyzerSpecialWIFI(Scenario scenario) {
        for (int i = 0; i < scenario.wifiList.size(); i++) {
            WIFIScanner.WIFIInformation wi = scenario.wifiList.get(i);
            if (wi.deviceType.equals("default")) {
                JSONArray positionProfileArray = PositionProfileUtility.getInstance().getPositionProfileArray(context);
                for (int j = 0; j < positionProfileArray.length(); j++) {
                    boolean foundDeviceType = false;
                    try {
                        JSONArray wifiDetailArray = positionProfileArray.getJSONObject(j).getJSONArray("wifi");
                        for (int k = 0; k < wifiDetailArray.length(); k++) {
                            String bssid = wifiDetailArray.getJSONObject(k).getString("bssid");
                            String deviceType = wifiDetailArray.getJSONObject(k).getString("deviceType");
                            if (wi.bssid.equals(bssid) && !deviceType.equals("default")) {
                                scenario.wifiList.set(i, new WIFIScanner.WIFIInformation(wi, deviceType));
                                foundDeviceType = true;
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "analyzerSpecialWIFI: ", e);
                        e.printStackTrace();
                    }
                    if (foundDeviceType) {
                        break;
                    }
                }
            }
        }
    }

    public void markSpecialWIFI(Scenario scenario, int index, String mark) {
        WIFIScanner.WIFIInformation wi = new WIFIScanner.WIFIInformation(scenario.wifiList.get(index), mark);
        scenario.wifiList.set(index, wi);
    }

    public void markSpecialBluetooth(Scenario scenario, int index, String mark) {
        BluetoothScanner.BluetoothInformation bi = new BluetoothScanner.BluetoothInformation(scenario.bluetoothList.get(index), mark);
        scenario.bluetoothList.set(index, bi);
    }

    public interface AnalyzerCallback {
        void onChanged(Scenario scenario);

        void onFinished(Scenario scenario);
    }


}
