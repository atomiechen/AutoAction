package com.hcifuture.contextactionlibrary.position.utility;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

public class PositionProfileUtility {
    private static final PositionProfileUtility instance = new PositionProfileUtility();
    private final String TAG = "PositionProfileUtility";
    private final String PATH = "POSITIONS";

    private PositionProfileUtility() {
    }

    public static PositionProfileUtility getInstance() {
        return instance;
    }

    public boolean createPositionProfile(Context context, JSONObject positionProfile) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        try {
            String fileName = String.format(Locale.CHINA, "%s.json", positionProfile.getString("name"));
            File file = new File(context.getExternalFilesDir(PATH), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(positionProfile.toString(4).getBytes(StandardCharsets.UTF_8));
            fos.close();
            return true;
        } catch (JSONException | IOException e) {
            Log.e(TAG, "createPositionProfile: ", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean deletePositionProfileByName(Context context, String name) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        try {
            String fileName = String.format(Locale.CHINA, "%s.json", name);
            File file = new File(context.getExternalFilesDir(PATH), fileName);
            return file.delete();
        } catch (Exception e) {
            Log.e(TAG, "deletePositionProfileByName: ", e);
            e.printStackTrace();
        }
        return false;
    }

    public JSONArray getPositionProfileArray(Context context) {
        JSONArray positionProfileArray = new JSONArray();
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File filesDir = context.getExternalFilesDir(PATH);
            for (File file : Objects.requireNonNull(filesDir.listFiles())) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[fis.available()];
                    int readStatus = fis.read(buffer);
                    if (readStatus == -1) {
                        return new JSONArray();
                    }
                    positionProfileArray.put(new JSONObject(new String(buffer, StandardCharsets.UTF_8)));
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "getPositionProfileArray: ", e);
                    e.printStackTrace();
                }
            }
        }
        return positionProfileArray;
    }
}
