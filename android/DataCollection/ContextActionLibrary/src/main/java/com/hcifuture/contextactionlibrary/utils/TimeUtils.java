package com.hcifuture.contextactionlibrary.utils;

import java.util.Calendar;
import java.text.SimpleDateFormat;

public class TimeUtils {
    public static String getCurrentTimePeriod() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        if (hourOfDay < 5) {
            return "midnight";
        } else if (hourOfDay < 9) {
            return "early morning";
        } else if (hourOfDay < 12) {
            return "morning";
        } else if (hourOfDay < 14) {
            return "noon";
        } else if (hourOfDay < 18) {
            return "afternoon";
        } else if (hourOfDay < 22) {
            return "evening";
        } else {
            return "night";
        }
    }

    public static String getCurrentDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
        return dayFormat.format(calendar.getTime());
    }
}
