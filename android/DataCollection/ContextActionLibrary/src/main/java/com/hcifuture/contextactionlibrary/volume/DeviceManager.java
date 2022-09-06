package com.hcifuture.contextactionlibrary.volume;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.contextaction.ContextActionContainer;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.sensor.collector.Collector;
import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;
import com.hcifuture.contextactionlibrary.utils.FileUtils;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DeviceManager extends TriggerManager {

    private static final String TAG = "DeviceManager";

    private final String FILE_DEVICE_LIST = "device.json";
    private final String FILE_DEVICE_HISTORY = "device_history.json";

    Context context;
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private final AudioManager audioManager;
    private final MediaRouter mediaRouter;

    private ScheduledFuture<?> scheduledDeviceDetection;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
//    AudioDeviceCallback audioDeviceCallback;
    MediaRouter.Callback routerCallback;
    private Device currentDevice;
    private List<Device> devices;
    private long lastTimestamp = 0;
    public static Integer latest_device;


    @RequiresApi(api = Build.VERSION_CODES.N)
    public DeviceManager(VolEventListener volEventListener, Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, LogCollector logCollector) {
        super(volEventListener);
        this.context = context;
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mediaRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.logCollector = logCollector;
        setPresentDevice(genDevice(getCurrentRouteInfo()));
        readDevices();

//        audioDeviceCallback = new AudioDeviceCallback() {
//            @Override
//            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
//                Log.e(TAG, "onAudioDevicesAdded");
//                for (AudioDeviceInfo deviceInfo : addedDevices) {
//                    logDevice(deviceInfo);
//                }
//                super.onAudioDevicesAdded(addedDevices);
//            }
//
//            @Override
//            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
//                Log.e(TAG, "onAudioDevicesRemoved");
//                for (AudioDeviceInfo deviceInfo : removedDevices) {
//                    logDevice(deviceInfo);
//                }
//                super.onAudioDevicesRemoved(removedDevices);
//            }
//        };

        routerCallback = new MediaRouter.Callback() {
            @Override
            public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
//                ContextActionContainer.handler.postDelayed(() -> checkRouteInfo(info, "onRouteSelected: " + type), 100);
                ContextActionContainer.handler.postDelayed(() -> checkDevice("onRouteSelected: " + type), 100);
            }

            @Override
            public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
//                logRouteInfo(info, "onRouteUnselected: " + type);
            }

            @Override
            public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
//                logRouteInfo(info, "onRouteAdded");
            }

            @Override
            public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
//                logRouteInfo(info, "onRouteRemoved");
            }

            @Override
            public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
