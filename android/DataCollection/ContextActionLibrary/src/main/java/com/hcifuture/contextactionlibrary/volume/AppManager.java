package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppManager extends TriggerManager {
    private static final String TAG = "AppManager";
    private String appName;
    private String last_appName;
    private boolean overlay_has_showed_for_other_reason;
    private String last_valid_widget;
    private boolean wechat_chatting_video_on;

    public AppManager(VolEventListener volEventListener) {
        super(volEventListener);
        appName = "";
        last_appName = "";
        last_valid_widget = "";
        wechat_chatting_video_on = false;
        overlay_has_showed_for_other_reason = true;
    }

    public static class AppItem {
        private String appName;
        private String packageName;

        public AppItem(String appName, String packageName) {
            this.appName = appName;
            this.packageName = packageName;
        }

        public String getAppName() {
            return appName;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    private final List<AppItem> need_overlay_apps = Arrays.asList(
            new AppItem("抖音", "com.ss.android.ugc.aweme"),
            new AppItem("抖音极速版", "com.ss.android.ugc.aweme.lite"),
            new AppItem("抖音火山版", "com.ss.android.ugc.live"),
            new AppItem("快手", "com.smile.gifmaker"),
            new AppItem("快手极速版", "com.kuaishou.nebula"),
            new AppItem("快手概念版", "com.kwai.thanos"),
            new AppItem("西瓜视频", "com.ss.android.article.video"),
            new AppItem("优酷视频", "com.youku.phone"),
            new AppItem("爱奇艺", "com.qiyi.video"),
            new AppItem("斗鱼", "air.tv.douyu.android"),
            new AppItem("虎牙直播", "com.duowan.kiwi"),
            new AppItem("搜狐视频", "com.sohu.sohuvideo"),
            new AppItem("AcFun", "tv.acfundanmaku.video"),
            new AppItem("腾讯视频", "com.tencent.qqlive"),
            new AppItem("哔哩哔哩", "tv.danmaku.bili"),
            new AppItem("哔哩哔哩概念版", "com.bilibili.app.blue"),
            new AppItem("腾讯会议", "com.tencent.wemeet.app"),
            new AppItem("Zoom", "us.zoom.videomeetings"),
            new AppItem("瞩目", "com.suirui.zhumu"),
            new AppItem("华为云会议", "com.huawei.CloudLink"),
            new AppItem("飞书", "com.ss.android.lark"),
            new AppItem("钉钉", "com.alibaba.android.rimet"),
            new AppItem("网易云音乐", "com.netease.cloudmusic"),
            new AppItem("QQ音乐", "com.tencent.qqmusic"),
            new AppItem("酷狗音乐", "com.kugou.android"),
            new AppItem("酷我音乐", "cn.kuwo.player"),
            new AppItem("咪咕音乐", "cmccwm.mobilemusic"),
            new AppItem("蜻蜓FM", "fm.qingting.qtradio"),
            new AppItem("全民K歌", "com.tencent.karaoke"),
            new AppItem("虾米音乐", "fm.xiami.main"),
            new AppItem("唱吧", "com.changba"),
            new AppItem("喜马拉雅", "com.ximalaya.ting.android")
    );

    private final List<AppItem> not_need_overlay_apps = Arrays.asList(
            new AppItem("微信", "com.tencent.mm"),
            new AppItem("知乎", "com.zhihu.android"),
            new AppItem("QQ", "com.tencent.mobileqq"),
            new AppItem("Soul", "cn.soulapp.android"),
            new AppItem("陌陌", "com.immomo.momo"),
            new AppItem("Summer", "cn.imsummer.summer"),
            new AppItem("百度贴吧", "com.baidu.tieba"),
            new AppItem("微博", "com.sina.weibo"),
            new AppItem("小红书", "com.xingin.xhs"),
            new AppItem("百度", "com.baidu.searchbox"),
            new AppItem("今日头条", "com.ss.android.article.news"),
            new AppItem("今日头条极速版", "com.ss.android.article.lite"),
            new AppItem("搜狐新闻", "com.sohu.newsclient"),
            new AppItem("网易新闻", "com.netease.newsreader.activity"),
            new AppItem("澎湃新闻", "com.wondertek.paper"),
            new AppItem("腾讯新闻", "com.tencent.news"),
            new AppItem("Keep", "com.gotokeep.keep"),
            new AppItem("高德地图", "com.autonavi.minimap"),
            new AppItem("腾讯地图", "com.tencent.map"),
            new AppItem("百度地图", "com.baidu.BaiduMap"),
            new AppItem("美团", "com.sankuai.meituan"),
            new AppItem("大众点评", "com.dianping.v1"),
            new AppItem("淘宝", "com.taobao.taobao"),
            new AppItem("京东", "com.jingdong.app.mall"),
            new AppItem("闲鱼", "com.taobao.idlefish"),
            new AppItem("苏宁易购", "com.suning.mobile.ebuy"),
            new AppItem("拼多多", "com.xunmeng.pinduoduo"),
            new AppItem("唯品会", "com.achievo.vipshop"),
            new AppItem("当当", "com.dangdang.buy2"),
            new AppItem("得物", "com.shizhuang.duapp")
    );

    private final List<String> video_widgets = Arrays.asList(
            "com.tencent.mm.plugin.finder.ui.FinderHomeAffinityUI",
            "com.tencent.mm.plugin.finder.ui.FinderShareFeedRelUI",
            "com.tencent.mm.plugin.sns.ui.SnsOnlineVideoActivity",
            "com.tencent.mm.plugin.finder.feed.ui.FinderLiveVisitorWithoutAffinityUI",
            "com.tencent.mm.ui.chatting.gallery.ImageGalleryUI",
            "com.tencent.mm.plugin.finder.feed.ui.FinderProfileTimeLineUI"
    );

    private final List<String> blank_widgets = Arrays.asList(
            "com.tencent.mm.ui.LauncherUI"
    );

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

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null && event.getPackageName().toString().equals("com.hcifuture.scanner"))
            return;
        if (event.getText() != null && event.getText().size() > 0) {
            String tmp_name = event.getText().get(0).toString();
            String packageName = "";
            if (event.getPackageName() != null)
                packageName = event.getPackageName().toString();
            if (isNeedOverlayApp(tmp_name, packageName) || isNotNeedOverlayApp(tmp_name, packageName)) {
                appName = tmp_name;
                if (!(appName.equals(last_appName))) {
                    if (isNeedOverlayApp(appName, packageName) && !(event.getClassName() != null && isVideoWidget(event.getClassName().toString()))) {
                        Bundle bundle = new Bundle();
                        bundle.putString("app", appName);
                        Log.e(TAG, "App Changed from " + last_appName + " to " + appName + ", timestamp: " + System.currentTimeMillis());
                        volEventListener.onVolEvent(VolEventListener.EventType.App, bundle);
                        overlay_has_showed_for_other_reason = true;
                    }
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
            Log.e("AccessibilityEventType", event.getClassName().toString());
            Log.e("Event", event.toString());
            if (event.getText() != null && event.getText().size() > 0) {
                Log.e("EventText", event.getText().get(0).toString());
                wechat_chatting_video_on = getWechatChattingVideoOn(event);
            }
        }
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

        return appItem.getPackageName().equals(packageName);
    }

    public boolean isNotNeedOverlayApp(String name, String packageName) {
        AppItem appItem = findByAppName(not_need_overlay_apps, name);
        if (appItem == null) return false;

        return appItem.getPackageName().equals(packageName);
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
