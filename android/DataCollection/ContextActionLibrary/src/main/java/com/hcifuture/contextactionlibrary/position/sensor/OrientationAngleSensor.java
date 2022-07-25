package com.hcifuture.contextactionlibrary.position.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class OrientationAngleSensor {
    private static final String TAG = "OrientationAngleSensor";

    private final Context context;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[][] orientationAngles = new float[10][3];
    private int round = 0;

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;

    private boolean isRunning;

    public OrientationAngleSensor(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
                }
                round = (round + 1) % 10;
                SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
                SensorManager.getOrientation(rotationMatrix, orientationAngles[round]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.i(TAG, "onAccuracyChanged: To " + accuracy);
            }
        };
        isRunning = false;
    }

    public void start() {
        if (!isRunning) {
            Log.i(TAG, "start: ");
            isRunning = true;
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }
            Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (magneticField != null) {
                sensorManager.registerListener(sensorEventListener, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }
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

    public float[] read() {
        // TODO: Get average angles.
        return orientationAngles[round];
    }
}
