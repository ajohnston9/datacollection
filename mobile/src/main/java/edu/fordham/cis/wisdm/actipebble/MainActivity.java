package edu.fordham.cis.wisdm.actipebble;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


/**
 * This activity is the one that displays the user's activity and allows them to begin the training
 * @author Andrew H. Johnston <a href="mailto:ajohnston9@fordham.edu">ajohnston9@fordham.edu</a>
 * @version 1.0STABLE
 */
public class MainActivity extends Activity {

    /**
     * String for logging purposes
     */
    private static final String TAG = "MainActivity";

    /**
     * Buttons to trigger events
     */
    private Button mStartButton, mStopButton;

    /**
     * Text views to display pebble accelerometer info
     */
    private TextView mEmail, mSex, mUsername;

    /**
     * Flag to send to watch to trigger the start of training
     */
    private static final String START_TRAINING = "/start-training";

    /**
     * The user's name
     */
    private String name;

    /**
     * The user's email
     */
    private String email;

    /**
     * The user's sex
     */
    private char sex;


    /**
     * Flag for determining if data collection is occuring
     */
    private boolean isRunning = false;

    /**
     * Enables the activity to track the screen being locked and trigger appropriate events
     */
    private ScreenLockReceiver mReceiver;

    /**
     * Flag to tell if Receiver is registered
     */
    private boolean isReceiverRegistered = false;

    /**
     * Enables communication between the watch and the phone
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Holds the data collection service (global so it can be stopped by the cancel button)
     */
    private Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = getIntent();
        if (i != null) {
            name  = i.getStringExtra("NAME");
            email = i.getStringExtra("EMAIL");
            sex = i.getCharExtra("SEX", 'M');
        } else {
            Toast.makeText(this, "Started without name, email, or sex", Toast.LENGTH_LONG).show();
        }

        mUsername = (TextView)findViewById(R.id.username);
//        mEmail = (TextView)findViewById(R.id.email);
//        mSex = (TextView)findViewById(R.id.sex);

        mUsername.setText(name);
        mEmail.setText(email);
        mSex.setText(sex);

        mStartButton = (Button)findViewById(R.id.start_button);
        mStopButton = (Button)findViewById(R.id.stop_button);


        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Please lock phone and place in pocket", Toast.LENGTH_SHORT).show();
                isRunning = true;
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    try {
                        unregisterReceiver(mReceiver);
                    } catch (IllegalArgumentException e) {
                        Log.i(TAG, "Unregistered receiver was asked to unregister. Ignoring.");
                    }
                    isRunning = false;
                    //Avoid some NullPointerExceptions
                    if (service != null) {
                        stopService(service);
                    }
                    finish();
            }
        });


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Connected to wearable.");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Connection to wearable suspended. Code: " + i);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "Fuck! Connection failed: " + connectionResult);
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenLockReceiver();
        registerReceiver(mReceiver, intentFilter);
        isReceiverRegistered = true;
    }


    @Override
    protected void onPause() {
        if(ScreenLockReceiver.wasScreenOn && isRunning) {

            service = new Intent(this, DataManagementService.class);
            service.putExtra("NAME", name);
            service.putExtra("EMAIL", email);
            service.putExtra("SEX", sex);
            startService(service);
            new Thread(new Worker()).start();
            try {
                unregisterReceiver(mReceiver);
            } catch (IllegalArgumentException e) {
                Log.i(TAG, "Unregistered receiver was asked to unregister. Ignoring.");
            }
            isRunning = false;
        }
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sends the "start training" message to the wearable. Needs to be a separate thread
     * because you can't call .await() from the UI thread
     */
    class Worker implements Runnable {

        @Override
        public void run() {
            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result;
                Log.d(TAG, "Started message sending process.");
                result= Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node.getId(), START_TRAINING, null).await();
                Log.d(TAG, "Sent to node: " + node.getId() + " with display name: " + node.getDisplayName());
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                } else {
                    Log.d(TAG, "Message Successfully sent.");
                }
            }
        }
    }
}

