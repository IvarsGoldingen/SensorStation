package com.example.sensorstation;

import java.util.Calendar;

public class Helper {
    public static String getDayNameString(int dayOfWeek){
        switch (dayOfWeek){
            case Calendar.MONDAY:
                return "Pirmd";
            case Calendar.TUESDAY:
                return "Otrd";
            case Calendar.WEDNESDAY:
                return "Tresd";
            case Calendar.THURSDAY:
                return "Ceturd";
            case Calendar.FRIDAY:
                return "Piektd";
            case Calendar.SATURDAY:
                return "Sestd";
            case Calendar.SUNDAY:
                return "Svetd";
            default:
                return"err";
        }
    }

}
