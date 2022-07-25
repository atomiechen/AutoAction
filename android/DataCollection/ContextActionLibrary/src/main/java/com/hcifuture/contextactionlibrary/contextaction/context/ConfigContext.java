package com.hcifuture.contextactionlibrary.contextaction.context;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.hcifuture.contextactionlibrary.sensor.collector.async.AudioCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.GPSCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.async.WifiCollector;
import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;
import com.hcifuture.contextactionlibrary.sensor.data.NonIMUData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleIMUData;
import com.hcifuture.contextactionlibrary.sensor.data.SingleWifiData;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;
import com.hcifuture.contextactionlibrary.volume.VolumeContext;
import com.hcifuture.contextactionlibrary.volume.VolumeRule;
import com.hcifuture.contextactionlibrary.volume.VolumeRuleManager;
import com.hcifuture.shared.communicate.config.ContextConfig;
import com.hcifuture.contextactionlibrary.contextaction.event.BroadcastEvent;
import com.hcifuture.shared.communicate.config.RequestConfig;
import com.hcifuture.shared.communicate.listener.ActionListener;
import com.hcifuture.shared.communicate.listener.ContextListener;
import com.hcifuture.shared.communicate.listener.RequestListener;
import com.hcifuture.shared.communicate.result.ActionResult;
import com.hcifuture.shared.communicate.result.ContextResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ConfigContext extends BaseContext {

    public static String NEED_AUDIO = "context.config.need_audio";
    public static String NEED_NONIMU = "context.config.need_nonimu";
    public static String NEED_SCAN = "context.config.need_scan";
    public static String NEED_POSITION = "context.config.need_position";


    private String last_packageName;
    private String packageName;
    private String present_name;
    private String latest_deviceType;
    private List<String> valid_packageNames;
    private List<String> useless_packageNames;
    private List<String> nochange_packageNames;
    private List<String> rules;
    private int brightness;
    private final HashMap<String, Integer> volume;

    private long last_record_all;
    private VolumeRuleManager volumeRuleManager;

    private final AtomicInteger mLogID = new AtomicInteger(0);

    public ConfigContext(Context context, ContextConfig config, RequestListener requestListener, List<ContextListener> contextListener, LogCollector logCollector, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList) {
        super(context, config, requestListener, contextListener, scheduledExecutorService, futureList);
        this.logCollector = logCollector;
        volumeRuleManager = new VolumeRuleManager();

        // initialize
        packageName = "";
        present_name = "";
        last_packageName = "";
        latest_deviceType = "speaker";
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

        last_record_all = 0;

        useless_packageNames = Arrays.asList("com.android.systemui",
                "miui.systemui.plugin",
                "com.huawei.android.launcher",
                "com.hcifuture.scanner",
                "com.xiaomi.bsp.gps.nps");
        valid_packageNames = Arrays.asList("tv.danmaku.bili",//B站
                "com.ss.android.ugc.aweme", //抖音
                "com.tencent.mm", //微信
                "com.tencent.qqmusic", //QQ音乐
                "com.tencent.wemeet.app", //腾讯会议
                "com.baidu.searchbox", //百度
                "com.baidu.searchbox.lite",
                "com.youku.phone", //优酷视频
                "com.netease.cloudmusic", //网易云音乐
                "com.gotokeep.keep", //Keep
                "com.tencent.qqlive", //腾讯视频
                "com.sina.weibo", //微博
                "com.tencent.karaoke", //全民K歌
                "com.xingin.xhs", //小红书
                "com.tencent.mobileqq", //QQ
                "com.taobao.taobao", //淘宝
                "com.bilibili.app.blue", //B站概念版
                "com.zhihu.android", //知乎
                "com.qiyi.video", //爱奇艺
                "com.baidu.tieba", //百度贴吧
                "com.smile.gifmaker", //快手
                "com.kuaishou.nebula", //快手极速版
                "com.qiyi.video.lite", //爱奇艺极速版
                "com.ss.android.ugc.aweme.lite", //抖音极速版
                "com.ss.android.ugc.live", //抖音火山版
                "air.tv.douyu.android", //斗鱼
                "com.duowan.kiwi", //虎牙
                "com.sohu.sohuvideo", //搜狐视频
                "com.autonavi.minimap", //高德地图
                "cn.soulapp.android",
                "com.kugou.android",
                "com.immomo.momo",
                "com.ss.android.article",
                "com.ss.android.article.news",
                "com.ss.android.article.lite",
                "com.ss.android.auto",
                "com.cubic.autohome",
                "com.xs.fm",
                "com.le123.ysdq",
                "com.ximalaya.ting.android",
                "com.p1.mobile.putong",
                "com.hunantv.imgo.activity",
                "com.taobao.litetao",
                "com.tencent.wework",
                "com.tmall.wireless",
                "com.taobao.live",
                "cn.xuexi.android",
                "com.tencent.map",
                "com.tencent.gamehelper.smoba",
                "com.tencent.gamehelper.pg",
                "com.mihoyo.hyperion",
                "com.tencent.tmgp.sgame",
                "com.tencent.tmgp.pubgmhd",
                "com.miHoYo.GenshinImpact",
                "com.tencent.jkchess",
                "com.tencent.lolm",
                "com.netease.sky.aligames",
                "com.knight.union.aligames",
                "com.tencent.tmgp.supercell.brawlstars",
                "com.miHoYo.bh3.uc",
                "com.quark.browser",
                "com.UCMobile",
                "com.tencent.mtt",
                "com.huawei.health",
                "com.douban.frodo",
                "cn.ledongli.ldl",
                "com.taptap",
                "com.jingdong.app.mall",
                "com.jd.jdlite",
                "com.achievo.vipshop",
                "com.xunmeng.pinduoduo",
                "org.mozilla.firefox",
                "com.microsoft.emmx",
                "com.android.chrome",
                "com.android.browser",
                "com.hicloud.browser",
                "com.huawei.browser",
                "com.dianping.v1",
                "com.taobao.idlefish",
                "com.suirui.zhumu",
                "us.zoom.videomeetings",
                "com.baidu.BaiduMap"); //百度地图
        nochange_packageNames = Arrays.asList(
                "com.sankuai.meituan",
                "com.sankuai.meituan.takeoutnew",
                "com.dragon.read",
                "com.eg.android.AlipayGphone",
                "com.wuba",
                "me.ele",
                "com.lemon.lv",
                "com.baidu.netdisk",
                "com.alibaba.android.rimet",
                "com.kmxs.reader",
                "com.xunlei.downloadprovider",
                "com.baidu.homework",
                "com.hpbr.bosszhipin",
                "com.shizhuang.duapp",
                "com.wuba.zhuanzhuan",
                "ctrip.android.view",
                "com.Qunar",
                "com.mt.mtxx.mtxx",
                "com.anjuke.android.app",
                "com.handsgo.jiakao.android",
                "com.zhaopin.social",
                "com.jingyao.easybike",
                "cn.wps.moffice_eng",
                "com.didapinche.booking",
                "com.MobileTicket",
                "com.icbc",
                "com.intsig.camscanner",
                "com.chinamworld.bocmbci",
                "cn.tape.tapeapp",
                "com.ss.android.lark",
                "com.coolapk.market"
                );
    }

    @Override
    public void start() {
        record_all("start");
    }

    @Override
    public void stop() {
        // do not perform record_all() in stop(),
        // it may cause crashes when frequently called
    }

    @Override
    public void onIMUSensorEvent(SingleIMUData data) {

    }

    @Override
    public void onNonIMUSensorEvent(NonIMUData data) {

    }

    @Override
    public void getContext() {
        long current_call = System.currentTimeMillis();
        // periodically record_all() every 30 min
        if (current_call - last_record_all >= 30 * 60000) {
            record_all("period_30m");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        if (pkg != null) {
            String tmp_name = event.getPackageName().toString();
            if (!useless_packageNames.contains(tmp_name))
                present_name = tmp_name;
            if (valid_packageNames.contains(present_name) || nochange_packageNames.contains(present_name)) {
                packageName = present_name;
                if (!(packageName.equals(last_packageName))) {
                    if (valid_packageNames.contains(packageName)) {
                        long now = System.currentTimeMillis();
                        int logID = incLogID();
                        notifyContext(NEED_AUDIO, now, logID, "app changed: " + packageName);
                        notifyContext(NEED_SCAN, now, logID, "app changed: " + packageName);
                        notifyContext(NEED_POSITION, now, logID, "app changed: " + packageName);
                        VolumeContext volumeContext = getPresentContext();
                        rules = getRules(volumeContext);
                        List<Integer> volumes = getVolumes(volumeContext);
                        if (requestListener != null) {
                            RequestConfig requestConfig = new RequestConfig();
                            requestConfig.putValue("needVolumeOverlay", true);
                            requestConfig.putString("rules", rules.toString());
                            requestConfig.putString("volumes", volumes.toString());
                            requestListener.onRequest(requestConfig);
                        }
                    }
                    last_packageName = packageName;
                }
            }
        }
    }

    public VolumeContext getPresentContext() {
        Date date = new Date();
        double latitude = 0.0;
        double longitude = 0.0;
        if (GPSCollector.latest_data != null) {
            latitude = GPSCollector.latest_data.getLatitude();
            longitude = GPSCollector.latest_data.getLongitude();
        }
        List<String> wifiIds = new ArrayList<>();
        if (WifiCollector.latest_data != null) {
            List<SingleWifiData> singleWifiDataList = WifiCollector.latest_data.getAps();
            for (SingleWifiData wifiData: singleWifiDataList) {
                String key = wifiData.getSsid() + wifiData.getBssid();
                wifiIds.add(key);
            }
        }
        double noise = AudioCollector.lastest_noise;
        String app = packageName;
        String deviceType = latest_deviceType;
        return new VolumeContext(date, latitude, longitude, wifiIds, noise, app, deviceType);
    }

    public List<String> getRules(VolumeContext volumeContext) {
        List<VolumeRule> volumeRules = volumeRuleManager.getRecommendation(volumeContext);
        List<String> _rules = new ArrayList<>();
        for (VolumeRule volumeRule: volumeRules) {
            _rules.add(volumeRule.getType().getText() + " volume=" + volumeRule.getVolume());
        }
        _rules.add(volumeContext.getDate().toString() + " (" + volumeContext.getLatitude() + "/" + volumeContext.getLongitude() + ") " + volumeContext.getNoise() + " " + volumeContext.getApp() + " " + last_packageName + " " + volumeContext.getDeviceType() + " " + volumeRuleManager.getContextListSize());
        return _rules;
    }

    public List<Integer> getVolumes(VolumeContext volumeContext) {
        List<VolumeRule> volumeRules = volumeRuleManager.getRecommendation(volumeContext);
        List<Integer> _volumes = new ArrayList<>();
        for (VolumeRule volumeRule: volumeRules) {
            _volumes.add(volumeRule.getVolume());
        }
        return _volumes;
    }

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
                record = true;
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
                        notifyContext(NEED_NONIMU, timestamp, logID, "screen brightness change");
                    } else if (database_key.startsWith("volume_")) {
                        if (!volume.containsKey(database_key)) {
                            // record new volume value
                            volume.put(database_key, value);
                        }
                        // record volume value difference and update
                        int diff = value - volume.put(database_key, value);
                        JSONUtils.jsonPut(json, "diff", diff);
                        notifyContext(NEED_AUDIO, timestamp, logID, "volume change: " + database_key);
                        notifyContext(NEED_SCAN, timestamp, logID, "volume change: " + database_key);
                        String[] tmp = database_key.split("_");
                        String deviceType = "";
                        for (int index = 2; index < tmp.length; index++) {
                            deviceType += tmp[index];
                            if (index < tmp.length - 1) {
                                deviceType += "_";
                            }
                        }
                        latest_deviceType = deviceType;
                    } else if (Settings.Global.BLUETOOTH_ON.equals(database_key) && value == 1) {
//                    notify(NEED_SCAN, timestamp, logID, "Bluetooth on via global setting");
                    } else if (Settings.Global.WIFI_ON.equals(database_key) && value == 2) {
//                    notify(NEED_SCAN, timestamp, logID, "Wifi on via global setting");
                    }
                }
            } else if ("BroadcastReceive".equals(type)) {
                record = true;
                switch (action) {
                    case Intent.ACTION_CONFIGURATION_CHANGED:
                        Configuration config = mContext.getResources().getConfiguration();
                        JSONUtils.jsonPut(json, "configuration", config.toString());
                        JSONUtils.jsonPut(json, "orientation", config.orientation);
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                    case Intent.ACTION_SCREEN_ON:
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
                }
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    notifyContext(NEED_SCAN, timestamp, logID, "screen on");
                } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action) && extras.getInt(WifiManager.EXTRA_WIFI_STATE) == WifiManager.WIFI_STATE_ENABLED) {
                    notifyContext(NEED_SCAN, timestamp, logID, "Wifi on via broadcast");
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action) && extras.getInt(BluetoothAdapter.EXTRA_STATE) == BluetoothAdapter.STATE_ON) {
                    notifyContext(NEED_SCAN, timestamp, logID, "Bluetooth on via broadcast");
                }
            } else if ("KeyEvent".equals(type)) {
                record = true;
                int keycode = extras.getInt("code");
                JSONUtils.jsonPut(json, "keycodeString", KeyEvent.keyCodeToString(keycode));

                switch (keycode) {
                    case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
                    case KeyEvent.KEYCODE_MEDIA_CLOSE:
                    case KeyEvent.KEYCODE_MEDIA_EJECT:
                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    case KeyEvent.KEYCODE_MEDIA_RECORD:
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                    case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
                    case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
                    case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
                    case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                    case KeyEvent.KEYCODE_MEDIA_TOP_MENU:
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                    case KeyEvent.KEYCODE_VOLUME_MUTE:
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        notifyContext(NEED_AUDIO, timestamp, logID, "key event: " + KeyEvent.keyCodeToString(keycode));
                        notifyContext(NEED_SCAN, timestamp, logID, "key event: " + KeyEvent.keyCodeToString(keycode));
                        notifyContext(NEED_POSITION, timestamp, logID, "key event: " + KeyEvent.keyCodeToString(keycode));

                        VolumeContext volumeContext = getPresentContext();
                        rules = getRules(volumeContext);
                        List<Integer> volumes = getVolumes(volumeContext);
                        if (requestListener != null) {
                            RequestConfig requestConfig = new RequestConfig();
                            requestConfig.putValue("needVolumeOverlay", true);
                            requestConfig.putString("rules", rules.toString());
                            requestConfig.putString("volumes", volumes.toString());
                            requestListener.onRequest(requestConfig);
                        }
                }
            }

            if (record) {
                JSONUtils.jsonPut(json, "package", packageName);
                JSONUtils.jsonPutBundle(json, extras);
                record(timestamp, logID, type, action, tag, json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONUtils.jsonPut(json, "package", packageName);
            JSONUtils.jsonPutBundle(json, extras);
            JSONUtils.jsonPut(json, "exception", e.toString());
            record(timestamp, logID, type, action, tag, json.toString());
        }
    }

    @Override
    public void onExternalEvent(Bundle bundle) {
        if (bundle.containsKey("selectedRule") && bundle.containsKey("finalVolume")) {
            long timestamp = System.currentTimeMillis();
            int logID = incLogID();
            String type = "volume overlay return";
            String action = "";
            String tag = "";
            JSONObject json = new JSONObject();
            int selected_rule = bundle.getInt("selectedRule");
            JSONUtils.jsonPut(json, "finalVolume", bundle.getInt("finalVolume"));
            JSONUtils.jsonPut(json, "selectedRule", rules.get(selected_rule));

            VolumeContext volumeContext = getPresentContext();
            volumeRuleManager.addRecord(volumeContext, bundle.getInt("finalVolume"));
            record(timestamp, logID, type, action, tag, json.toString());
        }
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
        Log.e("ConfigContext", "in record");
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
}
