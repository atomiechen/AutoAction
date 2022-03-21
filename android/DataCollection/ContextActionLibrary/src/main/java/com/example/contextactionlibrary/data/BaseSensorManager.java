package com.example.contextactionlibrary.data;

import android.content.Context;
import android.hardware.SensorEvent;
import android.view.accessibility.AccessibilityEvent;

import com.example.contextactionlibrary.contextaction.action.BaseAction;
import com.example.contextactionlibrary.contextaction.context.BaseContext;
import com.example.ncnnlibrary.communicate.event.BroadcastEvent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public abstract class BaseSensorManager {
    protected String TAG = "MySensorManager";
    protected String name;

    protected Context mContext;
    protected boolean isInitialized = false;
    protected boolean isStarted = false;
    protected boolean isSensorOpened = false;

    protected List<BaseAction> actions;
    protected List<BaseContext> contexts;

    protected BaseSensorManager(Context context, String name, List<BaseAction> actions, List<BaseContext> contexts) {
        this.mContext = context;
        this.name = name;
        this.actions = actions;
        this.contexts = contexts;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract List<Integer> getSensorTypeList();

    public abstract void start();
    public abstract void stop();

    // TODO: refactor this
    public abstract void onSensorChangedDex(SensorEvent event);
    public abstract void onAccessibilityEventDex(AccessibilityEvent event);
    public abstract void onBroadcastEventDex(BroadcastEvent event);

    public void stopLater(long millisecond) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stop();
            }
        }, millisecond);
    };

}