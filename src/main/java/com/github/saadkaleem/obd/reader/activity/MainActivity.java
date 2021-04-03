package com.github.saadkaleem.obd.reader.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.reader.R;
import com.github.saadkaleem.obd.reader.io.LogCSVWriter;
import com.github.saadkaleem.obd.reader.io.ObdCommandJob;
import com.github.saadkaleem.obd.reader.io.ObdProgressListener;
import com.github.saadkaleem.obd.reader.listeners.AccelerationListener;
import com.github.saadkaleem.obd.reader.listeners.GyroscopeListener;
import com.github.saadkaleem.obd.reader.listeners.OrientationListener;
import com.github.saadkaleem.obd.reader.listeners.SensorListener;
import com.github.saadkaleem.obd.reader.net.ObdReading;
import com.github.saadkaleem.obd.reader.net.UploadReadings;
import com.github.saadkaleem.obd.reader.service.BluetoothServiceConnection;
import com.github.saadkaleem.obd.reader.storage.SharedPrefManager;
import com.github.saadkaleem.obd.reader.trips.TripLog;
import com.github.saadkaleem.obd.reader.trips.TripRecord;
import com.google.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

//import com.github.saadkaleem.obd.reader.cam.ExampleRtspActivity;

@ContentView(R.layout.main)
public class MainActivity extends RoboActivity implements ObdProgressListener, LocationListener, GpsStatus.Listener {

    private static final String TAG = MainActivity.class.getName();
    private static final int NO_BLUETOOTH_ID = 0;
    private static final int BLUETOOTH_DISABLED = 1;
    private static final int START_LIVE_DATA = 2;
    private static final int STOP_LIVE_DATA = 3;
    private static final int START_CAM = 12;
    private static final int SETTINGS = 4;
    private static final int GET_DTC = 5;
    private static final int TABLE_ROW_MARGIN = 7;
    private static final int NO_ORIENTATION_SENSOR = 8;
    private static final int NO_GPS_SUPPORT = 9;
    private static final int TRIPS_LIST = 10;
    private static final int SAVE_TRIP_NOT_AVAILABLE = 11;
    private static final int LOGOUT = 13;
    private static boolean bluetoothDefaultIsEnable = false;
    private static final int EVENT_INTERVAL = 3;

    static {
        RoboGuice.setUseAnnotationDatabases(false);
    }

    private boolean stopped = true;

    public enum BluetoothServiceStatus {
        Connected,
        Connecting,
        Error,
        Disabled,
        Ok
    }

    public enum OBDServiceStatus {
        Read, Data, Disconnecting
    }

    public Map<String, String> commandResult = new HashMap<String, String>();
    boolean mGpsIsStarted = false;
    private LocationManager mLocService;
    private LocationProvider mLocProvider;
    private LogCSVWriter myCSVWriter;
    private Location mLastLocation;
    private Queue<ObdReading> readingEventQueue = new LinkedList<>();


    /// the trip log
    private TripLog triplog;
    private TripRecord currentTrip;

    private Context context;
    @InjectView(R.id.compass_text)
    private TextView compass;
    private final SensorListener accListener = new AccelerationListener();

    private final SensorListener gyroListener = new GyroscopeListener();
    private final SensorListener orientListener = new OrientationListener();

