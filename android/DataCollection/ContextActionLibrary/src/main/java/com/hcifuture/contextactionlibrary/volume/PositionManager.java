package com.hcifuture.contextactionlibrary.volume;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.sensor.collector.Collector;
import com.hcifuture.contextactionlibrary.sensor.collector.CollectorResult;
import com.hcifuture.contextactionlibrary.sensor.collector.async.GPSCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.WifiCollector;
import com.hcifuture.contextactionlibrary.sensor.data.GPSData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleWifiData;
import com.hcifuture.contextactionlibrary.sensor.data.WifiData;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.contextactionlibrary.sensor.uploader.TaskMetaBean;
import com.hcifuture.contextactionlibrary.utils.FileUtils;

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

public class PositionManager {
    private static final String TAG = "PositionManager";
    ScheduledExecutorService scheduledExecutorService;
    List<ScheduledFuture<?>> futureList;
    private GPSCollector gpsCollector;
    private WifiCollector wifiCollector;
    private VolEventListener volEventListener;

    private ScheduledFuture<?> scheduledPositionDetection;
    private long initialDelay = 0;
    private long period = 60000 * 15;  // detect noise every 30s
    private List<Position> positions;
    private List<HistoryItem> history;
    private Position lastPosition;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public PositionManager(ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, GPSCollector gpsCollector, WifiCollector wifiCollector, VolEventListener volEventListener) {
        this.volEventListener = volEventListener;
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
        this.gpsCollector = gpsCollector;
        this.wifiCollector = wifiCollector;
        positions = getPositionsFromFile();
        history = getPositionHistoryFromFile();
        lastPosition = null;
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Position> getPositionsFromFile() {
        Type type = new TypeToken<List<Position>>(){}.getType();
        List<Position> result = Collector.gson.fromJson(
                FileUtils.getFileContent(ConfigContext.VOLUME_SAVE_FOLDER + "position.json"),
                type
        );
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void writePositionsToFile() {
        String result = Collector.gson.toJson(positions);
        FileUtils.writeStringToFile(result, new File(ConfigContext.VOLUME_SAVE_FOLDER + "position.json"));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<HistoryItem> getPositionHistoryFromFile() {
        Type type = new TypeToken<List<HistoryItem>>(){}.getType();
        List<HistoryItem> result = Collector.gson.fromJson(
                FileUtils.getFileContent(ConfigContext.VOLUME_SAVE_FOLDER + "position_history.json"),
                type
        );
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void writeHistoryToFile() {
        String result = Collector.gson.toJson(history);
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
        return lastPosition.getId();
    }

    public List<HistoryItem> getHistory() {
        return history;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String scanAndGetId() {
        try {
            return scanAndUpdate().get().getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void start() {
        // detect position periodically
        Log.e(TAG, "schedule periodic position detection");
        scheduledPositionDetection = scheduledExecutorService.scheduleAtFixedRate(() -> {
            scanAndUpdate();
        }, initialDelay, period, TimeUnit.MILLISECONDS);
        futureList.add(scheduledPositionDetection);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public CompletableFuture<Position> scanAndUpdate() {
        CompletableFuture<Position> ft = new CompletableFuture<>();

        Log.e(TAG, "start to detect position");
        List<CompletableFuture<CollectorResult>> fts = new ArrayList<>();
        fts.add(gpsCollector.getData(new TriggerConfig()));
        fts.add(wifiCollector.getData(new TriggerConfig()));
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(fts.toArray(new CompletableFuture[0]));
        allFutures.thenAccept(v -> {
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
                if (lastPosition == null || !lastPosition.sameAs(position)) {
                    // 查找是否是新地点
                    Position tmp = findInList(position);
                    if (tmp == null) {
                        positions.add(position);
                        writePositionsToFile();
                        tmp = position;
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
                        volEventListener.onVolEvent(VolEventListener.EventType.Position, bundle);
                    }
                    lastPosition = tmp;
                }
                ft.complete(lastPosition);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return ft;
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
