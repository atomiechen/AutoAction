package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.sensor.collector.async.WifiCollector;
import com.hcifuture.contextactionlibrary.sensor.data.WifiData;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NetworkManager extends TriggerManager {
    public final String TAG = "NetworkManager";
    ConnectivityManager connectivityManager;
    private String networkType;
    private String last_networkType;
    private String wifiName;
    private ScheduledFuture<?> wifi_scan;
    private ScheduledExecutorService scheduledExecutorService;
    private List<ScheduledFuture<?>> futureList;
    private WifiManager wifiManager;

    public NetworkManager(VolEventListener volEventListener, Context context, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, WifiCollector wifiCollector) {
        super(volEventListener);
        networkType = "unknown";
        last_networkType = "unknown";
        wifiName = "unknown";
        //获取ConnectivityManager
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //创建NetworkRequest对象，定制化监听
        NetworkRequest customMonitor = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        //创建网路变化监听器
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            //当网络连接可用时回调
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.i(TAG, "on network connected");
                refreshNetworkInfo();
            }
            //当网络断开时回调
            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.i(TAG, "on network disconnected");
                refreshNetworkInfo();
            }
            //当网络属性变化时回调
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                refreshNetworkInfo();
            }
        };
        //注册网络监听器
        connectivityManager.registerNetworkCallback(customMonitor, networkCallback);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    private void refreshNetworkInfo() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                checkNetworkType(networkCapabilities);
                if (!networkType.equals(last_networkType)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("network_type", networkType);
                    bundle.putString("last_network_type", last_networkType);
                    volEventListener.onVolEvent(VolEventListener.EventType.NetworkChange, bundle);
                    last_networkType = networkType;
                }
            } else {
                networkType = "no internet connection";
                wifiName = "";
            }
        }
    }

    private void checkNetworkType(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities == null) {
            networkType = "unknown";
            return;
        }

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            networkType = "connected to Wi-Fi";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.i(TAG, ((WifiInfo)networkCapabilities.getTransportInfo()).toString());
            }
        }
        else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            networkType = "using mobile data";
            wifiName = "";
        } else {
            networkType = "no internet connection";
            wifiName = "";
        }
    }

    public String getNetworkType() {
        return networkType;
    }

    public String getWifiName() {
        return wifiName;
    }

//    @Override
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    public void start() {
//        if (wifi_scan == null) {
//            wifi_scan = scheduledExecutorService.scheduleAtFixedRate(() -> {
//                try {
//                    WifiData wifiData = (WifiData) wifiCollector.getData(new TriggerConfig()).get().getData();
//                    if (networkType.equals("connected to Wi-Fi")) {
//                        if (wifiData.getAps() != null && wifiData.getAps().size() > 0) {
//                            if(!wifiName.equals(wifiData.getAps().get(0).getSsid())) {
//                                Bundle bundle = new Bundle();
//                                bundle.putString("last_wifi", wifiName);
//                                bundle.putString("wifi", wifiData.getAps().get(0).getSsid());
//                                volEventListener.onVolEvent(VolEventListener.EventType.WifiChange, bundle);
//                                wifiName = wifiData.getAps().get(0).getSsid();
//                            }
//                        }
//                    }
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }, 0, 1000 * 60, TimeUnit.MILLISECONDS);
//            futureList.add(wifi_scan);
//        }
//    }

//    @Override
//    public void stop() {
//        if (wifi_scan != null) {
//            wifi_scan.cancel(true);
//            wifi_scan = null;
//        }
//    }

    public String getWifi() {
        if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getSSID() != null)
            return wifiManager.getConnectionInfo().getSSID();
        else
            return "";
    }
}
