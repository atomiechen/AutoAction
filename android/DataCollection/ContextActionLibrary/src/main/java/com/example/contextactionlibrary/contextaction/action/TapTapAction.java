package com.example.contextactionlibrary.contextaction.action;

import android.content.Context;
import android.hardware.SensorEvent;
import android.util.Log;

import com.example.contextactionlibrary.data.Preprocess;
import com.example.contextactionlibrary.utils.imu.TfClassifier;
import com.example.contextactionlibrary.utils.imu.Util;
import com.example.ncnnlibrary.communicate.config.ActionConfig;
import com.example.ncnnlibrary.communicate.listener.ActionListener;
import com.example.ncnnlibrary.communicate.listener.RequestListener;
import com.example.ncnnlibrary.communicate.result.ActionResult;


import java.util.ArrayList;
import java.util.List;

public class TapTapAction extends BaseAction {

    private String TAG = "TapTapAction";

    private List<Long> tapTimestamps = new ArrayList();
    private TfClassifier tflite;

    private Preprocess preprocess;

    private int seqLength;

    public TapTapAction(Context context, ActionConfig config, RequestListener requestListener, List<ActionListener> actionListener) {
        super(context, config, requestListener, actionListener);
        tflite = new TfClassifier(mContext.getAssets(), "tap7cls_pixel4.tflite");
        preprocess = Preprocess.getInstance();
        seqLength = (int)config.getValue("SeqLength");
    }

    @Override
    public synchronized void start() {
        if (isStarted) {
            Log.d(TAG, "Action is already started.");
            return;
        }
        isStarted = true;
    }

    @Override
    public synchronized void stop() {
        if (!isStarted) {
            Log.d(TAG, "Action is already stopped");
            return;
        }
        isStarted = false;
    }

    @Override
    public void onIMUSensorChanged(SensorEvent event) {
        // TODO: Split the logic of Preprocess here
    }

    @Override
    public void onProximitySensorChanged(SensorEvent event) {

    }

    private ArrayList<Float> getInput(int accIdx, int gyroIdx) {
        ArrayList<Float> res = new ArrayList<>();
        addFeatureData(preprocess.getXsAcc(), accIdx, 1, res);
        addFeatureData(preprocess.getYsAcc(), accIdx, 1, res);
        addFeatureData(preprocess.getZsAcc(), accIdx, 1, res);
        addFeatureData(preprocess.getXsGyro(), gyroIdx, 10, res);
        addFeatureData(preprocess.getYsGyro(), gyroIdx, 10, res);
        addFeatureData(preprocess.getZsGyro(), gyroIdx, 10, res);
        return res;
    }

    private void addFeatureData(List<Float> list, int startIdx, int scale, List<Float> res) {
        for (int i = 0; i < seqLength; i++) {
            if (i + startIdx >= list.size())
                res.add(0.0F);
            else
                res.add(scale * list.get(i + startIdx));
        }
    }
    
    private boolean checkDoubleTapTiming(long timestamp) {
        // remove old timestamps
        int idx = 0;
        for (; idx < tapTimestamps.size(); idx++) {
            if (timestamp - tapTimestamps.get(idx) <= 500000000L)
                break;
        }
        tapTimestamps = tapTimestamps.subList(idx, tapTimestamps.size());

        // just check no tap && double tap now
        if (!tapTimestamps.isEmpty()) {
            if (tapTimestamps.get(tapTimestamps.size() - 1) - tapTimestamps.get(0) > 100000000L) {
                tapTimestamps.clear();
                return true;
            }
        }
        return false;
    }

    @Override
    public void getAction() {
        int[] idxes = preprocess.shouldRunTapModel(seqLength);
        if (!isStarted || idxes[0] == -1) {
            return;
        }
        ArrayList<Float> input = getInput(idxes[0], idxes[1]);
        int result = Util.getMaxId((ArrayList)tflite.predict(input, 7).get(0));
        long timestamp = preprocess.getTimestamps().get(seqLength);
        if (result == 1) {
            tapTimestamps.add(timestamp);
            if (actionListener != null) {
                if (checkDoubleTapTiming(timestamp)) {
                    for (ActionListener listener: actionListener) {
                        listener.onAction(new ActionResult("TapTap"));
                    }
                }
            }
        }
    }
}