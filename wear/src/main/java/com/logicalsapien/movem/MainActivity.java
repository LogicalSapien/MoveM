package com.logicalsapien.movem;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.logicalsapien.movem.message.SendMessage;
import com.logicalsapien.movem.util.MapUtil;
import com.logicalsapien.movem.util.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends WearableActivity {

    Button startStop;
    private TextView sensorReading;
    private TextView maxReadingTextView;

    SharedPrefManager spm;

    static float maxReading = 0;

    int receivedMessageNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setMonitoringPreferences();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startStop = (Button) findViewById(R.id.startStopButton);
        if (isMyServiceRunning(ForegroundService.class)) {
            startStop.setText("Stop");
        }

        sensorReading = (TextView) findViewById(R.id.sensorReading);
        sensorReading.setText("");
        maxReadingTextView = (TextView) findViewById(R.id.maxReadingTextView);
        maxReadingTextView.setText("0");

        // Enables Always-on
        setAmbientEnabled();

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);
    }

    public void setMonitoringPreferences() {
        spm = new SharedPrefManager(getApplicationContext());

        // check if millisToWaitBeforeSending is present
        long millisToWaitBeforeSending = 1000;
        if (spm.checkIfPresent("millisToWaitBeforeSending")) {
            millisToWaitBeforeSending = spm.getLong("millisToWaitBeforeSending");
        } else {
            spm.putLong("millisToWaitBeforeSending", millisToWaitBeforeSending);
        }

        long millisToWaitBeforeReset = 15000;
        if (spm.checkIfPresent("millisToWaitBeforeReset")) {
            millisToWaitBeforeReset = spm.getLong("millisToWaitBeforeReset");
        } else {
            spm.putLong("millisToWaitBeforeReset", millisToWaitBeforeReset);
        }

        long dataPacketSize = 10;
        if (spm.checkIfPresent("dataPacketSize")) {
            dataPacketSize = spm.getLong("dataPacketSize");
        } else {
            spm.putLong("dataPacketSize", dataPacketSize);
        }

        float accelerometerThreshold = 2;
        if (spm.checkIfPresent("accelerometerThreshold")) {
            accelerometerThreshold = spm.getFloat("accelerometerThreshold");
        } else {
            spm.putFloat("accelerometerThreshold", accelerometerThreshold);
        }

//        Log.i("SharedPresets", "millisToWaitBeforeSending - " + spm.getLong("millisToWaitBeforeSending"));
//        Log.i("SharedPresets", "millisToWaitBeforeReset - " + spm.getLong("millisToWaitBeforeReset"));
//        Log.i("dataPacketSize", "dataPacketSize - " + spm.getLong("dataPacketSize"));
//        Log.i("SharedPresets", "accelerometerThreshold - " + spm.getFloat("accelerometerThreshold"));
    }

    public void startMonitoring(View view)  {
        if (startStop.getText().toString().equalsIgnoreCase("Start")) {
            startStop.setText("Stop");
            maxReadingTextView.setText(maxReading + "");
            startService();
        } else {
            startStop.setText("Start");
            stopService();
        }
    }

    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra("inputExtra", "MoveM Service");
        ContextCompat.startForegroundService(this, serviceIntent);

        // start the heart beat timer
        final Handler handler = new Handler();

        final MainActivity thisObj = this;

        Runnable run = new Runnable() {
            @Override
            public void run() {
//                Toast.makeText(thisObj,  "Runnuing", Toast.LENGTH_SHORT).show();

                // calculate battery percentage
                Intent batteryStatus   = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int level = batteryStatus .getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus .getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                float batteryPercentage = ((float)level / (float)scale) * 100.0f;

                String datapath = "/my_path";
                Map<String, String> hbD = new HashMap<>();
                hbD.put("heartBeat", System.currentTimeMillis() + "");
                hbD.put("batteryPercentage", batteryPercentage + "");


                new SendMessage(getApplicationContext(), datapath, MapUtil.mapToString(hbD)).start();

                handler.postDelayed(this, 150000);
            }
        };

        handler.post(run);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service
                : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Map<String, String> prefMap = MapUtil.stringToMap(message);
            Log.i("Receive", message + "--" + prefMap.size());

            // preference received from mobile,
            // update the shared pref

            // check if millisToWaitBeforeSending is present
            spm.putLong("millisToWaitBeforeSending", Long.parseLong(prefMap.get("millisToWaitBeforeSending")));
            spm.putLong("millisToWaitBeforeReset", Long.parseLong(prefMap.get("millisToWaitBeforeReset")));
            spm.putLong("dataPacketSize", Long.parseLong(prefMap.get("dataPacketSize")));
            spm.putFloat("accelerometerThreshold", Float.parseFloat(prefMap.get("accelerometerThreshold")));

            //  if the service is already running, stop and restart
            if (isMyServiceRunning(ForegroundService.class)) {
                stopService();
                startService();
            }
        }
    }
}
