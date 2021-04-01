package com.github.saadkaleem.obd.reader.listeners;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class GyroscopeListener implements SensorListener {
    private float[] lastGyroscope = new float[3];
    @Override
    public void onSensorChanged(SensorEvent event) {
        lastGyroscope[0] = event.values[0];
        lastGyroscope[1] = event.values[1];
        lastGyroscope[2] = event.values[2];

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public float[] getLastGyroscope() {
        return lastGyroscope;
    }

    @Override
    public Object getSensorData() {
        return getLastGyroscope();
    }
}
