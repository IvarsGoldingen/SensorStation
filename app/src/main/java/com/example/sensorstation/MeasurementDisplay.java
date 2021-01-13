package com.example.sensorstation;

/*
Used to format the measured values in the UI
* */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DecimalFormat;

public class MeasurementDisplay {
    //TODO: implement hysteresis
    //TODO: shared preferences can't store doubles directly

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
    private static final String PREFERENCE_FILE_KEY = "ALARM_WARNING_SETTINGS";


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
    private String preferenceKey;
    private Context context;

    public MeasurementDisplay() {

    }

    public MeasurementDisplay(
            Context context,
            String preferenceKey,
                              double defaultAlarmLowValue,
                              double defaultWarningLowValue,
                              double defaultWarningHighValue,
                              double defaultAlarmHighValue,
                              double defaultHysteresis,
                              DecimalFormat format) {
        this.preferenceKey = preferenceKey;
        this.context = context;
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        sharedPreferences.getFloat(preferenceKey + "HLA", (float) defaultAlarmHighValue);
        sharedPreferences.getFloat(preferenceKey + "HL", (float) defaultWarningHighValue);
        sharedPreferences.getFloat(preferenceKey + "LL", (float) defaultWarningLowValue);
        sharedPreferences.getFloat(preferenceKey + "LLA", (float) defaultAlarmLowValue);
        sharedPreferences.getFloat(preferenceKey + "HYST", (float) defaultHysteresis);

        this.alarmHighValue = sharedPreferences.getFloat(preferenceKey + "HLA", (float) defaultAlarmHighValue);
        this.warningHighValue = sharedPreferences.getFloat(preferenceKey + "HL", (float) defaultWarningHighValue);
        this.warningLowValue = sharedPreferences.getFloat(preferenceKey + "LL", (float) defaultWarningLowValue);
        this.alarmLowValue = sharedPreferences.getFloat(preferenceKey + "LLA", (float) defaultAlarmLowValue);
        this.hysteresis = sharedPreferences.getFloat(preferenceKey + "HYST", (float) defaultHysteresis);

        this.format = format;
    }

    //Get the settings Alert dialog from main and use the editexts to set the values of
    //warning and alarms
    //Returns true if setting values are correct
    public boolean setValuesFromView(View dialogView){
        EditText hlaEditText = dialogView.findViewById(R.id.sensor_setup_HLA);
        EditText hlEditText = dialogView.findViewById(R.id.sensor_setup_HLW);
        EditText llEditText = dialogView.findViewById(R.id.sensor_setup_LLW);
        EditText llaEditText = dialogView.findViewById(R.id.sensor_setup_LLA);
        EditText hystEditText = dialogView.findViewById(R.id.sensor_setup_HYST);

        String hla = hlaEditText.getText().toString();
        String hl = hlEditText.getText().toString();
        String ll = llEditText.getText().toString();
        String lla = llaEditText.getText().toString();
        String hyst = hystEditText.getText().toString();

        double newHLA;
        double newHL;
        double newLL;
        double newLLA;
        double newHYST;

        try {
            newHLA = Double.parseDouble(hla);
            newHL = Double.parseDouble(hl);
            newLL = Double.parseDouble(ll);
            newLLA = Double.parseDouble(lla);
            newHYST = Double.parseDouble(hyst);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            //Entered values are not numbers
            return false;
        }

        if (this.setAlarmHighValue(newHLA) &&
                this.setWarningHighValue(newHL) &&
                this.setWarningLowValue(newLL) &&
                this.setAlarmLowValue(newLLA) &&
                this.setHysteresis(newHYST)){
            //all values entered correctly
            return true;
        }
        //Some value entered does not make sense
        return false;
    }

    private void saveInPrefs(String key, double value){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(preferenceKey + key, (float)value);
        editor.apply();
    }

