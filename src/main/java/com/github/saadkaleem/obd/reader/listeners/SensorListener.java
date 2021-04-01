package com.github.saadkaleem.obd.reader.listeners;

import android.hardware.SensorEventListener;

public interface SensorListener extends SensorEventListener {
    public Object getSensorData();
}
