package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimeManager extends TriggerManager {
    private ScheduledExecutorService scheduledExecutorService;
    private List<ScheduledFuture<?>> futureList;
    private ScheduledFuture<?> timeCheckFt;

    public static Integer latest_formalizedTime;

    public TimeManager(VolEventListener volEventListener, ScheduledExecutorService scheduledExecutorService, List<ScheduledFuture<?>> futureList) {
        super(volEventListener);
        this.scheduledExecutorService = scheduledExecutorService;
        this.futureList = futureList;
    }

    @Override
    public void start() {
        startTimeCheck();
        super.start();
    }

    @Override
    public void stop() {
        if (timeCheckFt != null) {
            timeCheckFt.cancel(true);
        }
        super.stop();
    }

    public void startTimeCheck() {
        futureList.add(timeCheckFt = scheduledExecutorService.scheduleAtFixedRate(() -> {
            Calendar calendar = Calendar.getInstance();
            Integer new_formalizedTime = calendar.get(Calendar.DAY_OF_WEEK) * 10 + hour2piece(calendar.get(Calendar.HOUR_OF_DAY));
            if (!Objects.equals(latest_formalizedTime, new_formalizedTime)) {
                latest_formalizedTime = new_formalizedTime;
                Bundle bundle = new Bundle();
                bundle.putInt("Time", latest_formalizedTime);
                volEventListener.onVolEvent(VolEventListener.EventType.Time, bundle);
            }
        }, 0, 30 * 60 * 1000, TimeUnit.MILLISECONDS));
    }

    public int hour2piece(int hour) {
        if (hour <= 6)
            return 0;
        else if (hour <= 11)
            return 1;
        else if (hour <= 13)
            return 2;
        else if (hour <= 17)
            return 3;
        else if (hour <= 20)
            return 4;
        return 5;
    }
}
