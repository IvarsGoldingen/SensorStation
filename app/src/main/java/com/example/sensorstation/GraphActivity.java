package com.example.sensorstation;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
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
    //TODO:Delete values from FB that are older than a week
    //TODO:Display message if no data to show

    String TAG = "SensorStationGraph";
    @BindView(R.id.graph)
    GraphView graph;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mLgDBRef;
    private ChildEventListener mChildEventListener;
    private ArrayList<LogItem> mLogArrayList;

    //Series that will be used to show on the graph
    LineGraphSeries<DataPoint> series;
    //keeps track of the currently displaying value in the graph
    private int currentValueDisplay = GR_CO2;
    private static final int GR_CO2 = 1;
    private static final int GR_T1 = 2;
    private static final int GR_T2 = 3;
    private static final int GR_RH = 4;
    private static final int GR_TVOC = 5;
    private static final int GR_PR = 6;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);
        ButterKnife.bind(this);
        //Series that will be used to show on the graph
        series = new LineGraphSeries<DataPoint>();
        mLogArrayList = new ArrayList<>();
        setupGraph();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mLgDBRef = mFirebaseDatabase.getReference().child("Log");
        //Lsten for changes in the DB
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //snapshot - contains the added log
                LogItem logData = snapshot.getValue(LogItem.class);
                mLogArrayList.add(logData);
                updateGraph();
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

    //updates the graph when a new value is added
    void updateGraph(){
        //Get the last
        int numberOfItems = mLogArrayList.size();
        LogItem lastItem = mLogArrayList.get(numberOfItems - 1);
        DataPoint pointToAdd;
        switch (currentValueDisplay){
            case GR_CO2:
                pointToAdd = new DataPoint(lastItem.getTime(), lastItem.getCO2());
                break;
            case GR_T1:
                pointToAdd = new DataPoint(lastItem.getTime(), lastItem.getTemperature());
                break;
            case GR_T2:
                pointToAdd = new DataPoint(lastItem.getTime(), lastItem.getTemperature2());
                break;
            case GR_RH:
                pointToAdd = new DataPoint(lastItem.getTime(), lastItem.getHumidity());
                break;
            case GR_TVOC:
                pointToAdd = new DataPoint(lastItem.getTime(), lastItem.getTVOC());
                break;
            case GR_PR:
                pointToAdd = new DataPoint(lastItem.getTime(), lastItem.getPressure());
                break;
            default:
                Log.d(TAG, "Fault updating graph");
                pointToAdd = new DataPoint(lastItem.getTime(), lastItem.getCO2());
                break;
        }
        series.appendData(pointToAdd,true,200);
        //TODO:once failed and showed that the value to add was older than the previous one
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_graph_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        int numberOfItems = mLogArrayList.size();
        DataPoint[] newData = new DataPoint[numberOfItems];
        //Depending on which data is selected for showing in the Graph
        //Set Y min max and fill the data with the object from firebase
        switch (item.getItemId()) {
            case R.id.menu_itm_CO2:
                item.setChecked(true);
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getCO2());
                }
                graph.getGridLabelRenderer().setNumVerticalLabels(13);
                graph.getViewport().setMinY(400);
                graph.getViewport().setMaxY(1000);
                break;
            case R.id.menu_itm_t1:
                item.setChecked(true);
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getTemperature());
                }
                graph.getGridLabelRenderer().setNumVerticalLabels(9);
                graph.getViewport().setMinY(0);
                graph.getViewport().setMaxY(40);
                break;
            case R.id.menu_itm_t2:
                item.setChecked(true);
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getTemperature2());
                }
                graph.getGridLabelRenderer().setNumVerticalLabels(9);
                graph.getViewport().setMinY(0);
                graph.getViewport().setMaxY(40);
                break;
            case R.id.menu_itm_RH:
                item.setChecked(true);
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getHumidity());
                }
                graph.getGridLabelRenderer().setNumVerticalLabels(11);
                graph.getViewport().setMinY(0);
                graph.getViewport().setMaxY(100);
                break;
            case R.id.menu_itm_TVOC:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getTVOC());
                }
                graph.getGridLabelRenderer().setNumVerticalLabels(11);
                graph.getViewport().setMinY(0);
                graph.getViewport().setMaxY(1000);
                item.setChecked(true);
                break;
            case R.id.menu_itm_Pressure:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getPressure());
                }
                graph.getGridLabelRenderer().setNumVerticalLabels(6);
                graph.getViewport().setMinY(980);
                graph.getViewport().setMaxY(1030);
                item.setChecked(true);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        series.resetData(newData);
        return true;
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
