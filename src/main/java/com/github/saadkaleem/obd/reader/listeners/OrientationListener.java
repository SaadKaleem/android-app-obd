package com.github.saadkaleem.obd.reader.listeners;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class OrientationListener implements SensorListener {
    private float lastOrientation;
    private String lastDirection;
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        lastOrientation = x;
        String dir = "";
        if (x >= 337.5 || x < 22.5) {
            dir = "N";
        } else if (x >= 22.5 && x < 67.5) {
            dir = "NE";
        } else if (x >= 67.5 && x < 112.5) {
            dir = "E";
        } else if (x >= 112.5 && x < 157.5) {
            dir = "SE";
        } else if (x >= 157.5 && x < 202.5) {
            dir = "S";
        } else if (x >= 202.5 && x < 247.5) {
            dir = "SW";
        } else if (x >= 247.5 && x < 292.5) {
            dir = "W";
        } else if (x >= 292.5 && x < 337.5) {
            dir = "NW";
        }
        lastDirection=dir;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float getLastOrientation() {
        return lastOrientation;
    }

    public String getLastDirection() {
        return lastDirection;
    }

    @Override
    public Object getSensorData() {
        return getLastOrientation();
    }
}
