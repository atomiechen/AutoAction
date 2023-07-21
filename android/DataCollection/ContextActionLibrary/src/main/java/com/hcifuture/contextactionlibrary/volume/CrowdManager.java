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

import com.hcifuture.contextactionlibrary.sensor.collector.async.BluetoothCollector;
import com.hcifuture.contextactionlibrary.sensor.data.BluetoothData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleBluetoothData;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CrowdManager extends TriggerManager {
    private static final String TAG = "CrowdManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private BluetoothCollector bluetoothCollector;
    private List<BluetoothItem> bleList;
    private List<BluetoothItem> deviceList;
    public static Integer latest_bleNumLevel = -1;
    private Context mContext;
    private BLEManager bleManager;
    private int nearbyPCNum = 0;
    private int nearbyPhoneNum = 0;
    private List<Integer> linkedDeviceClasses = new ArrayList<>();

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
        private boolean linked;

        public BluetoothItem(String name, String address, int majorDeviceClass, int deviceClass, double distance, boolean linked) {
            this.name = name;
            this.address = address;
            this.majorDeviceClass = majorDeviceClass;
            this.deviceClass = deviceClass;
            this.distance = distance;
            this.linked = linked;
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
        resume();
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
        pause();
        bleManager.stopAdvertising();
    }

    @Override
    public void pause() {
        if (scheduledPhoneDetection != null) {
            scheduledPhoneDetection.cancel(true);
            scheduledPhoneDetection = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void resume() {
        // detect phones periodically
        Log.e(TAG, "schedule periodic phones detection");
        if (scheduledPhoneDetection == null) {
            scheduledPhoneDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
                scanAndUpdate();
            }, initialDelay, period, TimeUnit.MILLISECONDS);
            futureList.add(scheduledPhoneDetection);
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
        // 变次数扫描方法
        Log.e(TAG, "scanAndUpdate: start 3 times");
        return repeatScan(3).thenApply(listOfListOfList -> {
            deviceList = setBluetoothDeviceList(listOfListOfList.get(0));
            bleList = setBluetoothDeviceList(listOfListOfList.get(1));
            int pc_num = 0, phone_num = 0;
            List<Integer> device_classes = new ArrayList<>();
            for (BluetoothItem item: deviceList) {
                if (item.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER)
                    pc_num += 1;
                if (item.majorDeviceClass == BluetoothClass.Device.Major.PHONE)
                    phone_num += 1;
                if (item.linked && !device_classes.contains(item.majorDeviceClass))
                    device_classes.add(item.majorDeviceClass);
            }
            linkedDeviceClasses = device_classes;
            if (pc_num != nearbyPCNum) {
                Bundle bundle = new Bundle();
                bundle.putInt("nearby_PC_num_before", nearbyPCNum);
                bundle.putInt("nearby_PC_num_now", pc_num);
                if (pc_num > nearbyPCNum)
                    volEventListener.onVolEvent(VolEventListener.EventType.NearbyPCIncrease, bundle);
                else
                    volEventListener.onVolEvent(VolEventListener.EventType.NearbyPCDecrease, bundle);
                nearbyPCNum = pc_num;
            }
            if (phone_num != nearbyPhoneNum) {
                Bundle bundle = new Bundle();
                bundle.putInt("nearby_phone_num_before", nearbyPhoneNum);
                bundle.putInt("nearby_phone_num_now", phone_num);
                if (phone_num > nearbyPhoneNum)
                    volEventListener.onVolEvent(VolEventListener.EventType.NearbyPhoneIncrease, bundle);
                else
                    volEventListener.onVolEvent(VolEventListener.EventType.NearbyPhoneDecrease, bundle);
                nearbyPhoneNum = phone_num;
            }
//            Log.e(TAG, "scanAndUpdate: get device list " + deviceList);
//            Log.e(TAG, "scanAndUpdate: get ble list " + bleList);
            Log.i(TAG, "" + deviceList);
            Log.i(TAG, "pc_num: " + pc_num + ", phone_num: " + phone_num);
            Log.i(TAG, "linked_device_classes: " + getLinkedDeviceClasses());

            // record data to file
            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "phone_number", deviceList.size());
            JSONUtils.jsonPut(json, "ble_number", bleList.size());
            JSONUtils.jsonPut(json, "phone_devices", blItemList2StringList(deviceList));
            JSONUtils.jsonPut(json, "ble_devices", blItemList2StringList(bleList));
//            volEventListener.recordEvent(VolEventListener.EventType.Crowd, "crowd_bt_scan_3times", json.toString());

            return Arrays.asList(deviceList, bleList);
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
            JSONUtils.jsonPut(json, "phone_devices", blItemList2StringList(phoneScanList));
            JSONUtils.jsonPut(json, "ble_devices", blItemList2StringList(bleScanList));
//            volEventListener.recordEvent(VolEventListener.EventType.Crowd, "crowd_bt_scan", json.toString());
            return Arrays.asList(phoneScanList, bleScanList);
        });
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
                boolean linked = distance == 0;
                result.add(new BluetoothItem(item.getName(), item.getAddress(), item.getMajorDeviceClass(), item.getDeviceClass(), distance / cnt, linked));
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
            if (device.getUuids() != null) {
                if (singleBluetoothData.getLinked()) {
                    String name = device.getName();
                    String address = device.getAddress();
                    double distance = 0;
                    result.add(new BluetoothItem(name, address,
                            device.getBluetoothClass().getMajorDeviceClass(),
                            device.getBluetoothClass().getDeviceClass(),
                            distance, true));
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
                boolean linked = distance == 0;
                result.add(new BluetoothItem(name, address,
                        device.getBluetoothClass().getMajorDeviceClass(),
                        device.getBluetoothClass().getDeviceClass(),
                        distance, linked));
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
                boolean linked = distance == 0;
                result.add(new BluetoothItem(name, address,
                        device.getBluetoothClass().getMajorDeviceClass(),
                        device.getBluetoothClass().getDeviceClass(),
                        distance, linked));
            }
        }
        Log.e(TAG, "end scanResult2BtItem");
        return result;
    }

//    @SuppressLint("MissingPermission")
//    public boolean isFilteredDevice(BluetoothDevice bluetoothDevice) {
//        return bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE
//                || bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE;
//        return bluetoothDevice.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER;
//    }

    public double rssi2distance(int rssi) {
        double A = 59;
        double n = 2.0;
        double ratio = ((double)Math.abs(rssi) - A) / (10 * n);
        return Math.pow(10, ratio);
    }

    public List<BluetoothItem> getBleList() {
        return bleList;
    }

    public List<BluetoothItem> getDeviceList() { return deviceList; }

    public int getNearbyPCNum() {
        return nearbyPCNum;
    }

    public int getNearbyPhoneNum() {
        return nearbyPhoneNum;
    }

    public List<String> getLinkedDeviceClasses() {
        List<String> result = new ArrayList<>();
        for (Integer item: linkedDeviceClasses) {
            if (item == BluetoothClass.Device.Major.COMPUTER) {
                result.add("computer");
            } else if (item == BluetoothClass.Device.Major.PHONE) {
                result.add("phone");
            } else if (item == BluetoothClass.Device.Major.WEARABLE) {
                result.add("wearable");
            } else if (item == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                result.add("audio_video");
            } else if (item == BluetoothClass.Device.Major.HEALTH) {
                result.add("health");
            } else if (item == BluetoothClass.Device.Major.IMAGING) {
                result.add("imaging");
            } else if (item == BluetoothClass.Device.Major.MISC) {
                result.add("misc");
            } else if (item == BluetoothClass.Device.Major.NETWORKING) {
                result.add("networking");
            } else if (item == BluetoothClass.Device.Major.PERIPHERAL) {
                result.add("peripheral");
            } else if (item == BluetoothClass.Device.Major.TOY) {
                result.add("toy");
            } else if (item == BluetoothClass.Device.Major.UNCATEGORIZED) {
                result.add("uncategorized");
            } else {
                result.add("unknown");
            }
        }
        return result;
    }
}
