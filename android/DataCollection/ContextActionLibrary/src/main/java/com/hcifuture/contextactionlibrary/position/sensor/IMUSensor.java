package com.hcifuture.contextactionlibrary.position.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class IMUSensor {
    private static final String TAG = "IMUSensor";

    private final Context context;
    private final List<SensorCallback> sensorCallbackList = new ArrayList<>();
    private SensorManager sensorManager;
    private Sensor stepDetectorSensor;
    private SensorEventListener sensorEventListener;
    private boolean isRunning;

    public IMUSensor(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[0] == 1.0f) {
                    for (SensorCallback callback : sensorCallbackList) {
                        callback.onChanged();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.i(TAG, "onAccuracyChanged: To " + accuracy);
            }
        };
        removeAllCallbacks();
        isRunning = false;
    }

    public void start() {
        if (!isRunning) {
            Log.i(TAG, "start: ");
            isRunning = true;
            sensorManager.registerListener(sensorEventListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Log.i(TAG, "start: Already started.");
        }
    }

    public void stop() {
        if (isRunning) {
            sensorManager.unregisterListener(sensorEventListener);
            isRunning = false;
            Log.i(TAG, "stop: ");
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean addCallback(SensorCallback callback) {
        return sensorCallbackList.add(callback);
    }

    public boolean removeCallback(SensorCallback callback) {
        return sensorCallbackList.remove(callback);
    }

    public void removeAllCallbacks() {
        sensorCallbackList.clear();
    }

    public interface SensorCallback {
        void onChanged();
    }
}
