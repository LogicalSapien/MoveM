package com.logicalsapien.movem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends WearableActivity implements SensorEventListener {

//    private TextView mTextView;

    private SensorManager mSensorManager;
    private Sensor allSensor;

    private TextView sensorReading;
    private TextView maxReadingTextView;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    static private float accelThreshold = 2;
    static int counter = 0;
    static String csData = "";
    static int millisToWait = 1000;
    static int millisToReset = 15000;
    static int noOfReadings = 10;
    static float maxReading = 0;
    static Long startTime;
    static int dataPacketSize = 10;

    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;

    Button talkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mTextView = (TextView) findViewById(R.id.text);
        sensorReading = (TextView) findViewById(R.id.sensorReading);
        sensorReading.setText("");
        maxReadingTextView = (TextView) findViewById(R.id.maxReadingTextView);
        maxReadingTextView.setText("0");

        talkButton =  findViewById(R.id.talkClick);

        // Enables Always-on
        setAmbientEnabled();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        allSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ALL);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        talkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String onClickMessage = "I just sent the handheld a message " + sentMessageNumber++;
                Log.i("Sendingggg",  "messagge");

//Make sure you’re using the same path value//

                String datapath = "/my_path";
                new SendMessage(datapath, onClickMessage).start();

            }
        });

        IntentFilter newFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, newFilter);
    }

    public void startMonitoring(View view)  {
        Button startStop = (Button)findViewById(R.id.startStopButton);
        if (startStop.getText().toString().equalsIgnoreCase("Start")) {
            startStop.setText("Stop");
            boolean sensorRegistered = mSensorManager.registerListener(this,
                    allSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.d("Sensor Status:",
                    " Sensor registered: " + (sensorRegistered ? "yes" : "no"));
            maxReadingTextView.setText(maxReading + "");
        } else {
            startStop.setText("Start");
            mSensorManager.unregisterListener(this);
            sensorReading.setText("");
        }
        maxReading = 0;
        counter = 0;
        startTime = null;

        String onClickMessage = "I just sent the handheld a message " + sentMessageNumber++;

        String datapath = "/my_path";
        new SendMessage(datapath, onClickMessage).start();

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
            if(mAccel > accelThreshold){
                // notable movement
//                Log.i("SensorChanged", "Accelerometer :  " + mAccel);
                sensorReading.setText("Accelerometer : " + mAccel);
                if (startTime == null) {
                    startTime = System.currentTimeMillis();
                }

                Long currentTime = System.currentTimeMillis();

                Long elapsedTime = currentTime - startTime;

//                Log.i("Elapsed Time : ",  elapsedTime + "");

                if (elapsedTime > millisToWait && elapsedTime < millisToReset){
                    Log.i("Elapsed Time Entry : ",  elapsedTime + " - " + counter);
                    csData = csData + mAccel + ",";
                    counter++;
                    if (counter >= dataPacketSize){
                        Log.i("Sending Data : ",  elapsedTime + "");
                        counter = 0;
//                        sendDataOverHttp(csData);
                        // send to mobile..
                        String datapath = "/my_path";
                        new SendMessage(datapath, csData).start();
                        csData = "";
                        startTime = null;
                    }

                    if (maxReading < mAccel) {
                        maxReading = mAccel;
                        maxReadingTextView.setText(maxReading + "");
                    }
                } else if (elapsedTime > millisToReset) {
                    startTime = currentTime;
                }

            }
        }
    }

    private void sendDataOverHttp(final String mAccel) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url ="http://192.168.1.251:3000?sensonReading=" + mAccel;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("sensorReading", 2.343);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String mRequestBody = jsonBody.toString();

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // That worked
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // That didn't work
                error.printStackTrace();
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String onMessageReceived = "I just received a  message from the handheld " + receivedMessageNumber++;
//            textView.setText(onMessageReceived);
            Log.i("onReceive", onMessageReceived);


        }
    }

    class SendMessage extends Thread {
        String path;
        String message;

//Constructor///

        SendMessage(String p, String m) {
            path = p;
            message = m;
        }

//Send the message via the thread. This will send the message to all the currently-connected devices//

        public void run() {

//Get all the nodes//

            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

//Block on a task and get the result synchronously//

                List<Node> nodes = Tasks.await(nodeListTask);

//Send the message to each device//

                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {



                        Integer result = Tasks.await(sendMessageTask);


//Handle the errors//

                    } catch (ExecutionException exception) {

//TO DO//

                    } catch (InterruptedException exception) {

//TO DO//

                    }

                }

            } catch (ExecutionException exception) {

//TO DO//

            } catch (InterruptedException exception) {

//TO DO//

            }
        }
    }
}
