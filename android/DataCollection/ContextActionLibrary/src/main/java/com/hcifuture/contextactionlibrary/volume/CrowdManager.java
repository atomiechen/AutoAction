package com.hcifuture.contextactionlibrary.volume;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.sensor.collector.CollectorResult;
import com.hcifuture.contextactionlibrary.sensor.collector.async.BluetoothCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;
import com.hcifuture.contextactionlibrary.sensor.data.BluetoothData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleBluetoothData;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CrowdManager extends TriggerManager {
    private static final String TAG = "CrowdManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private BluetoothCollector bluetoothCollector;
    private List<BluetoothItem> bleList;
    private List<BluetoothItem> phoneList;
    public static Integer latest_bleNumLevel = -1;
    private Context mContext;
    private BLEManager bleManager;

    private ScheduledFuture<?> scheduledPhoneDetection;
    private ScheduledFuture<?> repeatScan;
    private long initialDelay = 0;
    private long period = 1000 * 60;  // detect phones every 15s

    @SuppressLint("MissingPermission")
    public CrowdManager(VolEventListener volEventListener, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, BluetoothCollector bluetoothCollector, Context context) {
        super(volEventListener);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.bluetoothCollector = bluetoothCollector;
        bleList = new ArrayList<>();
        mContext = context;
        bleManager = new BLEManager(scheduledExecutorService, futureList, mContext);
    }

    public static class BluetoothItem {
        private String name;
        private String address;
        private double distance;
        private int majorDeviceClass;
        private int deviceClass;

        public BluetoothItem(String name, String address, int majorDeviceClass, int deviceClass, double distance) {
            this.name = name;
            this.address = address;
            this.majorDeviceClass = majorDeviceClass;
            this.deviceClass = deviceClass;
            this.distance = distance;
        }

        public String getName() { return name; }

        public String getAddress() { return address; }

        public double getDistance() { return distance; }

        public int getMajorDeviceClass() {
            return majorDeviceClass;
        }

        public int getDeviceClass() {
            return deviceClass;
        }

        public boolean isPhone() {
            return majorDeviceClass == BluetoothClass.Device.Major.PHONE;
        }

        public boolean isWearable() {
            return majorDeviceClass == BluetoothClass.Device.Major.WEARABLE;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        @Override
        public String toString() {
            return "BluetoothItem{" +
                    "name='" + name + '\'' +
                    ", address='" + address + '\'' +
                    ", distance=" + distance + '\'' +
                    ", majorDeviceClass=" + majorDeviceClass + '\'' +
                    ", deviceClass=" + deviceClass + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BluetoothItem that = (BluetoothItem) o;
            return address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void start() {
        bleManager.startAdvertising();
        // detect phones periodically
        Log.e(TAG, "schedule periodic phones detection");
        scheduledPhoneDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
            scanAndUpdate();
        }, initialDelay, period, TimeUnit.MILLISECONDS);
        futureList.add(scheduledPhoneDetection);
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
//    public List<List<BluetoothItem>> scanAndGet() {
//        List<List<BluetoothItem>> result = new ArrayList<>();
//        try {
//            result = scanAndUpdate().get();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return result;
//    }

    @Override
    public void stop() {
//        bleManager.stopAdvertising();
        if (scheduledPhoneDetection != null) {
            scheduledPhoneDetection.cancel(true);
        }
    }

    public static List<String> blItemList2StringList(List<BluetoothItem> list) {
        List<String> result = new ArrayList<>();
        if (list != null) {
            for (BluetoothItem item: list) {
                result.add(item.toString());
            }
        }
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public CompletableFuture<List<List<BluetoothItem>>> scanAndUpdate() {
//        // 手写3次扫描的方法
//        List<List<BluetoothItem>> listOfList = new ArrayList<>();
//        return toScan().thenCompose(v1 -> {
//            listOfList.add(v1);
//            return toScan().thenCompose(v2 -> {
//                listOfList.add(v2);
//                return toScan().thenApply(v3 -> {
//                    listOfList.add(v3);
//                    phoneList = setBluetoothDeviceList(listOfList.get(0), listOfList.get(1), listOfList.get(2));
//                    return phoneList;
//                });
//            });
//        });

        // 变次数扫描方法
        Log.e(TAG, "scanAndUpdate: start 3 times");
        return repeatScan(3).thenApply(listOfListOfList -> {
            phoneList = setBluetoothDeviceList(listOfListOfList.get(0));
            bleList = setBluetoothDeviceList(listOfListOfList.get(1));
            int after_size = (bleList == null) ? 0 : bleList.size();
            if (!Objects.equals(latest_bleNumLevel, getBluetoothLevel(after_size))) {
                latest_bleNumLevel = getBluetoothLevel(after_size);
                Bundle bundle = new Bundle();
                bundle.putInt("BluetoothLevel", latest_bleNumLevel);
                volEventListener.onVolEvent(VolEventListener.EventType.Bluetooth, bundle);
            }
            Log.e(TAG, "scanAndUpdate: get phone list " + phoneList);
            Log.e(TAG, "scanAndUpdate: get ble list " + bleList);
            return Arrays.asList(phoneList, bleList);
        });
    }

    public Integer getBluetoothLevel(int num) {
        if (num <= 0)
            return 0;
        else if (num <= 3)
            return 1;
        else
            return 2;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public CompletableFuture<List<List<List<BluetoothItem>>>> repeatScan(int times) {
        // 重复扫描times次，times >= 1
        List<List<BluetoothItem>> listOfPhoneList = new ArrayList<>();
        List<List<BluetoothItem>> listOfBleList = new ArrayList<>();
        Function<List<List<BluetoothItem>>, CompletableFuture<List<List<BluetoothItem>>>> function = v -> {
            listOfPhoneList.add(v.get(0));
            listOfBleList.add(v.get(1));
            return toScan();
        };
        CompletableFuture<List<List<BluetoothItem>>> ft = toScan();
        for (int i = 0; i < times - 1; i++) {
            ft = ft.thenCompose(function);
        }
        return ft.thenApply(v -> {
            listOfPhoneList.add(v.get(0));
            listOfBleList.add(v.get(1));
            return Arrays.asList(listOfPhoneList, listOfBleList);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public CompletableFuture<List<List<BluetoothItem>>> toScan() {
        Log.e(TAG, "start to detect phones");
        List<CompletableFuture<List<BluetoothItem>>> fts = new ArrayList<>();
        return bluetoothCollector.getData(new TriggerConfig().setBluetoothScanTime(10000)).thenApply(v -> {
            BluetoothData data = (BluetoothData) v.getData();
            List<BluetoothItem> phoneScanList = device2BluetoothItem(data);
            List<BluetoothItem> bleScanList = scanResult2BtItem(data
                            .getDevices()
                            .stream()
                            .map(SingleBluetoothData::getScanResult)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
            Log.e(TAG, "toScan: get phone list " + phoneScanList);

            // record data to file
            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "phone_number", phoneScanList.size());
            JSONUtils.jsonPut(json, "ble_number", bleScanList.size());
            JSONUtils.jsonPut(json, "phone_devices", bluetoothItem2JSONArray(phoneScanList));
            JSONUtils.jsonPut(json, "ble_devices", bluetoothItem2JSONArray(bleScanList));
            volEventListener.recordEvent(VolEventListener.EventType.Crowd, "crowd_bt_scan", json.toString());
            return Arrays.asList(phoneScanList, bleScanList);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    JSONArray bluetoothItem2JSONArray(List<BluetoothItem> bluetoothItemList) {
        JSONArray jsonArray = new JSONArray();
        bluetoothItemList.forEach(bluetoothItem -> {
            JSONArray array = new JSONArray();
            array.put(bluetoothItem.getAddress());
            array.put(bluetoothItem.getName());
            try {
                array.put(bluetoothItem.getDistance());
            } catch (JSONException e) {
                e.printStackTrace();
                array.put(-1);
            }
            jsonArray.put(array);
        });
        return jsonArray;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<BluetoothItem> setBluetoothDeviceList(List<List<BluetoothItem>> listOfList) {
        Log.e(TAG, "start setBluetoothDeviceList");
        List<BluetoothItem> result = new ArrayList<>();
        List<BluetoothItem> tmp = new ArrayList<>();
        for (List<BluetoothItem> list : listOfList) {
            for (BluetoothItem item : list) {
//                if (!result.contains(item)) {
//                    result.add(item);
//                }
                if (findByDistance(tmp, item.getDistance()) != null && findByDistance(tmp, item.getDistance()).getAddress().equals(item.getAddress()))
                    continue;
                tmp.add(item);
            }
        }
        Log.e(TAG, "tmp: " + tmp);
        for (BluetoothItem item: tmp) {
////            if (list2.contains(item) && list3.contains(item)) {
////                Log.e(TAG, "" + item.getDistance() + " " + findByAddress(list2, item.getAddress()).getDistance() + " " + findByAddress(list3, item.getAddress()).getDistance());
////                result.add(new BluetoothItem(
////                        item.getName(),
////                        item.getAddress(),
////                        item.getMajorDeviceClass(),
////                        item.getDeviceClass(),
////                        (item.getDistance() + findByAddress(list2, item.getAddress()).getDistance() + findByAddress(list3, item.getAddress()).getDistance()) / 3));
////            }
//            double distance = 0;
//            int cnt = 0;
//            for (List<BluetoothItem> list : listOfList) {
//                int idx = list.indexOf(item);
//                if (idx >= 0) {
//                    distance += list.get(idx).getDistance();
//                    cnt += 1;
//                }
//            }
//            item.setDistance(distance / cnt);
            if (findByAddress(result, item.getAddress()) == null) {
                double distance = 0;
                int cnt = 0;
                for (BluetoothItem item1: tmp) {
                    if (item1.getAddress().equals(item.getAddress())) {
                        distance += item1.getDistance();
                        cnt += 1;
                    }
                }
                result.add(new BluetoothItem(item.getName(), item.getAddress(), item.getMajorDeviceClass(), item.getDeviceClass(), distance / cnt));
            }
        }
        result.sort((a, b) -> {
            double diff = a.getDistance() - b.getDistance();
            if (diff > 0) {
                return 1;
            } else if (diff < 0) {
                return -1;
            } else {
                return 0;
            }
        });
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

    public BluetoothItem findByDistance(List<BluetoothItem> list, double distance) {
        for (BluetoothItem bluetoothItem: list) {
            if (distance == bluetoothItem.getDistance())
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
            BluetoothDevice device = singleBluetoothData.getDevice();
            if (isFilteredDevice(device)) {
                if (device.getUuids() != null) {
                    if (singleBluetoothData.getLinked()) {
                        String name = device.getName();
                        String address = device.getAddress();
                        double distance = 0;
                        result.add(new BluetoothItem(name, address,
                                device.getBluetoothClass().getMajorDeviceClass(),
                                device.getBluetoothClass().getDeviceClass(),
                                distance));
                    }
                } else {
                    String name = device.getName();
                    String address = device.getAddress();
                    double distance = -1;
                    if (singleBluetoothData.getScanResult() != null) {
                        distance = rssi2distance(singleBluetoothData.getScanResult().getRssi());
                    } else if (singleBluetoothData.getIntentExtra() != null) {
                        int rssi = singleBluetoothData.getIntentExtra().getShort(BluetoothDevice.EXTRA_RSSI, (short) 0);
                        if (rssi < 0)
                            distance = rssi2distance(rssi);
                    }
                    result.add(new BluetoothItem(name, address,
                            device.getBluetoothClass().getMajorDeviceClass(),
                            device.getBluetoothClass().getDeviceClass(),
                            distance));
                }
            }
        }
        Log.e(TAG, "end device2item");
        return result;
    }

    @SuppressLint("MissingPermission")
    public List<BluetoothItem> scanResult2BtItem(List<ScanResult> scanResults) {
        Log.e(TAG, "start scanResult2BtItem");
        List<BluetoothItem> result = new ArrayList<>();
        Log.e(TAG, "scanResults size: " + scanResults.size());
        for (ScanResult scanResult: scanResults) {
            BluetoothDevice device = scanResult.getDevice();
            ScanRecord scanRecord = scanResult.getScanRecord();
            if (scanRecord != null && Arrays.equals(scanRecord.getManufacturerSpecificData(2), BLEManager.MANUFACTURER_DATA)) {
                double distance = rssi2distance(scanResult.getRssi());
                String name = device.getName();
                String address = device.getAddress();
                result.add(new BluetoothItem(name, address,
                        device.getBluetoothClass().getMajorDeviceClass(),
                        device.getBluetoothClass().getDeviceClass(),
                        distance));
            }
        }
        Log.e(TAG, "end scanResult2BtItem");
        return result;
    }

    @SuppressLint("MissingPermission")
    public boolean isFilteredDevice(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE
                || bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE;
    }

    public double rssi2distance(int rssi) {
        double A = 59;
        double n = 2.0;
        double ratio = ((double)Math.abs(rssi) - A) / (10 * n);
        return Math.pow(10, ratio);
    }

    public List<BluetoothItem> getBleList() {
        return bleList;
    }

    public List<BluetoothItem> getPhoneList() { return phoneList; }
}
