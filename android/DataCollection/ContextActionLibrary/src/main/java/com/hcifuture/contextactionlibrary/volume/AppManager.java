package com.hcifuture.contextactionlibrary.volume;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;
import com.hcifuture.contextactionlibrary.utils.JSONUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AppManager extends TriggerManager {
    private static final String TAG = "AppManager";
    private String appName;
    public static Integer latest_id = -1;
    private String last_appName;
    private boolean overlay_has_showed_for_other_reason;
    private String last_valid_widget;
    private boolean wechat_chatting_video_on;
    private Context mContext;

    public AppManager(VolEventListener volEventListener, Context context) {
        super(volEventListener);
        appName = "";
        last_appName = "";
        last_valid_widget = "";
        wechat_chatting_video_on = false;
        overlay_has_showed_for_other_reason = true;
        mContext = context;

        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        int tmp_cnt = need_overlay_apps.size() + not_need_overlay_apps.size();
        for (ResolveInfo ri : resolveInfo) {
            tmp_cnt++;
            not_need_overlay_apps.add(new AppItem(tmp_cnt, "系统桌面", ri.activityInfo.packageName));
        }
    }

    public static class AppItem {
        private final int id;
        private final String appName;
        private final String packageName;

        public AppItem(int id, String appName, String packageName) {
            this.id = id;
            this.appName = appName;
            this.packageName = packageName;
        }

        public String getAppName() {
            return appName;
        }

        public String getPackageName() {
            return packageName == null ? "": packageName;
        }
    }

    private final List<AppItem> need_overlay_apps = new ArrayList<>(Arrays.asList(
            new AppItem(1, "抖音", "com.ss.android.ugc.aweme"),
            new AppItem(2, "抖音极速版", "com.ss.android.ugc.aweme.lite"),
            new AppItem(3, "抖音火山版", "com.ss.android.ugc.live"),
            new AppItem(4, "快手", "com.smile.gifmaker"),
            new AppItem(5, "快手极速版", "com.kuaishou.nebula"),
            new AppItem(6, "快手概念版", "com.kwai.thanos"),
            new AppItem(7, "西瓜视频", "com.ss.android.article.video"),
            new AppItem(8, "优酷视频", "com.youku.phone"),
            new AppItem(9, "爱奇艺", "com.qiyi.video"),
            new AppItem(10, "斗鱼", "air.tv.douyu.android"),
            new AppItem(11, "虎牙直播", "com.duowan.kiwi"),
            new AppItem(12, "搜狐视频", "com.sohu.sohuvideo"),
            new AppItem(13, "AcFun", "tv.acfundanmaku.video"),
            new AppItem(14, "腾讯视频", "com.tencent.qqlive"),
            new AppItem(15, "哔哩哔哩", "tv.danmaku.bili"),
            new AppItem(16, "哔哩哔哩概念版", "com.bilibili.app.blue"),
            new AppItem(17, "腾讯会议", "com.tencent.wemeet.app"),
            new AppItem(18, "Zoom", "us.zoom.videomeetings"),
            new AppItem(19, "瞩目", "com.suirui.zhumu"),
            new AppItem(20, "华为云会议", "com.huawei.CloudLink"),
            new AppItem(21, "飞书", "com.ss.android.lark"),
            new AppItem(22, "钉钉", "com.alibaba.android.rimet"),
            new AppItem(23, "网易云音乐", "com.netease.cloudmusic"),
            new AppItem(24, "QQ音乐", "com.tencent.qqmusic"),
            new AppItem(25, "酷狗音乐", "com.kugou.android"),
            new AppItem(26, "酷我音乐", "cn.kuwo.player"),
            new AppItem(27, "咪咕音乐", "cmccwm.mobilemusic"),
            new AppItem(28, "蜻蜓FM", "fm.qingting.qtradio"),
            new AppItem(29, "全民K歌", "com.tencent.karaoke"),
            new AppItem(30, "虾米音乐", "fm.xiami.main"),
            new AppItem(31, "唱吧", "com.changba"),
            new AppItem(32, "喜马拉雅", "com.ximalaya.ting.android")
    ));

    private final List<AppItem> not_need_overlay_apps = new ArrayList<>(Arrays.asList(
            new AppItem(33, "微信", "com.tencent.mm"),
            new AppItem(34, "知乎", "com.zhihu.android"),
            new AppItem(35, "QQ", "com.tencent.mobileqq"),
            new AppItem(36, "Soul", "cn.soulapp.android"),
            new AppItem(37, "陌陌", "com.immomo.momo"),
            new AppItem(38, "Summer", "cn.imsummer.summer"),
            new AppItem(39, "百度贴吧", "com.baidu.tieba"),
            new AppItem(40, "微博", "com.sina.weibo"),
            new AppItem(41, "小红书", "com.xingin.xhs"),
            new AppItem(42, "百度", "com.baidu.searchbox"),
            new AppItem(43, "今日头条", "com.ss.android.article.news"),
            new AppItem(44, "今日头条极速版", "com.ss.android.article.lite"),
            new AppItem(45, "搜狐新闻", "com.sohu.newsclient"),
            new AppItem(46, "网易新闻", "com.netease.newsreader.activity"),
            new AppItem(47, "澎湃新闻", "com.wondertek.paper"),
            new AppItem(48, "腾讯新闻", "com.tencent.news"),
            new AppItem(49, "Keep", "com.gotokeep.keep"),
            new AppItem(50, "高德地图", "com.autonavi.minimap"),
            new AppItem(51, "腾讯地图", "com.tencent.map"),
            new AppItem(52, "百度地图", "com.baidu.BaiduMap"),
            new AppItem(53, "美团", "com.sankuai.meituan"),
            new AppItem(54, "大众点评", "com.dianping.v1"),
            new AppItem(55, "淘宝", "com.taobao.taobao"),
            new AppItem(56, "京东", "com.jingdong.app.mall"),
            new AppItem(57, "闲鱼", "com.taobao.idlefish"),
            new AppItem(58, "苏宁易购", "com.suning.mobile.ebuy"),
            new AppItem(59, "拼多多", "com.xunmeng.pinduoduo"),
            new AppItem(60, "唯品会", "com.achievo.vipshop"),
            new AppItem(61, "当当", "com.dangdang.buy2"),
            new AppItem(62, "得物", "com.shizhuang.duapp")
    ));

    private final List<String> video_widgets = new ArrayList<>(Arrays.asList(
            "com.tencent.mm.plugin.finder.ui.FinderHomeAffinityUI",
            "com.tencent.mm.plugin.finder.ui.FinderShareFeedRelUI",
            "com.tencent.mm.plugin.sns.ui.SnsOnlineVideoActivity",
            "com.tencent.mm.plugin.finder.feed.ui.FinderLiveVisitorWithoutAffinityUI",
            "com.tencent.mm.ui.chatting.gallery.ImageGalleryUI",
            "com.tencent.mm.plugin.finder.feed.ui.FinderProfileTimeLineUI"
    ));

    private final List<String> blank_widgets = new ArrayList<>(Arrays.asList(
            "com.tencent.mm.ui.LauncherUI"
    ));

    public AppItem findByAppName(List<AppItem> list, String name) {
        for (AppItem appItem: list) {
            if (appItem.getAppName().equals(name)) {
                return appItem;
            }
        }
        return null;
    }

    public AppItem findByPackageName(List<AppItem> list, String name) {
        for (AppItem appItem: list) {
            if (appItem.getPackageName().equals(name)) {
                return appItem;
            }
        }
        return null;
    }

    // TODO: Only call front-end when begin to play audio
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null && event.getPackageName().toString().equals("com.hcifuture.scanner"))
            return;
        if (event.getText() != null && event.getText().size() > 0 && event.getText().get(0) != null) {
            String tmp_name = event.getText().get(0).toString();
            String packageName = "";
            if (event.getPackageName() != null)
                packageName = event.getPackageName().toString();
            if (isNeedOverlayApp(tmp_name, packageName) || isNotNeedOverlayApp(tmp_name, packageName)) {
                appName = tmp_name;
                if (findByAppName(need_overlay_apps, appName) != null)
                    latest_id = findByAppName(need_overlay_apps, appName).id;
                else if (findByAppName(not_need_overlay_apps, appName) != null)
                    latest_id = findByAppName(not_need_overlay_apps, appName).id;
                if (!(appName.equals(last_appName))) {
                    if (isNeedOverlayApp(appName, packageName) && !(event.getClassName() != null && isVideoWidget(event.getClassName().toString()))) {
                        Bundle bundle = new Bundle();
                        bundle.putString("app", appName);
                        Log.e(TAG, "App Changed from " + last_appName + " to " + appName + ", timestamp: " + System.currentTimeMillis());
                        volEventListener.onVolEvent(VolEventListener.EventType.App, bundle);
                        overlay_has_showed_for_other_reason = true;
                    }
                    JSONObject json = new JSONObject();
                    JSONUtils.jsonPut(json, "last_app", last_appName);
                    JSONUtils.jsonPut(json, "last_app_type", getAppType(last_appName));
                    JSONUtils.jsonPut(json, "new_app", appName);
                    JSONUtils.jsonPut(json, "new_app_type", getAppType(appName));
                    volEventListener.recordEvent(VolEventListener.EventType.App, "app_change", json.toString());
                    last_appName = appName;
                }
            }
        }
        if (event.getClassName() != null) {
            if (isVideoWidget(event.getClassName().toString())) {
                if (overlay_has_showed_for_other_reason || !last_valid_widget.equals(event.getClassName().toString())) {
                    Bundle bundle = new Bundle();
                    bundle.putString("app", appName);
                    Log.e(TAG, "App Changed from " + last_appName + " to " + event.getClassName().toString() + ", timestamp: " + System.currentTimeMillis());
                    volEventListener.onVolEvent(VolEventListener.EventType.App, bundle);
                    overlay_has_showed_for_other_reason = false;
                    last_valid_widget = event.getClassName().toString();
                }
            }
            if (blank_widgets.contains(event.getClassName().toString())) {
                overlay_has_showed_for_other_reason = true;
            }
//            Log.e("AccessibilityEventType", event.getClassName().toString());
//            Log.e("Event", event.toString());
            if (event.getText() != null && event.getText().size() > 0) {
//                Log.e("EventText", event.getText().get(0).toString());
                wechat_chatting_video_on = getWechatChattingVideoOn(event);
            }
        }
    }

    private int getAppType(String name) {
        if (findByAppName(need_overlay_apps, name) != null)
            return 1;
        else if (findByAppName(not_need_overlay_apps, name) != null)
            return 2;
        else
            return 0;
    }

    public boolean getWechatChattingVideoOn(AccessibilityEvent event) {
        if (event == null)
            return false;
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED)
            return false;
        if (event.getClassName() == null || !event.getClassName().toString().equals("android.widget.FrameLayout"))
            return false;
        if (event.getPackageName() == null || !event.getPackageName().toString().equals("com.tencent.mm"))
            return false;
        if (event.getText() == null || event.getText().size() == 0 || event.getText().get(0) == null)
            return false;
        String tmp = event.getText().get(0).toString();
        if (tmp.length() < 4) return false;
        return tmp.charAt(tmp.length() - 3) == ':';
    }

    public boolean isVideoWidget(String name) {
        if (video_widgets.contains(name)) {
            if (name.equals("com.tencent.mm.ui.chatting.gallery.ImageGalleryUI")) {
                return wechat_chatting_video_on;
            }
            return true;
        }
        return false;
    }

    public boolean isNeedOverlayApp(String name, String packageName) {
        AppItem appItem = findByAppName(need_overlay_apps, name);
        if (appItem == null) return false;

        return appItem.getPackageName() != null && appItem.getPackageName().equals(packageName);
    }

    public boolean isNotNeedOverlayApp(String name, String packageName) {
        AppItem appItem = findByAppName(not_need_overlay_apps, name);
        if (appItem == null) return false;

        return appItem.getPackageName() != null && appItem.getPackageName().equals(packageName);
    }

    public String getPresentApp() {
        return appName;
    }

    public List<String> getAppList() {
        List<String> result = new ArrayList<>();
        for (AppItem appItem: need_overlay_apps) {
            result.add(appItem.getAppName());
        }
        return result;
    }
}
