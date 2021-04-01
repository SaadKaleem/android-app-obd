package com.github.saadkaleem.obd.reader.net;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.util.Log;

import com.github.saadkaleem.obd.reader.activity.ConfigActivity;
import com.github.saadkaleem.obd.reader.service.BluetoothServiceConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Queue;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UploadReadings extends AsyncTask<Object, Void, Void> {
    private static final String TAG = UploadReadings.class.getName();
    private final int TONE_TYPE_DANGEROUS = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
    private final int TONE_TYPE_SAFE = ToneGenerator.TONE_DTMF_B;
    private SharedPreferences prefs;

    public UploadReadings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    protected Void doInBackground(Object... params) {
        Queue<ObdReading> readings = (Queue<ObdReading>) params[0];
        Log.d(TAG, "Uploading " + readings.size() + " readings..");
        // instantiate reading service client
        final String HOST = prefs.getString(ConfigActivity.UPLOAD_URL_KEY, "192.168.100.176");
        final String S_PORT = prefs.getString(ConfigActivity.UPLOAD_PORT_KEY, "65432");
        final int PORT = Integer.parseInt(S_PORT);
        Log.d(TAG, "upload_url: " + HOST);
        Log.d(TAG, "upload_PORT: " + PORT);
//            Log.d(TAG, "Endpoint: " + endpoint);
        URL url = null;
//            final String HOST = "192.168.100.3";
//            final int PORT = 65432;
        try {
            url = new URL("http", HOST, PORT, "/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "URL STRING: " + url.toString());
        final String urlString = url.toString();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ObdService service = retrofit.create(ObdService.class);

        //merge json readings
        JSONObject jsonReading = null;
        JSONArray mergedReadings = new JSONArray();
        for (ObdReading reading : readings) {
            try {
//                    Log.d(TAG, "OBD ReadingS: " + reading.toString());
                jsonReading = new JSONObject(reading.toString());
//                    Log.d(TAG, "JSON ReadingS: " + jsonReading.toString());
                mergedReadings.put(jsonReading);

            } catch (Error | JSONException re) {
                Log.e(TAG, re.toString());
            }
        }
        Log.d(TAG, "Merged ReadingS: " + mergedReadings.toString());
        // upload readings
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (mergedReadings.toString()));
        Call<ResponseBody> call = service.uploadReading(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseString = response.body().string();
                    Log.d(TAG, "Response String: " + response.body().string());
                    Integer pred = Integer.parseInt(responseString.substring(15, 16));
                    Log.d(TAG, String.valueOf(pred));
                    if (pred == 1) {
                        Log.d(TAG, "Dangerous");
                        playDangerousSound();
                    } else if (pred == 0) {
                        Log.d(TAG, "Safe");
//                            playSafeSound();
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }

        });

        Log.d(TAG, "Done");
        return null;
    }

    public void playDangerousSound() {
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(TONE_TYPE_DANGEROUS, 300);
    }

}
