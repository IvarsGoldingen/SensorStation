package com.example.sensorstation;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

//TODO: timer to go offline from the database if no data received for some time

public class TestWorker extends Worker {
    private static String TAG = "Worker Test";
    private final String NOTIFICATION_CHANNEL_ID = "MyNotificationChannel";

    private DatabaseReference fbRef;
    private ValueEventListener listener;
    private FirebaseDatabase database;
    private Context context;

    public TestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        Log.d(TAG, "Worker INITIALIZED");
    }

    @NonNull
    @Override
    public Result doWork() {
        //Toast.makeText(context, "Worker text", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Worker doing stuff");
        database = FirebaseDatabase.getInstance();
        fbRef = database.getReference().child("SensorStation");
        listener = (new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Worker data changed in FB");
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                SensorStation sensors = dataSnapshot.getValue(SensorStation.class);
                int co2 = sensors.getCO2();
                if (co2 > 1000){
                    //Show a notification if the CO2 value too high
                    createNotification(sensors.getCO2());
                }
                //Remove connections to FB DB
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

    void createNotification(int co2Value){
        createNotificationChannel();

        //Create an intent that will open this app
        Intent intent = new Intent(context, TestWorker.class);
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
            CharSequence name = "MyChannelName";
            String description = "MyChannelDescription";
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
