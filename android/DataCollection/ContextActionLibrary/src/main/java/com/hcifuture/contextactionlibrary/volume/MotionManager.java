package com.hcifuture.contextactionlibrary.volume;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.hcifuture.contextactionlibrary.sensor.data.SingleIMUData;

public class MotionManager extends TriggerManager {

    static final String TAG = "MotionManager";

    private final float[] accMark = new float[3];
    private final float[] magMark = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final int ORIENTATION_CHECK_NUMBER = 10;
    private final float[][] orientationMark = new float[ORIENTATION_CHECK_NUMBER][3];

    private int linearStaticCount = 0;
    private int gyroStaticCount = 0;

    private boolean isFaceDown = false;

    public MotionManager(VolEventListener volEventListener) {
        super(volEventListener);
    }

    public void onIMUSensorEvent(SingleIMUData data) {
        switch (data.getType()) {
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                checkIsStatic(data);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accMark[0] = data.getValues().get(0);
                accMark[1] = data.getValues().get(1);
                accMark[2] = data.getValues().get(2);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magMark[0] = data.getValues().get(0);
                magMark[1] = data.getValues().get(1);
                magMark[2] = data.getValues().get(2);
                updateOrientationAngles();
                break;
            default:
                break;
        }
    }

    private void checkIsStatic(SingleIMUData data) {
        float linearAccThreshold = 0.05f;
        float gyroThreshold = 0.02f;
        // linear acc
        if (data.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (Math.abs(data.getValues().get(0)) <= linearAccThreshold && Math.abs(data.getValues().get(1)) <= linearAccThreshold)
                linearStaticCount = Math.min(20, linearStaticCount + 1);
            else
                linearStaticCount = Math.max(0, linearStaticCount - 1);
        }
        // gyro
        else {
            if (Math.abs(data.getValues().get(0)) <= gyroThreshold && Math.abs(data.getValues().get(1)) <= gyroThreshold && Math.abs(data.getValues().get(2)) <= gyroThreshold)
                gyroStaticCount = Math.min(40, gyroStaticCount + 1);
            else
                gyroStaticCount = Math.max(0, gyroStaticCount - 1);
        }
        getContext();
    }

    private boolean checkIsHorizontal() {
        for (int i = 0; i < ORIENTATION_CHECK_NUMBER; i++)
            if (Math.abs(orientationMark[i][1]) > 0.1 || Math.abs(orientationMark[i][2]) < 3.0)
                return false;
        return true;
    }

    private void updateOrientationAngles() {
//        Log.e(TAG, "updateOrientationAngles: " + orientationAngles[0] + "," + orientationAngles[1] + "," + orientationAngles[2]);
        SensorManager.getRotationMatrix(rotationMatrix, null, accMark, magMark);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        for (int i = 0; i < ORIENTATION_CHECK_NUMBER - 1; i++)
            System.arraycopy(orientationMark[i + 1], 0, orientationMark[i], 0, 3);
        System.arraycopy(orientationAngles, 0, orientationMark[ORIENTATION_CHECK_NUMBER - 1], 0, 3);
    }

    public void getContext() {
//        Log.e(TAG, "getContext: checkIsHorizontal=" + checkIsHorizontal()
//                + " linearStaticCount=" + linearStaticCount
//                + " gyroStaticCount=" + gyroStaticCount);

        // Honor V40 Lite has no linear acceleration
        if (gyroStaticCount > 20 && checkIsHorizontal()) {
//        if (linearStaticCount > 10 && gyroStaticCount > 20 && checkIsHorizontal()) {
            if (isFaceDown)
                return;
            isFaceDown = true;
            Bundle bundle = new Bundle();
            bundle.putString("motion", "faceDown");
            volEventListener.onVolEvent(VolEventListener.EventType.Motion, bundle);
        } else {
            if (!isFaceDown)
                return;
            isFaceDown = false;
            Bundle bundle = new Bundle();
            bundle.putString("motion", "notFaceDown");
            volEventListener.onVolEvent(VolEventListener.EventType.Motion, bundle);
        }
    }
}
