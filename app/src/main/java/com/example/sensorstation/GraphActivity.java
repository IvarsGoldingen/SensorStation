package com.example.sensorstation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GraphActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    //TODO:Create y axis min max setups
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
    protected void onDestroy() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);;
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);
        ButterKnife.bind(this);
        readItemToShowFromPrefs();
        //LIsten for changes in preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
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
                if(Helper.isLogValueOld(logData.getTime())){
                    //if value is old, delete the data
                    Log.d(TAG, "Value is old, delete");
                    snapshot.getRef().removeValue();
                } else {
                    //if the value is not old, add the data to the arrayList
                    Log.d(TAG, "Value is new, adding to array List");
                    mLogArrayList.add(logData);
                    updateGraph();
                }
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

    void readItemToShowFromPrefs(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        currentValueDisplay = sharedPreferences.getInt("PREF_KEY_GR_VALUE_TO_DISPLAY", GR_CO2);
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
        //TODO:Debug - sometimes failed and showed that the value to add was older than the previous one
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

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        //Set the initial window to 6 hours
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(6*3600*1000);
        //For CO2 these are the normal values
        setUpGraphForValue();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_graph_activity, menu);
        //Called, because not sure if on create will finish before this
        readItemToShowFromPrefs();
        //Set the check in the correct item
        switch (currentValueDisplay){
            case GR_CO2:
                menu.findItem(R.id.menu_itm_CO2).setChecked(true);
                //menu.findItem(R.id.menu_graph_selection).getSubMenu().getItem(R.id.menu_itm_CO2).setChecked(true);
                break;
            case GR_T1:
                menu.findItem(R.id.menu_itm_t1).setChecked(true);
                break;
            case GR_T2:
                menu.findItem(R.id.menu_itm_t2).setChecked(true);
                break;
            case GR_RH:
                menu.findItem(R.id.menu_itm_RH).setChecked(true);
                break;
            case GR_TVOC:
                menu.findItem(R.id.menu_itm_TVOC).setChecked(true);
                break;
            case GR_PR:
                menu.findItem(R.id.menu_itm_Pressure).setChecked(true);
                break;
            default:
                Log.d(TAG, "Fault updating graph");
                break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_itm_CO2:
                item.setChecked(true);
                currentValueDisplay = GR_CO2;
                getSupportActionBar().setTitle("CO2");
                fillUpGraphData();
                break;
            case R.id.menu_itm_t1:
                item.setChecked(true);
                currentValueDisplay = GR_T1;
                getSupportActionBar().setTitle("Temperature 1");
                fillUpGraphData();
                break;
            case R.id.menu_itm_t2:
                item.setChecked(true);
                currentValueDisplay = GR_T2;
                getSupportActionBar().setTitle("Temperature 2");
                fillUpGraphData();
                break;
            case R.id.menu_itm_RH:
                item.setChecked(true);
                currentValueDisplay = GR_RH;
                getSupportActionBar().setTitle("Humidity");
                fillUpGraphData();
                break;
            case R.id.menu_itm_TVOC:
                currentValueDisplay = GR_TVOC;
                item.setChecked(true);
                getSupportActionBar().setTitle("TVOC");
                fillUpGraphData();
                break;
            case R.id.menu_itm_Pressure:
                currentValueDisplay = GR_PR;
                item.setChecked(true);
                getSupportActionBar().setTitle("Pressure");
                fillUpGraphData();
                break;
            case R.id.menu_graph_settings:
                Intent startSettinsActivity = new Intent (this, GraphSettingsActivity.class);
                startActivity(startSettinsActivity);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("PREF_KEY_GR_VALUE_TO_DISPLAY", currentValueDisplay);
        editor.apply();

        return true;
    }

    void fillUpGraphData(){
        int numberOfItems = mLogArrayList.size();
        DataPoint[] newData = new DataPoint[numberOfItems];
        switch (currentValueDisplay){
            case GR_CO2:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getCO2());
                }
                break;
            case GR_T1:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getTemperature());
                }
                break;
            case GR_T2:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getTemperature2());
                }
                break;
            case GR_RH:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getHumidity());
                }
                break;
            case GR_TVOC:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getTVOC());
                }
                break;
            case GR_PR:
                for (int i = 0; i < numberOfItems; i++){
                    newData[i] = new DataPoint(mLogArrayList.get(i).getTime(), mLogArrayList.get(i).getPressure());
                }
                break;
            default:
                Log.d(TAG, "Fault updating graph");
                break;
        }
        series.resetData(newData);
        setUpGraphForValue();
    }
    /*
     * Setsup the graph so that the currently drawn graph is with correct values
     * */
    private void setUpGraphForValue(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        switch (currentValueDisplay) {
            case GR_CO2:
                modifyGraphUI(
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_CO2_Y_MIN", "400")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_CO2_Y_MAX", "1000")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_CO2_VERT_LABELS", "13")),
                                "CO2");
                break;
            case GR_T1:
                modifyGraphUI(
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T1_Y_MIN", "0")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T1_Y_MAX", "40")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T1_VERT_LABELS", "9")),
                        "Temperature 1");
                break;
            case GR_T2:
                modifyGraphUI(
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T2_Y_MIN", "0")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T2_Y_MAX", "40")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_T2_VERT_LABELS", "9")),
                        "Temperature 2");
                break;
            case GR_RH:
                modifyGraphUI(
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_RH_Y_MIN", "0")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_RH_Y_MAX", "100")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_RH_VERT_LABELS", "11")),
                        "Humidity");
                break;
            case GR_TVOC:
                modifyGraphUI(
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_TVOC_Y_MIN", "0")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_TVOC_Y_MAX", "1000")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_TVOC_VERT_LABELS", "11")),
                        "TVOC");
                break;
            case GR_PR:
                modifyGraphUI(
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_PR_Y_MIN", "980")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_PR_Y_MAX", "1030")),
                        Integer.parseInt(sharedPreferences.getString("PREF_KEY_GR_PR_VERT_LABELS", "6")),
                        "Pressure");
                break;
            default:
                Log.d(TAG, "UNKNOWN GRAPH");
                break;
        }
    }

    private void modifyGraphUI(
            int minY,
            int maxY,
            int verticalLabels,
            String title){
        graph.getGridLabelRenderer().setNumVerticalLabels(verticalLabels);
        graph.getViewport().setMinY(minY);
        graph.getViewport().setMaxY(maxY);
        getSupportActionBar().setTitle(title);
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

    //Listen for changes in the Graph settings
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "Prefs changed");
        //TODO: execute only if the changed setting corresponds to the current graph
        fillUpGraphData();
    }
}
