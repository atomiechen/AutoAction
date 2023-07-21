package com.hcifuture.contextactionlibrary.volume;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.hcifuture.contextactionlibrary.sensor.collector.sync.LogCollector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimeManager extends TriggerManager {
    public final static String TAG = "TimeManager";
    private Calendar calendar = Calendar.getInstance();
    private ScheduledExecutorService scheduledExecutorService;
    private List<ScheduledFuture<?>> futureList;
    private ScheduledFuture<?> time_scan;
    private String last_time;
    private String last_day_of_week;

    public TimeManager(VolEventListener volEventListener, ScheduledExecutorService executorService, List<ScheduledFuture<?>> futureList) {
        super(volEventListener);
        this.scheduledExecutorService = executorService;
        this.futureList = futureList;
        this.last_day_of_week = getWeekString();
        this.last_time = getTimeString();
    }

    @Override
    public void start() {
        if (time_scan == null) {
            time_scan = scheduledExecutorService.scheduleAtFixedRate(() -> {
                String time = getTimeString();
                String day_of_week = getWeekString();
                if(!time.equals(last_time)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("time", time);
                    volEventListener.onVolEvent(VolEventListener.EventType.TimeChange, bundle);
                    last_time = time;
                }
                if(!day_of_week.equals(last_day_of_week)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("day_of_week", day_of_week);
                    volEventListener.onVolEvent(VolEventListener.EventType.DayOfWeekChange, bundle);
                    last_day_of_week = day_of_week;
                }
            }, 0, 60 * 1000, TimeUnit.MILLISECONDS);
            futureList.add(time_scan);
        }
    }

    @Override
    public void stop() {
        if (time_scan != null) {
            time_scan.cancel(true);
            time_scan = null;
        }
    }

    public String getTimeString() {
        int time = calendar.get(Calendar.HOUR_OF_DAY);
        if (time >= 4 && time < 8)
            return "early morning";
        else if (time >= 8 && time < 11)
            return "morning";
        else if (time >= 11 && time < 13)
            return "noon";
        else if (time >= 13 && time < 18)
            return "afternoon";
        else if (time >= 18 && time < 20)
            return "evening";
        else if (time >= 20 && time < 23)
            return "night";
        else
            return "midnight";
    }

    public String getWeekString() {
        int week = calendar.get(Calendar.DAY_OF_WEEK);
        List<String> week_list = new ArrayList<>(Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));
        if (week <= 7 && week > 0)
            return week_list.get(week - 1);
        return "error";
    }

    public String getExactTime() {
        return new Date().toString();
    }

    public long getTimestamp() {
        return System.currentTimeMillis();
    }
}
