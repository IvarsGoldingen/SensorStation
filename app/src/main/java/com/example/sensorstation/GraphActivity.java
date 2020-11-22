package com.example.sensorstation;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GraphActivity extends AppCompatActivity {

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
                //Date date =
                series.appendData(new DataPoint(logData.getTime(), logData.getCO2()),true,100);
                series.notify();
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
        //Add the listener to the db
        mLgDBRef.addChildEventListener(mChildEventListener);

        graph.addSeries(series);

    }

    void setupGraph(){
        // activate horizontal zooming and scrolling
        graph.getViewport().setScalable(true);
        // activate horizontal scrolling
        graph.getViewport().setScrollable(true);
        // activate horizontal and vertical zooming and scrolling
        graph.getViewport().setScalableY(true);
        // activate vertical scrolling
        graph.getViewport().setScrollableY(true);
        //Disabled to have readable dates
        graph.getGridLabelRenderer().setHumanRounding(false);
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(GraphActivity.this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space
        graph.getGridLabelRenderer().setNumVerticalLabels(10); // only 4 because of the space
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
