package com.example.sensorstation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class HistoryListActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mLgDBRef;
    private ChildEventListener mChildEventListener;
    private ArrayList<LogItem> mLogArrayList;

    String TAG = "SensorStationLogList";

    private LogItemAdapter mAdapter;
    private RecyclerView mLogList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        mLogArrayList = new ArrayList<LogItem>();
        mLogList = (RecyclerView) findViewById(R.id.rv_log_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mLogList.setLayoutManager(layoutManager);
        mLogList.setHasFixedSize(true);
        mAdapter = new LogItemAdapter(mLogArrayList);
        mLogList.setAdapter(mAdapter);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mLgDBRef = mFirebaseDatabase.getReference().child("Log");

        //Lsten for changes in the DB
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //snapshot - contains the added log
                LogItem logData = snapshot.getValue(LogItem.class);
                mLogArrayList.add(logData);
                mAdapter.notifyDataSetChanged();
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
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Paused");
        mFirebaseDatabase.goOffline();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Resumed");
        mFirebaseDatabase.goOnline();
        super.onResume();
    }
}