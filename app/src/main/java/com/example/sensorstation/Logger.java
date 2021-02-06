package com.example.sensorstation;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

//Write log data to a local file
public class Logger {
    private static final String TAG = "LOGGER";

    //Returns true on success
    static public boolean writeDatedLog(String logContent, Context context){
        //Create a filename
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy_MM_dd");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
        Date now = new Date();
        String timeNow = timeFormatter.format(now);
        String fileName = "Log_" + dateFormatter.format(now) + ".txt";
        try
        {
            //Get app directory
            File root = context.getExternalFilesDir("Logs");
            if (!root.exists())
            {
                //If the root does not already exist create it
                root.mkdirs();
            }
            //Create or append to the file
            File logFile = new File(root, fileName);
            FileWriter writer = new FileWriter(logFile,true);
            logContent = timeNow + ": " +  logContent + "\n";
            writer.append(logContent);
            writer.flush();
            writer.close();
        }
        catch(IOException e)
        {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
