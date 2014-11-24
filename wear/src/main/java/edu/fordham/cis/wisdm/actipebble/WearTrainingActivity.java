package edu.fordham.cis.wisdm.actipebble;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


public class WearTrainingActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private TextView mPrompt;
    private TextView      mProgress;

    private GoogleApiClient googleApiClient;

    private ArrayList<AccelerationRecord> mAccelerationRecords = new ArrayList<AccelerationRecord>();

    private AtomicBoolean shouldCollect = new AtomicBoolean(false);

    private static final String TAG = "WearTrainingActivity";

    private int delay         = 1000 * 120; 
    private int maxNumRecords = (delay / 1000) * 20;
    private int recordCount   = 0;

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
        //Collect at 20Hz (Once every 50,000 microseconds)
        mSensorManager.registerListener(this, mAccelerometer, 50000);
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
        new Thread(new CollectTask()).start();
        shouldCollect.set(true);
        Log.wtf(TAG, "Started collecting");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (shouldCollect.get()) {
            long timestamp = System.currentTimeMillis();
            mAccelerationRecords.add(
                    new AccelerationRecord(event.values[0], event.values[1], event.values[2], timestamp)
            );
            if (mAccelerationRecords.size() % 20 == 0) {
                Log.wtf(TAG, "Current size of acceleration records is " + mAccelerationRecords.size());
            }
            recordCount++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Not used but must be overridden
    }

    class CollectTask implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (recordCount > maxNumRecords) {
                    shouldCollect.set(false);
                    Log.wtf(TAG, "Ending stream");
                    ByteArrayOutputStream baos = null;
                    try {
                        baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(mAccelerationRecords);
                        oos.flush();
                        oos.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Something fucky happened: " + e.getMessage());
                    }
                    byte[] data = baos.toByteArray();
                    PutDataRequest request = PutDataRequest.create("/data");
                    request.setData(data);
                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(googleApiClient, request);
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
                    break; //Leave the thread
                }
            }
        }
    }
}
