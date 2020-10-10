package com.example.sensorstation;

public class LogItem {

    private static final int NO_DATA_PROVIDED = -1;
    private int CO2 = NO_DATA_PROVIDED;
    private double Humidity = NO_DATA_PROVIDED;
    private int TVOC = NO_DATA_PROVIDED;
    private double Temperature = NO_DATA_PROVIDED;
    long Time = NO_DATA_PROVIDED;

    //empty constructor for FireBase
    public LogItem(){

    }

    public LogItem(
            double humidity,
            double temperature,
            int CO2,
            int TVOC,
            long Time){
        this.Humidity = humidity;
        this.Temperature = temperature;
        this.CO2 = CO2;
        this.TVOC = TVOC;
        this.Time = Time;
    }

    public int getCO2() {
        return CO2;
    }

    public void setCO2(int CO2) {
        this.CO2 = CO2;
    }

    public double getHumidity() {
        return Humidity;
    }

    public void setHumidity(double humidity) {
        Humidity = humidity;
    }

    public int getTVOC() {
        return TVOC;
    }

    public void setTVOC(int TVOC) {
        this.TVOC = TVOC;
    }

    public double getTemperature() {
        return Temperature;
    }

    public void setTemperature(double temperature) {
        Temperature = temperature;
    }

    public long getTime() {
        return Time;
    }

    public void setTime(long time) {
        Time = time;
    }
}
