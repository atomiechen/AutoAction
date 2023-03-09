package com.hcifuture.contextactionlibrary.volume;

import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.contextaction.ContextActionContainer;
import com.hcifuture.contextactionlibrary.sensor.data.SingleIMUData;
import com.hcifuture.contextactionlibrary.status.Heart;
import com.hcifuture.shared.communicate.listener.ActionListener;
import com.hcifuture.shared.communicate.result.ActionResult;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

public class ActivityManager extends TriggerManager {

    static final String TAG = "ActivityManager";

    public static String ACTION_STATIC = "action.static.action";
    public static String ACTION_WALKING = "action.walking.action";
    public static String ACTION_RUNNING = "action.running.action";
    public static String ACTION_CYCLING = "action.cycling.action";
    public static String ACTION_OTHERS = "action.others.action";

    private Module imuModule = null;
    private String prevActivity = ACTION_OTHERS;

    private static int seqLength = 500;
    private float[] imuInput = new float[seqLength * 6];
    private long[] imuSize = new long[]{1, 6, seqLength};
    private long[] lastTime = new long[2];
    private int skipNum = seqLength;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ActivityManager(VolEventListener volEventListener) {
        super(volEventListener);
        try {
            imuModule = LiteModuleLoader.load(ContextActionContainer.getSavePath() + "activity.ptl");
            Log.i(TAG, "Model load succeed");
        } catch (Exception e) {
            Log.e(TAG, "Model load fail: " + e);
            e.printStackTrace();
        }
        prevActivity = ACTION_OTHERS;
    }

    public void onIMUSensorEvent(SingleIMUData data) {
        if (data.getType() != Sensor.TYPE_GYROSCOPE && data.getType() != Sensor.TYPE_LINEAR_ACCELERATION)
            return;
        int idx = (data.getType() == Sensor.TYPE_LINEAR_ACCELERATION) ? 0 : 1;
        if (data.getTimestamp() < lastTime[idx] + 3 * 1e6)
            return;
        lastTime[idx] = data.getTimestamp();
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < seqLength - 1; j++)
                imuInput[i * seqLength + j] = imuInput[i * seqLength + j + 1];
        imuInput[(3 * idx + 1) * seqLength - 1] = data.getValues().get(0);
        imuInput[(3 * idx + 2) * seqLength - 1] = data.getValues().get(1);
        imuInput[(3 * idx + 3) * seqLength - 1] = data.getValues().get(2);
        if (idx == 0) {
            if (skipNum > 0) {
                skipNum--;
            } else {
                skipNum = seqLength;
                recognize();
            }
        }
    }

    private void recognize() {
        Tensor imuInputTensor = Tensor.fromBlob(imuInput, imuSize);
        Tensor imuOutputTensor = imuModule.forward(IValue.from(imuInputTensor)).toTensor();
        float[] imuScores = imuOutputTensor.getDataAsFloatArray();
        int activity = 0;
        for (int i = 1; i < imuScores.length; i++) {
            if (imuScores[i] > imuScores[activity])
                activity = i;
        }
        Log.i(TAG, "Label: " + activity);
        String curActivity = ACTION_OTHERS;
        if (activity <= 3 || activity == 15 || activity == 16)
            curActivity = ACTION_WALKING;
        else if (activity == 4 || activity == 17)
            curActivity = ACTION_RUNNING;
        else if (activity == 5)
            curActivity = ACTION_CYCLING;
        else if (activity == 11 || activity == 12 || activity == 14)
            curActivity = ACTION_STATIC;
        if (prevActivity != curActivity) {
            prevActivity = curActivity;
            Bundle bundle = new Bundle();
            bundle.putString("activity", curActivity);
            volEventListener.onVolEvent(VolEventListener.EventType.Activity, bundle);
        }
        Log.i(TAG, curActivity);
    }

    public String getActivity() {
        return prevActivity;
    }
}
