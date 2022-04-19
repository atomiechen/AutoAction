package com.hcifuture.contextactionlibrary.contextaction;

import android.content.Context;
import android.hardware.SensorEvent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.contextaction.action.ExampleAction;
import com.hcifuture.contextactionlibrary.contextaction.collect.CloseCollector;
import com.hcifuture.contextactionlibrary.contextaction.collect.ConfigCollector;
import com.hcifuture.contextactionlibrary.contextaction.collect.ExampleCollector;
import com.hcifuture.contextactionlibrary.contextaction.collect.FlipCollector;
import com.hcifuture.contextactionlibrary.contextaction.collect.InformationalContextCollector;
import com.hcifuture.contextactionlibrary.contextaction.collect.TapTapCollector;
import com.hcifuture.contextactionlibrary.contextaction.collect.TimedCollector;
import com.hcifuture.contextactionlibrary.contextaction.context.ConfigContext;
import com.hcifuture.contextactionlibrary.contextaction.context.informational.InformationalContext;
import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;
import com.hcifuture.contextactionlibrary.sensor.data.Data;
import com.hcifuture.contextactionlibrary.sensor.distributor.DataDistributor;
import com.hcifuture.contextactionlibrary.sensor.trigger.ClickTrigger;
import com.hcifuture.contextactionlibrary.contextaction.action.BaseAction;
import com.hcifuture.contextactionlibrary.contextaction.action.CloseAction;
import com.hcifuture.contextactionlibrary.contextaction.action.FlipAction;
import com.hcifuture.contextactionlibrary.contextaction.action.PocketAction;
import com.hcifuture.contextactionlibrary.contextaction.action.TapTapAction;
import com.hcifuture.contextactionlibrary.contextaction.action.TopTapAction;
import com.hcifuture.contextactionlibrary.contextaction.collect.BaseCollector;
import com.hcifuture.contextactionlibrary.contextaction.context.BaseContext;
import com.hcifuture.contextactionlibrary.contextaction.context.physical.ProximityContext;
import com.hcifuture.contextactionlibrary.contextaction.context.physical.TableContext;
import com.hcifuture.contextactionlibrary.sensor.collector.CollectorManager;
import com.hcifuture.contextactionlibrary.sensor.trigger.TriggerConfig;
import com.hcifuture.shared.communicate.config.ActionConfig;
import com.hcifuture.shared.communicate.config.ContextConfig;
import com.hcifuture.shared.communicate.event.BroadcastEvent;
import com.hcifuture.shared.communicate.listener.ActionListener;
import com.hcifuture.shared.communicate.SensorType;
import com.hcifuture.shared.communicate.listener.ContextListener;
import com.hcifuture.shared.communicate.listener.RequestListener;
import com.hcifuture.shared.communicate.result.ActionResult;
import com.hcifuture.shared.communicate.result.ContextResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ContextActionContainer implements ActionListener, ContextListener {
    private Context mContext;

    // private ThreadPoolExecutor executor;

    private List<BaseAction> actions;
    private List<BaseContext> contexts;

    // private List<BaseSensorManager> sensorManagers;

    private boolean fromDex = false;
    private boolean openSensor = false;
    private List<ActionConfig> actionConfig;
    private List<ContextConfig> contextConfig;
    private ActionListener actionListener;
    private ContextListener contextListener;
    private RequestListener requestListener;

    private ClickTrigger clickTrigger;

    private List<BaseCollector> collectors;

    private ScheduledExecutorService scheduledExecutorService;
    private List<ScheduledFuture<?>> futureList;

    private TapTapAction tapTapAction;
    private String markTimestamp;

    private ScheduledFuture<?> actionFuture;
    private ScheduledFuture<?> contextFuture;

    private CollectorManager collectorManager;
    private DataDistributor dataDistributor;

    private static String SAVE_PATH;

    public ContextActionContainer(Context context, List<BaseAction> actions, List<BaseContext> contexts, RequestListener requestListener, String SAVE_PATH) {
        this.mContext = context;
        this.actions = actions;
        this.contexts = contexts;
        this.actionFuture = null;
        this.contextFuture = null;
        ContextActionContainer.SAVE_PATH = SAVE_PATH;
        /*
        this.executor = new ThreadPoolExecutor(1,
                1,
                1000, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(2),
                new ThreadPoolExecutor.DiscardOldestPolicy());
         */
        // this.sensorManagers = new ArrayList<>();

        /*
        if (NcnnInstance.getInstance() == null) {
            NcnnInstance.init(context,
                    BuildConfig.SAVE_PATH + "best.param",
                    BuildConfig.SAVE_PATH + "best.bin",
                    4,
                    128,
                    6,
                    1,
                    2);
        }
         */

        // clickTrigger = new ClickTrigger(context, Arrays.asList(Trigger.CollectorType.CompleteIMU, Trigger.CollectorType.Bluetooth));
        this.futureList = new ArrayList<>();

        // scheduleCleanData();
    }

    public ContextActionContainer(Context context,
                                  List<ActionConfig> actionConfig, ActionListener actionListener,
                                  List<ContextConfig> contextConfig, ContextListener contextListener,
                                  RequestListener requestListener,
                                  boolean fromDex, boolean openSensor, String SAVE_PATH) {
        this(context, new ArrayList<>(), new ArrayList<>(), requestListener, SAVE_PATH);
        this.actionConfig = actionConfig;
        this.actionListener = actionListener;
        this.contextConfig = contextConfig;
        this.contextListener = contextListener;
        this.requestListener = requestListener;
        this.fromDex = fromDex;
        this.openSensor = openSensor;
    }

    public static String getSavePath() {
        return SAVE_PATH;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void start() {
        initialize();
        /*
        if (openSensor) {
            for (BaseSensorManager sensorManager: sensorManagers) {
                sensorManager.start();
            }
        }
         */

        for (BaseAction action: actions) {
            action.start();
        }
        for (BaseContext context: contexts) {
            context.start();
        }
        monitorAction();
        monitorContext();
        if (collectorManager != null) {
            collectorManager.resume();
        }
        /*
        if (clickTrigger != null) {
            clickTrigger.resume();
        }
         */
    }

    public void stop() {
        /*
        if (openSensor) {
            for (BaseSensorManager sensorManager: sensorManagers) {
                sensorManager.stop();
            }
        }
         */
        for (BaseAction action: actions) {
            action.stop();
        }
        for (BaseContext context: contexts) {
            context.stop();
        }
        /*
        if (clickTrigger != null) {
            clickTrigger.pause();
            clickTrigger.close();
        }
         */
        if (collectorManager != null) {
            if (dataDistributor != null) {
                collectorManager.unregisterListener(dataDistributor);
            }
            collectorManager.pause();
            collectorManager.close();
        }
        for (ScheduledFuture<?> future: futureList) {
            future.cancel(true);
        }
        futureList.clear();
        scheduledExecutorService.shutdownNow();
    }

    public void pause() {
        for (BaseAction action: actions) {
            action.stop();
        }
        for (BaseContext context: contexts) {
            context.stop();
        }
        if (collectorManager != null) {
            collectorManager.pause();
        }
        /*
        if (clickTrigger != null) {
            clickTrigger.pause();
        }
         */
    }

    public void resume() {
        for (BaseAction action: actions) {
            action.start();
        }
        for (BaseContext context: contexts) {
            context.start();
        }
        if (actionFuture != null && (actionFuture.isDone() || actionFuture.isCancelled())) {
            monitorAction();
        }
        if (contextFuture != null && (contextFuture.isDone() || contextFuture.isCancelled())) {
            monitorContext();
        }
        /*
        if (clickTrigger != null) {
            clickTrigger.resume();
        }
         */
        if (collectorManager != null) {
            collectorManager.resume();
        }
    }

    private List<BaseAction> selectBySensorTypeAction(List<BaseAction> actions, SensorType sensorType) {
        List<BaseAction> result = new ArrayList<>();
        for (BaseAction action: actions) {
            if (action.getConfig().getSensorType().contains(sensorType)) {
                result.add(action);
            }
        }
        return result;
    }

    private List<BaseContext> selectBySensorTypeContext(List<BaseContext> contexts, SensorType sensorType) {
        List<BaseContext> result = new ArrayList<>();
        for (BaseContext context: contexts) {
            if (context.getConfig().getSensorType().contains(sensorType)) {
                result.add(context);
            }
        }
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void initialize() {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(32);
        ((ScheduledThreadPoolExecutor)scheduledExecutorService).setRemoveOnCancelPolicy(true);

        this.collectorManager = new CollectorManager(mContext, Arrays.asList(
                CollectorManager.CollectorType.IMU,
                CollectorManager.CollectorType.Location,
                CollectorManager.CollectorType.Audio,
                CollectorManager.CollectorType.Bluetooth,
                CollectorManager.CollectorType.Wifi,
                CollectorManager.CollectorType.NonIMU
        ), scheduledExecutorService, futureList);

        this.clickTrigger = new ClickTrigger(mContext, collectorManager, scheduledExecutorService, futureList);

        // cwh: do not use Arrays.asList() to assign to collectors,
        // because it returns a fixed-size list backed by the specified array and we cannot perform add()
        collectors = new ArrayList<>();
        collectors.add(new TapTapCollector(mContext, scheduledExecutorService, futureList, requestListener, clickTrigger));
        collectors.add(new CloseCollector(mContext, scheduledExecutorService, futureList, requestListener, clickTrigger));
        collectors.add(new FlipCollector(mContext, scheduledExecutorService, futureList, requestListener, clickTrigger));

        TimedCollector timedCollector = new TimedCollector(mContext, scheduledExecutorService, futureList, requestListener, clickTrigger)
                .scheduleFixedDelayUpload(CollectorManager.CollectorType.Audio, new TriggerConfig().setBluetoothScanTime(10000).setAudioLength(5000), 15000, 0)
                .scheduleFixedDelayUpload(CollectorManager.CollectorType.Wifi, new TriggerConfig().setWifiScanTime(10000), 1000, 0)
                .scheduleFixedRateUpload(CollectorManager.CollectorType.Location, new TriggerConfig(), 10000, 0);
        collectors.add(timedCollector);

        if (fromDex) {
            for (int i = 0; i < actionConfig.size(); i++) {
                ActionConfig config = actionConfig.get(i);
                switch (config.getAction()) {
                    case "TapTap":
                        tapTapAction = new TapTapAction(mContext, config, requestListener, Arrays.asList(this, actionListener), scheduledExecutorService, futureList);
                        actions.add(tapTapAction);
                        break;
                    case "TopTap":
                        TopTapAction topTapAction = new TopTapAction(mContext, config, requestListener, Arrays.asList(this, actionListener), scheduledExecutorService, futureList);
                        actions.add(topTapAction);
                        break;
                    case "Flip":
                        FlipAction flipAction = new FlipAction(mContext, config, requestListener, Arrays.asList(this, actionListener), scheduledExecutorService, futureList);
                        actions.add(flipAction);
                        break;
                    case "Close":
                        CloseAction closeAction = new CloseAction(mContext, config, requestListener, Arrays.asList(this, actionListener), scheduledExecutorService, futureList);
                        actions.add(closeAction);
                        break;
                    case "Pocket":
                        PocketAction pocketAction = new PocketAction(mContext, config, requestListener, Arrays.asList(this, actionListener), scheduledExecutorService, futureList);
                        actions.add(pocketAction);
                        break;
                    case "Example":
                        LogCollector logCollector = collectorManager.newLogCollector("Log0", 100);
                        timedCollector.scheduleTimedLogUpload(logCollector, 5000, 0, "Example");
                        collectors.add(new ExampleCollector(mContext, scheduledExecutorService, futureList, requestListener, clickTrigger, logCollector));
                        ExampleAction exampleAction = new ExampleAction(mContext, config, requestListener, Arrays.asList(this, actionListener), logCollector, scheduledExecutorService, futureList);
                        actions.add(exampleAction);
                        break;
                }
            }
            for (int i = 0; i < contextConfig.size(); i++) {
                ContextConfig config = contextConfig.get(i);
                switch (config.getContext()) {
                    case "Proximity":
                        ProximityContext proximityContext = new ProximityContext(mContext, config, requestListener, Arrays.asList(this, contextListener), scheduledExecutorService, futureList);
                        contexts.add(proximityContext);
                        break;
                    case "Table":
                        TableContext tableContext = new TableContext(mContext, config, requestListener, Arrays.asList(this, contextListener), scheduledExecutorService, futureList);
                        contexts.add(tableContext);
                        break;
                    case "Informational":
                        LogCollector informationLogCollector = collectorManager.newLogCollector("Informational", 8192);
                        timedCollector.scheduleTimedLogUpload(informationLogCollector, 60000, 5000, "Informational");
                        collectors.add(new InformationalContextCollector(mContext, scheduledExecutorService, futureList, requestListener, clickTrigger, informationLogCollector));
                        InformationalContext informationalContext = new InformationalContext(mContext, config, requestListener, Arrays.asList(this, contextListener),informationLogCollector, scheduledExecutorService, futureList);
                        contexts.add(informationalContext);
                        break;
                    case "Config":
                        LogCollector configLogCollector = collectorManager.newLogCollector("Config", 8192);
                        timedCollector.scheduleTimedLogUpload(configLogCollector, 60000, 5000, "Config");
                        collectors.add(new ConfigCollector(mContext, scheduledExecutorService, futureList, requestListener, clickTrigger, configLogCollector));
                        ConfigContext configContext = new ConfigContext(mContext, config, requestListener, Arrays.asList(this, contextListener), configLogCollector, scheduledExecutorService, futureList);
                        contexts.add(configContext);
                        break;
                }
            }
        }

        this.dataDistributor = new DataDistributor(actions, contexts);
        collectorManager.registerListener(dataDistributor);

        // init sensor
        /*
        sensorManagers.add(new IMUSensorManager(mContext,
                "IMUSensorManager",
                selectBySensorTypeAction(actions, SensorType.IMU),
                selectBySensorTypeContext(contexts, SensorType.IMU),
                SensorManager.SENSOR_DELAY_FASTEST
        ));

        sensorManagers.add(new ProximitySensorManager(mContext,
                "ProximitySensorManager",
                selectBySensorTypeAction(actions, SensorType.PROXIMITY),
                selectBySensorTypeContext(contexts, SensorType.PROXIMITY),
                SensorManager.SENSOR_DELAY_FASTEST
        ));

        sensorManagers.add(new AccessibilityEventManager(mContext,
                "AccessibilityEventManager",
                selectBySensorTypeAction(actions, SensorType.ACCESSIBILITY),
                selectBySensorTypeContext(contexts, SensorType.ACCESSIBILITY)
        ));

        sensorManagers.add(new BroadcastEventManager(mContext,
                "BroadcastEventManager",
                selectBySensorTypeAction(actions, SensorType.BROADCAST),
                selectBySensorTypeContext(contexts, SensorType.BROADCAST)
        ));
         */
    }

    private void monitorAction() {
        actionFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                for (BaseAction action : actions) {
                    action.getAction();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 3000L, 20L, TimeUnit.MILLISECONDS);
        futureList.add(actionFuture);
    }

    private void monitorContext() {
        contextFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                for (BaseContext context: contexts) {
                    context.getContext();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 3000L, 1000L, TimeUnit.MILLISECONDS);
        futureList.add(contextFuture);
    }

    public void onSensorChangedDex(SensorEvent event) {
        int type = event.sensor.getType();
        /*
        for (BaseSensorManager sensorManager: sensorManagers) {
            if (sensorManager.getSensorTypeList() != null && sensorManager.getSensorTypeList().contains(type)) {
                sensorManager.onSensorChangedDex(event);
            }
        }
         */
    }

    public void onAccessibilityEventDex(AccessibilityEvent event) {
        for (BaseContext context: contexts) {
            context.onAccessibilityEvent(event);
        }
        /*
        for (BaseSensorManager sensorManager: sensorManagers) {
            sensorManager.onAccessibilityEventDex(event);
        }
         */
    }

    public void onBroadcastEventDex(BroadcastEvent event) {
        for (BaseContext context: contexts) {
            context.onBroadcastEvent(event);
        }
        /*
        for (BaseSensorManager sensorManager: sensorManagers) {
            sensorManager.onBroadcastEventDex(event);
        }
         */
    }

    /*
    private void scheduleCleanData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),3, 0, 0);
        Date date = calendar.getTime();
        if (date.before(new Date())) {
            calendar.add(Calendar.DATE, 1);
            date = calendar.getTime();
        }

        Timer timer= new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (clickTrigger != null) {
                    clickTrigger.cleanData();
                }
                Log.e("TapTapCollector", "triggered");
            }
        }, date, 24 * 60 * 60 * 1000);

    }
     */

    /*
    @Override
    public void onActionRecognized(ActionResult action) {
        if (action.getAction().equals("TapTapConfirmed")) {
            markTimestamp = action.getTimestamp();
            tapTapAction.onConfirmed();
        }
        else if (action.getAction().equals("TopTap")) {
            markTimestamp = action.getTimestamp();
        }
    }
     */

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onAction(ActionResult action) {
        if (collectors != null) {
            for (BaseCollector collector: collectors) {
                collector.onAction(action);
            }
        }
            /*
        if (action.getAction().equals("TapTap") || action.getAction().equals("TopTap") || action.getAction().equals("Pocket")) {
            if (clickTrigger != null) {
                clickTrigger.trigger(Collections.singletonList(Trigger.CollectorType.CompleteIMU), new TriggerConfig());
            }
        }
             */
//        if (collectors != null) {
//            for (BaseCollector collector: collectors) {
//                collector.onAction(action);
//            }
//        }
    }

    /*
    @Override
    public void onActionSave(ActionResult action) {
        if (action.getAction().equals("TapTap") || action.getAction().equals("TopTap")) {
            action.setTimestamp(markTimestamp);
        }
        if (collectors != null) {
            for (BaseCollector collector : collectors) {
                collector.onAction(action);
            }
        }
    }
     */

    @Override
    public void onContext(ContextResult context) {
        if (collectors != null) {
            for (BaseCollector collector: collectors) {
                collector.onContext(context);
            }
        }
    }
}
