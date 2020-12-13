package com.example.sensorstation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.CO2value)
    TextView co2TV;
    @BindView(R.id.TVOCvalue)
    TextView tvocTV;
    @BindView(R.id.Tvalue)
    TextView tempTV;
    @BindView(R.id.Humvalue)
    TextView hum2TV;
    @BindView(R.id.Tvalue2) TextView temp2TV;
    @BindView(R.id.PressValue) TextView pressureTV;

    DatabaseReference fbRef;
    ValueEventListener listener;
    FirebaseDatabase database;

    //If the value received from firebase is older than this, display it to the user
    private static final long MAX_TIME_DIF_S = 60;
    private static final long MAX_TIME_DIF_MS = MAX_TIME_DIF_S * 1000;
    private static final int COLOR_OLD = 0xFF656363;

    //For the initial read from the firebase
    boolean firstValue = true;
    //Indicates that the calues are old
    boolean oldValues = false;
    //CO2 value smoothed
    int smoothCO2 = 400;
    //Last CO2 measurement
    double lastCO2Measurement = 400;
    //smoothing coeficient
    double smoothing = 0.4;
    //timer for smoothing
    Timer smoothCO2Timer;
    //TAG
    String TAG = "Sensor Station MainActivity";

    //objects for displaying the values in the main activity
    MeasurementDisplay display_CO2 = new MeasurementDisplay(
            AlarmWarningSettings.CO2_LA,
            AlarmWarningSettings.CO2_LW,
            AlarmWarningSettings.CO2_HW,
            AlarmWarningSettings.CO2_HA,
            AlarmWarningSettings.CO2_HYS,
            AlarmWarningSettings.CO2_FORMAT);
    MeasurementDisplay display_TVOC = new MeasurementDisplay(
            AlarmWarningSettings.TVOC_LA,
            AlarmWarningSettings.TVOC_LW,
            AlarmWarningSettings.TVOC_HW,
            AlarmWarningSettings.TVOC_HA,
            AlarmWarningSettings.TVOC_HYS,
            AlarmWarningSettings.TVOC_FORMAT);
    MeasurementDisplay display_T1 = new MeasurementDisplay(
            AlarmWarningSettings.T1_LA,
            AlarmWarningSettings.T1_LW,
            AlarmWarningSettings.T1_HW,
            AlarmWarningSettings.T1_HA,
            AlarmWarningSettings.T1_HYS,
            AlarmWarningSettings.T1_FORMAT);
    MeasurementDisplay display_T2 = new MeasurementDisplay(
            AlarmWarningSettings.T2_LA,
            AlarmWarningSettings.T2_LW,
            AlarmWarningSettings.T2_HW,
            AlarmWarningSettings.T2_HA,
            AlarmWarningSettings.T2_HYS,
            AlarmWarningSettings.T2_FORMAT);
    MeasurementDisplay display_RH = new MeasurementDisplay(
            AlarmWarningSettings.RH_LA,
            AlarmWarningSettings.RH_LW,
            AlarmWarningSettings.RH_HW,
            AlarmWarningSettings.RH_HA,
            AlarmWarningSettings.RH_HYS,
            AlarmWarningSettings.RH_FORMAT);
    MeasurementDisplay display_PR = new MeasurementDisplay(
            AlarmWarningSettings.PR_LA,
            AlarmWarningSettings.PR_LW,
            AlarmWarningSettings.PR_HW,
            AlarmWarningSettings.PR_HA,
            AlarmWarningSettings.PR_HYS,
            AlarmWarningSettings.PR_FORMAT);

    /*
    //TODO:display message if no data for some time. Make a timer that checks if values are
    //comming in
    //TODO:Update FB security rules
    //TODO: in MC set auto reconnects to Wifi
    //TODO: in MC set loading animation for WiFi connection
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w("TAG", "Start");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        fbRef = database.getReference().child("SensorStation");
        listener = (new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                SensorStation sensors = dataSnapshot.getValue(SensorStation.class);
                updateSensorDataUI(sensors);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
        fbRef.addValueEventListener(listener);
    }

    private void updateSensorDataUI(SensorStation sensors){
        //lastCO2Measurement = sensors.getCO2();
        display_CO2.setValid(sensors.isSGP_valid());
        lastCO2Measurement = sensors.getCO2();
        //CO2_display.setValue(sensors.getCO2());
        //Set the reading in the UI
        if (firstValue){
            //set the CO2 value directly only for the first time
            display_CO2.setValue(sensors.getCO2());
            display_CO2.updateView(co2TV);
            //Update CO2 value by filtering using a timer
            startTimer();
            //the first vale has been read
            firstValue = false;
            //get the time reading to check if the first value is new
            checkTimestammp(sensors.getLastUpdate());
            //pressure sonsor does not give indication if it is ready
            //set valid always
            display_PR.setValid(true);
            display_T2.setValid(true);
        } else if (oldValues){
            //Remove gray background from all values since a new value has been received
            oldValues = false;
            setAllDisplaysOld(false);
        }
        //Set valid
        display_TVOC.setValid(sensors.isSGP_valid());
        display_T1.setValid(sensors.isDHT_valid());
        display_RH.setValid(sensors.isDHT_valid());
        //Set value
        display_TVOC.setValue(sensors.getTVOC());
        display_T1.setValue(sensors.getTemperature());
        display_T2.setValue(sensors.getTemperature2());
        display_RH.setValue(sensors.getHumidity());
        display_PR.setValue(sensors.getPressure());
        //update UI
        display_TVOC.updateView(tvocTV);
        display_T1.updateView(tempTV);
        display_T2.updateView(temp2TV);
        display_RH.updateView(hum2TV);
        display_PR.updateView(pressureTV);
    }

    //check if the last value received from FirebaseDatabase is up to date
    //This will work for the first value downloaded from FB
    private void checkTimestammp(long timeOfValue){
        Date currentDate = Calendar.getInstance().getTime();
        long currentDateEpoch = currentDate.getTime();
        //time difference in miliseconds
        long differenceMS = currentDateEpoch - timeOfValue;
        Log.d(TAG, "Last update ms ago: " + String.valueOf(differenceMS));
        if (differenceMS > MAX_TIME_DIF_MS){
            View sv = findViewById(R.id.main_root);
            Snackbar.make(sv, "Old value", Snackbar.LENGTH_SHORT).show();
            setOldValuesUI();
            //setup all display fields to show up as old
            setAllDisplaysOld(true);
            oldValues = true;
        }
    }

    private void setOldValuesUI(){
        co2TV.getBackground().setColorFilter(COLOR_OLD, PorterDuff.Mode.SRC_ATOP);
        tvocTV.getBackground().setColorFilter(COLOR_OLD, PorterDuff.Mode.SRC_ATOP);
        tempTV.getBackground().setColorFilter(COLOR_OLD, PorterDuff.Mode.SRC_ATOP);
        temp2TV.getBackground().setColorFilter(COLOR_OLD, PorterDuff.Mode.SRC_ATOP);
        hum2TV.getBackground().setColorFilter(COLOR_OLD, PorterDuff.Mode.SRC_ATOP);
        pressureTV.getBackground().setColorFilter(COLOR_OLD, PorterDuff.Mode.SRC_ATOP);
    }

    //Call the runnnalbe on the UI thread
    private void timerMethod(){
        this.runOnUiThread(FilterCO2);
    }

    //Timer that smooths out the CO2 measurement
    private void startTimer(){
        smoothCO2Timer = new Timer();
        smoothCO2Timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerMethod();
            }
        },1000,1000);
    }

    private void setAllDisplaysOld(boolean isOld){
        display_CO2.setOld(isOld);
        display_TVOC.setOld(isOld);
        display_T1.setOld(isOld);
        display_T2.setOld(isOld);
        display_RH.setOld(isOld);
        display_PR.setOld(isOld);
    }

    //A runnable that the timer can run on the UI thread
    private Runnable FilterCO2 = new Runnable() {
        @Override
        public void run() {
            display_CO2.setValue((double)(display_CO2.getValue() +
                    ((lastCO2Measurement - display_CO2.getValue()) * smoothing)));
            display_CO2.updateView(co2TV);
        }
    };

    @Override
    protected void onResume() {
        Log.d(TAG, "On resume");
        database.goOnline();
        super.onResume();
    }

    @Override
    protected void onPause() {
        database.goOffline();
        smoothCO2Timer.cancel();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_main_graphs:
                Intent graphIntent = new Intent(MainActivity.this, GraphActivity.class);
                startActivity(graphIntent);
                break;
            case R.id.menu_main_list:
                Intent logListIntent = new Intent(MainActivity.this, HistoryListActivity.class);
                startActivity(logListIntent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}