    public boolean setAlarmHighValue(double alarmHighValue) {
        if (alarmHighValue >= warningHighValue){
            saveInPrefs("HLA", alarmHighValue);
            this.alarmHighValue = alarmHighValue;
            return true;
        }
        return false;
    }

    public boolean setWarningHighValue(double warningHighValue) {
        if (warningHighValue <= alarmHighValue &&
                warningHighValue > warningLowValue){
            saveInPrefs("HL", warningHighValue);
            this.warningHighValue = warningHighValue;
            return true;
        }
        return false;
    }

    public boolean setWarningLowValue(double warningLowValue) {
        if (warningLowValue < warningHighValue &&
                warningLowValue >= alarmLowValue){
            saveInPrefs("LL", warningLowValue);
            this.warningLowValue = warningLowValue;
            return true;
        }
        return false;
    }

    public boolean setAlarmLowValue(double alarmLowValue) {
        if (alarmLowValue <= warningLowValue){
            saveInPrefs("LLA", alarmLowValue);
            this.alarmLowValue = alarmLowValue;
            return true;
        }
        return false;
    }


    public boolean setHysteresis(double hysteresis) {
        if (hysteresis >= 0){
            saveInPrefs("HYST", hysteresis);
            this.hysteresis = hysteresis;
            return true;
        }
        return false;
    }

    public void updateView (TextView view){
        boolean changeColor = true;
        int colorToUse = COLOR_INVALID;
        if (old){
            colorToUse = COLOR_OLD;
        } else if (valid){
            //value valid
            if (state == STATE_OK){
                //if the state was OK, the color settings can be done without extra checks, because
                //hysteresis calculation is not needed.
                colorToUse = getColorSetState();
            } else {
                //The alarm already is in a ALARM OR WARNING STATE,
                //check for hysteresis
                //If hysteresis value for change + hysteresis achieved set color, else leave the
                //previous
                if (state == STATE_LOW_ALARM){
                    if (value > alarmLowValue + hysteresis){
                        colorToUse = getColorSetState();
                    } else {
                        changeColor = false;
                    }
                } else if (state == STATE_LOW_WARNING){
                    if (value > warningLowValue + hysteresis ||
                        value <= alarmLowValue){
                        colorToUse = getColorSetState();
                    }else {
                        changeColor = false;
                    }
                } else if (state == STATE_HIGH_WARNING){
                    if (value < warningHighValue - hysteresis ||
                        value >= alarmHighValue){
                        colorToUse = getColorSetState();
                    }else {
                        changeColor = false;
                    }
                } else if (state == STATE_HIGH_ALARM){
                    if (value < alarmHighValue - hysteresis){
                        colorToUse = getColorSetState();
                    }else {
                        changeColor = false;
                    }
                }
            }
        }
        if (changeColor){
            view.getBackground().setColorFilter(colorToUse,
                    PorterDuff.Mode.SRC_ATOP
            );
        }

        //TODO: this is not needed here
        view.setText(format.format(value));
    }

    //
    private int getColorSetState(){
        int colorToUse;
        if (value <= alarmLowValue) {
            colorToUse = COLOR_ALARM;
            state = STATE_LOW_ALARM;
        } else if (value >= alarmHighValue) {
            colorToUse = COLOR_ALARM;
            state = STATE_HIGH_ALARM;
        } else if (value >= warningHighValue){
            colorToUse = COLOR_WARNING;
            state = STATE_HIGH_WARNING;
        } else if (value <= warningLowValue){
            colorToUse = COLOR_WARNING;
            state = STATE_LOW_WARNING;
        } else {
            colorToUse = COLOR_OK;
        }
        return colorToUse;
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

    public double getWarningHighValue() {
        return warningHighValue;
    }



    public double getWarningLowValue() {
        return warningLowValue;
    }



    public double getAlarmHighValue() {
        return alarmHighValue;
    }

    public double getAlarmLowValue() {
        return alarmLowValue;
    }

    public double getHysteresis() {
        return hysteresis;
    }


}
