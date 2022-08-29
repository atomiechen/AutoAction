package com.hcifuture.contextactionlibrary.volume;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BLEManager {
    public final String TAG = "BLEManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    Context mContext;
    BluetoothLeAdvertiser bluetoothLeAdvertiser;
    BluetoothLeScanner bluetoothLeScanner;
    List<ScanResult> mScanResults;

    public BLEManager (ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, Context context) {
        mContext = context;
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        setAdvertiserAndScanner();
        startAdvertising();
    }

    private void setAdvertiserAndScanner() {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth Unable");
            bluetoothLeAdvertiser = null;
            bluetoothLeScanner = null;
        } else {
            bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e(TAG, "BLE广播开启成功");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "BLE广播开启失败,错误码:" + errorCode);
        }
    };

    // ref: https://www.jianshu.com/p/1586f916f3d0
    @SuppressLint("MissingPermission")
    public void startAdvertising() {
        if (bluetoothLeAdvertiser == null)
            return;
        //广播设置(必须)
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
                .setTimeout(0)
                .setConnectable(true) //能否连接,广播分为可连接广播和不可连接广播
                .build();

        //广播数据(必须，广播启动就会发送)
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true) //包含蓝牙名称
                .setIncludeTxPowerLevel(true) //包含发射功率级别
                .addManufacturerData(1, new byte[]{23, 33}) //设备厂商数据，自定义
                .build();

        try {
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, mAdvertiseCallback);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

    @SuppressLint("MissingPermission")
    public void stopAdvertising() {
        if (bluetoothLeAdvertiser == null)
            return;
        bluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    public CompletableFuture<List<ScanResult>> startScan() {
        CompletableFuture<List<ScanResult>> ft = new CompletableFuture<>();
        List<ScanResult> scanResults = new ArrayList<>();
        ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                scanResults.add(result);
            }
        };
        bluetoothLeScanner.startScan(mScanCallback);
        futureList.add(scheduledExecutorService.schedule(() -> {
            bluetoothLeScanner.stopScan(mScanCallback);
            mScanResults = scanResults;
            ft.complete(mScanResults);
        }, 3000, TimeUnit.MILLISECONDS));
        return ft;
    }
}