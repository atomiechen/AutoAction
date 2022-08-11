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
import android.os.Handler;
import android.util.Log;

import com.hcifuture.contextactionlibrary.contextaction.ContextActionContainer;
import com.hcifuture.contextactionlibrary.contextaction.event.BroadcastEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DeviceManager {

    private static final String TAG = "DeviceManager";

    Context context;
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
//    private AudioManager audioManager;
    private MediaRouter mediaRouter;
    private VolEventListener volEventListener;

    private ScheduledFuture<?> scheduledDeviceDetection;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
//    AudioDeviceCallback audioDeviceCallback;
    MediaRouter.Callback routerCallback;
    private String currentDeviceID;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DeviceManager(Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, VolEventListener volEventListener) {
        this.context = context;
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
//        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mediaRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.volEventListener = volEventListener;
        currentDeviceID = genDeviceID(getCurrentRouteInfo());

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

    public void start() {
//        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
        mediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, routerCallback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        futureList.add(scheduledDeviceDetection = scheduledExecutorService.scheduleAtFixedRate(this::checkDevice, 3000, 60000, TimeUnit.MILLISECONDS));
        context.registerReceiver(broadcastReceiver, intentFilter, null, ContextActionContainer.handler);
    }

    public void stop() {
//        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        mediaRouter.removeCallback(routerCallback);
        scheduledDeviceDetection.cancel(true);
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
        Log.e(TAG, String.format(prefix + " deviceID: %s", genDeviceID(info)));
    }

    public String genDeviceID(MediaRouter.RouteInfo info) {
        int deviceType = info.getDeviceType();
        String name = info.getName() == null? "NoName" : info.getName().toString();
        String description = info.getDescription() == null? "NoDescription" : info.getDescription().toString();
        return "$" + deviceType + "$" + name + "$" + description;
    }

    public void checkRouteInfo(MediaRouter.RouteInfo info, String prefix) {
        logRouteInfo(info, prefix);
        checkRouteInfo(info);
    }

    public void checkRouteInfo(MediaRouter.RouteInfo info) {
        String deviceID = genDeviceID(info);
        if (!currentDeviceID.equals(deviceID)) {
            Bundle bundle = new Bundle();
            bundle.putString("deviceID", deviceID);
            volEventListener.onVolEvent(VolEventListener.EventType.Device, bundle);
            currentDeviceID = deviceID;
        }
    }

}
