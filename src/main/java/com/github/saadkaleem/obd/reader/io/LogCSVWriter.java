package com.github.saadkaleem.obd.reader.io;

import android.os.Environment;
import android.util.Log;

import com.github.saadkaleem.obd.reader.net.ObdReading;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * Created by Max Christ on 14.07.15.
 */

public class LogCSVWriter {

    private static final String TAG = LogCSVWriter.class.getName();
    private static final String HEADER_CSV = "Trip Log";
    private static final String[] NAMES_COLUMNS = {"TIME", "LATITUDE", "LONGITUDE", "LATITUDE", "ACCELEROMETER (X)", "ACCELEROMETER (Y)",
            "ACCELEROMETER (Z)", "GYROSCOPE (X)", "GYROSCOPE (Y)", "GYROSCOPE (Z)", "ORIENTATION", "ENGINE_LOAD", "ENGINE_RPM", "SPEED",
            "THROTTLE_POS"};
    private static final String[] NAMES_COLUMNS_ONLY_READINGS = {
            "ENGINE_LOAD", "ENGINE_RPM", "SPEED", "THROTTLE_POS",};
    private static String dir;
    private boolean isFirstLine;
    private BufferedWriter buf;

    public LogCSVWriter(String filename, String dirname) {

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + File.separator + dirname);
        dir.mkdirs();

        Log.d(TAG, "Path is " + sdCard.getAbsolutePath() + File.separator + dirname);

        File file = new File(dir, filename);
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        OutputStreamWriter osw = new OutputStreamWriter(fos);
        this.buf = new BufferedWriter(osw);
        this.isFirstLine = true;

        Log.d(TAG, "Constructed the LogCSVWriter");

    }

    public void closeLogCSVWriter() {
        try {
            buf.flush();
            buf.close();
            Log.d(TAG, "Flushed and closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLineCSV(ObdReading reading) {
        String crl = new String();

        if (isFirstLine) {
            crl = HEADER_CSV + reading.toString();
            addLine(crl);
            isFirstLine = false;

            // Add line with the columns
            crl = "";
            for (String ccln : NAMES_COLUMNS) {
                crl += ccln + ",";
            }
            addLine(crl.substring(0, crl.length() - 1)); // remove last ","

        } else {

            crl = reading.getTimestamp() + "," +
                    reading.getLatitude() + "," +
                    reading.getLongitude() + "," +
                    reading.getAltitude() + "," +
                    reading.getAccX() + "," +
                    reading.getAccY() + "," +
                    reading.getAccZ() + "," +
                    reading.getGyroX() + "," +
                    reading.getGyroY() + "," +
                    reading.getGyroZ() + "," +
                    reading.getOrientation() + "," ;


            Map<String, String> read = reading.getReadings();

            for (String ccln : NAMES_COLUMNS_ONLY_READINGS) {
                crl += read.get(ccln) + ",";
            }

            addLine(crl.substring(0, crl.length() - 1));
        }
    }


    private void addLine(String line) {
        if (line != null) {
            try {
                buf.write(line, 0, line.length());
                buf.newLine();
                Log.d(TAG, "LogCSVWriter: Wrote" + line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
