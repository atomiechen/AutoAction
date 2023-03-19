package com.hcifuture.contextactionlibrary.volume;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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
import java.util.jar.Attributes;

public class AppManager extends TriggerManager {
    private static final String TAG = "AppManager";
    private String appName;
    public static Integer latest_id = -1;
    private String last_appName;
    private String last_packageName;
    private boolean overlay_has_showed_for_other_reason;
    private String last_valid_widget;
    private boolean wechat_chatting_video_on;
    private Context mContext;
    private PackageManager packageManager;
    public static final List<String> systemPackages = new ArrayList<>(Arrays.asList(
            "com.android.systemui",
            "com.huawei.android.launcher",
            "com.bbk.launcher2",
            "com.miui.home",
            "com.android.launcher",
            "com.hihonor.android.launcher",
            "com.google.android.inputmethod.latin",
            "com.vivo.hiboard",
            "com.oppo.launcher",
            "com.huawei.ohos.inputmethod",
            "com.huawei.aod",
            "net.oneplus.launcher",
            "com.hihonor.aod",
            "com.android.incallui",
            "com.baidu.input_huawei",
            "com.baidu.input_oppo",
            "com.android.settings",
            "android",
            "com.android.mms",
            "com.miui.personalassistant",
            "com.huawei.android.totemweather",
            "com.huawei.magazine",
            "com.java.myfirsttest",
            "com.baidu.input_mi",
            "miui.systemui.plugin",
            "com.android.permissioncontroller",
            "com.hcifuture.scanner",
            "com.ss.android.lark"
    ));

    public AppManager(VolEventListener volEventListener, Context context) {
        super(volEventListener);
        appName = "";
        last_appName = "";
        last_packageName = "";
        last_valid_widget = "";
        wechat_chatting_video_on = false;
        overlay_has_showed_for_other_reason = true;
        mContext = context;

        this.packageManager = mContext.getPackageManager();
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
        if (event.getPackageName() == null || event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return;
        // 去掉拍拍助手和飞书以及其他系统应用
        if (systemPackages.contains(event.getPackageName().toString()))
            return;
        String packageName = event.getPackageName().toString();
        try {
            if (!isUserApp(packageManager.getPackageInfo(packageName, 0))) {
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String name = getNameByPackageName(packageName);
        if (!name.equals(appName)) {
            Bundle bundle = new Bundle();
            bundle.putString("last_app", appName);
            bundle.putString("new_app", name);
            volEventListener.onVolEvent(VolEventListener.EventType.AppChange, bundle);
            last_appName = appName;
            appName = name;
            Log.i(TAG, "new app: " + appName);
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

    public String getPresentApp() {
        return appName;
    }
    public String getPresentPackage() {
        return last_packageName;
    }

    public String getNameByPackageName(String packageName) {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            if (applicationInfo != null)
                return applicationInfo.loadLabel(packageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return packageName;
    }

    public String getAppType(String packageName) {
        try {
            PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
            // 是系统软件或者是系统软件更新
            if (packageName.contains("mail") || packageName.equals("com.google.android.gm")){
                return "mail";
            } else if (packageName.equals("com.android.mms")) {
                return "SMS";
            } else if (isSystemApp(pInfo) || isSystemUpdateApp(pInfo)) {
                return "system";
            } else {
                return "APP messages";
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    /**
     * 是否是系统软件或者是系统软件的更新软件
     */
    public static boolean isSystemApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static boolean isSystemUpdateApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    public static boolean isUserApp(PackageInfo pInfo) {
        return (!isSystemApp(pInfo) && !isSystemUpdateApp(pInfo));
    }
}
