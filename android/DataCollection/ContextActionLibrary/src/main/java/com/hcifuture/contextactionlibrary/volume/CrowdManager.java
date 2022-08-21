package com.hcifuture.contextactionlibrary.volume;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.sensor.collector.CollectorResult;
import com.hcifuture.contextactionlibrary.sensor.collector.async.BluetoothCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.GPSCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.WifiCollector;
import com.hcifuture.contextactionlibrary.sensor.data.BluetoothData;
import com.hcifuture.contextactionlibrary.sensor.data.GPSData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleBluetoothData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleWifiData;
import com.hcifuture.contextactionlibrary.sensor.data.WifiData;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;

import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CrowdManager extends TriggerManager {
    private static final String TAG = "CrowdManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private BluetoothCollector bluetoothCollector;
    private List<BluetoothItem> phoneList;

    private ScheduledFuture<?> scheduledPhoneDetection;
    private ScheduledFuture<?> repeatScan;
    private long initialDelay = 0;
    private long period = 1000 * 60;  // detect phones every 15s

    public CrowdManager(VolEventListener volEventListener, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, BluetoothCollector bluetoothCollector) {
        super(volEventListener);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.bluetoothCollector = bluetoothCollector;
        phoneList = new ArrayList<>();
    }

    public static class BluetoothItem {
        private String name;
        private String address;
        private double distance;

        public BluetoothItem(String name, String address, double distance) {
            this.name = name;
            this.address = address;
            this.distance = distance;
        }

        public String getName() { return name; }

        public String getAddress() { return address; }

        public double getDistance() { return distance; }

        @Override
        public String toString() {
            return "BluetoothItem{" +
                    "name='" + name + '\'' +
                    ", address='" + address + '\'' +
                    ", distance=" + distance +
                    '}';
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<BluetoothItem> scanAndGetPhones() {
        try {
            return scanAndUpdate().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void start() {
        // detect phones periodically
        Log.e(TAG, "schedule periodic phones detection");
        scheduledPhoneDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
            scanAndUpdate();
        }, initialDelay, period, TimeUnit.MILLISECONDS);
        futureList.add(scheduledPhoneDetection);
    }

    @Override
    public void stop() {
        if (scheduledPhoneDetection != null) {
            scheduledPhoneDetection.cancel(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public CompletableFuture<List<BluetoothItem>> scanAndUpdate() {
        CompletableFuture<List<BluetoothItem>> ft = new CompletableFuture<>();
        List<List<BluetoothItem>> listOfList = new ArrayList<>();
//        repeatScan = scheduledExecutorService.scheduleWithFixedDelay(() -> {
//            try {
//                listOfList.add(toScan().get());
//                if (listOfList.size() >= 3) {
//                    repeatScan.cancel(false);
//                    ft.complete(setBluetoothDeviceList(listOfList.get(0), listOfList.get(1), listOfList.get(2)));
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, 0, 0, TimeUnit.MILLISECONDS);
//        futureList.add(repeatScan);
        try {
            listOfList.add(toScan().get());
            listOfList.add(toScan().get());
            listOfList.add(toScan().get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        phoneList = setBluetoothDeviceList(listOfList.get(0), listOfList.get(1), listOfList.get(2));
        ft.complete(phoneList);

        return ft;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public CompletableFuture<List<BluetoothItem>> toScan() {
        CompletableFuture<List<BluetoothItem>> ft = new CompletableFuture<>();

        Log.e(TAG, "start to detect phones");
        bluetoothCollector.getData(new TriggerConfig()).thenAccept(v -> {
            try {
                Log.e(TAG, "get crowd data");
                phoneList = device2BluetoothItem((BluetoothData) v.getData());
                ft.complete(phoneList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return ft;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<BluetoothItem> setBluetoothDeviceList(List<BluetoothItem> list1, List<BluetoothItem> list2, List<BluetoothItem> list3) {
        Log.e(TAG, "start setBluetoothDeviceList");
        List<BluetoothItem> result = new ArrayList<>();
        for (BluetoothItem item: list1) {
            if (containAddress(list2, item.getAddress()) && containAddress(list3, item.getAddress())) {
                Log.e(TAG, "" + item.getDistance() + " " + findByAddress(list2, item.getAddress()).getDistance() + " " + findByAddress(list3, item.getAddress()).getDistance());
                result.add(new BluetoothItem(item.getName(), item.getAddress(), (item.getDistance() + findByAddress(list2, item.getAddress()).getDistance() + findByAddress(list3, item.getAddress()).getDistance()) / 3));
            }
        }
        return result;
    }

    public boolean containAddress(List<BluetoothItem> list, String address) {
        for (BluetoothItem bluetoothItem: list) {
            if (address.equals(bluetoothItem.getAddress()))
                return true;
        }
        return false;
    }

    public BluetoothItem findByAddress(List<BluetoothItem> list, String address) {
        for (BluetoothItem bluetoothItem: list) {
            if (address.equals(bluetoothItem.getAddress()))
                return bluetoothItem;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    public List<BluetoothItem> device2BluetoothItem(BluetoothData bluetoothData) {
//        FileUtils.writeStringToFile(Collector.gson.toJson(bluetoothData), new File(ConfigContext.VOLUME_SAVE_FOLDER + "BluetoothScan_" + System.currentTimeMillis() + ".json"));
        Log.e(TAG, "start device2item");
        List<BluetoothItem> result = new ArrayList<>();
        List<SingleBluetoothData> list = bluetoothData.getDevices();
        for (SingleBluetoothData singleBluetoothData: list) {
            if (isPhone(singleBluetoothData.getDevice())) {
                if (singleBluetoothData.getDevice().getUuids() != null) {
                    if (singleBluetoothData.getLinked()) {
                        String name = singleBluetoothData.getDevice().getName();
                        String address = singleBluetoothData.getDevice().getAddress();
                        double distance = 0;
                        result.add(new BluetoothItem(name, address, distance));
                    }
                } else {
                    String name = singleBluetoothData.getDevice().getName();
                    String address = singleBluetoothData.getDevice().getAddress();
                    double distance = -1;
                    if (singleBluetoothData.getScanResult() != null) {
                        distance = rssi2distance(singleBluetoothData.getScanResult().getRssi());
                    } else if (singleBluetoothData.getIntentExtra() != null) {
                        int rssi = singleBluetoothData.getIntentExtra().getShort("android.bluetooth.device.extra.RSSI", (short) 0);
                        if (rssi < 0)
                            distance = rssi2distance(rssi);
                    }
                    result.add(new BluetoothItem(name, address, distance));
                }
            }
        }
        Log.e(TAG, "end device2item");
        return result;
    }

    @SuppressLint("MissingPermission")
    public boolean isPhone(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == 512;
    }

    public double rssi2distance(int rssi) {
        double A = 59;
        double n = 2.0;
        double ratio = ((double)Math.abs(rssi) - A) / (10 * n);
        return Math.pow(10, ratio);
    }

    public List<BluetoothItem> getPhoneList() {
        return phoneList;
    }
}
