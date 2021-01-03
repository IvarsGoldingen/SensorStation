package com.example.sensorstation;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class Helper {
    private static final long MAX_AGE_OF_VALUE_DAYS = 20;
    private static final long MAX_AGE_OF_VALUE_HOURS = MAX_AGE_OF_VALUE_DAYS * 24;
    private static final long MAX_AGE_OF_VALUE_MINUTES = MAX_AGE_OF_VALUE_HOURS * 60;
    private static final long MAX_AGE_OF_VALUE_SECONDS = MAX_AGE_OF_VALUE_MINUTES * 60;
    private static final long MAX_AGE_OF_VALUE_MS = MAX_AGE_OF_VALUE_SECONDS * 1000;
    private static final String TAG = "Helper functions";

    public static String getDayNameString(int dayOfWeek){
        switch (dayOfWeek){
            case Calendar.MONDAY:
                return "PIRMD";
            case Calendar.TUESDAY:
                return "OTRD";
            case Calendar.WEDNESDAY:
                return "TREŠD";
            case Calendar.THURSDAY:
                return "CETURTD";
            case Calendar.FRIDAY:
                return "PIEKTD";
            case Calendar.SATURDAY:
                return "SESTD";
            case Calendar.SUNDAY:
                return "SVĒTD";
            default:
                return"ERR";
        }
    }

    public static boolean isLogValueOld(long timeAdded){
        Date currentDate = Calendar.getInstance().getTime();
        long currentDateEpoch = currentDate.getTime();
        //time difference in miliseconds
        long differenceMS = currentDateEpoch - timeAdded;
        showItemAge(differenceMS);
        if (differenceMS > MAX_AGE_OF_VALUE_MS){
            return true;
        }
        return false;
    }

    private static void showItemAge(long timeMs){
        double timeDays = (double)timeMs / 1000 / 60 / 60 / 24;
        Log.d(TAG, "Value is " + String.valueOf(timeDays) + " days old");
    }

}
