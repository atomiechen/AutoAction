package com.hcifuture.contextactionlibrary.volume;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.contextaction.ContextActionContainer;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.sensor.collector.Collector;
import com.hcifuture.contextactionlibrary.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DeviceManager extends TriggerManager {

    private static final String TAG = "DeviceManager";

    Context context;
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
//    private AudioManager audioManager;
    private MediaRouter mediaRouter;

    private ScheduledFuture<?> scheduledDeviceDetection;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
//    AudioDeviceCallback audioDeviceCallback;
    MediaRouter.Callback routerCallback;
    private Device currentDevice;
    private List<Device> devices;
    private long lastTimestamp = 0;

    private final String FILE_DEVICE_LIST = "device.json";
    private final String FILE_DEVICE_HISTORY = "device_history.json";

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DeviceManager(VolEventListener volEventListener, Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList) {
        super(volEventListener);
        this.context = context;
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
//        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mediaRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
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
                ContextActionContainer.handler.post(() -> checkRouteInfo(info, "onRouteSelected: " + type));
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
                ContextActionContainer.handler.post(() -> checkRouteInfo(info, "onRouteChanged"));
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
                ContextActionContainer.handler.post(() -> checkRouteInfo(info, "onRouteVolumeChanged"));
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
                        checkDevice();
                        break;
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        futureList.add(scheduledExecutorService.schedule(() -> checkDevice(), 5000, TimeUnit.MILLISECONDS));
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
        setPresentDevice(genDevice(getCurrentRouteInfo()));

//        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
        mediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, routerCallback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        context.registerReceiver(broadcastReceiver, intentFilter, null, ContextActionContainer.handler);
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
        checkRouteInfo(getCurrentRouteInfo(), "checkDevice");
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
        String deviceID = "$" + deviceType + "$" + name + "$" + description;
        return new Device(deviceID, name, description, deviceType);
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
        lastTimestamp = System.currentTimeMillis();
    }

}
