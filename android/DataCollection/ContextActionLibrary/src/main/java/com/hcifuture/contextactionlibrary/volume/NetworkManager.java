package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hcifuture.contextactionlibrary.sensor.collector.async.WifiCollector;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class NetworkManager {
    public final String TAG = "NetworkManager";
    ConnectivityManager connectivityManager;
    WifiManager wifiManager;
    private String networkType;
    private String wifiName;

    public NetworkManager(Context context) {
        networkType = "unknown";
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
    }

    private void refreshNetworkInfo() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                checkNetworkType(networkCapabilities);
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
                wifiName = ((WifiInfo) networkCapabilities.getTransportInfo()).getSSID();
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
}