    @InjectView(R.id.BT_STATUS)
    private TextView btStatusTextView;
    @InjectView(R.id.OBD_STATUS)
    private TextView obdStatusTextView;
    @InjectView(R.id.GPS_POS)
    private TextView gpsStatusTextView;
    @InjectView(R.id.vehicle_view)
    private LinearLayout vv;
    @InjectView(R.id.data_table)
    private TableLayout tl;
    @InjectView(R.id.TrackingBtn)
    private Button trackingBtn;
    @Inject
    private SensorManager sensorManager;
    @Inject
    private PowerManager powerManager;
    @Inject
    private SharedPreferences prefs;
    //    private boolean isServiceBound;
//    private AbstractGatewayService service;
    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            if (serviceConn != null && serviceConn.isRunning() && serviceConn.getService().queueEmpty()) {
            serviceConn.queueCommands(prefs);

            double lat = 0;
            double lon = 0;
            double alt = 0;
            final int posLen = 7;
            if (mGpsIsStarted && mLastLocation != null) {
                lat = mLastLocation.getLatitude();
                lon = mLastLocation.getLongitude();
                alt = mLastLocation.getAltitude();

                StringBuilder sb = new StringBuilder();
                sb.append("Lat: ");
                sb.append(String.valueOf(mLastLocation.getLatitude()).substring(0, posLen));
                sb.append(" Lon: ");
                sb.append(String.valueOf(mLastLocation.getLongitude()).substring(0, posLen));
                sb.append(" Alt: ");
                sb.append(String.valueOf(mLastLocation.getAltitude()));
                gpsStatusTextView.setText(sb.toString());
            }
            if (prefs.getBoolean(ConfigActivity.UPLOAD_DATA_KEY, false)) {
                // Upload the current reading by http
                final String vin = prefs.getString(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                Map<String, String> temp = new HashMap<String, String>();
                Log.d(TAG, "CommandResult: " + commandResult.toString());
                temp.putAll(commandResult);
                Log.d(TAG, "Temp: " + temp.toString());
                ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp, accListener.getSensorData(), gyroListener.getSensorData(), orientListener.getSensorData());

                if (readingEventQueue.size() == EVENT_INTERVAL) {
                    readingEventQueue.clear();
                }

                readingEventQueue.add(reading);
                if (readingEventQueue.size() == EVENT_INTERVAL) {
                    new UploadReadings(prefs, MainActivity.this).execute(readingEventQueue);
                }
            } else if (prefs.getBoolean(ConfigActivity.ENABLE_FULL_LOGGING_KEY, false)) {
                // Write the current reading to CSV
                final String vin = prefs.getString(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                Map<String, String> temp = new HashMap<String, String>();
                temp.putAll(commandResult);
                ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp, accListener.getSensorData(), gyroListener.getSensorData(), orientListener.getSensorData());
                myCSVWriter.writeLineCSV(reading);
            }
            commandResult.clear();
            }
            // run again in period defined in preferences
            if (!stopped) {
                new Handler().postDelayed(mQueueCommands, ConfigActivity.getObdUpdatePeriod(prefs));
            }
        }
    };
    private Sensor orientSensor = null;
    private Sensor accSensor = null;
    private Sensor gyroSensor = null;
    private PowerManager.WakeLock wakeLock = null;

    private BluetoothServiceConnection serviceConn = new BluetoothServiceConnection(this);

    public static String LookUpCommand(String txt) {
        for (AvailableCommandNames item : AvailableCommandNames.values()) {
            if (item.getValue().equals(txt)) return item.name();
        }
        return txt;
    }

    public void updateTextView(final TextView view, final String txt) {
        new Handler().post(new Runnable() {
            public void run() {
                view.setText(txt);
            }
        });
    }

    public void stateUpdate(final ObdCommandJob job) {
        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
        final String cmdID = LookUpCommand(cmdName);

        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
            if (cmdResult != null) {
                obdStatusTextView.setText(cmdResult.toLowerCase());
            }
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {
            cmdResult = getString(R.string.status_obd_no_support);
        } else {
            cmdResult = job.getCommand().getCalculatedResult();
            obdStatusTextView.setText(getString(R.string.status_obd_data));
        }

        if (vv.findViewWithTag(cmdID) != null) {
            TextView existingTV = (TextView) vv.findViewWithTag(cmdID);
            existingTV.setText(cmdResult);
        } else addTableRow(cmdID, cmdName, cmdResult);
        Log.d(TAG, "cmdID" + cmdID.toLowerCase().replace(" ", "") + " " + "cmdResult" + cmdResult);
        commandResult.put(cmdID.toLowerCase().replace(" ", ""), cmdResult);
        updateTripStatistic(job, cmdID);
    }

    private boolean gpsInit() {
        mLocService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mLocService != null) {
            mLocProvider = mLocService.getProvider(LocationManager.GPS_PROVIDER);
            if (mLocProvider != null) {
                mLocService.addGpsStatusListener(this);
                if (mLocService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gpsStatusTextView.setText(getString(R.string.status_gps_ready));
                    return true;
                }
            }
        }
        gpsStatusTextView.setText(getString(R.string.status_gps_no_support));
        showDialog(NO_GPS_SUPPORT);
        Log.e(TAG, "Unable to get GPS PROVIDER");
        // todo disable gps controls into Preferences
        return false;
    }

    private void updateTripStatistic(final ObdCommandJob job, final String cmdID) {

        if (currentTrip != null) {
            if (cmdID.equals(AvailableCommandNames.SPEED.toString())) {
                SpeedCommand command = (SpeedCommand) job.getCommand();
                currentTrip.setSpeedMax(command.getMetricSpeed());
            } else if (cmdID.equals(AvailableCommandNames.ENGINE_RPM.toString())) {
                RPMCommand command = (RPMCommand) job.getCommand();
                currentTrip.setEngineRpmMax(command.getRPM());
            } else if (cmdID.endsWith(AvailableCommandNames.ENGINE_RUNTIME.toString())) {
                RuntimeCommand command = (RuntimeCommand) job.getCommand();
                currentTrip.setEngineRuntime(command.getFormattedResult());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(MainActivity.this, SharedPrefManager.getInstance(MainActivity.this).getToken(), Toast.LENGTH_LONG);
        trackingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stopped){
                    stopped = false;
                    startLiveData();
                    trackingBtn.setText("Stop Tracking");
                }
                else{
                    stopped = true;
                    stopLiveData();
                    trackingBtn.setText("Start Tracking");
                }
            }
        });
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null)
            bluetoothDefaultIsEnable = btAdapter.isEnabled();

        // get Orientation sensor
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (sensors.size() > 0) {
            orientSensor = sensors.get(0);
        } else
            showDialog(NO_ORIENTATION_SENSOR);


        context = this.getApplicationContext();
        // create a log instance for use by this application
        triplog = TripLog.getInstance(context);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "Entered onStart...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLocService != null) {
            mLocService.removeGpsStatusListener(this);
            mLocService.removeUpdates(this);
        }

        releaseWakeLockIfHeld();
        serviceConn.destroy();


        endTrip();

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && btAdapter.isEnabled() && !bluetoothDefaultIsEnable)
            btAdapter.disable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Pausing..");
        releaseWakeLockIfHeld();
    }

    /**
     * If lock is held, release. Lock will be held when the service is running.
     */
    private void releaseWakeLockIfHeld() {
        if (wakeLock.isHeld())
            wakeLock.release();
    }

    //        @SuppressLint("InvalidWakeLockTag")
    @SuppressLint("InvalidWakeLockTag")
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resuming..");
        sensorManager.registerListener(orientListener, orientSensor,
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(accListener, accSensor,
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(gyroListener, gyroSensor,
                SensorManager.SENSOR_DELAY_UI);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "ObdReader");

        // get Bluetooth device
        final BluetoothAdapter btAdapter = BluetoothAdapter
                .getDefaultAdapter();

        serviceConn.setPreRequisites(btAdapter != null && btAdapter.isEnabled());
        if (!serviceConn.isPreRequisites() && prefs.getBoolean(ConfigActivity.ENABLE_BT_KEY, false)) {
            serviceConn.setPreRequisites(btAdapter.enable());
        }

        gpsInit();

        if (!serviceConn.isPreRequisites()) {
            showDialog(BLUETOOTH_DISABLED);
            btStatusTextView.setText(getString(R.string.status_bluetooth_disabled));
        } else {
            btStatusTextView.setText(getString(R.string.status_bluetooth_ok));
        }
    }

    private void updateConfig() {
        startActivity(new Intent(this, ConfigActivity.class));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(0, START_LIVE_DATA, 0, getString(R.string.menu_start_live_data));
//        menu.add(0, STOP_LIVE_DATA, 0, getString(R.string.menu_stop_live_data));
        menu.add(0, GET_DTC, 0, getString(R.string.menu_get_dtc));
        menu.add(0, TRIPS_LIST, 0, getString(R.string.menu_trip_list));
        menu.add(0, SETTINGS, 0, getString(R.string.menu_settings));
        menu.add(0, LOGOUT, 0, "Logout");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case START_LIVE_DATA:
//                stopped=false;
//                startLiveData();
//                return true;
//            case STOP_LIVE_DATA:
//                stopped=true;
//                stopLiveData();
//                return true;
            case SETTINGS:
                updateConfig();
                return true;
            case GET_DTC:
                getTroubleCodes();
                return true;
            case TRIPS_LIST:
                startActivity(new Intent(this, TripListActivity.class));
                return true;
            case LOGOUT:
                SharedPrefManager.getInstance(MainActivity.this).clear();
                startActivity(new Intent(this, LoginActivity.class));
                return true;

            // case COMMAND_ACTIVITY:
            // staticCommand();
            // return true;
        }
        return false;
    }

    private void getTroubleCodes() {
        startActivity(new Intent(this, TroubleCodesActivity.class));
    }


    private void startLiveData() {
        Log.d(TAG, "Starting live data..");

        tl.removeAllViews(); //start fresh
        serviceConn.doBindService();

        currentTrip = triplog.startTrip();
        if (currentTrip == null)
            showDialog(SAVE_TRIP_NOT_AVAILABLE);

        // start command execution
        new Handler().post(mQueueCommands);

        if (prefs.getBoolean(ConfigActivity.ENABLE_GPS_KEY, false))
            gpsStart();
        else
            gpsStatusTextView.setText(getString(R.string.status_gps_not_used));

        // screen won't turn off until wakeLock.release()
        wakeLock.acquire();

        if (prefs.getBoolean(ConfigActivity.ENABLE_FULL_LOGGING_KEY, false)) {

            // Create the CSV Logger
            long mils = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("_dd_MM_yyyy_HH_mm_ss");

            myCSVWriter = new LogCSVWriter("Log" + sdf.format(new Date(mils)).toString() + ".csv",
                    prefs.getString(ConfigActivity.DIRECTORY_FULL_LOGGING_KEY,
                            getString(R.string.default_dirname_full_logging))
            );
        }
    }

    private void stopLiveData() {
        Log.d(TAG, "Stopping live data..");

        gpsStop();

        serviceConn.doUnbindService();
        endTrip();

        releaseWakeLockIfHeld();
//        final String devemail = prefs.getString(ConfigActivity.DEV_EMAIL_KEY, null);
//        if (devemail != null) {
//            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    switch (which) {
//                        case DialogInterface.BUTTON_POSITIVE:
//                            ObdGatewayService.saveLogcatToFile(getApplicationContext(), devemail);
//                            break;
//
//                        case DialogInterface.BUTTON_NEGATIVE:
//                            //No button clicked
//                            break;
//                    }
//                }
//            };
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setMessage("Where there issues?\nThen please send us the logs.\nSend Logs?").setPositiveButton("Yes", dialogClickListener)
//                    .setNegativeButton("No", dialogClickListener).show();
//        }

        if (myCSVWriter != null) {
            myCSVWriter.closeLogCSVWriter();
        }
    }

    protected void endTrip() {
        if (currentTrip != null) {
            currentTrip.setEndDate(new Date());
            triplog.updateRecord(currentTrip);
        }
    }

    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        switch (id) {
            case NO_BLUETOOTH_ID:
                build.setMessage(getString(R.string.text_no_bluetooth_id));
                return build.create();
            case BLUETOOTH_DISABLED:
                build.setMessage(getString(R.string.text_bluetooth_disabled));
                return build.create();
            case NO_ORIENTATION_SENSOR:
                build.setMessage(getString(R.string.text_no_orientation_sensor));
                return build.create();
            case NO_GPS_SUPPORT:
                build.setMessage(getString(R.string.text_no_gps_support));
                return build.create();
            case SAVE_TRIP_NOT_AVAILABLE:
                build.setMessage(getString(R.string.text_save_trip_not_available));
                return build.create();
        }
        return null;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem startItem = menu.findItem(START_LIVE_DATA);
