package com.hcifuture.contextactionlibrary.position.recorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.hcifuture.contextactionlibrary.position.scenario.Scenario;
import com.hcifuture.contextactionlibrary.position.sensor.BluetoothScanner;
import com.hcifuture.contextactionlibrary.position.sensor.LocalNetworkSearcher;
import com.hcifuture.contextactionlibrary.position.sensor.WIFIScanner;
import com.hcifuture.contextactionlibrary.position.utility.LoggerUtility;
import com.hcifuture.contextactionlibrary.position.utility.NotificationUtility;
import com.hcifuture.contextactionlibrary.position.utility.PositionProfileUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class NormalRecorder {
    private static final String TAG = "NormalRecorder";

    private final Context context;

    public NormalRecorder(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");
    }

    @SuppressLint("MissingPermission")
    public void recordPosition(Scenario scenario) {
        JSONObject positionProfile = new JSONObject();
        try {
            positionProfile.put("name", scenario.name);

            JSONArray wifiDetailArray = new JSONArray();
            for (WIFIScanner.WIFIInformation wi : scenario.wifiList) {
                JSONObject wifiDetail = new JSONObject();
                wifiDetail.put("ssid", wi.ssid);
                wifiDetail.put("bssid", wi.bssid);
                wifiDetail.put("level", wi.level);
                wifiDetail.put("deviceType", wi.deviceType);
                wifiDetailArray.put(wifiDetail);
            }
            positionProfile.put("wifi", wifiDetailArray);

            JSONArray bluetoothDetailArray = new JSONArray();
            for (BluetoothScanner.BluetoothInformation bi : scenario.bluetoothList) {
                JSONObject bluetoothDetail = new JSONObject();
                bluetoothDetail.put("name", bi.name);
                bluetoothDetail.put("address", bi.address);
                bluetoothDetail.put("type", bi.type);
                bluetoothDetail.put("bluetoothClass", bi.bluetoothClass);
                bluetoothDetail.put("bondState", bi.bondState);
                bluetoothDetail.put("deviceType", bi.deviceType);
                bluetoothDetailArray.put(bluetoothDetail);
            }
            positionProfile.put("bluetooth", bluetoothDetailArray);

            JSONArray othersDetailArray = new JSONArray();
            for (LocalNetworkSearcher.DeviceUnderNetwork dun : scenario.deviceUnderNetworkList) {
                JSONObject deviceUnderNetworkDetail = new JSONObject();
                deviceUnderNetworkDetail.put("ip", dun.ip);
                deviceUnderNetworkDetail.put("mac", dun.mac);
                deviceUnderNetworkDetail.put("deviceType", dun.deviceType);
                othersDetailArray.put(deviceUnderNetworkDetail);
            }
            positionProfile.put("others", othersDetailArray);

            String message = String.format(Locale.CHINA, "记录位置 - %s - wifi: %d - bluetooth: %d - others: %d",
                    positionProfile.getString("name"),
                    positionProfile.getJSONArray("wifi").length(),
                    positionProfile.getJSONArray("bluetooth").length(),
                    positionProfile.getJSONArray("others").length());
            LoggerUtility.getInstance().addRecordPositionLog(context, message);
        } catch (JSONException e) {
            Log.e(TAG, "recordPosition: ", e);
            e.printStackTrace();
        }
        if (!PositionProfileUtility.getInstance().createPositionProfile(context, positionProfile)) {
            NotificationUtility.sendShortToast(context, "未能创建位置信息文件");
            String message = "未能创建位置信息文件";
            LoggerUtility.getInstance().addRecordPositionLog(context, message);
        }
    }
}
