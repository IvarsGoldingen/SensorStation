package com.example.sensorstation;

import java.text.DecimalFormat;

public class DefaultAlarmWarningSettings {
    public static final float CO2_LA = 399;
    public static final float CO2_LW = 399;
    public static final float CO2_HW = 600;
    public static final float CO2_HA = 800;
    public static final float CO2_HYS = 20;
    public static final DecimalFormat CO2_FORMAT  = new DecimalFormat("0");

    public static final float TVOC_LA = -1;
    public static final float TVOC_LW = -1;
    public static final float TVOC_HW = 100;
    public static final float TVOC_HA = 200;
    public static final float TVOC_HYS = 10;
    public static final DecimalFormat TVOC_FORMAT  = new DecimalFormat("0");

    public static final float T1_LA = 18;
    public static final float T1_LW = 20;
    public static final float T1_HW = 24;
    public static final float T1_HA = 26;
    public static final float T1_HYS = 1;
    public static final DecimalFormat T1_FORMAT  = new DecimalFormat("0.0");

    public static final float RH_LA = 20;
    public static final float RH_LW = 30;
    public static final float RH_HW = 50;
    public static final float RH_HA = 60;
    public static final float RH_HYS = 2;
    public static final DecimalFormat RH_FORMAT  = new DecimalFormat("0.0");

    public static final float T2_LA = 18;
    public static final float T2_LW = 20;
    public static final float T2_HW = 24;
    public static final float T2_HA = 26;
    public static final float T2_HYS = 1;
    public static final DecimalFormat T2_FORMAT  = new DecimalFormat("0.0");

    public static final float PR_LA = 950;
    public static final float PR_LW = 990;
    public static final float PR_HW = 1020;
    public static final float PR_HA = 1050;
    public static final float PR_HYS = 1;
    public static final DecimalFormat PR_FORMAT  = new DecimalFormat("0.0");
}
