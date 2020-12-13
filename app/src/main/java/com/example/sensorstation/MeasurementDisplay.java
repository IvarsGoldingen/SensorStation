package com.example.sensorstation;

/*
Used to format the measured values in the UI
* */

import android.graphics.PorterDuff;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class MeasurementDisplay {
    //TODO: implement hysteresis

    private static final int NO_DATA_PROVIDED = -1;
    private static final int COLOR_WARNING = 0xFFFF6D00;
    private static final int COLOR_ALARM = 0xFFFF0000;
    private static final int COLOR_OK = 0xFF4CAF50;
    private static final int COLOR_INVALID = 0xFF000000;
    private static final int COLOR_OLD = 0xFF656363;
    private static final int STATE_LOW_ALARM = 1;
    private static final int STATE_LOW_WARNING = 2;
    private static final int STATE_HIGH_WARNING = 3;
    private static final int STATE_HIGH_ALARM = 4;
    private static final int STATE_OK = 5;


    //actual reading
    private double value = NO_DATA_PROVIDED;
    private boolean valid = false;
    //indicates old reading
    private boolean old = false;
    //limits for alarms and warning
    private double warningHighValue = NO_DATA_PROVIDED;
    private double warningLowValue = NO_DATA_PROVIDED;
    private double alarmHighValue = NO_DATA_PROVIDED;
    private double alarmLowValue = NO_DATA_PROVIDED;
    private double hysteresis = NO_DATA_PROVIDED;
    private int state = STATE_OK;
    private DecimalFormat format;

    public MeasurementDisplay() {

    }


    public MeasurementDisplay(double alarmLowValue,
                              double warningLowValue,
                              double warningHighValue,
                              double alarmHighValue,
                              double hysteresis,
                              DecimalFormat format) {
        this.warningHighValue = warningHighValue;
        this.warningLowValue = warningLowValue;
        this.alarmHighValue = alarmHighValue;
        this.alarmLowValue = alarmLowValue;
        this.hysteresis = hysteresis;
        this.format = format;
    }

    public void updateView (TextView view){

        int colorToUse = COLOR_INVALID;
        if (old){
            colorToUse = COLOR_OLD;
        } else if (valid){
            //TODO:hysteresis
//            if (state != STATE_OK){
//                if (state == STATE_LOW_ALARM){
//
//                }
//            }
            //value valid
            if (value <= alarmLowValue ||
                    value >= alarmHighValue) {
                //ALARM STATE
                colorToUse = COLOR_ALARM;
            } else if (value <= warningLowValue ||
                    value >= warningHighValue){
                //WARNING STATE
                colorToUse = COLOR_WARNING;
            } else {
                //view.setBackgroundColor(COLOR_OK);
                colorToUse = COLOR_OK;
            }
        }
        view.getBackground().setColorFilter(colorToUse,
                PorterDuff.Mode.SRC_ATOP
        );
        view.setText(format.format(value));
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isOld() {
        return old;
    }

    public void setOld(boolean old) {
        this.old = old;
    }
}
