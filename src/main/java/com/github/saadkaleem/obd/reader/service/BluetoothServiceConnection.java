package com.github.saadkaleem.obd.reader.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.reader.R;
import com.github.saadkaleem.obd.reader.activity.MainActivity;
import com.github.saadkaleem.obd.reader.config.ObdConfig;
import com.github.saadkaleem.obd.reader.io.AbstractGatewayService;
import com.github.saadkaleem.obd.reader.io.MockObdGatewayService;
import com.github.saadkaleem.obd.reader.io.ObdCommandJob;
import com.github.saadkaleem.obd.reader.io.ObdGatewayService;

import java.io.IOException;

public class BluetoothServiceConnection implements ServiceConnection {
    private static final String TAG = BluetoothServiceConnection.class.getName();

    private MainActivity context;

    private boolean isServiceBound;
    private boolean preRequisites;

    private AbstractGatewayService service;

    public BluetoothServiceConnection(MainActivity context) {
        this.context = context;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
        Log.d(TAG, className.toString() + " service is bound");
        isServiceBound = true;
        service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
        service.setContext(context);
        Log.d(TAG, "Starting live data");
        try {
            service.startService();
            if (preRequisites)
                context.updateBluetoothStatusText(MainActivity.BluetoothServiceStatus.Connected);

        } catch (IOException ioe) {
            Log.e(TAG, "Failure Starting live data");
            context.updateBluetoothStatusText(MainActivity.BluetoothServiceStatus.Error);
            doUnbindService();
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    @Override
    public void onServiceDisconnected(ComponentName className) {
        Log.d(TAG, className.toString() + " service is unbound");
        isServiceBound = false;
    }

    public void doUnbindService() {
        if (isServiceBound) {
            if (service.isRunning()) {
                service.stopService();
                if (preRequisites)
                    context.updateBluetoothStatusText(MainActivity.BluetoothServiceStatus.Ok);
            }
            Log.d(TAG, "Unbinding OBD service..");
            context.unbindService(this);
            isServiceBound = false;
            context.updateOBDStatusText(MainActivity.OBDServiceStatus.Disconnecting);

        }
    }

    public void doBindService() {
        if (!isServiceBound) {
            Log.d(TAG, "Binding OBD service..");
            if (preRequisites) {
                context.updateBluetoothStatusText(MainActivity.BluetoothServiceStatus.Connecting);

            } else {

                context.updateBluetoothStatusText(MainActivity.BluetoothServiceStatus.Disabled);
            }
            startIntent();
        }
    }

    public void destroy() {
        if (isServiceBound) {
            doUnbindService();
        }
    }

    public boolean isRunning() {
        return service != null && service.isRunning();
    }


    public void queueCommands(SharedPreferences prefs) {
        if (isServiceBound) {
            for (ObdCommand Command : ObdConfig.getCommands()) {
                if (prefs.getBoolean(Command.getName(), true))
                    service.queueJob(new ObdCommandJob(Command));
            }
        }
    }

    public AbstractGatewayService getService() {
        return service;
    }

    private void startIntent() {
        Intent serviceIntent = new Intent(context, MockObdGatewayService.class);
        context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    }

    public void setPreRequisites(boolean preRequisites) {
        this.preRequisites = preRequisites;
    }

    public boolean isPreRequisites() {
        return preRequisites;
    }
}
