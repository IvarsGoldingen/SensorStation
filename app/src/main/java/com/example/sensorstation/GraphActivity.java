package com.example.sensorstation;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GraphActivity extends AppCompatActivity {

    //TODO:Create y axis min max setups
    //TODO:Get all the values from FB and allow the user to switch between different measurements
    //TODO:Delete values from FB that are older than a week

    String TAG = "SensorStationGraph";
    @BindView(R.id.graph)
    GraphView graph;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mLgDBRef;
    private ChildEventListener mChildEventListener;
    private ArrayList<LogItem> mLogArrayList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);
        ButterKnife.bind(this);
        //Series that will be used to show on the graph
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
        setupGraph();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mLgDBRef = mFirebaseDatabase.getReference().child("Log");
        //Lsten for changes in the DB
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //snapshot - contains the added log
                LogItem logData = snapshot.getValue(LogItem.class);
                series.appendData(new DataPoint(logData.getTime(), logData.getCO2()),true,100);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildChanged");
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onChildRemoved");
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildMoved");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("TAG", "onCancelled");
            }
        };
        graph.addSeries(series);
        //Add the listener to the db
        mLgDBRef.addChildEventListener(mChildEventListener);
    }

    void setupGraph(){
        // activate horizontal zooming and scrolling
        graph.getViewport().setScalable(true);
        // activate horizontal scrolling
        graph.getViewport().setScrollable(true);
        //Formatter to display day of week and time in 24h format
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX){
                    //Format the time to date and hour in 24h fromat
                    Date date = new Date ((long)value);
                    //To get parts of the complete date
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    String weekday = Helper.getDayNameString(cal.get(Calendar.DAY_OF_WEEK));
                    String hour = String.format("%02d",cal.get(Calendar.HOUR_OF_DAY));
                    String minute = String.format("%02d",cal.get(Calendar.MINUTE));
                    return weekday + "\n" + hour + ":" + minute;
                } else {
                    //Y axis is the measured value. No need to format that
                    return super.formatLabel(value, isValueX);
                }
            }
        });
        graph.getGridLabelRenderer().setNumHorizontalLabels(7);
        graph.getGridLabelRenderer().setNumVerticalLabels(13);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        //Set the initial window to 6 hours
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(6*3600*1000);
        //For CO2 these are the normal values
        graph.getViewport().setMinY(400);
        graph.getViewport().setMaxY(1000);
    }

    @Override
    protected void onPause() {
        mFirebaseDatabase.goOffline();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mFirebaseDatabase.goOnline();
        super.onResume();
    }
}
