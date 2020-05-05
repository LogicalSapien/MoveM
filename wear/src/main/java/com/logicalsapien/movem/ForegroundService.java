package com.logicalsapien.movem;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.logicalsapien.movem.message.SendMessage;
import com.logicalsapien.movem.util.MapUtil;
import com.logicalsapien.movem.util.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

public class ForegroundService extends Service implements SensorEventListener {

    // Sensor variables
    private SensorManager mSensorManager;
    private Sensor allSensor;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    static float maxReading = 0;
    static int counter = 0;
    static Long startTime;

    // messaging variable
    static final String datapath = "/my_path";

    static Map<Long, Float> sensorData = new HashMap<>();

    private Float accelerometerThreshold;
    private Long millisToWaitBeforeSending;
    private Long millisToWaitBeforeReset;
    private Long dataPacketSize;

    SharedPrefManager spm;

    public static final String CHANNEL_ID = "MoveMNotificationChannel";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MoveM Running")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_cc_settings_button_center)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        resetMoveMonitorParams();

        // get the shared pref values
        getMonitoringPreferences();

        //do the background task
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        allSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ALL);

        boolean sensorRegistered = mSensorManager.registerListener(this,
                allSensor, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d("Sensor Status:", " Sensor registered: " + (sensorRegistered ? "yes" : "no"));
        //stopSelf();
        return START_NOT_STICKY;
    }

    public void resetMoveMonitorParams()  {
        maxReading = 0;
        counter = 0;
        startTime = null;
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        resetMoveMonitorParams();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "MoveM Notification",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void getMonitoringPreferences() {
        spm = new SharedPrefManager(getApplicationContext());
        millisToWaitBeforeSending = spm.getLong("millisToWaitBeforeSending");
        millisToWaitBeforeReset = spm.getLong("millisToWaitBeforeReset");
        dataPacketSize = spm.getLong("dataPacketSize");
        accelerometerThreshold = spm.getFloat("accelerometerThreshold");

//        Log.i("SharedPresets", "millisToWaitBeforeSending - " + spm.getLong("millisToWaitBeforeSending"));
//        Log.i("SharedPresets", "millisToWaitBeforeReset - " + spm.getLong("millisToWaitBeforeReset"));
//        Log.i("SharedPresets", "dataPacketSize - " + spm.getLong("dataPacketSize"));
//        Log.i("SharedPresets", "accelerometerThreshold - " + spm.getFloat("accelerometerThreshold"));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x*x + y*y + z*z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            // Make this higher or lower according to how much
            // motion you want to detect
            if(mAccel > accelerometerThreshold){
                // notable movement
                if (startTime == null) {
                    startTime = System.currentTimeMillis();
                }
                Long currentTime = System.currentTimeMillis();
                Long elapsedTime = currentTime - startTime;
                // Log.i("Elapsed Time : ",  elapsedTime + "");

                if (elapsedTime > millisToWaitBeforeSending && elapsedTime < millisToWaitBeforeReset){
                    // Log.i("Elapsed Time Entry : ",  elapsedTime + " - " + counter);
                    sensorData.put(currentTime, mAccel);
                    counter++;
                    if (counter >= dataPacketSize){
                        // Log.i("Sending Data : ",  elapsedTime + "");
                        counter = 0;
                        // send to mobile..
                        new SendMessage(getApplicationContext(), datapath, MapUtil.mapToLongString(sensorData)).start();
                        sensorData = new HashMap<>();
                        startTime = null;
                    }

                    if (maxReading < mAccel) {
                        maxReading = mAccel;
                    }
                } else if (elapsedTime > millisToWaitBeforeReset) {
                    startTime = currentTime;
                }
            }
        }
    }
}
