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

//import com.getpebble.android.kit.PebbleKit;
//import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
//import com.getpebble.android.kit.util.PebbleDictionary;


public class MainActivity extends Activity {

    /**
     * String for logging purposes
     */
    private static final String TAG = "MainActivity";

    /**
     * Buttons to trigger events
     */
    private Button mStartButton, mStopButton, mCancelButton;

    /**
     * Text views to display pebble accelerometer info
     */
    private TextView mActivity, mUsername;


    /**
     * Key for Pebble accelerometer values
     */
    private static final int KEY_XYZ = 45;

    private static final int NUM_SAMPLES = 25;
    private static final int FREQUENCY = 50;
    private static final int INCREMENT = 1000/FREQUENCY;
    private static final String START_TRAINING = "/start-training";

    private char   label;
    private String name;
    private String actname;

    private boolean isRunning = false;
    private ScreenLockReceiver mReceiver;

    private GoogleApiClient mGoogleApiClient;
    private Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = getIntent();
        if (i != null) {
            label = i.getCharExtra("ACTIVITY", 'A');
            name  = i.getStringExtra("NAME");
            actname = i.getStringExtra("ACTIVITY_NAME");
        } else {
            Toast.makeText(this, "Started without name or activity label", Toast.LENGTH_LONG).show();
        }
        mActivity = (TextView)findViewById(R.id.activity_label);
        mUsername = (TextView)findViewById(R.id.username);

        mUsername.setText(name);
        mActivity.setText(actname);

        mStartButton = (Button)findViewById(R.id.start_button);
        mStopButton = (Button)findViewById(R.id.stop_button);
        mCancelButton = (Button)findViewById(R.id.cancel_button);


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
                if (isRunning) {
                    isRunning = false;
                    stopService(service);
                    finish();
                }
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(service);
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
    }


    @Override
    protected void onPause() {
        if(ScreenLockReceiver.wasScreenOn && isRunning) {

            service = new Intent(this, DataManagementService.class);
            service.putExtra("NAME", name);
            service.putExtra("ACTIVITY", label);
            startService(service);
            new Thread(new Worker()).start();
            unregisterReceiver(mReceiver);
            isRunning = false;
        }
        super.onPause();
    }

//    @Override
//    protected void onDestroy() {
//        super.onPause();
//    }


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

