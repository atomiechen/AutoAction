package com.hcifuture.contextactionlibrary.position.sensor;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalNetworkSearcher {
    public LocalNetworkSearcher(Context context) {
        this.context=context;
    }

    public class DeviceUnderNetwork{
        public String ip;
        public String mac;
        public String deviceType;
    }
    public void init() {
        Log.i(TAG, "init: ");
    }
    private final Context context;
    private final String TAG="LocalNetworkSearcher";
    private final List<LocalNetworkCallback> localNetworkCallbackList = new ArrayList<>();
    private ArrayList<DeviceUnderNetwork> deviceUnderNetworkSet=new ArrayList<>();
    public String getHostIP() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("kalshen", "SocketException");
            e.printStackTrace();
        }
        return hostIp;
    }
    private void sendDataToLocal() {
        //局域网内存在的ip集合
        final List<String> ipList = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();

        //获取本机所在的局域网地址
        String hostIP = getHostIP();
        int lastIndexOf = hostIP.lastIndexOf(".");
        final String substring = hostIP.substring(0, lastIndexOf + 1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                    int position = 2;
                    while (position < 255) {
//                        Log.e("Scanner ", "run: udp-" + substring + position);
                        dp.setAddress(InetAddress.getByName(substring + String.valueOf(position)));
                        socket.send(dp);
                        position++;
                        if (position == 125) {
                            socket.close();
                            socket = new DatagramSocket();
                        }
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void readArp() {
        System.out.println("read data 1");
        try {
//            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("ip neigh show");
            proc.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            //读取arp表，安卓10特性导致不能直接new
            String line = "";
            String ip = "";
            String flag = "";
            String mac = "";
            System.out.println("get BR");
            if (br.readLine() == null) {
                Log.i("scanner", "readArp: null");
                System.out.println("null");
            }
            while ((line = br.readLine()) != null && !line.contains("FAILED") ) {
//                System.out.println("not null");
                line = line.trim();
//                System.out.println(line);
                if (!line.contains("lladdr"))
                {
                    continue;
                }
                System.out.println(line);
//                System.out.println(line);
//                System.out.println(line.length());
//                if (line.length() < 63) continue;
//                if (line.toUpperCase(Locale.US).contains("IP")) continue;
                ip = line.substring(0, 17).replaceAll("[a-zA-Z]","").trim();
//                flag = line.substring(29, 32).trim();
//                mac = line.substring(41, 63).trim();
                if (mac.contains("00:00:00:00:00:00")) continue;
                DeviceUnderNetwork tempDeviceUnderNetwork=new DeviceUnderNetwork();
                tempDeviceUnderNetwork.ip=ip;
//                mac=line.substring(line.length()-27,line.length()-10).trim();
                String tempLine=line.replaceAll("[A-Z]","").trim();
                mac=tempLine.substring(tempLine.length()-18,tempLine.length()).trim();
                tempDeviceUnderNetwork.mac=mac;
                deviceUnderNetworkSet.add(tempDeviceUnderNetwork);
//                Log.i("scanner", ip+"       "+mac);
            }
            br.close();
        } catch (Exception ignored) {
        }
        for (LocalNetworkCallback callback : localNetworkCallbackList) {
            Log.i(TAG,"add callback");
            callback.onChanged(deviceUnderNetworkSet);
        }
        System.out.println("end read");
    }

    public void start()
    {
        sendDataToLocal();
        deviceUnderNetworkSet.clear();
//        removeAllCallbacks();
//        localNetworkCallbackList.clear();
        readArp();
        for (LocalNetworkCallback ca : localNetworkCallbackList) {
            Log.i(TAG,"add callback");
            ca.onChanged(deviceUnderNetworkSet);
        }
        Log.i(TAG,"end start");
    }

    public void stop()
    {
        Log.i(TAG,"stop");
        for(DeviceUnderNetwork de:deviceUnderNetworkSet)
        {
            System.out.println("ip:"+de.ip+"  "+"mac:"+ de.mac);
        }
    }
    public boolean addCallback(LocalNetworkCallback callback) {
        return localNetworkCallbackList.add(callback);
    }

    public boolean removeCallback(LocalNetworkCallback callback) {
        return localNetworkCallbackList.remove(callback);
    }

    public void removeAllCallbacks() {
        localNetworkCallbackList.clear();
    }
    public interface LocalNetworkCallback {
        void onChanged(ArrayList<DeviceUnderNetwork> deviceUnderNetworkArrayList);
    }



}
