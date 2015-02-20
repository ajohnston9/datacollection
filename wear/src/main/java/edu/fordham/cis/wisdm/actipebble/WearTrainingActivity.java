package edu.fordham.cis.wisdm.actipebble;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.common.collect.Lists;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the collection of data and the transmission of the data back to the phone in batches.
 * @author Andrew H. Johnston <a href="mailto:ajohnston9@fordham.edu">ajohnston9@fordham.edu</a>
 * @version 1.0STABLE
 */
public class WearTrainingActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private TextView mPrompt;
    private TextView      mProgress;

    private GoogleApiClient googleApiClient;

    private ArrayList<AccelerationRecord> mAccelerationRecords = new ArrayList<AccelerationRecord>();
    private ArrayList<GyroscopeRecord> mGyroscopeRecords = new ArrayList<GyroscopeRecord>();

    /**
     * Tells the Sensor if it should save the records it
     */
    private boolean shouldCollect = false;

    /**
     * Debugging tag
     */
    private static final String TAG = "WearTrainingActivity";

    /**
     * One of the strategies to keep the watch screen on
     */
    private PowerManager.WakeLock wakeLock;

    /**
     * Constant used to add clarity to formulae below
     */
    private static final short MILLIS_IN_A_SECOND = 1000;
    /**
     * Change the second multiplicand to change the number of seconds of data collected
     */
    private int delay  = MILLIS_IN_A_SECOND * 120;
    /**
     * Expressed in a formula so that value makes more sense
     */
    private int maxNumRecords = (delay / MILLIS_IN_A_SECOND) * 20;
    /**
     * Keeps track of the number of records obtained. This is more accurate than a time-based
     * approach.
     */
    private int recordCount   = 0;
    /**
     * Sample rate, expressed as number of microseconds between samplings
     */
    private static final int SAMPLE_RATE = 50000;
    /**
     * Maximum number of records we can send to the phone in one transmission.
     */
    private static final int MAX_RECORDS_SENT_AT_ONCE = 3500;
    /**
     * Flag that signals the end of data transmission to the phone
     */
    private static final String DATA_COLLECTION_DONE = "/thats-all-folks";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        //Easy way to keep watch from sleeping on me
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mProgress = (TextView) findViewById(R.id.txtProgress);
        mPrompt = (TextView) findViewById(R.id.txtPrompt);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mProgress = (TextView) findViewById(R.id.txtProgress);
                mPrompt = (TextView) findViewById(R.id.txtPrompt);
            }
        });
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope     = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //Collect at 20Hz (Once every 50,000 microseconds)
        mSensorManager.registerListener(this, mAccelerometer, SAMPLE_RATE);
        mSensorManager.registerListener(this, mGyroscope, SAMPLE_RATE);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "Connected to phone.");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Connection to phone suspended. Code: " + i);
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
        googleApiClient.connect();
        shouldCollect = true;
        Log.d(TAG, "Started collecting");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK,
                "WearTrainingWakelock");
        wakeLock.acquire();
    }

    @Override
    protected void onPause() {
            super.onPause();
            mSensorManager.unregisterListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SAMPLE_RATE);
        mSensorManager.registerListener(this, mGyroscope, SAMPLE_RATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (shouldCollect) {
            long timestamp = System.currentTimeMillis();
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            switch(event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccelerationRecords.add(new AccelerationRecord(x,y,z,timestamp));
                    recordCount++;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    GyroscopeRecord gyro = new GyroscopeRecord(x,y,z,timestamp);
                    //Clean up debugging output a little
                    if (recordCount % 10 == 0) {
                        Log.d(TAG, "Record is: " + gyro.toString());
                    }
                    mGyroscopeRecords.add(gyro);
            }
            if (recordCount > maxNumRecords) {
                shouldCollect = false;
                new Thread(new SendDataToPhoneTask()).start();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Not used but must be overridden
    }



    class SendDataToPhoneTask implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "Ending stream");
            try {
                List<List<AccelerationRecord>> accelLists =
                        Lists.partition(mAccelerationRecords, MAX_RECORDS_SENT_AT_ONCE);
                List<List<GyroscopeRecord>> gyroLists =
                        Lists.partition(mGyroscopeRecords, MAX_RECORDS_SENT_AT_ONCE);
                 /* I know the following two for loops look like they could be
                  * abstracted into a single generic method, but due to type erasure
                  * of generics I can't do this with a single polymoprhic method.
                  */
                for (List<AccelerationRecord> list : accelLists) {
                    Log.d(TAG, "Sending list of acceleration records...");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(list);
                    oos.flush();
                    oos.close();
                    byte[] data = baos.toByteArray();
                    PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/accel-data");
                    dataMapRequest.getDataMap().putByteArray("/accel", data);
                    PutDataRequest request = dataMapRequest.asPutDataRequest();
                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(googleApiClient, request);
                }
                for (List<GyroscopeRecord> list : gyroLists) {
                    Log.d(TAG, "Sending list of gyroscope records...");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oosG = new ObjectOutputStream(baos);
                    oosG.writeObject(list);
                    oosG.flush();
                    oosG.close();
                    byte[] data = baos.toByteArray();
                    PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/gyro-data");
                    dataMapRequest.getDataMap().putByteArray("/gyro", data);
                    PutDataRequest request = dataMapRequest.asPutDataRequest();
                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(googleApiClient, request);
                }
                //Signal end of data collection
                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result;
                    Log.d(TAG, "Started message sending process.");
                    result= Wearable.MessageApi.sendMessage(
                            googleApiClient, node.getId(), DATA_COLLECTION_DONE, null).await();
                    Log.d(TAG, "Sent to node: " + node.getId() + " with display name: " + node.getDisplayName());
                    if (!result.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    } else {
                        Log.d(TAG, "Message Successfully sent.");
                    }
                }
                //Vibrate and tell the user to check their phone
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(500L); //Vibrate for half a second
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPrompt.setText("Please finish the training by opening your phone.");
                        mProgress.setText("");
                    }
                });
            } catch (IOException e) {
                Log.d(TAG, "Something fucky happened: " + e.getMessage());
            } finally {
                //Screen can turn off now.
                wakeLock.release();
            }

        }
    }
}
