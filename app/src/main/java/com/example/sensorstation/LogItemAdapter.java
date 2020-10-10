package com.example.sensorstation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

import static java.text.DateFormat.getDateTimeInstance;

public class LogItemAdapter extends RecyclerView.Adapter<LogItemAdapter.LogItemViewHolder> {
    String TAG = "Sensor Station Adapter";
    private int mNrOfLogs;
    private ArrayList<LogItem> mLogItemArrayList;

    public LogItemAdapter(ArrayList<LogItem> logList){
        mLogItemArrayList = logList;
    }

    @NonNull
    @Override
    public LogItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.log_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachParrentImidiately = false;
        View view = inflater.inflate(layoutIdForListItem, parent,shouldAttachParrentImidiately);
        LogItemViewHolder viewHolder = new LogItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull LogItemViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        if (mLogItemArrayList != null){
            return mLogItemArrayList.size();
        }
        return 0;
    }
//    public LogItemAdapter (View itemView){
//        super(itemView);
//        tvDate = (TextView) itemView.findViewById(R.d)
//    }

    class LogItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.listDate)
        TextView tvDate;
        @BindView(R.id.listCO2)
        TextView tvCO2;
        @BindView(R.id.listHum)
        TextView tvHu;
        @BindView(R.id.listTemp)
        TextView tvTemp;
        @BindView(R.id.listTVOC)
        TextView tvTvoc;

        public LogItemViewHolder(View itemVew){
            super(itemVew);
            ButterKnife.bind(this, itemVew);
        }

        void bind (int listIndex){
            LogItem logItem = mLogItemArrayList.get(listIndex);
            int co2 = logItem.getCO2();
            int TVOC = logItem.getTVOC();
            double hum = logItem.getHumidity();
            double temp = logItem.getTemperature();
            long timeStamp = logItem.getTime();
            String timeSt = getTimeDate(timeStamp);
            Log.d(TAG, timeSt);
            //int time = logItem.get
            tvDate.setText(timeSt);
            tvCO2.setText(String.valueOf(co2));
            tvHu.setText(String.valueOf(hum));
            tvTemp.setText(String.valueOf(temp));
            tvTvoc.setText(String.valueOf(TVOC));
        }

        public String getTimeDate(long timestamp){
            try{
                DateFormat dateFormat = getDateTimeInstance();
                Date netDate = (new Date(timestamp));
                return dateFormat.format(netDate);
            } catch(Exception e) {
                return "date";
            }
        }
    }
}
