package com.example.sensorstation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    /**
     * TODO: Crete worker only if it is enabled
     * TODO: Trouble shoot sometimes main not receiving new values
     * TODO: Remove signed in notification
     * TODO: Troubleshoot old values comming in from Worker
     * TODO: Sometimes the MC only uploads the time without values
     * TODO: Notification of high CO2
     * TODO: CO2 filter value and frequency setting
     * TODO: Work with withings app
     * TODO: Save averages in an SQL database. Avarage CO2 over night
     * TODO: On start make the value some color - problem with CO2 view
    */

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
    @BindView(R.id.view_CO2_frame)
    LinearLayout frameCO2;
    @BindView(R.id.view_TVOC_frame)
    LinearLayout frameTVOC;
    @BindView(R.id.view_T1_frame)
    LinearLayout frameT1;
    @BindView(R.id.view_T2_frame)
    LinearLayout frameT2;
    @BindView(R.id.view_RH_frame)
    LinearLayout frameRH;
    @BindView(R.id.view_PR_frame)
    LinearLayout framePR;

    private DatabaseReference fbRef;
    private ValueEventListener listener;
    private FirebaseDatabase database;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //Determines when the worker should execute
    private static final int WORKER_REPEAT_INTERVAL_M = 60;
    private static final int WORKER_FLEX_INTERVAL = 30;
    private static final String MY_WORKER_NAME = "myWorkerName";
    //If the value received from firebase is older than this, display it to the user
    private static final long MAX_TIME_DIF_S = 60;
    private static final long MAX_TIME_DIF_MS = MAX_TIME_DIF_S * 1000;
    private static final int COLOR_OLD = 0xFF656363;
    // Arbitrary request code value
    private static final int RC_SIGN_IN = 123;
    // The maximum amount of time allowed between receiving new values
    private static final long OLD_VALUE_TIME_MS = 10000;
    //TAG
    private static final String TAG = "Sensor Station MainActivity";

    //for notification
    private final String CHANNEL_ID = "MyChannel";

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

    //objects for displaying the values in the main activity
    MeasurementDisplay display_CO2;
    MeasurementDisplay display_TVOC;
    MeasurementDisplay display_T1;
    MeasurementDisplay display_T2;
    MeasurementDisplay display_RH;
    MeasurementDisplay display_PR;

    void createMeasurementDisplayObjects(){
        //objects for displaying the values in the main activity
        display_CO2 = new MeasurementDisplay(
                this,
                "CO2_PREFS",
                DefaultAlarmWarningSettings.CO2_LA,
                DefaultAlarmWarningSettings.CO2_LW,
                DefaultAlarmWarningSettings.CO2_HW,
                DefaultAlarmWarningSettings.CO2_HA,
                DefaultAlarmWarningSettings.CO2_HYS,
                DefaultAlarmWarningSettings.CO2_FORMAT);
        display_TVOC = new MeasurementDisplay(
                this,
                "TVOC_PREFS",
                DefaultAlarmWarningSettings.TVOC_LA,
                DefaultAlarmWarningSettings.TVOC_LW,
                DefaultAlarmWarningSettings.TVOC_HW,
                DefaultAlarmWarningSettings.TVOC_HA,
                DefaultAlarmWarningSettings.TVOC_HYS,
                DefaultAlarmWarningSettings.TVOC_FORMAT);
        display_T1 = new MeasurementDisplay(
                this,
                "T1_PREFS",
                DefaultAlarmWarningSettings.T1_LA,
                DefaultAlarmWarningSettings.T1_LW,
                DefaultAlarmWarningSettings.T1_HW,
                DefaultAlarmWarningSettings.T1_HA,
                DefaultAlarmWarningSettings.T1_HYS,
                DefaultAlarmWarningSettings.T1_FORMAT);
        display_T2 = new MeasurementDisplay(
                this,
                "T2_PREFS",
                DefaultAlarmWarningSettings.T2_LA,
                DefaultAlarmWarningSettings.T2_LW,
                DefaultAlarmWarningSettings.T2_HW,
                DefaultAlarmWarningSettings.T2_HA,
                DefaultAlarmWarningSettings.T2_HYS,
                DefaultAlarmWarningSettings.T2_FORMAT);
        display_RH = new MeasurementDisplay(
                this,
                "RH_PREFS",
                DefaultAlarmWarningSettings.RH_LA,
                DefaultAlarmWarningSettings.RH_LW,
                DefaultAlarmWarningSettings.RH_HW,
                DefaultAlarmWarningSettings.RH_HA,
                DefaultAlarmWarningSettings.RH_HYS,
                DefaultAlarmWarningSettings.RH_FORMAT);
        display_PR = new MeasurementDisplay(
                this,
                "PR_PREFS",
                DefaultAlarmWarningSettings.PR_LA,
                DefaultAlarmWarningSettings.PR_LW,
                DefaultAlarmWarningSettings.PR_HW,
                DefaultAlarmWarningSettings.PR_HA,
                DefaultAlarmWarningSettings.PR_HYS,
                DefaultAlarmWarningSettings.PR_FORMAT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Start");
        super.onCreate(savedInstanceState);

        Logger.writeDatedLog("Main", this);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        createMeasurementDisplayObjects();

        database = FirebaseDatabase.getInstance();
        database.goOnline();
        mFirebaseAuth = FirebaseAuth.getInstance();

        //Start a timer which will display that no new values have been added for some time
        startOldValueTimer();

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

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null){
                    //user signed in
                    View sv = findViewById(R.id.main_root);
                    if (firstValue){
                        //so the message does not apear every time resuming the app
                        Snackbar.make(sv, "Signed in!", Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    //user not signed in
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.AnonymousBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
        View.OnClickListener sensorItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                createAlertDialogForReading(id);
            }
        };

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

        frameCO2.setOnClickListener(sensorItemClickListener);
        frameTVOC.setOnClickListener(sensorItemClickListener);
        frameT1.setOnClickListener(sensorItemClickListener);
        frameT2.setOnClickListener(sensorItemClickListener);
        frameRH.setOnClickListener(sensorItemClickListener);
        framePR.setOnClickListener(sensorItemClickListener);
    }




    //A worker that will create a notification if CO2 value is too high
    void createCo2CheckWorker(){
        Log.d(TAG, "Starting worker");
        PeriodicWorkRequest testPeriodicWorker =
                new PeriodicWorkRequest.Builder(TestWorker.class,
                        WORKER_REPEAT_INTERVAL_M, TimeUnit.MINUTES,
                        WORKER_FLEX_INTERVAL, TimeUnit.MINUTES)
                        .build();

        WorkManager workManager = WorkManager.getInstance(this);
        workManager
                .enqueueUniquePeriodicWork(
                        MY_WORKER_NAME,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        testPeriodicWorker);
    }

    void stopWorker(){
        Log.d(TAG, "Stopping worker");
        WorkManager
                .getInstance(this)
                .cancelUniqueWork(MY_WORKER_NAME);
    }

    void createNotification(){
        createNotificationChannel();
        //Create an intent that will open this app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_eco_48)
                .setContentTitle("Test notification")
                .setContentText("Test notification text")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notManager = NotificationManagerCompat.from(this);
        notManager.notify(5, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MyChannelName";
            String description = "MyChannelDescription";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    //Create alert dialog for filling HLA, HL, LL, LLA,HYSt values
    private void createAlertDialogForReading(int clickedReadingId){
        //get the layout for the AlertDialog
        final View view = getLayoutInflater().inflate(R.layout.sensor_setup_layout, null);
        MeasurementDisplay clickedMeasurement = findAccordingSensorDisplay(clickedReadingId);
        fillInAlertDialogWithValues(view,clickedMeasurement);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle("Test");
        alertBuilder.setView(view);
        alertBuilder.setPositiveButton("SAVE",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean settingsCorrect = saveAlarmWarningSettings(clickedReadingId, view);
                        View sv = findViewById(R.id.main_root);
                        if (settingsCorrect){
                            Snackbar.make(sv, "Values saved successfully", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(sv, "Error saving values", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
        AlertDialog dialog = alertBuilder.create();
        dialog.show();

        //SET THE SAVE BUTTON IN THE CENTER
        final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)positiveButton.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        positiveButton.setLayoutParams(params);
    }

    private boolean saveAlarmWarningSettings (int clickedReadingId, View dialogView){
        boolean result = false;
        switch (clickedReadingId) {
            case R.id.view_CO2_frame:
                result = display_CO2.setValuesFromView(dialogView);
                break;
            case R.id.view_TVOC_frame:
                result = display_TVOC.setValuesFromView(dialogView);
                break;
            case R.id.view_T1_frame:
                result = display_T1.setValuesFromView(dialogView);
                break;
            case R.id.view_T2_frame:
                result = display_T2.setValuesFromView(dialogView);
                break;
            case R.id.view_RH_frame:
                result = display_RH.setValuesFromView(dialogView);
                break;
            case R.id.view_PR_frame:
                result = display_PR.setValuesFromView(dialogView);
                break;
            default:
                break;
        }
        return result;
    }

    //Find the MeasurementDisplay object that was clicked on
    private MeasurementDisplay findAccordingSensorDisplay(int clickedReadingId) {
        MeasurementDisplay selectedSensor = null;
        switch (clickedReadingId) {
            case R.id.view_CO2_frame:
                selectedSensor = display_CO2;
                break;
            case R.id.view_TVOC_frame:
                selectedSensor = display_TVOC;
                break;
            case R.id.view_T1_frame:
                selectedSensor = display_T1;
                break;
            case R.id.view_T2_frame:
                selectedSensor = display_T2;
                break;
            case R.id.view_RH_frame:
                selectedSensor = display_RH;
                break;
            case R.id.view_PR_frame:
                selectedSensor = display_PR;
                break;
            default:
                selectedSensor = new MeasurementDisplay();
                break;
        }
        return selectedSensor;
    }

    //Fill in the alert dialog with values from the clicked measurement
    void fillInAlertDialogWithValues(View dialogView, MeasurementDisplay clickedMeasurement){
        EditText hlaEditText = dialogView.findViewById(R.id.sensor_setup_HLA);
        EditText hlEditText = dialogView.findViewById(R.id.sensor_setup_HLW);
        EditText llEditText = dialogView.findViewById(R.id.sensor_setup_LLW);
        EditText llaEditText = dialogView.findViewById(R.id.sensor_setup_LLA);
        EditText hystEditText = dialogView.findViewById(R.id.sensor_setup_HYST);

        hlaEditText.setText(String.valueOf(clickedMeasurement.getAlarmHighValue()));
        hlEditText.setText(String.valueOf(clickedMeasurement.getWarningHighValue()));
        llEditText.setText(String.valueOf(clickedMeasurement.getWarningLowValue()));
        llaEditText.setText(String.valueOf(clickedMeasurement.getAlarmLowValue()));
        hystEditText.setText(String.valueOf(clickedMeasurement.getHysteresis()));
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
            display_CO2.setValid(sensors.isSGP_valid());
            //Update CO2 value by filtering using a timer
            startCO2FilterTimer();
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
        //New value has come in, restart the timer looking for old values
        restartOldValueTimer();
        //Set valid
        display_CO2.setValid(sensors.isSGP_valid());
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
        Log.d(TAG, "Last update ms ago: " + differenceMS);
        if (differenceMS > MAX_TIME_DIF_MS){
            View sv = findViewById(R.id.main_root);
            Snackbar.make(sv, "Old values", Snackbar.LENGTH_SHORT).show();
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
        this.runOnUiThread(FilterCO2Runnable);
    }

    //Timer that smooths out the CO2 measurement
    private void startCO2FilterTimer(){
        if (smoothCO2Timer == null){
            Log.d(TAG, "Starting CO2 filter timer");
            smoothCO2Timer = new Timer();
            smoothCO2Timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timerMethod();
                }
            },1000,1000);
        } else {
            Log.d(TAG, "Timer is not null");
        }
    }

    //A runnable that the timer can run on the UI thread
    private final Runnable FilterCO2Runnable = new Runnable() {
        @Override
        public void run() {
            display_CO2.setValue((display_CO2.getValue() +
                    ((lastCO2Measurement - display_CO2.getValue()) * smoothing)));
            display_CO2.updateView(co2TV);
        }
    };

    private void setAllDisplaysOld(boolean isOld){
        display_CO2.setOld(isOld);
        display_TVOC.setOld(isOld);
        display_T1.setOld(isOld);
        display_T2.setOld(isOld);
        display_RH.setOld(isOld);
        display_PR.setOld(isOld);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "On resume");
        database.goOnline();
        firstValue = true;
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        //When the app is open do not use the worker
        stopWorker();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "On pause");
        database.goOffline();
        if (smoothCO2Timer != null){
            Log.d(TAG, "Cancel and purge");
            smoothCO2Timer.cancel();
            smoothCO2Timer.purge();
            smoothCO2Timer = null;
        } else {
            Log.d(TAG, "No timer to cancel");
        }
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        timerHandler.removeCallbacks(runnableOldValueTimer);
        //Make a background CO2 checker when leaving the app
        createCo2CheckWorker();
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
            case R.id.menu_main_settings:
                Intent startSettingActivity = new Intent(this,MainSettingsActivity.class);
                startActivity(startSettingActivity);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    //Timer for checking if new values have stoppped comming in
    Handler timerHandler = new Handler();
    Runnable runnableOldValueTimer = new Runnable() {
        @Override
        public void run() {
            //user signed in
            View sv = findViewById(R.id.main_root);
            Snackbar.make(sv, "No new values comming in", Snackbar.LENGTH_SHORT).show();
            //setup all display fields to show up as old
            setAllDisplaysOld(true);
            oldValues = true;
        }
    };

    private void startOldValueTimer(){
        timerHandler.postDelayed(runnableOldValueTimer, OLD_VALUE_TIME_MS);
    }

    private void restartOldValueTimer(){
        timerHandler.removeCallbacks(runnableOldValueTimer);
        timerHandler.postDelayed(runnableOldValueTimer, OLD_VALUE_TIME_MS);
    }
}