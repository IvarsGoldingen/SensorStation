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
import java.util.Timer;
import java.util.TimerTask;

public class TestWorker extends Worker {
    private static String TAG = "Worker Test";
    private final String NOTIFICATION_CHANNEL_ID = "MyNotificationChannel";
    //If the value received from firebase is older than this, do not display notification
    private static final long MAX_TIME_DIF_S = 60;
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

    //A timer that keeps track of how long the worker has been running and stops it
    //if a value from FB database cannot be retreived
    private Timer workerTimer = new Timer();
    private static final long MAX_WORKER_RUNTIME_MS = 60 * 1000;
    //Used to check whether an old value notification should be shown
    private long ageOfLastValue = 0;

    public TestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        Logger.writeDatedLog("***************************************", context);
        Logger.writeDatedLog("Worker constructor", context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.writeDatedLog("Worker doWork()", context);
        if (notificationsAllowed()){
            createTimer();
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
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            });
            fbRef.addValueEventListener(listener);
        }
        return Result.success();
    }

    //Create timer to allow a connection to the DB only for a certain time
    private void createTimer(){
        //timer test
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                //The timer passed and a recent value was not received
                finishWorker();
                //Check if a notification of a very old value should be shown.
                checkAgeOflastValue();
            }
        };
        workerTimer.schedule(task, MAX_WORKER_RUNTIME_MS);
    }

    private void finishWorker(){
        workerTimer.cancel();
        workerTimer.purge();
        database.goOffline();
    }

    private void checkAgeOflastValue(){
        //value is old
        if (ageOfLastValue > CRITICAL_VALUE_AGE_MS){
            Logger.writeDatedLog("Value is very old :D", context);
            int differenceInHours = (int)(ageOfLastValue/1000/60/60);
            //Notify of old values in database
            createNotification(
                    "Database not updated",
                    "Values is " + differenceInHours + "h old!",
                    "Old values notification",
                    "Notifies of old values in database"
            );
        }
    }

    //Check if notifications are allowed at all and if at this time
    private boolean notificationsAllowed(){
        //Get notification settings from prefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean notifEnabled = sharedPreferences.getBoolean("PREF_KEY_MAIN_NOTIFICATIONS_ENABLED", true);
        String endTime = sharedPreferences.getString("PREF_KEY_MAIN_STOP_NOTIFICATIONS", "22:00");
        String startTime = sharedPreferences.getString("PREF_KEY_MAIN_START_NOTIFICATIONS", "07:00");
        //Check if notification enabled at all
        if (!notifEnabled){
            //this should not happen, since the worker should be not turned on at all
            Logger.writeDatedLog("Notifications not allowed", context);
            return false;
        } else {
            Logger.writeDatedLog("Notifications allowed", context);
        }

        //Create int from String setting
        String [] stopTimeNumbers = endTime.split(":", 2);
        String [] startTimeNumbers = startTime.split(":", 2);
        int stopHours = -10;
        int stopMinutes = -10;
        int startHours = -10;
        int startMinutes = -10;
        try {
            stopHours = Integer.parseInt(stopTimeNumbers[0]);
            stopMinutes = Integer.parseInt(stopTimeNumbers[1]);
            startHours = Integer.parseInt(startTimeNumbers[0]);
            startMinutes = Integer.parseInt(startTimeNumbers[1]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Logger.writeDatedLog("Error creating time ints", context);
            //Value cannot be converted to numbers
            return false;
        }

        //Compare time setting with current time
        Calendar time = Calendar.getInstance();
        int currentHour = time.get(Calendar.HOUR_OF_DAY);
        int currentMinute = time.get(Calendar.MINUTE);
        Logger.writeDatedLog("Start hour: " + startHours + "Start minute" + startMinutes, context);
        Logger.writeDatedLog("Stop hour: " + stopHours + "Stop minute" + stopMinutes, context);
        Logger.writeDatedLog("Current time: " + currentHour + ":" + currentMinute, context);
        if (currentHour >= startHours &&
            currentHour <= stopHours){
            if (currentHour == startHours){
                if (currentMinute >= startMinutes){
                    return true;
                }
            } else if (currentHour == stopHours) {
                if (currentMinute < stopMinutes){
                    return true;
                }
            } else {
                return true;
            }
        }
        Logger.writeDatedLog("Out of time bounds", context);
        return false;
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
            //a recent value from FB DB has been gotten, finish the worker
            finishWorker();
        }
    }

    private boolean isValueRecent(long timeOfValue){
        Date currentDate = Calendar.getInstance().getTime();
        long currentDateEpoch = currentDate.getTime();
        //time difference in miliseconds
        ageOfLastValue = currentDateEpoch - timeOfValue;
        String timeLog = "Current time ms: " + currentDateEpoch + "\n" +
                "Value save time:" + timeOfValue + "\n" +
                "Difference: " + ageOfLastValue;
        Logger.writeDatedLog(timeLog, context);
        Log.d(TAG, "Worker Last update ms ago: " + ageOfLastValue);
        if (ageOfLastValue > MAX_CO2_VALUE_AGE){
            Logger.writeDatedLog("Value is old", context);
            return false;
        } else {
            Logger.writeDatedLog("Value is recent", context);
            return true;
        }
    }

    int getCo2HoghAlarmValue(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int co2 = Integer.parseInt(sharedPreferences.
                getString("CO2_PREFS" + "HLA", "1000"));
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
