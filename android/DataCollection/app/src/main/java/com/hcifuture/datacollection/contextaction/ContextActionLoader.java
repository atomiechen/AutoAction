package com.hcifuture.datacollection.contextaction;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.hcifuture.datacollection.BuildConfig;
import com.hcifuture.datacollection.contextaction.sensor.IMUSensorManager;
import com.hcifuture.datacollection.contextaction.sensor.ProximitySensorManager;
import com.hcifuture.shared.communicate.listener.ActionListener;
import com.hcifuture.shared.communicate.listener.ContextListener;
import com.hcifuture.shared.communicate.listener.RequestListener;
import com.hcifuture.shared.communicate.status.Heartbeat;

import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class ContextActionLoader {
    private Context mContext;

    private ClassLoader classLoader;
    private Class containerClass;

    private Object container;

    private IMUSensorManager imuSensorManager;
    private ProximitySensorManager proximitySensorManager;

    private Method onAccessibilityEvent;
    private Method onKeyEvent;

    public ContextActionLoader(Context context, DexClassLoader classLoader) {
        this.mContext = context;
        this.classLoader = classLoader;
        try {
            containerClass = classLoader.loadClass("com.hcifuture.contextactionlibrary.contextaction.ContextActionContainer");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object newContainer(ActionListener actionListener, ContextListener contextListener, RequestListener requestListener) {
        try {
            return containerClass.getDeclaredConstructor(Context.class,
                    ActionListener.class, ContextListener.class,
                    RequestListener.class,
                    boolean.class, boolean.class, String.class, String.class)
                    .newInstance(mContext,
                            actionListener, contextListener,
                            requestListener,
                            true, false, BuildConfig.SAVE_PATH, BuildConfig.WEB_SERVER);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void startContainer(Object container) {
        try {
            Method start = containerClass.getMethod("start");
            start.invoke(container);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCollectors(Object container) {
        try {
            Method start = containerClass.getMethod("startCollectors");
            start.invoke(container);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopContainer(Object container) {
        try {
            Method stop = containerClass.getMethod("stop");
            stop.invoke(container);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Method getOnSensorChanged(Object container) {
        try {
            Method method = containerClass.getMethod("onSensorChangedDex", SensorEvent.class);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Method getOnAccessibilityEvent(Object container) {
        try {
            Method method = containerClass.getMethod("onAccessibilityEventDex", AccessibilityEvent.class);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Method getOnKeyEvent(Object container) {
        try {
            Method method = containerClass.getMethod("onKeyEventDex", KeyEvent.class);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void startSensorManager(Object container, Method onSensorChanged) {
        imuSensorManager = new IMUSensorManager(mContext,
                SensorManager.SENSOR_DELAY_FASTEST,
                "AlwaysOnSensorManager",
                container,
                onSensorChanged
                );

        proximitySensorManager = new ProximitySensorManager(mContext,
                SensorManager.SENSOR_DELAY_FASTEST,
                "ProximitySensorManager",
                container,
                onSensorChanged
        );
    }

    public void startDetection(ActionListener actionListener, ContextListener contextListener, RequestListener requestListener) {
        try {
            container = newContainer(actionListener, contextListener, requestListener);
            Method onSensorChanged = getOnSensorChanged(container);
            startSensorManager(container, onSensorChanged);
            onAccessibilityEvent = getOnAccessibilityEvent(container);
            onKeyEvent = getOnKeyEvent(container);
            startContainer(container);
            startCollectors(container);
            imuSensorManager.start();
            proximitySensorManager.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDetection() {
        if (imuSensorManager != null) {
            imuSensorManager.start();
        }
        if (proximitySensorManager != null) {
            proximitySensorManager.start();
        }
        if (container != null) {
            startContainer(container);
        }
    }

    public void stopDetection() {
        if (imuSensorManager != null) {
            imuSensorManager.stop();
        }
        if (proximitySensorManager != null) {
            proximitySensorManager.stop();
        }
        if (container != null) {
            stopContainer(container);
        }
    }

    public void onExternalEvent(Bundle bundle) {
        try {
            if (container != null) {
                Method method = containerClass.getMethod("onExternalEventDex", Bundle.class);
                method.invoke(container, bundle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (onAccessibilityEvent != null) {
                onAccessibilityEvent.invoke(container, event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onKeyEvent(KeyEvent event) {
        try {
            if (onKeyEvent != null) {
                onKeyEvent.invoke(container, event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Heartbeat getHeartbeat() {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("getHeartbeat");
                Object heartbeat = method.invoke(container);
                if (heartbeat == null) {
                    return null;
                } else {
                    return (Heartbeat)heartbeat;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    public boolean setNumberExternalStatus(String key, Number value) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("setNumberExternalStatusDex",
                        String.class, Number.class);
                Object result = method.invoke(container, key, value);
                return (Boolean)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    public boolean setBooleanExternalStatus(String key, Boolean value) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("setBooleanExternalStatusDex",
                        String.class, Boolean.class);
                Object result = method.invoke(container, key, value);
                return (Boolean)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    public boolean setStringExternalStatus(String key, String value) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("setStringExternalStatusDex",
                        String.class, String.class);
                Object result = method.invoke(container, key, value);
                return (Boolean)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    public Integer getIntegerExternalStatus(String key) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("getIntegerExternalStatusDex",
                        String.class);
                Object result = method.invoke(container, key);
                return (Integer)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    public Long getLongExternalStatus(String key) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("getLongExternalStatusDex",
                        String.class);
                Object result = method.invoke(container, key);
                return (Long)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    public Float getFloatExternalStatus(String key) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("getFloatExternalStatusDex",
                        String.class);
                Object result = method.invoke(container, key);
                return (Float)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    public Boolean getBooleanExternalStatus(String key) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("getBooleanExternalStatusDex",
                        String.class);
                Object result = method.invoke(container, key);
                return (Boolean)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    public String getStringExternalStatus(String key) {
        if (container != null) {
            try {
                Method method = containerClass.getMethod("getStringExternalStatusDex",
                        String.class);
                Object result = method.invoke(container, key);
                return (String)result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }
}

