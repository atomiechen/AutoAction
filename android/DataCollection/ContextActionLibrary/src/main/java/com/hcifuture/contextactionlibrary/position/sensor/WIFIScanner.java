package com.hcifuture.contextactionlibrary.position.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WIFIScanner {
    private static final String TAG = "WIFIScanner";

    private final Context context;

    private final List<ScannerCallback> scannerCallbackList = new ArrayList<>();

    private final List<WIFIInformation> wifiList = new ArrayList<>();

    private boolean isRunning;

    private BroadcastReceiver wifiScanReceiver;

    public WIFIScanner(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");
        wifiList.clear();
        prepareScanSettings();
        removeAllCallbacks();
        isRunning = false;
    }

    public void start() {
        if (!isRunning) {
            Log.i(TAG, "start: ");
            isRunning = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            context.registerReceiver(wifiScanReceiver, intentFilter);
            boolean success = getWifiManager().startScan();
            if (!success) {
                scanFailure();
            }
        } else {
            Log.i(TAG, "start: Already started.");
        }
    }

    private void prepareScanSettings() {
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    scanFailure();
                }
            }
        };
    }

    public void stop() {
        if (isRunning) {
            context.unregisterReceiver(wifiScanReceiver);
            isRunning = false;
            Log.i(TAG, "stop: ");
        }
    }

    private void scanSuccess() {
        Log.i(TAG, "scanSuccess: ");
        List<ScanResult> scanResultList = getWifiManager().getScanResults();
        wifiList.clear();
        for (ScanResult sr : scanResultList) {
            WIFIInformation wifiInformation = new WIFIInformation();
            wifiInformation.bssid = sr.BSSID;
            wifiInformation.level = String.valueOf(sr.level);
            wifiInformation.ssid = sr.SSID;
            wifiInformation.deviceType = "default";
            wifiList.add(wifiInformation);
        }
        for (ScannerCallback callback : scannerCallbackList) {
            callback.onChanged(true, scanResultList, wifiList);
        }
    }

    private void scanFailure() {
        Log.i(TAG, "scanFailure: ");
        List<ScanResult> scanResultList = getWifiManager().getScanResults();
        wifiList.clear();
        for (ScanResult sr : scanResultList) {
            WIFIInformation wifiInformation = new WIFIInformation();
            wifiInformation.bssid = sr.BSSID;
            wifiInformation.level = String.valueOf(sr.level);
            wifiInformation.ssid = sr.SSID;
            wifiInformation.deviceType = "default";
            wifiList.add(wifiInformation);
        }
        for (ScannerCallback callback : scannerCallbackList) {
            callback.onChanged(false, scanResultList, wifiList);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    private WifiManager getWifiManager() {
        return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean addCallback(ScannerCallback callback) {
        return scannerCallbackList.add(callback);
    }

    public boolean removeCallback(ScannerCallback callback) {
        return scannerCallbackList.remove(callback);
    }

    public void removeAllCallbacks() {
        scannerCallbackList.clear();
    }

    public interface ScannerCallback {
        void onChanged(boolean isLatest, List<ScanResult> scanResultList, List<WIFIInformation> wifiInformationList);
    }

    public static class WIFIInformation {
        public String ssid;
        public String bssid;
        public String level;
        public String deviceType;

        public WIFIInformation() {
        }

        public WIFIInformation(WIFIInformation wifi, String deviceType) {
            ssid = wifi.ssid;
            bssid = wifi.bssid;
            level = wifi.level;
            this.deviceType = deviceType;
        }
    }
}
