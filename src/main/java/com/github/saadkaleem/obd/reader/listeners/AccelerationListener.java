package com.github.saadkaleem.obd.reader.listeners;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class AccelerationListener implements SensorListener {
    private float[] lastAcceleration = new float[3];
    @Override
    public void onSensorChanged(SensorEvent event) {
        lastAcceleration[0] = event.values[0];
        lastAcceleration[1] = event.values[1];
        lastAcceleration[2] = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float[] getLastAcceleration() {
        return lastAcceleration;
    }

    @Override
    public Object getSensorData() {
        return getLastAcceleration();
    }
}
