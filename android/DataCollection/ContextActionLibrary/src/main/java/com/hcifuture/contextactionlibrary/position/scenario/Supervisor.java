package com.hcifuture.contextactionlibrary.position.scenario;

import android.content.Context;
import android.util.Log;

import com.hcifuture.contextactionlibrary.position.sensor.BluetoothScanner;
import com.hcifuture.contextactionlibrary.position.sensor.WIFIScanner;
import com.hcifuture.contextactionlibrary.position.utility.LoggerUtility;

public class Supervisor {
    private static final String TAG = "Supervisor";
    private final Context context;

    public BluetoothScanner.BluetoothInformation myComputer = new BluetoothScanner.BluetoothInformation();
    public WIFIScanner.WIFIInformation bedroomWIFI = new WIFIScanner.WIFIInformation();
    public WIFIScanner.WIFIInformation oldWIFI = new WIFIScanner.WIFIInformation();

    public Supervisor(Context context) {
        this.context = context;
    }

    public boolean needRecollectSensorData(Scenario oldScenario) {
        Log.i(TAG, "needRecollectSensorData: True");
        LoggerUtility.getInstance().addAnalyzeScenarioLog(context, "管理员判断是否需要重新收集位置信息 - 需要");
        String oldInternetIP = oldScenario.internetIP;
        return true;
    }
}