//        MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);
        MenuItem settingsItem = menu.findItem(SETTINGS);
        MenuItem getDTCItem = menu.findItem(GET_DTC);

        if (serviceConn != null && serviceConn.isRunning()) {
            getDTCItem.setEnabled(false);
//            startItem.setEnabled(false);
//            stopItem.setEnabled(true);
            settingsItem.setEnabled(false);
        } else {
            getDTCItem.setEnabled(true);
//            startItem.setEnabled(true);
//            stopItem.setEnabled(false);
            settingsItem.setEnabled(true);
        }

        return true;
    }

    private void addTableRow(String id, String key, String val) {

        TableRow tr = new TableRow(this);
        MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN,
                TABLE_ROW_MARGIN);
        tr.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setGravity(Gravity.RIGHT);
        name.setText(key + ": ");
        TextView value = new TextView(this);
        value.setGravity(Gravity.LEFT);
        value.setText(val);
        value.setTag(id);
        tr.addView(name);
        tr.addView(value);
        tl.addView(tr, params);
    }


    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    public void onGpsStatusChanged(int event) {

        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                gpsStatusTextView.setText(getString(R.string.status_gps_started));
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                gpsStatusTextView.setText(getString(R.string.status_gps_stopped));
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                gpsStatusTextView.setText(getString(R.string.status_gps_fix));
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                break;
        }
    }


    private synchronized void gpsStart() {
        if (!mGpsIsStarted && mLocProvider != null && mLocService != null && mLocService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocService.requestLocationUpdates(mLocProvider.getName(), ConfigActivity.getGpsUpdatePeriod(prefs), ConfigActivity.getGpsDistanceUpdatePeriod(prefs), this);
            mGpsIsStarted = true;
        } else if (mGpsIsStarted && mLocProvider != null && mLocService != null) {
        } else {
            gpsStatusTextView.setText(getString(R.string.status_gps_no_support));
        }
    }

    private synchronized void gpsStop() {
        if (mGpsIsStarted) {
            mLocService.removeUpdates(this);
            mGpsIsStarted = false;
            gpsStatusTextView.setText(getString(R.string.status_gps_stopped));
        }
    }

    public void updateBluetoothStatusText(BluetoothServiceStatus status) {
        switch (status) {
            case Connected:
                btStatusTextView.setText(getString(R.string.status_bluetooth_connected));
                break;
            case Disabled:
                btStatusTextView.setText(getString(R.string.status_bluetooth_disabled));
                break;
            case Error:
                btStatusTextView.setText(getString(R.string.status_bluetooth_error_connecting));
                break;
            case Ok:
                btStatusTextView.setText(getString(R.string.status_bluetooth_ok));
                break;
        }
    }

    public void updateOBDStatusText(OBDServiceStatus status) {
        switch (status) {
            case Data:
                btStatusTextView.setText(getString(R.string.status_bluetooth_connected));
                break;
            case Disconnecting:
                btStatusTextView.setText(getString(R.string.status_bluetooth_disabled));
                break;
            case Read:
                btStatusTextView.setText(getString(R.string.status_bluetooth_error_connecting));
                break;

        }
    }
}
