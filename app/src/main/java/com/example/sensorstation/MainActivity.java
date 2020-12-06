package com.example.sensorstation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
    private static final long MAX_TIME_DIF_S = 10;
    private static final long MAX_TIME_DIF_MS = MAX_TIME_DIF_S * 1000;

    //For the initial read from the firebase
    boolean firstValue = true;
    //CO2 value smoothed
    int smoothCO2 = 400;
    //Last CO2 measurement
    int lastCO2Measurement = 400;
    //smoothing coeficient
    double smoothing = 0.4;
    //timer for smoothing
    Timer smoothCO2Timer;

    String TAG = "Sensor Station MainActivity";

    //TODO:change colors of items to indicate critical and so on
    //TODO:display message if no data for some time. Make a
    //Update FB security rules

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
                lastCO2Measurement = sensors.getCO2();
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
        //Set the reading in the UI
        if (firstValue){
            //set the CO2 value directly only for the first time
            co2TV.setText(String.valueOf(sensors.getCO2()));
            //Update CO2 value by filtering using a timer
            startTimer();
            //the first vale has been read
            firstValue = false;
            //get the time reading to check if the first value is new
            checkTimestammp(sensors.getLastUpdate());
        }
        tvocTV.setText(String.valueOf(sensors.getTVOC()));
        tempTV.setText(String.valueOf(String.format("%.1f",sensors.getTemperature())));
        hum2TV.setText(String.valueOf(String.format("%.1f",sensors.getHumidity())));
        pressureTV.setText(String.format("%.1f",sensors.getPressure()));
        temp2TV.setText(String.format("%.1f",sensors.getTemperature2()));
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
        }
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

    //A runnable that the timer can run on the UI thread
    private Runnable FilterCO2 = new Runnable() {
        @Override
        public void run() {
            smoothCO2 += ((double)(lastCO2Measurement - smoothCO2)) * smoothing;
            co2TV.setText(String.valueOf(smoothCO2));
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