package com.hcifuture.contextactionlibrary.contextaction.context;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.google.gson.reflect.TypeToken;
import com.hcifuture.contextactionlibrary.sensor.collector.Collector;
import com.hcifuture.contextactionlibrary.sensor.collector.CollectorManager;
import com.hcifuture.contextactionlibrary.sensor.collector.async.AudioCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.BluetoothCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.GPSCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.WifiCollector;
import com.hcifuture.contextactionlibrary.sensor.data.NonIMUData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleIMUData;
import com.hcifuture.contextactionlibrary.utils.FileUtils;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;
import com.hcifuture.contextactionlibrary.volume.AppManager;
import com.hcifuture.contextactionlibrary.volume.CrowdManager;
import com.hcifuture.contextactionlibrary.volume.DeviceManager;
import com.hcifuture.contextactionlibrary.volume.MotionManager;
import com.hcifuture.contextactionlibrary.volume.Position;
import com.hcifuture.contextactionlibrary.volume.PositionManager;
import com.hcifuture.contextactionlibrary.volume.SoundManager;
import com.hcifuture.contextactionlibrary.volume.TimeManager;
import com.hcifuture.contextactionlibrary.volume.VolEventListener;
import com.hcifuture.contextactionlibrary.volume.VolumeContext;
import com.hcifuture.contextactionlibrary.volume.NoiseManager;
import com.hcifuture.contextactionlibrary.volume.VolumeManager;
import com.hcifuture.contextactionlibrary.volume.VolumeRuleManager;
import com.hcifuture.contextactionlibrary.volume.data.DataUtils;
import com.hcifuture.contextactionlibrary.volume.data.Reason;
import com.hcifuture.shared.communicate.config.ContextConfig;
import com.hcifuture.contextactionlibrary.contextaction.event.BroadcastEvent;
import com.hcifuture.shared.communicate.listener.ContextListener;
import com.hcifuture.shared.communicate.listener.RequestListener;
import com.hcifuture.shared.communicate.result.ContextResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ConfigContext extends BaseContext implements VolEventListener {

    private static final String TAG = "ConfigContext";

    public static String VOLUME_SAVE_FOLDER;
    public static String FILE_TMP_DATA = "tmp_data.csv";
    public static String FILE_CONTEXT_MAP = "context2fid.json";
    
    public static String NEED_AUDIO = "context.config.need_audio";
    public static String NEED_NONIMU = "context.config.need_nonimu";
    public static String NEED_SCAN = "context.config.need_scan";
    public static String NEED_POSITION = "context.config.need_position";

    // communication from UI
    static final String EXTERNAL_TYPE = "event.ui.volume";
    static final int EVENT_POPUP = 1;
    static final int EVENT_QUIETMODE = 2;
    static final int EVENT_CAPTURE_PERMISSION = 3;

    // communication to UI
    static final String CONTEXT_VOLUME = "context.volume";
    static final int CONTEXT_EVENT_POPUP = 1;
    static final int CONTEXT_EVENT_CAPTURE_PERMISSION = 3;
    static final int CONTEXT_EVENT_REASONS = 4;

    // front end state & popup reason
    static int TYPE_OFF = -1;
    static final int REASON_MANUAL = 0;
    static final int REASON_AUTO_DIRECT = 1;
    static final int REASON_AUTO_COUNTDOWN = 2;

    // modes
    public static int MODE_NORMAL = 0;
    public static int MODE_QUIET = 1;

    // key gesture
    private int keyDownCount = 0;
    private long lastKeyDownTime = 0;

    private String appName;
    private int brightness;
    private final HashMap<String, Integer> volume;

    private long last_record_all;
    private VolumeRuleManager volumeRuleManager;

    private final AtomicInteger mLogID = new AtomicInteger(0);

    private int frontEndState = TYPE_OFF;
    private int currentMode = MODE_NORMAL;

    private final NoiseManager noiseManager;
    private final AppManager appManager;
    private final PositionManager positionManager;
    private final CrowdManager crowdManager;
    private final DeviceManager deviceManager;
    private final SoundManager soundManager;
    private final MotionManager motionManager;
    private final TimeManager timeManager;

    private final DataUtils dataUtils;

    private final VolumeManager volumeManager;

    private Map<String, String> context2FID;

    private CompletableFuture<Double> detectedNoiseFt = null;
    private CompletableFuture<List<List<CrowdManager.BluetoothItem>>> scanBluetoothDeviceFt = null;
    private CompletableFuture<Position> detectPositionFt = null;
    private CompletableFuture<Void> allFutures = null;
    private List<CompletableFuture<?>> fts = new ArrayList<>();

    public ConfigContext(Context context, ContextConfig config, RequestListener requestListener, List<ContextListener> contextListener, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList, CollectorManager collectorManager) {
        super(context, config, requestListener, contextListener, scheduledExecutorService, futureList);

        VOLUME_SAVE_FOLDER = context.getExternalMediaDirs()[0].getAbsolutePath() + "/Data/Volume/";

        volumeRuleManager = new VolumeRuleManager();

        volumeManager = new VolumeManager();

        noiseManager = new NoiseManager(this, scheduledExecutorService, futureList,
                (AudioCollector) collectorManager.getCollector(CollectorManager.CollectorType.Audio));

        deviceManager = new DeviceManager(this, mContext, scheduledExecutorService, futureList);

        soundManager = new SoundManager(this, mContext, scheduledExecutorService, futureList);

        appManager = new AppManager(this, mContext);

        crowdManager = new CrowdManager(this, scheduledExecutorService, futureList,
                (BluetoothCollector) collectorManager.getCollector(CollectorManager.CollectorType.Bluetooth), mContext);

        positionManager = new PositionManager(this, scheduledExecutorService, futureList,
                (GPSCollector) collectorManager.getCollector(CollectorManager.CollectorType.GPS),
                (WifiCollector) collectorManager.getCollector(CollectorManager.CollectorType.Wifi));

        motionManager = new MotionManager(this);

        timeManager = new TimeManager(this, scheduledExecutorService, futureList);

        dataUtils = new DataUtils(mContext);

        readContextMap();

        // initialize
        appName = "";
        brightness = 0;
        volume = new HashMap<>();
        // speaker
        volume.put("volume_music_speaker", 0);
        volume.put("volume_ring_speaker", 0);
        volume.put("volume_alarm_speaker", 0);
        volume.put("volume_voice_speaker", 0);
        volume.put("volume_tts_speaker", 0);
        // headset
        volume.put("volume_music_headset", 0);
        volume.put("volume_voice_headset", 0);
        volume.put("volume_tts_headset", 0);
        // headphone
        volume.put("volume_music_headphone", 0);
        volume.put("volume_voice_headphone", 0);
        volume.put("volume_tts_headphone", 0);
        // Bluetooth A2DP
        volume.put("volume_music_bt_a2dp", 0);
        volume.put("volume_voice_bt_a2dp", 0);
        volume.put("volume_tts_bt_a2dp", 0);
        // earpiece
        volume.put("volume_music_earpiece", 0);

        last_record_all = 0;

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void start() {
        record_all("start");
        appManager.start();
        noiseManager.start();
        deviceManager.start();
        positionManager.start();
        crowdManager.start();
        soundManager.start();
        timeManager.start();

        // get audio capture permission
        if (!soundManager.hasCapturePermission()) {
            notifyRequestRecordPermission();
        }
    }

    @Override
    public void stop() {
        Log.e(TAG, "stop");
        // do not perform record_all() in stop(),
        // it may cause crashes when frequently called

        soundManager.stop();
        crowdManager.stop();
        positionManager.stop();
        deviceManager.stop();
        noiseManager.stop();
        appManager.stop();
        timeManager.stop();
    }

    @Override
    public void onIMUSensorEvent(SingleIMUData data) {
        motionManager.onIMUSensorEvent(data);
    }

    @Override
    public void onNonIMUSensorEvent(NonIMUData data) {

    }

    @Override
    public void getContext() {
        long current_call = System.currentTimeMillis();
        // periodically record_all() every 30 min
        if (current_call - last_record_all >= 30 * 60000) {
//            record_all("period_30m");
            JSONObject json = new JSONObject();
            JSONUtils.jsonPut(json, "devices", deviceManager.getDeviceIDs());
            JSONUtils.jsonPut(json, "positions", positionManager.getPositionList());
            record(current_call, incLogID(), "period_30m", "", "", json.toString());
            last_record_all = current_call;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        appManager.onAccessibilityEvent(event);
    }

    public VolumeContext getPresentContext() {
        Calendar c = Calendar.getInstance();
        int time = c.get(Calendar.HOUR_OF_DAY) * 100 + c.get(Calendar.MINUTE);
        double latitude = -200;
        double longitude = -200;
        List<String> wifiIds = new ArrayList<>();
        Position curPos = positionManager.findById(positionManager.getPresentPosition());
        if (curPos != null) {
            latitude = curPos.getLatitude();
            longitude = curPos.getLongitude();
            wifiIds = curPos.getWifiIds();
        }
        
        double noise = noiseManager.getPresentNoise();
//        try {
//            // length: 300 is ok, 200 is not
//            noise = audioCollector.getNoiseLevel(300, 10).get(500, TimeUnit.MILLISECONDS);
//        } catch (ExecutionException | InterruptedException | TimeoutException e) {
//            e.printStackTrace();
//            Log.e(TAG, "getPresentContext: error happens");
//            noise = audioCollector.lastest_noise;
//        }
        Log.e(TAG, "getPresentContext: noise = " + noise);
        String app = appName;
        String device = deviceManager.getPresentDeviceID();
        Log.e("noise", "" + noise);
        Log.e("device", device);
        Log.e("latitude", "" + latitude);
        Log.e("longitude", "" + longitude);
        Log.e("time", "" + time);
        Log.e("app", app);
        return new VolumeContext(-1, noise, device, -1, -1, latitude, longitude, wifiIds, 4, time, time, app, -1);
    }

    public Bundle getRules(VolumeContext volumeContext, int type) {
        Bundle result = volumeRuleManager.getRecommendation(volumeContext);
        if (result != null)
            result.putInt("type", type);
        return result;
    }

//    public List<Double> getVolumes(VolumeContext volumeContext) {
//        List<VolumeRule> volumeRules = volumeRuleManager.getRecommendation(volumeContext);
//        List<Double> _volumes = new ArrayList<>();
//        for (VolumeRule volumeRule: volumeRules) {
//            _volumes.add(volumeRule.getVolume());
//        }
//        return _volumes;
//    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onBroadcastEvent(BroadcastEvent event) {
        long timestamp = event.getTimestamp();
        int logID = incLogID();
        String action = event.getAction();
        String type = event.getType();
        String tag = "";
        Bundle extras = event.getExtras();

        boolean record = false;
        JSONObject json = new JSONObject();

        try {
            if ("ContentChange".equals(type)) {
//                record = true;
                if (!"uri_null".equals(action)) {
                    Uri uri = Uri.parse(action);

                    String database_key = uri.getLastPathSegment();
                    String inter = uri.getPathSegments().get(0);
                    if ("system".equals(inter)) {
                        tag = Settings.System.getString(mContext.getContentResolver(), database_key);
                    } else if ("global".equals(inter)) {
                        tag = Settings.Global.getString(mContext.getContentResolver(), database_key);
                    } else if ("secure".equals(inter)) {
                        tag = Settings.Secure.getString(mContext.getContentResolver(), database_key);
                    }

                    int value = Settings.System.getInt(mContext.getContentResolver(), database_key, 0);

                    // record special information
                    if (Settings.System.SCREEN_BRIGHTNESS.equals(database_key)) {
                        // record brightness value difference and update
                        int diff = value - brightness;
                        JSONUtils.jsonPut(json, "diff", diff);
                        brightness = value;
                        // record brightness mode
                        int mode = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
                        if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                            JSONUtils.jsonPut(json, "mode", "man");
                        } else if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                            JSONUtils.jsonPut(json, "mode", "auto");
                        } else {
                            JSONUtils.jsonPut(json, "mode", "unknown");
                        }
//                        notifyContext(NEED_NONIMU, timestamp, logID, "screen brightness change");
                    } else if (database_key.startsWith("volume_")) {
                        record = true;
                        if (!volume.containsKey(database_key)) {
                            // record new volume value
                            volume.put(database_key, value);
                        }
                        // record volume value difference and update
                        int diff = value - volume.put(database_key, value);
                        JSONUtils.jsonPut(json, "diff", diff);
//                        notifyContext(NEED_AUDIO, timestamp, logID, "volume change: " + database_key);
//                        notifyContext(NEED_SCAN, timestamp, logID, "volume change: " + database_key);
//                        String[] tmp = database_key.split("_");
//                        String deviceType = "";
//                        for (int index = 2; index < tmp.length; index++) {
//                            deviceType += tmp[index];
//                            if (index < tmp.length - 1) {
//                                deviceType += "_";
//                            }
//                        }
//                        latest_deviceType = deviceType;
                    } else if (Settings.Global.BLUETOOTH_ON.equals(database_key) && value == 1) {
//                    notify(NEED_SCAN, timestamp, logID, "Bluetooth on via global setting");
                    } else if (Settings.Global.WIFI_ON.equals(database_key) && value == 2) {
//                    notify(NEED_SCAN, timestamp, logID, "Wifi on via global setting");
                    }
                }
            } else if ("BroadcastReceive".equals(type)) {
                switch (action) {
                    case Intent.ACTION_CONFIGURATION_CHANGED:
                        Configuration config = mContext.getResources().getConfiguration();
                        JSONUtils.jsonPut(json, "configuration", config.toString());
                        JSONUtils.jsonPut(json, "orientation", config.orientation);
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                    case Intent.ACTION_SCREEN_ON:
                        record = true;
                        // ref: https://stackoverflow.com/a/17348755/11854304
                        DisplayManager dm = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
                        if (dm != null) {
                            Display[] displays = dm.getDisplays();
                            int[] states = new int[displays.length];
                            for (int i = 0; i < displays.length; i++) {
                                states[i] = displays[i].getState();
                            }
                            JSONUtils.jsonPut(json, "displays", states);
                        }
                        break;
                    case Intent.ACTION_POWER_CONNECTED:
                    case Intent.ACTION_POWER_DISCONNECTED:
                        record = true;
                        break;
                }
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
//                    notifyContext(NEED_SCAN, timestamp, logID, "screen on");
                } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action) && extras.getInt(WifiManager.EXTRA_WIFI_STATE) == WifiManager.WIFI_STATE_ENABLED) {
//                    notifyContext(NEED_SCAN, timestamp, logID, "Wifi on via broadcast");
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) && extras.getInt(BluetoothAdapter.EXTRA_STATE) == BluetoothAdapter.STATE_ON) {
//                    notifyContext(NEED_SCAN, timestamp, logID, "Bluetooth on via broadcast");
                }
            } else if ("KeyEvent".equals(type)) {
                record = true;
                int keycode = extras.getInt("code");
                int keyAction = extras.getInt("action");
                JSONUtils.jsonPut(json, "keycodeString", KeyEvent.keyCodeToString(keycode));

//                detectKeyGesture(keycode, keyAction);
//                detectKeyVolume(keycode);

//                switch (keycode) {
//                    case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
//                    case KeyEvent.KEYCODE_MEDIA_CLOSE:
//                    case KeyEvent.KEYCODE_MEDIA_EJECT:
//                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
//                    case KeyEvent.KEYCODE_MEDIA_NEXT:
//                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
//                    case KeyEvent.KEYCODE_MEDIA_PLAY:
//                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
//                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
//                    case KeyEvent.KEYCODE_MEDIA_RECORD:
//                    case KeyEvent.KEYCODE_MEDIA_REWIND:
//                    case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
//                    case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
//                    case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
//                    case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
//                    case KeyEvent.KEYCODE_MEDIA_STOP:
//                    case KeyEvent.KEYCODE_MEDIA_TOP_MENU:
//                    case KeyEvent.KEYCODE_VOLUME_DOWN:
//                    case KeyEvent.KEYCODE_VOLUME_MUTE:
//                    case KeyEvent.KEYCODE_VOLUME_UP:
//                        notifyContext(NEED_AUDIO, timestamp, logID, "key event: " + KeyEvent.keyCodeToString(keycode));
//                        notifyContext(NEED_SCAN, timestamp, logID, "key event: " + KeyEvent.keyCodeToString(keycode));
//                        notifyContext(NEED_POSITION, timestamp, logID, "key event: " + KeyEvent.keyCodeToString(keycode));
//                }

//                // special test for audio capture functionality
//                if (keycode == KeyEvent.KEYCODE_VOLUME_UP && keyAction == KeyEvent.ACTION_DOWN) {
//                    // record 10s
//                    soundManager.startAudioCapture(10000);
//                }
            }

            if (record) {
                JSONUtils.jsonPut(json, "package", appName);
                JSONUtils.jsonPutBundle(json, extras);
                record(timestamp, logID, type, action, tag, json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONUtils.jsonPut(json, "package", appName);
            JSONUtils.jsonPutBundle(json, extras);
            JSONUtils.jsonPut(json, "exception", e.toString());
            record(timestamp, logID, type, action, tag, json.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onExternalEvent(Bundle bundle) {
        if (bundle.containsKey("type") && EXTERNAL_TYPE.equals(bundle.getString("type"))) {
            int event = bundle.getInt("event");
            switch (event) {
                case EVENT_POPUP:
                    boolean popup = bundle.getBoolean("popup");
                    recordEvent(EventType.FrontEnd, "popup", Collector.gson.toJson(bundle));
                    if (popup) {
                        // popup
                        int reason = bundle.getInt("reason");
                        frontEndState = reason;
                        Log.e(TAG, "onExternalEvent: pop up, reason: " + reason);
                        if (reason == REASON_MANUAL) {
                            // detect current noise
                            if (detectedNoiseFt == null) {
                                detectedNoiseFt = noiseManager.detectNoise(5000, 10);
                            }
                            if (detectPositionFt == null) {
                                detectPositionFt = positionManager.scanAndUpdate();
                            }
                            if (scanBluetoothDeviceFt == null) {
                                scanBluetoothDeviceFt = crowdManager.scanAndUpdate();
                            }
                            fts.clear();
                            fts.add(detectedNoiseFt);
                            fts.add(detectPositionFt);
                            fts.add(scanBluetoothDeviceFt);
                            allFutures = CompletableFuture.allOf(fts.toArray(new CompletableFuture[0]));

                            Log.e(TAG, "Local Context Data: " + String.format("%d,%f,%s,%s,%s,%b,%d",
                                    System.currentTimeMillis(),
                                    noiseManager.getPresentNoise(),
                                    deviceManager.getPresentDeviceID(),
                                    appManager.getPresentApp(),
                                    positionManager.getPresentPosition(),
                                    soundManager.isAudioOn(),
                                    soundManager.getAudioMode()
                            ));
                            List<CrowdManager.BluetoothItem> bluetoothItemList = crowdManager.getBleList();
                            for (CrowdManager.BluetoothItem bluetoothItem: bluetoothItemList) {
                                Log.e(TAG, bluetoothItem.toString());
                            }
                            Bundle bundle1 = new Bundle();
                            bundle1.putInt("event", CONTEXT_EVENT_REASONS);
                            bundle1.putStringArrayList("factors", new ArrayList<>(dataUtils.getReasonStringList()));
                            notifyFrontend(CONTEXT_VOLUME, bundle1);
                        }
                    } else {
                        // exit
                        Log.e(TAG, "onExternalEvent: UI exit");
                        int from = bundle.getInt("from");
                        double finalVolume = -1;
                        int systemVolume = -1;
                        int streamType = -1;
                        String keyFactor = null;
                        if (from == 1) {
                            int behavior = bundle.getInt("behavior");
                            streamType = bundle.getInt("streamType");
                            finalVolume = bundle.getDouble("finalVolume");
                            systemVolume = bundle.getInt("systemVolume");
                            keyFactor = bundle.getString("keyFactor");
                            if (keyFactor == null) {
                                recordEvent(EventType.FrontEnd, "volume_adjust_without_choosing_reason", "");
                            }
                            Log.e(TAG, "onExternalEvent: from:" + from + ", behavior:" + behavior + ", systemVolume:" + systemVolume + ", finalVolume:" + finalVolume + ", keyFactor:" + keyFactor);
//                            if (frontEndState == REASON_MANUAL) {
//                                if (detectedNoiseFt != null) {
//                                    Log.e(TAG, "onExternalEvent: check manual noise detection");
//                                    detectedNoiseFt.whenComplete((v, e) -> {
//                                        // 当用户调整结束且噪音检测结束时，记录用户调整音量及噪音值
//                                        Log.e(TAG, "onExternalEvent: manual detectedNoiseFt " + v + " " + e);
//                                        addData(finalVolume, REASON_MANUAL, behavior, "updated_noise", false);
//                                        Log.e(TAG, "onExternalEvent: recorded to file");
//                                        detectedNoiseFt = null;
//                                    });
//                                } else {
//                                    Log.e(TAG, "onExternalEvent: manual null detectedNoiseFt (already stopped) ");
//                                    addData(finalVolume, frontEndState, behavior, "last_noise", false);
//                                    Log.e(TAG, "onExternalEvent: recorded to file");
//                                }
//                            } else if (frontEndState == REASON_AUTO_DIRECT || frontEndState == REASON_AUTO_COUNTDOWN) {
//                                if (behavior != 0) {
//                                    Log.e(TAG, "onExternalEvent: auto modified by hand");
//                                    addData(finalVolume, frontEndState, behavior, "last_noise", true);
//                                    Log.e(TAG, "onExternalEvent: recorded to file");
//                                }
//                            }
                        } else if (from == 2) {
                            streamType = bundle.getInt("streamType");
                            finalVolume = bundle.getDouble("finalVolume");
                            systemVolume = bundle.getInt("systemVolume");
                            ArrayList<String> factors = bundle.getStringArrayList("factors");
                            String newFactor = bundle.getString("newFactor");
                            keyFactor = bundle.getString("keyFactor");
                            boolean behavior = bundle.getBoolean("behavior");
                            if (!behavior) {
                                recordEvent(EventType.FrontEnd, "volume_adjust_without_choosing_reason", "");
                            }
                            Log.e(TAG, "onExternalEvent: from:" + from + ", finalVolume:" + finalVolume + ", newFactor:" + newFactor + ", keyFactor:" + keyFactor);
                            if (newFactor != null)
                                dataUtils.addReason(new Reason("" + System.currentTimeMillis(), newFactor));
                        }
                        if (frontEndState == REASON_MANUAL) {
                            Bundle context = new Bundle();
                            context.putDouble("volume", finalVolume);
                            context.putInt("systemVolume", systemVolume);
                            context.putInt("streamType", streamType);
                            context.putLong("time", System.currentTimeMillis());
                            context.putDouble("audio", SoundManager.SYSTEM_VOLUME);
                            context.putString("app", appManager.getPresentApp());
                            context.putString("device", deviceManager.getPresentDeviceID());
                            if (allFutures != null) {
                                Log.e(TAG, "onExternalEvent: check manual detection");
                                String finalKeyFactor = keyFactor;
                                allFutures.whenComplete((v, e) -> {
                                    if (fts.size() == 3) {
                                        try {
                                            context.putDouble("noise", (double) fts.get(0).get());
                                            context.putString("position", ((Position) fts.get(1).get()).getId());
                                            List<List<CrowdManager.BluetoothItem>> listOfList = (List<List<CrowdManager.BluetoothItem>>) fts.get(2).get();
                                            if (listOfList != null && listOfList.size() == 2) {
                                                context.putStringArrayList("bleDevices", new ArrayList<>(CrowdManager.blItemList2StringList(listOfList.get(1))));
                                                context.putStringArrayList("filteredDevices", new ArrayList<>(CrowdManager.blItemList2StringList(listOfList.get(0))));
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                    if (finalKeyFactor != null)
                                        dataUtils.addContextForReason(DataUtils.getReasonByName(dataUtils.getReasonList(), finalKeyFactor), context);
//                                    record(System.currentTimeMillis(), incLogID(), TAG, "manual_detect", "reason: " + finalKeyFactor, Collector.gson.toJson(context));
                                    recordEvent(EventType.FrontEnd, "manual_detect", Collector.gson.toJson(context));
                                    fts.clear();
                                    allFutures = null;
                                });
                            } else {
                                Log.e(TAG, "onExternalEvent: manual null detection (already stopped) ");
                                recordEvent(EventType.FrontEnd, "manual_detect", Collector.gson.toJson(context));
                            }
                        }
                        frontEndState = TYPE_OFF;
                    }
                    break;
                case EVENT_QUIETMODE:
                    currentMode = bundle.getBoolean("quietMode")? MODE_QUIET : MODE_NORMAL;
                    Log.e(TAG, "onExternalEvent: quiet mode changed to " + currentMode);
                    break;
                case EVENT_CAPTURE_PERMISSION:
                    int resultCode = bundle.getInt("resultCode");
                    Intent data = bundle.getParcelable("data");
                    Log.e(TAG, "onExternalEvent: get capture permission result: " + resultCode);
                    soundManager.saveAudioCaptureToken(resultCode, data);
                    soundManager.start();
                    break;
            }
        }
    }

    public void writeContextMap() {
        String result = Collector.gson.toJson(context2FID);
        FileUtils.writeStringToFile(result, new File(ConfigContext.VOLUME_SAVE_FOLDER + FILE_CONTEXT_MAP));
    }

    public void readContextMap() {
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        context2FID = Collector.gson.fromJson(
                FileUtils.getFileContent(ConfigContext.VOLUME_SAVE_FOLDER + FILE_CONTEXT_MAP),
                type
        );
        if (context2FID == null) {
            context2FID = new HashMap<>();
        }
    }

    private String getCurrentFID() {
        String currentContextID = getCurrentContextID();
        if (context2FID.containsKey(currentContextID)) {
            return context2FID.get(currentContextID);
        } else {
            String newFID = volumeManager.newFunction();
            context2FID.put(currentContextID, newFID);
            writeContextMap();
            return newFID;
        }
    }

    private String getCurrentContextID() {
        return "@" + deviceManager.getPresentDeviceID() + "@" + appManager.getPresentApp() + "@" + positionManager.getPresentPosition();
    }

    private void addData(double volume, int frontEndState, int behavior, String tag, boolean force) {
        appendLine(String.format("%d,%f,%f,%s,%s,%s,%b,%d,%d,%d,%s,%d",
                System.currentTimeMillis(),
                noiseManager.getPresentNoise(),
                volume,
                deviceManager.getPresentDeviceID(),
                appManager.getPresentApp(),
                positionManager.getPresentPosition(),
                soundManager.isAudioOn(),
                soundManager.getAudioMode(),
                frontEndState,
                behavior,
                tag,
                currentMode
        ), FILE_TMP_DATA);
        if (force || soundManager.isAudioOn() && currentMode == MODE_NORMAL) {
            // only record when audio is on and in normal mode
            double presentNoise = noiseManager.getPresentNoise();
            Log.e(TAG, "Add record: [FID]" + getCurrentFID() + " [Noise]" + presentNoise + " [Volume]" + volume);
            volumeManager.addRecord(getCurrentFID(), presentNoise, volume);
        }
    }

    private void appendLine(String line, String filename) {
        FileUtils.writeStringToFile(line, new File(VOLUME_SAVE_FOLDER + filename), true);
    }

    private int incLogID() {
        return mLogID.getAndIncrement();
    }

    private void record(long timestamp, int logID, String type, String action, String tag, String other) {
        if (logCollector == null) {
            return;
        }

        String line = timestamp + "\t" + logID + "\t" + type + "\t" + action + "\t" + tag + "\t" + other;
        logCollector.addLog(line);
        Log.e("ConfigContext", "record: " + line);
    }

    private synchronized void record_all(String action) {
        if (logCollector == null) {
            return;
        }

        last_record_all = System.currentTimeMillis();
        int logID = incLogID();
        JSONObject json = new JSONObject();

        // store brightness
        brightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        JSONUtils.jsonPut(json, "brightness", brightness);

        // store volumes
        for (String key : volume.keySet()) {
            int value = Settings.System.getInt(mContext.getContentResolver(), key, 0);
            volume.put(key, value);
            JSONUtils.jsonPut(json, key, value);
        }

        // store configuration and orientation
        Configuration config = mContext.getResources().getConfiguration();
        JSONUtils.jsonPut(json, "configuration", config.toString());
        JSONUtils.jsonPut(json, "orientation", config.orientation);

        // store system settings
        jsonPutSettings(json, "system", Settings.System.class);

        // store global settings
        jsonPutSettings(json, "global", Settings.Global.class);

        // store secure settings
        jsonPutSettings(json, "secure", Settings.Secure.class);

        // record
        record(last_record_all, logID, "static", action, "", json.toString());
    }

    private void jsonPutSettings(JSONObject json, String key, Class<?> c) {
        JSONArray jsonArray = new JSONArray();
        Field[] fields_glb = c.getFields();
        for (Field f : fields_glb) {
            if (Modifier.isStatic(f.getModifiers())) {
                try {
                    String name = f.getName();
                    Object obj = f.get(null);
                    if (obj != null) {
                        String database_key = obj.toString();
                        Method method = c.getMethod("getString", ContentResolver.class, String.class);
                        String value_s = (String) method.invoke(null, mContext.getContentResolver(), database_key);
                        jsonArray.put(new JSONArray().put(name).put(database_key).put(value_s));
                    }
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        JSONUtils.jsonPut(json, key, jsonArray);
    }

    private void notifyContext(String context, long timestamp, int logID, String reason) {
        if (contextListener != null) {
            Log.e("ConfigContext", "broadcast context: " + context);
            for (ContextListener listener: contextListener) {
                ContextResult contextResult = new ContextResult(context, reason);
                contextResult.setTimestamp(timestamp);
                contextResult.getExtras().putInt("logID", logID);
                listener.onContext(contextResult);
            }
        }
    }

    private void detectKeyVolume(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_VOLUME_UP:
                // detect current noise
                if (detectedNoiseFt == null) {
                    detectedNoiseFt = noiseManager.detectNoise(5000, 10);
                }
                Log.e(TAG, "Local Context Data: " + String.format("%d,%f,%s,%s,%s,%b,%d",
                        System.currentTimeMillis(),
                        noiseManager.getPresentNoise(),
                        deviceManager.getPresentDeviceID(),
                        appManager.getPresentApp(),
                        positionManager.getPresentPosition(),
                        soundManager.isAudioOn(),
                        soundManager.getAudioMode()
                ));
                List<CrowdManager.BluetoothItem> bluetoothItemList = crowdManager.getBleList();
                for (CrowdManager.BluetoothItem bluetoothItem: bluetoothItemList) {
                    Log.e(TAG, bluetoothItem.toString());
                }
                Log.e(TAG, "KeyEvent End");
                tryPopUpFrontend(REASON_MANUAL, 0);
        }
    }

    private void detectKeyGesture(int keycode, int keyAction) {
        if (keycode == KeyEvent.KEYCODE_VOLUME_DOWN && keyAction == KeyEvent.ACTION_DOWN && soundManager.getVolume() == 0) {
            if (currentMode != MODE_QUIET) {
                long curKeyDownTime = System.currentTimeMillis();
                if (keyDownCount == 0) {
                    keyDownCount = 1;
                    lastKeyDownTime = curKeyDownTime;
                } else if (keyDownCount == 1) {
                    if (curKeyDownTime - lastKeyDownTime <= 500) {  // 500ms
                        // trigger quiet mode
                        keyDownCount = 0;
                        currentMode = MODE_QUIET;
                        changeToQuietMode(20);
                    } else {
                        // duration too long
                        lastKeyDownTime = curKeyDownTime;
                    }
                }
            }
        }
    }

    @Override
    public void onVolEvent(EventType eventType, Bundle bundle) {
        // popup == false ONLY FOR EXPERIMENT_1
        boolean popup = false;
        double noise = noiseManager.getPresentNoise();
        switch (eventType) {
            case Noise:
                noise = bundle.getDouble("noise");
                double lastTriggerNoise = bundle.getDouble("lastTriggerNoise");
                Log.e(TAG, "onVolEvent: " + eventType + " " + noise + " " + lastTriggerNoise);
                break;
            // TODO: adjust mapping function
            case Device:
                String deviceID = bundle.getString("deviceID");
                Log.e(TAG, "onVolEvent: " + eventType + " " + deviceID);
                break;
            case App:
                String appID = bundle.getString("app");
                Log.e(TAG, "onVolEvent: " + eventType + " " + appID);
                break;
            case Position:
                String positionID = bundle.getString("id");
                String positionName = bundle.getString("name");
                Log.e(TAG, "onVolEvent: " + eventType + " " + positionID + " " + positionName);
                break;
            case Motion:
                String motion = bundle.getString("motion");
                popup = false;
                Log.e(TAG, "onVolEvent: " + motion);
                break;
            // TODO
            case Bluetooth:
            case Audio:
            case Time:
                popup = false;
                break;
        }
        if (popup && currentMode != MODE_QUIET && !soundManager.isAudioOn()) {
            // map noise to volume and adjust
//            double adjustedVolume = fakeMapping(noise);
            double adjustedVolume = volumeManager.predict(getCurrentFID(), noise);
            Log.e(TAG, "onVolEvent: noise = " + noise +  " adjust volume = " + adjustedVolume);
            // do not adjust, if:
            // (1) new context (no data yet), adjustedVolume = -1
            // (2) the predicted volume is the same as current volume
            if (adjustedVolume >= 0 && soundManager.percent2int(adjustedVolume) != soundManager.getVolume()) {
                tryPopUpFrontend(REASON_AUTO_DIRECT, adjustedVolume);
            }
        }
    }

    public double fakeMapping(double noise) {
        double result = noise / 150 * 100;
        if (result > 100) {
            result = 100;
        }
        if (result < 0) {
            result = 0;
        }
        return result;
    }

    public void tryPopUpFrontend(int reason, double adjustedVolume) {
        if (frontEndState == TYPE_OFF) {
            VolumeContext volumeContext = getPresentContext();
            Bundle rules = getRules(volumeContext, reason);
            if (reason != REASON_MANUAL) {
                List<Bundle> rulesList = rules.getParcelableArrayList("rules");
                rulesList.get(0).putDouble("volume", adjustedVolume);
            }
//            rules.putBoolean("quietMode", currentMode == MODE_QUIET);
            rules.putInt("event", CONTEXT_EVENT_POPUP);
            rules.putInt("reason", reason);
            notifyFrontend(CONTEXT_VOLUME, rules);
            Log.e(TAG, "tryPopUpFrontend: Volume UI pops up, type: " + reason);
        } else {
            Log.e(TAG, "tryPopUpFrontend: not pop up because already on");
        }
    }

    private void changeToQuietMode(double volume) {
        // pop up the panel
        tryPopUpFrontend(REASON_MANUAL, 0);
        // then adjust volume
        Bundle bundle = new Bundle();
        bundle.putDouble("vol", volume);
        bundle.putBoolean("quietMode", currentMode == MODE_QUIET);
        notifyFrontend("on quiet-mode changed volume adjust", bundle);
        Log.e(TAG, "changeToQuietMode signaled");
    }

    private void notifyRequestRecordPermission() {
        Bundle bundle = new Bundle();
        bundle.putInt("event", CONTEXT_EVENT_CAPTURE_PERMISSION);
        notifyFrontend(CONTEXT_VOLUME, bundle);
    }

    private void notifyFrontend(String context, Bundle bundle) {
        if (contextListener != null) {
            for (ContextListener listener: contextListener) {
                ContextResult contextResult = new ContextResult(context, "");
                contextResult.setTimestamp(System.currentTimeMillis());
                contextResult.setExtras(bundle);
                listener.onContext(contextResult);
            }
        }
    }

    @Override
    public void recordEvent(EventType type, String action, String other) {
        record(System.currentTimeMillis(), incLogID(), type.toString(), action, "", other);
    }
}
