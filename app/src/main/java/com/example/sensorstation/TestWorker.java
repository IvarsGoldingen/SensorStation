package com.example.sensorstation;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Date;

public class TestWorker extends Worker {
    private static String TAG = "Worker Test";
    private final String NOTIFICATION_CHANNEL_ID = "MyNotificationChannel";
    //If the value received from firebase is older than this, do not display notification
    private static final long MAX_TIME_DIF_S = 6000;
    private static final long MAX_CO2_VALUE_AGE = MAX_TIME_DIF_S * 1000;
    //Old values in DB notification will be shown if value received from FB is this old
    private static final long CRITICAL_VALUE_AGE_H = 6;
    private static final long CRITICAL_VALUE_AGE_M = CRITICAL_VALUE_AGE_H * 60;
    private static final long CRITICAL_VALUE_AGE_S = CRITICAL_VALUE_AGE_M * 60;
    private static final long CRITICAL_VALUE_AGE_MS = CRITICAL_VALUE_AGE_S * 1000;



    private DatabaseReference fbRef;
    private ValueEventListener listener;
    private FirebaseDatabase database;
    private Context context;

    public TestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        Logger.writeDatedLog("***************************************", context);
        Logger.writeDatedLog("Worker constructor", context);
        Log.d(TAG, "Worker INITIALIZED");
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeDatedLog("Worker doWork()", context);
        Log.d(TAG, "Worker doing stuff");
        database = FirebaseDatabase.getInstance();
        database.goOnline();
        fbRef = database.getReference().child("SensorStation");
        listener = (new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Logger.writeDatedLog("onDataChange", context);
                Log.d(TAG, "Worker data changed in FB");
                SensorStation sensorStation = dataSnapshot.getValue(SensorStation.class);
                analyzeData(sensorStation);

                database.goOffline();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
        fbRef.addListenerForSingleValueEvent(listener);
        return Result.success();
    }

    //Determines if and what notification should be shown to the user
    void analyzeData(SensorStation sensors){
        if (isValueRecent(sensors.getLastUpdate())){
            int co2 = sensors.getCO2();
            Log.d(TAG, "Worker CO2 is " + co2);
            Log.d(TAG, "CO2 HLA is " + getCo2HoghAlarmValue());
            if (co2 >= getCo2HoghAlarmValue()){
                Logger.writeDatedLog("CO2 is high: " + co2, context);
                Log.d(TAG, "Worker CO2 value is too HIGH");
                //Show a notification if the CO2 value too high
                createNotification(
                        "High CO2 value",
                        co2 + " PPM",
                        "CO2 high notification",
                        "Notifies of high CO2 level"
                );
            } else {
                Logger.writeDatedLog("CO2 is good: " + co2, context);
                Log.d(TAG, "Worker CO2 value is OK");
            }
        }
    }

    private boolean isValueRecent(long timeOfValue){
        Date currentDate = Calendar.getInstance().getTime();
        long currentDateEpoch = currentDate.getTime();
        //time difference in miliseconds
        long differenceMS = currentDateEpoch - timeOfValue;
        String timeLog = "Current time ms: " + currentDateEpoch + "\n" +
                "Value save time:" + timeOfValue + "\n" +
                "Difference: " + differenceMS;
        Logger.writeDatedLog(timeLog, context);
        Log.d(TAG, "Worker Last update ms ago: " + differenceMS);
        if (differenceMS > MAX_CO2_VALUE_AGE){
            Logger.writeDatedLog("Value is old", context);
            //value is old
            if (differenceMS > CRITICAL_VALUE_AGE_MS){
                Logger.writeDatedLog("Value is very old :D", context);
                int differenceInHours = (int)(differenceMS/1000/60/60);
                //Notify of old values in database
                createNotification(
                        "Database not updated",
                        "Values is " + differenceInHours + "h old!",
                        "Old values notification",
                        "Notifies of old values in database"
                );
            }
            return false;
        } else {
            Logger.writeDatedLog("Value is recent", context);
            return true;
        }
    }

    int getCo2HoghAlarmValue(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int co2 = Integer.parseInt(sharedPreferences.
                getString("PREF_KEY_GR_CO2_Y_MAX", "1000"));
        return co2;
    }

    void createNotification(String contentTitle, String contentText,
        String notificationName, String notificationDescription){
        createNotificationChannel(notificationName, notificationDescription);
        //Create an intent that will open this app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_eco_48)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notManager = NotificationManagerCompat.from(context);
        notManager.notify(5, notificationBuilder.build());
    }

    private void createNotificationChannel(String notificationName, String notificationDescription) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = notificationName;
            String description = notificationDescription;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    void createNotification(int co2Value){
        createNotificationChannel();
        //Create an intent that will open this app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent,0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_eco_48)
                .setContentTitle("High CO2 value")
                .setContentText(co2Value + " PPM")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notManager = NotificationManagerCompat.from(context);
        notManager.notify(5, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "CO2 high notification";
            String description = "Notifies of high CO2 level";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onStopped() {
        Log.d(TAG, "Worker stopped");
        super.onStopped();
    }
}
