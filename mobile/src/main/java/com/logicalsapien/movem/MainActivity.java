package com.logicalsapien.movem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.logicalsapien.movem.util.MapUtil;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.logicalsapien.movem.MyBroadcastReceiver.ACTION_SNOOZE;
import static com.logicalsapien.movem.MyBroadcastReceiver.EXTRA_NOTIFICATION_ID;

public class MainActivity extends AppCompatActivity {

    protected Handler myHandler;
    int receivedMessageNumber = 1;
    int sentMessageNumber = 1;

    TextView sensorTextview;

    EditText millisToWaitBeforeSending;
    EditText millisToWaitBeforeReset;
    EditText dataPacketSize;
    EditText accelerometerThreshold;

    LongOperation lo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorTextview = findViewById(R.id.sensorTextview);
        sensorTextview.setText("No Data Yet..");

        millisToWaitBeforeSending   = (EditText)findViewById(R.id.millisToWaitBeforeSending);
        millisToWaitBeforeReset   = (EditText)findViewById(R.id.millisToWaitBeforeReset);
        dataPacketSize   = (EditText)findViewById(R.id.dataPacketSize);
        accelerometerThreshold   = (EditText)findViewById(R.id.accelerometerThreshold);

        millisToWaitBeforeSending.setText(1000+"");
        millisToWaitBeforeReset.setText(15000+"");
        dataPacketSize.setText(10+"");
        accelerometerThreshold.setText(2+"");

        myHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                messageText(stuff.getString("messageText"));
                return true;
            }
        });

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        Receiver messageReceiver = new Receiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        createNotificationChannel();
        lo = new LongOperation(this);

    }

    private void createNotificationChannel() {
//        Log.i("TTT","wewew");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void onButtonPress(View view) {
        lo = new LongOperation(this);
        lo.execute("There is a movement..!!!!");
        Map<String, String> prefMap = new HashMap<>();
        prefMap.put("millisToWaitBeforeSending", millisToWaitBeforeSending.getText().toString());
        prefMap.put("millisToWaitBeforeReset", millisToWaitBeforeReset.getText().toString());
        prefMap.put("dataPacketSize", dataPacketSize.getText().toString());
        prefMap.put("accelerometerThreshold", accelerometerThreshold.getText().toString());

        Log.i("SendingMessage", MapUtil.mapToString(prefMap));

        new NewThread("/my_path", MapUtil.mapToString(prefMap)).start();
    }

    public void messageText(String newinfo) {
        if (newinfo.compareTo("") != 0) {
//            textview.append("\n" + newinfo);
            Log.i("messageText", "messageText");
        }
    }


    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            receivedMessageNumber++;
//            String message = "I just received a message from the wearable " + receivedMessageNumber++;;
//            textview.setText(message);
            String message = intent.getStringExtra("message");
            Map<String, String> data = MapUtil.stringToMap(message);
            if (message.contains("heartBeat")) {
                sensorTextview.setText("Last Heart Beat " + DateFormat.getDateTimeInstance().format(new Date()) + "\n");
            } else {
                StringBuilder textViewM = new StringBuilder();
                textViewM.append("Data Received at " + DateFormat.getDateTimeInstance().format(new Date()) + "\n");
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    textViewM.append(key + " - " + value + "\n");
                }
                sensorTextview.setText(textViewM.toString());
                lo = new LongOperation(context);
                lo.execute("There is a movement..!!!!");
                Log.i("Received", message);
            }
        }
    }


    public void sendmessage(String messageText) {
        Bundle bundle = new Bundle();
        bundle.putString("messageText", messageText);
        Message msg = myHandler.obtainMessage();
        msg.setData(bundle);
        myHandler.sendMessage(msg);
    }


    class NewThread extends Thread {
        String path;
        String message;

        NewThread(String p, String m) {
            path = p;
            message = m;
        }


        public void run() {

            Task<List<Node>> wearableList =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {

                List<Node> nodes = Tasks.await(wearableList);
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {

                        Integer result = Tasks.await(sendMessageTask);
                        sendmessage("I just sent the wearable a message " + sentMessageNumber++);

                    } catch (ExecutionException exception) {

                        //TO DO: Handle the exception//


                    } catch (InterruptedException exception) {

                    }

                }

            } catch (ExecutionException exception) {

                //TO DO: Handle the exception//

            } catch (InterruptedException exception) {

                //TO DO: Handle the exception//
            }

        }
    }

    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String TAG = "main";

    private class LongOperation extends AsyncTask<String, String, String> {

        private static final String TAG = "longoperation";
        private Context ctx;
        private AtomicInteger notificationId = new AtomicInteger(0);

        LongOperation(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        protected String doInBackground(String... params) {
            for (String s : params) {
                Log.e(TAG, s);

                publishProgress(s);

                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                    }
                }
            }
            return "Executed";
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String title: values) {
                sendNotification(title, notificationId.incrementAndGet());
            }
        }

        void sendNotification(String title, int notificationId) {

            // Create an explicit intent for an Activity in your app
        /* Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0); */

            Intent snoozeIntent = new Intent(ctx, MyBroadcastReceiver.class);
            snoozeIntent.setAction(ACTION_SNOOZE);
            snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

            Log.e(TAG, snoozeIntent.getExtras().toString());

            Log.e(TAG, "snoozeIntent id: " + snoozeIntent.getIntExtra(EXTRA_NOTIFICATION_ID, -1));

            PendingIntent snoozePendingIntent =
                    PendingIntent.getBroadcast(ctx, notificationId, snoozeIntent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(String.format("%s (id %d)", title, notificationId))
                    .setContentText("A considerable movement has been recorded by the watch")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(false)
                    // Add the action button
                    .addAction(R.drawable.ic_launcher_foreground, ctx.getString(R.string.snooze),
                            snoozePendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId, builder.build());
        }
    }
}
