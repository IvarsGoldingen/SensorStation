package com.example.sensorstation;

public class SensorStation {
    private static final int NO_DATA_PROVIDED = -1;
    private int CO2 = NO_DATA_PROVIDED;
    private double Humidity = NO_DATA_PROVIDED;
    private int TVOC = NO_DATA_PROVIDED;
    private double Temperature = NO_DATA_PROVIDED;
    private double temperature2 = NO_DATA_PROVIDED;
    private double pressure = NO_DATA_PROVIDED;
    private boolean DHT_valid = false;
    private boolean SGP_valid = false;

    //empty constructor for FireBase
    public SensorStation(){

    }

    public SensorStation(
            double humidity,
            double temperature,
            int CO2,
            int TVOC,
            boolean DHT_valid,
            boolean SGP_valid,
            double temperature2,
            double pressure){
        this.Humidity = humidity;
        this.Temperature = temperature;
        this.CO2 = CO2;
        this.TVOC = TVOC;
        this.DHT_valid = DHT_valid;
        this.SGP_valid = SGP_valid;
        this.temperature2 = temperature2;
        this.pressure = pressure;
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
        this.Humidity = humidity;
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
        this.Temperature = temperature;
    }

    public boolean isDHT_valid() {
        return DHT_valid;
    }

    public void setDHT_valid(boolean DHT_valid) {
        this.DHT_valid = DHT_valid;
    }

    public boolean isSGP_valid() {
        return SGP_valid;
    }

    public void setSGP_valid(boolean SGP_valid) {
        this.SGP_valid = SGP_valid;
    }

    public double getTemperature2() {
        return temperature2;
    }

    public void setTemperature2(double temperature2) {
        this.temperature2 = temperature2;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }
}