//                ContextActionContainer.handler.postDelayed(() -> checkRouteInfo(info, "onRouteChanged"), 100);
                ContextActionContainer.handler.postDelayed(() -> checkDevice("onRouteChanged"), 100);
            }

            @Override
            public void onRouteGrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group, int index) {
//                logRouteInfo(info, "onRouteGrouped: " + index);
            }

            @Override
            public void onRouteUngrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group) {
//                logRouteInfo(info, "onRouteUngrouped");
            }

            @Override
            public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo info) {
//                ContextActionContainer.handler.postDelayed(() -> checkRouteInfo(info, "onRouteVolumeChanged"), 100);
                ContextActionContainer.handler.postDelayed(() -> checkDevice("onRouteVolumeChanged"), 100);
            }
        };

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.e(TAG, "onReceive " + action);
                switch (action) {
                    case Intent.ACTION_HEADSET_PLUG:
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        ContextActionContainer.handler.postDelayed(() -> checkDevice(action), 100);
//                        checkDevice();
                        break;
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        ContextActionContainer.handler.postDelayed(() -> checkDevice(action), 5000);
//                        futureList.add(scheduledExecutorService.schedule(() -> checkDevice(), 5000, TimeUnit.MILLISECONDS));
                        break;
                }
            }
        };
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
    }

    @Override
    public void start() {
//        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
        mediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, routerCallback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        context.registerReceiver(broadcastReceiver, intentFilter, null, ContextActionContainer.handler);

        setPresentDevice(genDevice(getCurrentRouteInfo()));

        futureList.add(scheduledDeviceDetection = scheduledExecutorService.scheduleAtFixedRate(this::checkDevice, 3000, 60000, TimeUnit.MILLISECONDS));
    }

    @Override
    public void stop() {
//        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        scheduledDeviceDetection.cancel(true);
        try {
            context.unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mediaRouter.removeCallback(routerCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MediaRouter.RouteInfo getCurrentRouteInfo() {
        return mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO);
    }

    public void checkDevice() {
        checkDevice("checkDevice");
    }

    public void checkDevice(String prefix) {
        checkRouteInfo(getCurrentRouteInfo(), prefix);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void logDevice(AudioDeviceInfo audioDeviceInfo) {
        int deviceType = audioDeviceInfo.getType();
        String productName = audioDeviceInfo.getProductName() == null? "" : audioDeviceInfo.getProductName().toString();
        Log.e(TAG, String.format("AudioDevice: %s %d %b %b", productName, deviceType, audioDeviceInfo.isSink(), audioDeviceInfo.isSource()));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void logRouteInfo(MediaRouter.RouteInfo info, String prefix) {
        Log.e(TAG, String.format(prefix + " deviceID: %s", genDevice(info)));
    }

    public Device genDevice(MediaRouter.RouteInfo info) {
        int deviceType = info.getDeviceType();
        String name = info.getName() == null? "NULL" : info.getName().toString();
        String description = info.getDescription() == null? "NULL" : info.getDescription().toString();
        if (deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN) {
            for (AudioDeviceInfo audioDeviceInfo : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                if (audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                    name = "wired_headset";
                    description = audioDeviceInfo.getProductName() == null? "NULL" : audioDeviceInfo.getProductName().toString();
                    break;
                } else if (audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    name = "wired_headphones";
                    description = audioDeviceInfo.getProductName() == null? "NULL" : audioDeviceInfo.getProductName().toString();
                    break;
                } else if (audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    name = "usb_headset";
                    description = audioDeviceInfo.getProductName() == null? "NULL" : audioDeviceInfo.getProductName().toString();
                    break;
                } else if (audioDeviceInfo.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    name = "phone";
                    description = audioDeviceInfo.getProductName() == null? "NULL" : audioDeviceInfo.getProductName().toString();
                }
            }
        }
        return new Device(name, description, deviceType);
    }

    public void checkRouteInfo(MediaRouter.RouteInfo info, String prefix) {
        Device device = genDevice(info);
        Log.e(TAG, String.format(prefix + " deviceID: %s", device.deviceID));
        if (!currentDevice.equals(device)) {
            Bundle bundle = new Bundle();
            bundle.putString("deviceID", device.deviceID);
            if (!devices.contains(device)) {
                devices.add(device);
                writeDevices();
            }
            volEventListener.onVolEvent(VolEventListener.EventType.Device, bundle);
            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "last_device", currentDevice.deviceID);
            JSONUtils.jsonPut(json, "new_device", device.deviceID);
            record(System.currentTimeMillis(), incLogID(), TAG, "device_change", "", json.toString());
            setPresentDevice(device);
        }
    }

    public void writeDevices() {
        String result = Collector.gson.toJson(devices);
        FileUtils.writeStringToFile(result, new File(ConfigContext.VOLUME_SAVE_FOLDER + FILE_DEVICE_LIST));
    }

    public void readDevices() {
        Type type = new TypeToken<List<Device>>(){}.getType();
        devices = Collector.gson.fromJson(
                FileUtils.getFileContent(ConfigContext.VOLUME_SAVE_FOLDER + FILE_DEVICE_LIST),
                type
        );
        if (devices == null) {
            devices = new ArrayList<>();
        }
    }

    public List<String> getDeviceIDs() {
        return devices.stream().map(device -> device.deviceID).collect(Collectors.toList());
    }

    public String getPresentDeviceID() {
        return currentDevice.deviceID;
    }

    private void setPresentDevice(Device device) {
        currentDevice = device;
        latest_device = currentDevice.deviceID.hashCode();
        lastTimestamp = System.currentTimeMillis();
    }

    public static class Device {
        public String deviceID;
        public String name;
        public String description;
        public int deviceType;

        Device(String name, String description, int deviceType) {
            this.name = name;
            this.description = description;
            this.deviceType = deviceType;
            this.deviceID = "$" + deviceType + "$" + name + "$" + description;
        }

        @NonNull
        @Override
        public String toString() {
            return deviceID;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Device device = (Device) o;
            return deviceID.equals(device.deviceID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceID);
        }
    }
}
