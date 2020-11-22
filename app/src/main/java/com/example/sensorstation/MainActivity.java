package com.example.sensorstation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.CO2value)
    TextView co2TV;
    @BindView(R.id.TVOCvalue)
    TextView tvocTV;
    @BindView(R.id.Tvalue)
    TextView tempTV;
    @BindView(R.id.Humvalue)
    TextView hum2TV;
    @BindView(R.id.list_btn)
    Button listBtn;
    @BindView(R.id.graph_btn)
    Button graphBtn;
    @BindView(R.id.Tvalue2) TextView temp2TV;
    @BindView(R.id.PressValue) TextView pressureTV;

    DatabaseReference fbRef;
    ValueEventListener listener;
    FirebaseDatabase database;

    //CO2 value smoothed
    int smoothCO2 = 400;
    //Last CO2 measurement
    int lastCO2Measurement = 400;
    //smoothing coeficient
    double smoothing = 0.4;
    //timer for smoothing
    Timer smoothCO2Timer;

    String TAG = "Sensor Station";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w("TAG", "Start");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //TODO: Make this work
        //makePerfectCirclesUi();

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
                Log.d(TAG, "Data changed");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
        fbRef.addValueEventListener(listener);
    }

    @OnClick(R.id.list_btn)
    public void openListActivity(){
        Intent logListIntent = new Intent(MainActivity.this, HistoryListActivity.class);
        startActivity(logListIntent);
    }

    @OnClick(R.id.graph_btn)
    public void openGraphActivity(){
        Intent logListIntent = new Intent(MainActivity.this, GraphActivity.class);
        startActivity(logListIntent);
    }

    private void updateSensorDataUI(SensorStation sensors){
        //co2TV.setText(String.valueOf(lastCO2Measurement));
        tvocTV.setText(String.valueOf(sensors.getTVOC()));
        tempTV.setText(String.valueOf(String.format("%.1f",sensors.getTemperature())));
        hum2TV.setText(String.valueOf(String.format("%.1f",sensors.getHumidity())));
        pressureTV.setText(String.format("%.1f",sensors.getPressure()));
        temp2TV.setText(String.format("%.1f",sensors.getTemperature2()));
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
            Log.d(TAG, "Filtering");
            smoothCO2 += ((double)(lastCO2Measurement - smoothCO2)) * smoothing;
            co2TV.setText(String.valueOf(smoothCO2));
        }
    };

    @Override
    protected void onResume() {
        Log.d(TAG, "On resume");
        //myRef.addValueEventListener(listener);
        database.goOnline();
        startTimer();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "On pause");
        //myRef.removeEventListener(listener);
        database.goOffline();
        smoothCO2Timer.cancel();
        super.onPause();
    }
}