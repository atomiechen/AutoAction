package com.hcifuture.contextactionlibrary.position.sensor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class BluetoothScanner {
    private static final String TAG = "BluetoothScanner";

    private final Context context;

    private final Map<String, Integer> beaconLatency = new HashMap<>();
    private final Integer normalLatency = 29;
    private final Set<ScanResult> beaconSet = new TreeSet<>((o1, o2) -> o2.getRssi() - o1.getRssi());

    private final Set<BluetoothDevice> deviceSet = new HashSet<>();
    private final List<BluetoothInformation> bluetoothList = new ArrayList<>();

    private final List<ScannerCallback> scannerCallbackList = new ArrayList<>();

    private final List<ScanFilter> scanFilterList = new ArrayList<>();
    private ScanCallback scanCallback;
    private ScanSettings scanSettings;

    private BroadcastReceiver classicBluetoothBroadcastReceiver;

    private boolean isRunning;

    public BluetoothScanner(Context context) {
        this.context = context;
    }

    public void init() {
        Log.i(TAG, "init: ");
        prepareScanSettings();
        removeAllCallbacks();
        isRunning = false;
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (!isRunning) {
            Log.i(TAG, "start: ");
            isRunning = true;
            beaconSet.clear();
            deviceSet.clear();
            beaconLatency.clear();
            bluetoothList.clear();
            // getScanner().startScan(scanFilterList, scanSettings, scanCallback);
            startClassicBluetoothDiscover();
        } else {
            Log.i(TAG, "start: Already started.");
        }
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        if (isRunning) {
            // getScanner().stopScan(scanCallback);
            stopClassicBluetoothDiscover();
            isRunning = false;
            Log.i(TAG, "stop: ");
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    private String getBTMajorDeviceClass(int major) {
        switch (major) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                return "AUDIO_VIDEO";
            case BluetoothClass.Device.Major.COMPUTER:
                return "COMPUTER";
            case BluetoothClass.Device.Major.HEALTH:
                return "HEALTH";
            case BluetoothClass.Device.Major.IMAGING:
                return "IMAGING";
            case BluetoothClass.Device.Major.MISC:
                return "MISC";
            case BluetoothClass.Device.Major.NETWORKING:
                return "NETWORKING";
            case BluetoothClass.Device.Major.PERIPHERAL:
                return "PERIPHERAL";
            case BluetoothClass.Device.Major.PHONE:
                return "PHONE";
            case BluetoothClass.Device.Major.TOY:
                return "TOY";
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return "UNCATEGORIZED";
            case BluetoothClass.Device.Major.WEARABLE:
                return "WEARABLE";
            default:
                return "unknown!";
        }
    }

    private boolean isConnected(String macAddress) {
        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            return false;
        }
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        boolean isConnected;
        try {
            Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
            isConnectedMethod.setAccessible(true);
            isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            isConnected = false;
        }
        return isConnected;
    }

    @SuppressLint("MissingPermission")
    private void startClassicBluetoothDiscover() {
        BluetoothAdapter myBluetooth = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : pairedDevices) {
            if (isConnected(bluetoothDevice.getAddress())) {
                BluetoothInformation tempBluetooth = new BluetoothInformation();
                tempBluetooth.address = bluetoothDevice.getAddress();
                int androidType = bluetoothDevice.getBluetoothClass().getMajorDeviceClass();
                tempBluetooth.bluetoothClass = getBTMajorDeviceClass(androidType);
                tempBluetooth.name = bluetoothDevice.getName();
//                tempBluetooth.uuids=bluetoothDevice.getUuids().toString();
                tempBluetooth.type = bluetoothDevice.getType() + "";
                tempBluetooth.deviceType = "1"; //表示已经连接的蓝牙设备
                Boolean ifIn = false;
                for (BluetoothInformation bl : bluetoothList) {
                    if (bl.address.equals(tempBluetooth.address)) {
                        ifIn = true;
                        break;
                    }
                }
                if (!ifIn) {
                    bluetoothList.add(tempBluetooth);
                }

                deviceSet.add(bluetoothDevice);
                for (ScannerCallback callback : scannerCallbackList) {
                    callback.onChanged(deviceSet, beaconSet, bluetoothList);
                }
            }
        }
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(classicBluetoothBroadcastReceiver, filter);
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    @SuppressLint("MissingPermission")
    private void stopClassicBluetoothDiscover() {
        context.unregisterReceiver(classicBluetoothBroadcastReceiver);
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    }

    private void prepareScanSettings() {
        classicBluetoothBroadcastReceiver = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BluetoothInformation tempBluetooth = new BluetoothInformation();
                    tempBluetooth.address = bluetoothDevice.getAddress();
                    int androidType = bluetoothDevice.getBluetoothClass().getMajorDeviceClass();
                    tempBluetooth.bluetoothClass = getBTMajorDeviceClass(androidType);
                    tempBluetooth.name = bluetoothDevice.getName();
//                    tempBluetooth.uuids=bluetoothDevice.getUuids();
                    tempBluetooth.type = bluetoothDevice.getType() + "";
                    tempBluetooth.deviceType = "2"; //表示未连接的蓝牙设备
                    Boolean ifIn = false;
                    for (BluetoothInformation bl : bluetoothList) {
                        if (bl.address.equals(tempBluetooth.address)) {
                            ifIn = true;
                            break;
                        }
                    }
                    if (!ifIn) {
                        bluetoothList.add(tempBluetooth);
                    }
                    deviceSet.add(bluetoothDevice);
                    for (ScannerCallback callback : scannerCallbackList) {
                        callback.onChanged(deviceSet, beaconSet, bluetoothList);
                    }
                }
            }
        };

        scanCallback = new ScanCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (result.getDevice().getName() != null && Pattern.matches("RFstar.*", result.getDevice().getName())) {
                    beaconSet.add(result);
                    Integer currentLatency = beaconLatency.getOrDefault(result.getDevice().getName(), normalLatency);
                    beaconLatency.put(result.getDevice().getName(), Objects.requireNonNull(currentLatency) + 1);
                    beaconLatency.replaceAll((k, v) -> Objects.requireNonNull(beaconLatency.get(k)) - 1);
                    beaconLatency.entrySet().removeIf(entry -> entry.getValue() <= 0);
                    beaconSet.removeIf(item -> !beaconLatency.containsKey(item.getDevice().getName()));
                    for (ScannerCallback callback : scannerCallbackList) {
                        callback.onChanged(deviceSet, beaconSet, bluetoothList);
                    }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };
        scanSettings = new ScanSettings.Builder()
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        scanFilterList.add(new ScanFilter.Builder().build());
    }

    @SuppressLint("MissingPermission")
    private BluetoothLeScanner getScanner() {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(intent);
        }
        return adapter.getBluetoothLeScanner();
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
        void onChanged(Set<BluetoothDevice> deviceSet, Set<ScanResult> beaconSet, List<BluetoothInformation> bluetoothInformationArrayList);
    }

    public static class BluetoothInformation {
        public String name;
        public String address;
        public String type;
        public String bondState;
        public String bluetoothClass;
        public String deviceType;

        public BluetoothInformation() {

        }

        public BluetoothInformation(BluetoothInformation bl, String deviceType) {
            name = bl.name;
            address = bl.address;
            type = bl.type;
            bondState = bl.bondState;
            bluetoothClass = bl.bluetoothClass;
            this.deviceType = deviceType;
        }
    }
}

