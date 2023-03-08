package com.hcifuture.contextactionlibrary.volume;

import android.os.Bundle;

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

public class TimeManager {
    private Calendar calendar = Calendar.getInstance();

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
}
