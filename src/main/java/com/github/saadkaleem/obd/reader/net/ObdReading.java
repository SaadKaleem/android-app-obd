/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.saadkaleem.obd.reader.net;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for OBD readings.
 */
public class ObdReading {
    private double latitude, longitude, altitude;
    private long timestamp;
    private String vehicleid;
    private float accX;
    private float accY;
    private float accZ;
    private float gyroX;
    private float gyroY;
    private float gyroZ;
    private float orientation;
    private Map<String, String> readings;

    private static final String[] NAMES_COLUMNS_ONLY_READINGS = {
            "engine_load", "engine_rpm", "speed", "throttle_pos",};

    public ObdReading() {
        readings = new HashMap<>();
    }

    public ObdReading(double latitude, double longitude, double altitude, long timestamp,
                      String vehicleid, Map<String, String> readings, Object acc, Object gyro, Object orientation) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timestamp = timestamp;
        this.vehicleid = vehicleid;
        Map<String, String> temp = new HashMap<String, String>();

        for (String ccln : NAMES_COLUMNS_ONLY_READINGS) {
            temp.put(ccln, readings.get(ccln));
        }
        this.readings = temp;


        this.accX = ((float[])acc)[0];
        this.accY = ((float[])acc)[1];
        this.accZ = ((float[])acc)[2];
        this.gyroX = ((float[])gyro)[0];
        this.gyroY = ((float[])gyro)[1];
        this.gyroZ = ((float[])gyro)[2];
        this.orientation = (float)orientation;
    }

    public float getAccX() {
        return accX;
    }

    public float getAccY() {
        return accY;
    }

    public float getAccZ() {
        return accZ;
    }

    public float getGyroX() {
        return gyroX;
    }

    public float getGyroY() {
        return gyroY;
    }

    public float getGyroZ() {
        return gyroZ;
    }

    public float getOrientation() {
        return orientation;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getVin() {
        return vehicleid;
    }

    public void setVin(String vehicleid) {
        this.vehicleid = vehicleid;
    }

    public Map<String, String> getReadings() {
        return readings;
    }

    public void setReadings(Map<String, String> readings) {
        this.readings = readings;
    }

    public String toString() {
        return "{" +
                "time:" + timestamp + "," +
                "lat:" + latitude + "," +
                "long:" + longitude + "," +
                "alt:" + altitude + "," +
                "accX:" + accX + "," +
                "accY:" + accY + "," +
                "accZ:" + accZ + "," +
                "gyroX:" + gyroX + "," +
                "gyroY:" + gyroY + "," +
                "gyroZ:" + gyroZ + "," +
                "orientation:" + orientation + "," +
                "readings:" + readings.toString() +
                 "}";
    }
//               "readings:" + readings.toString().substring(10).replace("}", "").replace(",", ",") +
}
