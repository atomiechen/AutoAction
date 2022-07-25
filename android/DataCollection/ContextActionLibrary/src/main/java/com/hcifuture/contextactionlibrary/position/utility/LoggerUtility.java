package com.hcifuture.contextactionlibrary.position.utility;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LoggerUtility {
    private static final LoggerUtility instance = new LoggerUtility();
    private final String TAG = "LoggerUtility";
    private final String PATH = "LOGGER";

    private LoggerUtility() {
    }

    public static LoggerUtility getInstance() {
        return instance;
    }

    public boolean addRecordPositionLog(Context context, String message) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        try {
            String fileName = "record_position_log.txt";
            File file = new File(context.getExternalFilesDir(PATH), fileName);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()).getBytes(StandardCharsets.UTF_8));
            fos.write(" - ".getBytes(StandardCharsets.UTF_8));
            fos.write(message.getBytes(StandardCharsets.UTF_8));
            fos.write("\n".getBytes(StandardCharsets.UTF_8));
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "addRecordPositionLog: ", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean addRecognizePositionLog(Context context, String message) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        try {
            String fileName = "recognize_position_log.txt";
            File file = new File(context.getExternalFilesDir(PATH), fileName);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()).getBytes(StandardCharsets.UTF_8));
            fos.write(" - ".getBytes(StandardCharsets.UTF_8));
            fos.write(message.getBytes(StandardCharsets.UTF_8));
            fos.write("\n".getBytes(StandardCharsets.UTF_8));
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "addRecognizePositionLog: ", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean addAnalyzeScenarioLog(Context context, String message) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        try {
            String fileName = "analyze_scenario_log.txt";
            File file = new File(context.getExternalFilesDir(PATH), fileName);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()).getBytes(StandardCharsets.UTF_8));
            fos.write(" - ".getBytes(StandardCharsets.UTF_8));
            fos.write(message.getBytes(StandardCharsets.UTF_8));
            fos.write("\n".getBytes(StandardCharsets.UTF_8));
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "addAnalyzeScenarioLog: ", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean addTriggerLog(Context context, String message) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        try {
            String fileName = "trigger_log.txt";
            File file = new File(context.getExternalFilesDir(PATH), fileName);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()).getBytes(StandardCharsets.UTF_8));
            fos.write(" - ".getBytes(StandardCharsets.UTF_8));
            fos.write(message.getBytes(StandardCharsets.UTF_8));
            fos.write("\n".getBytes(StandardCharsets.UTF_8));
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "addTriggerLog: ", e);
            e.printStackTrace();
        }
        return false;
    }
}
