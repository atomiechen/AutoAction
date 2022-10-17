package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.hcifuture.contextactionlibrary.sensor.collector.async.IMUCollector;
import com.hcifuture.contextactionlibrary.sensor.data.IMUData;
import com.hcifuture.contextactionlibrary.sensor.data.NonIMUData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleIMUData;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.contextactionlibrary.utils.FileSaver;
import com.hcifuture.contextactionlibrary.utils.FileUtils;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.RequiresApi;

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
    private boolean isFaceUp = false;

    private final ScheduledExecutorService scheduledExecutorService;
    private final List<ScheduledFuture<?>> futureList;
    private final IMUCollector imuCollector;

    private ScheduledFuture<?> scheduledIMU;
    private final String FILE_DIR;
    private final AtomicInteger mFileIDCounter = new AtomicInteger(0);
    private String mCurrentFilename;
    private int mCurrentFileID;
    private final int intervalFile = 30000; // save IMU file every 30s

    private long offsetInNano = 0;
    private int samplePoints = 0;
    private int stableCount = 0;

    public MotionManager(VolEventListener volEventListener, Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, IMUCollector imuCollector) {
        super(volEventListener);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.imuCollector = imuCollector;

        // 放在Data/Click/下，Uploader会监听文件夹、自动重传
        FILE_DIR = context.getExternalMediaDirs()[0].getAbsolutePath() + "/Data/Click/IMU_Continuous/";
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void start() {
        super.start();
        scheduledIMU = scheduledExecutorService.schedule(() -> {
            // periodically record IMU data
            while (true) {
                try {
                    Thread.sleep(intervalFile);

                    // update file ID
                    mCurrentFileID = mFileIDCounter.getAndIncrement();
                    String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    mCurrentFilename = FILE_DIR + "IMU_" + dateTime + "_" + mCurrentFileID + ".bin";
                    Log.e(TAG, "recording to " + mCurrentFilename);

                    long start_file_time = System.currentTimeMillis();

                    imuCollector.getData(new TriggerConfig().setImuGetAll(true)).thenCompose(v -> {
                        long end_file_time = System.currentTimeMillis();
                        v.getExtras().putLong("offset_in_nano", getOffsetInNano());
                        // check if empty file
                        if (((IMUData) v.getData()).getData().size() > 0) {
                            return FileSaver.getInstance().writeIMUDataToFile(v, new File(mCurrentFilename)).thenAccept(v1 -> {
                                // upload current file
                                volEventListener.upload(mCurrentFilename, start_file_time, end_file_time, "Volume_IMU", "", v.getExtras());
                                JSONObject json = new JSONObject();
                                JSONUtils.jsonPut(json, "imu_filename", mCurrentFilename);
                                JSONUtils.jsonPut(json, "imu_file_start_time", start_file_time);
                                JSONUtils.jsonPut(json, "imu_file_end_time", end_file_time);
                                JSONUtils.jsonPut(json, "imu_file_offset_in_nano", v.getExtras().getLong("offset_in_nano"));
                                volEventListener.recordEvent(VolEventListener.EventType.IMU, "imu_upload", json.toString());
                            });
                        } else {
                            // empty file, reset file ID
                            mFileIDCounter.getAndDecrement();
                            // remove file
                            FileUtils.deleteFile(new File(mCurrentFilename), "");
                            return CompletableFuture.completedFuture(null);
                        }
                    }).get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    // error happens, reset file ID
                    mFileIDCounter.getAndDecrement();
                    // remove file
                    FileUtils.deleteFile(new File(mCurrentFilename), "");
                }
            }
        }, 0, TimeUnit.MILLISECONDS);
        futureList.add(scheduledIMU);
    }

    @Override
    public void stop() {
        if (scheduledIMU != null) {
            scheduledIMU.cancel(true);
        }
        super.stop();
    }

//    @Override
//    public void pause() {
//
//    }
//
//    @Override
//    public void resume() {
//
//    }

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
        sampleOffset();
    }

    public void onNonIMUSensorEvent(NonIMUData data) {
        if (data.getType() == Sensor.TYPE_STEP_COUNTER) {
            int curCount = (int)data.getStepCounter();
            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "step_count", curCount);
            JSONUtils.jsonPut(json, "sensor_timestamp", data.getTimestamp());
            volEventListener.recordEvent(VolEventListener.EventType.Step, "get_step_counter", json.toString());
        }
        sampleOffset();
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

    private boolean checkIsHorizontalDown() {
        for (int i = 0; i < ORIENTATION_CHECK_NUMBER; i++)
            if (Math.abs(orientationMark[i][1]) > 0.1 || Math.abs(orientationMark[i][2]) < 3.0)
                return false;
        return true;
    }

    private boolean checkIsHorizontalUp() {
        for (int i = 0; i < ORIENTATION_CHECK_NUMBER; i++)
            if (Math.abs(orientationMark[i][1]) > 0.1 || Math.abs(orientationMark[i][2]) > 0.1)
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
//        if (linearStaticCount > 10 && gyroStaticCount > 20) {
        boolean isGesture = false;
        if (gyroStaticCount > 20) {
            if (checkIsHorizontalDown()) {
                isGesture = true;
                if (isFaceDown)
                    return;
                isFaceDown = true;
                isFaceUp = false;
                Bundle bundle = new Bundle();
                bundle.putString("motion", "faceDown");
                volEventListener.onVolEvent(VolEventListener.EventType.Motion, bundle);

                JSONObject json = new JSONObject();
                JSONUtils.jsonPut(json, "gesture", "faceDown");
                volEventListener.recordEvent(VolEventListener.EventType.Motion, "motion_change", json.toString());
            } else if (checkIsHorizontalUp()) {
                isGesture = true;
                if (isFaceUp)
                    return;
                isFaceUp = true;
                isFaceDown = false;
                Bundle bundle = new Bundle();
                bundle.putString("motion", "faceUp");
                volEventListener.onVolEvent(VolEventListener.EventType.Motion, bundle);

                JSONObject json = new JSONObject();
                JSONUtils.jsonPut(json, "gesture", "faceUp");
                volEventListener.recordEvent(VolEventListener.EventType.Motion, "motion_change", json.toString());
            }
        }

        if (!isGesture) {
            if (!isFaceDown && !isFaceUp)
                return;
            isFaceDown = false;
            isFaceUp = false;
            Bundle bundle = new Bundle();
            bundle.putString("motion", "noGesture");
            volEventListener.onVolEvent(VolEventListener.EventType.Motion, bundle);

            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "gesture", "noGesture");
            volEventListener.recordEvent(VolEventListener.EventType.Motion, "motion_change", json.toString());
        }
    }

    public long getOffsetInNano() {
        return offsetInNano;
    }

    synchronized private void sampleOffset() {
        long offset = System.currentTimeMillis() * 1000000 - SystemClock.elapsedRealtimeNanos();
        long diff_abs = offset - offsetInNano;
        if (diff_abs > 100000000) {
            // if diff too large (> 100ms), reset counter
            Log.e(TAG, "sampleOffset: reset counter because diff_abs = " + diff_abs);
            offsetInNano = offset;
            samplePoints = 1;
            stableCount = 0;
        } else if (stableCount < 20) {
            samplePoints++;
            long diff = diff_abs / samplePoints;
            if (diff != 0) {
                // update offset according to diff
                offsetInNano += diff;
                stableCount = 0;
//                    Log.e(TAG, "sampleOffset: diff offset = " + diff + " sample points = " + samplePoints);
            } else {
                stableCount++;
//                Log.e(TAG, "sampleOffset: stable offset = " + offsetInNano + " count = " + stableCount + " sample points = " + samplePoints);
            }
        }
    }
}
