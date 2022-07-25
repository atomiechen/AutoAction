package com.hcifuture.contextactionlibrary.position.trigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.util.Log;

import com.hcifuture.contextactionlibrary.position.utility.LoggerUtility;

import java.util.ArrayList;
import java.util.List;

public class NormalTrigger {
    public static final int TRIGGER_TYPE_FORCE = 100;
    public static final int TRIGGER_TYPE_SCREEN = 101;
    public static final int TRIGGER_TYPE_MOTION = 102;

    private static final String TAG = "NormalTrigger";
    private final Context context;

    private final List<TriggerCallback> triggerCallbackList = new ArrayList<>();

    private BroadcastReceiver screenStatusBroadcastReceiver;

    private boolean isRunning;

    private TriggerEventListener significantMotionTriggerEventListener;
    private SensorManager sensorManager;
    private Sensor significantMotionSensor;

    private boolean haveSignificantMotion;
    private boolean haveScreen;

    public NormalTrigger(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");
        removeAllCallbacks();

        significantMotionTriggerEventListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                Log.i(TAG, "trigger: Significant motion event occurred.");
                haveSignificantMotion = true;
                if (haveScreen) {
                    LoggerUtility.getInstance().addTriggerLog(context, "发生了显著运动 - 同时屏幕亮起");
                    for (TriggerCallback callback : triggerCallbackList) {
                        callback.onTriggered(true, TRIGGER_TYPE_MOTION);
                    }
                    haveSignificantMotion = false;
                    sensorManager.requestTriggerSensor(significantMotionTriggerEventListener, significantMotionSensor);
                } else {
                    LoggerUtility.getInstance().addTriggerLog(context, "发生了显著运动 - 同时屏幕关闭");
                }
            }
        };
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        significantMotionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        screenStatusBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    Log.i(TAG, "trigger: Screen is on.");
                    haveScreen = true;
                    if (haveSignificantMotion) {
                        LoggerUtility.getInstance().addTriggerLog(context, "屏幕亮起 - 之前发生了显著运动");
                        for (TriggerCallback callback : triggerCallbackList) {
                            callback.onTriggered(true, TRIGGER_TYPE_MOTION);
                        }
                        haveSignificantMotion = false;
                    } else {
                        LoggerUtility.getInstance().addTriggerLog(context, "屏幕亮起 - 之前没有发生显著运动");
                        for (TriggerCallback callback : triggerCallbackList) {
                            callback.onTriggered(true, TRIGGER_TYPE_SCREEN);
                        }
                    }
                }
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    Log.i(TAG, "trigger: Screen is off.");
                    haveScreen = false;
                    LoggerUtility.getInstance().addTriggerLog(context, "屏幕关闭");
                    for (TriggerCallback callback : triggerCallbackList) {
                        callback.onTriggered(false, TRIGGER_TYPE_SCREEN);
                    }
                }
                sensorManager.requestTriggerSensor(significantMotionTriggerEventListener, significantMotionSensor);
            }
        };
        haveSignificantMotion = false;
        haveScreen = false;
        isRunning = false;
    }

    /**
     * 开始触发
     */
    public void start() {
        if (!isRunning) {
            Log.i(TAG, "start: ");
            LoggerUtility.getInstance().addTriggerLog(context, "触发器开始工作");
            isRunning = true;
            sensorManager.requestTriggerSensor(significantMotionTriggerEventListener, significantMotionSensor);
            IntentFilter screenOnIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            IntentFilter screenOffIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            context.registerReceiver(screenStatusBroadcastReceiver, screenOnIntentFilter);
            context.registerReceiver(screenStatusBroadcastReceiver, screenOffIntentFilter);

            for (TriggerCallback callback : triggerCallbackList) {
                callback.onTriggered(true, TRIGGER_TYPE_FORCE);
            }
        } else {
            Log.i(TAG, "start: Already started.");
        }
    }

    /**
     * 停止触发 注意此处不移除已注册的回调函数 后期可以再次开始触发
     */
    public void stop() {
        if (isRunning) {
            sensorManager.cancelTriggerSensor(significantMotionTriggerEventListener, significantMotionSensor);
            context.unregisterReceiver(screenStatusBroadcastReceiver);
            isRunning = false;
            Log.i(TAG, "stop: ");
            LoggerUtility.getInstance().addTriggerLog(context, "触发器停止工作");
        }
    }

    public boolean addCallback(TriggerCallback callback) {
        return triggerCallbackList.add(callback);
    }

    public boolean removeCallback(TriggerCallback callback) {
        return triggerCallbackList.remove(callback);
    }

    public void removeAllCallbacks() {
        triggerCallbackList.clear();
    }

    public interface TriggerCallback {
        void onTriggered(boolean start, int triggerType);
    }
}
