package com.hcifuture.contextactionlibrary.volume;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.sensor.collector.Collector;
import com.hcifuture.contextactionlibrary.sensor.collector.CollectorResult;
import com.hcifuture.contextactionlibrary.sensor.collector.async.BluetoothCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.GPSCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.WifiCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;
import com.hcifuture.contextactionlibrary.sensor.data.BluetoothData;
import com.hcifuture.contextactionlibrary.sensor.data.GPSData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleBluetoothData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleWifiData;
import com.hcifuture.contextactionlibrary.sensor.data.WifiData;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.contextactionlibrary.sensor.uploader.TaskMetaBean;
import com.hcifuture.contextactionlibrary.utils.FileUtils;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PositionManager extends TriggerManager {
    private static final String TAG = "PositionManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private GPSCollector gpsCollector;
    private WifiCollector wifiCollector;

    private ScheduledFuture<?> scheduledPositionDetection;
    private long initialDelay = 0;
    private long period = 1000 * 15;  // detect position every 15s
    private List<Position> positions;
    private List<HistoryItem> history;
    private Position lastPosition;
    public static Integer latest_position;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public PositionManager(VolEventListener volEventListener, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, GPSCollector gpsCollector, WifiCollector wifiCollector, LogCollector logCollector) {
        super(volEventListener);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.gpsCollector = gpsCollector;
        this.wifiCollector = wifiCollector;
        positions = getPositionsFromFile();
        history = getPositionHistoryFromFile();
        lastPosition = history.size() > 0? findById(history.get(history.size() - 1).getId()) : null;
        this.logCollector = logCollector;
    }

    public static class HistoryItem {
        private String id;
        private long inTime;
        private long outTime;

        public HistoryItem(String id, long inTime, long outTime) {
            this.id = id;
            this.inTime = inTime;
            this.outTime = outTime;
        }

        public String getId() { return id; }

        public long getInTime() { return inTime; }

        public long getOutTime() { return outTime; }

        public void setInTime(long inTime) { this.inTime = inTime; }

        public void setOutTime(long outTime) { this.outTime = outTime; }
    }

    public Position findById(String id) {
        if (positions == null) return null;
        for (Position position: positions) {
            if (position.getId().equals(id))
                return position;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Position> getPositionsFromFile() {
        Type type = new TypeToken<List<Position>>(){}.getType();
        List<Position> result = Collector.gson.fromJson(
                FileUtils.getFileContent(ConfigContext.VOLUME_SAVE_FOLDER + "position.json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void writePositionsToFile() {
        String result = Collector.gson.toJson(positions);
        Log.e(TAG, "Position List: " + result);
        FileUtils.writeStringToFile(result, new File(ConfigContext.VOLUME_SAVE_FOLDER + "position.json"));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<HistoryItem> getPositionHistoryFromFile() {
        Type type = new TypeToken<List<HistoryItem>>(){}.getType();
        List<HistoryItem> result = Collector.gson.fromJson(
                FileUtils.getFileContent(ConfigContext.VOLUME_SAVE_FOLDER + "position_history.json"),
                type
        );
        if (result == null)
            result = new ArrayList<>();
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void writeHistoryToFile() {
        String result = Collector.gson.toJson(history);
        Log.e(TAG, "History List: " + result);
        FileUtils.writeStringToFile(result, new File(ConfigContext.VOLUME_SAVE_FOLDER + "position_history.json"));
    }

    public List<String> getPositionList() {
        List<String> result = new ArrayList<>();
        for (Position position: positions) {
            result.add(position.getId());
        }
        return  result;
    }

    public String getPresentPosition() {
        if (lastPosition == null) {
            return "NULL";
        } else {
            return lastPosition.getId();
        }
    }

    public List<HistoryItem> getHistory() {
        return history;
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
//    public String scanAndGetId() {
//        try {
//            return scanAndUpdate().get().getId();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return "ERROR";
//    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void start() {
        // detect position periodically
        Log.e(TAG, "schedule periodic position detection");
        scheduledPositionDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
            scanAndUpdate();
        }, initialDelay, period, TimeUnit.MILLISECONDS);
        futureList.add(scheduledPositionDetection);
    }

    @Override
    public void stop() {
        if (scheduledPositionDetection != null) {
            scheduledPositionDetection.cancel(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public CompletableFuture<Position> scanAndUpdate() {
//        CompletableFuture<Position> ft = new CompletableFuture<>();

        Log.e(TAG, "start to detect position");
        List<CompletableFuture<CollectorResult>> fts = new ArrayList<>();
        fts.add(gpsCollector.getData(new TriggerConfig()));
        fts.add(wifiCollector.getData(new TriggerConfig()));
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(fts.toArray(new CompletableFuture[0]));
        return allFutures.thenApply(v -> {
            try {
                GPSData gpsData = (GPSData) fts.get(0).get().getData();
                WifiData wifiData = (WifiData) fts.get(1).get().getData();

                List<String> wifiIds = new ArrayList<>();
                if (wifiData != null) {
                    List<SingleWifiData> singleWifiDataList = wifiData.getAps();
                    for (SingleWifiData singleWifiData: singleWifiDataList) {
                        String key = singleWifiData.getSsid() + singleWifiData.getBssid();
                        wifiIds.add(key);
                    }
                }
                double latitude = -200;
                double longitude = -200;
                if (gpsData != null) {
                    latitude = gpsData.getLatitude();
                    longitude = gpsData.getLongitude();
                }
                Position position = new Position("" + System.currentTimeMillis(), "unknown", latitude, longitude, wifiIds);
                String type;
                if (lastPosition != null && lastPosition.sameAs(position)) {
                    Log.e(TAG, "Old Position: " + lastPosition.getId());
                    type = "old";
                    JSONObject json = new JSONObject();
                    JSONUtils.jsonPut(json, "change", false);
                    JSONUtils.jsonPut(json, "position", lastPosition.getId());
                    record(System.currentTimeMillis(), incLogID(), TAG, "periodic_scan", "", json.toString());
                }
                if (lastPosition == null || !lastPosition.sameAs(position)) {
                    // 查找是否是新地点
                    Position tmp = findInList(position);
                    if (tmp == null) {
                        positions.add(position);
                        Log.e(TAG, "New Position: " + position.getId());
                        writePositionsToFile();
                        tmp = position;
                        type = "new";
                    } else {
                        Log.e(TAG, "Old Position: " + tmp.getId());
                        type = "old";
                    }
                    // 更新地点历史
                    if (history.size() > 0) {
                        HistoryItem historyItem = history.get(history.size() - 1);
                        historyItem.setOutTime(System.currentTimeMillis());
                        history.remove(history.size() - 1);
                        history.add(historyItem);
                    }
                    history.add(new HistoryItem(tmp.getId(), System.currentTimeMillis(), -1));
                    writeHistoryToFile();
                    if (lastPosition != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", tmp.getId());
                        bundle.putString("name", tmp.getName());
                        Log.e(TAG, "Position Changed from " + lastPosition.getId() + " to " + tmp.getId() + ", timestamp: " + System.currentTimeMillis());
                        volEventListener.onVolEvent(VolEventListener.EventType.Position, bundle);
                    }
                    JSONObject json = new JSONObject();
                    JSONUtils.jsonPut(json, "change", true);
                    JSONUtils.jsonPut(json, "previous_position", lastPosition.getId());
                    JSONUtils.jsonPut(json, "now_position", tmp.getId());
                    JSONUtils.jsonPut(json, "type", type);
                    record(System.currentTimeMillis(), incLogID(), TAG, "periodic_scan", "", json.toString());
                    lastPosition = tmp;
                    latest_position = Integer.parseInt(lastPosition.getId());
                }
//                ft.complete(lastPosition);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lastPosition;
        });

//        return ft;
    }

    public Position findInList(Position position) {
        if (positions == null) return null;

        for (Position _position: positions) {
            if (_position.sameAs(position)) {
                return _position;
            }
        }
        return null;
    }
}
