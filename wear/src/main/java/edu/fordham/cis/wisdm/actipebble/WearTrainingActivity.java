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
    private Sensor mGyroscope;
    private TextView mPrompt;
    private TextView      mProgress;

    private GoogleApiClient googleApiClient;

    private ArrayList<AccelerationRecord> mAccelerationRecords = new ArrayList<AccelerationRecord>();
    private ArrayList<GyroscopeRecord> mGyroscopeRecords = new ArrayList<GyroscopeRecord>();

    private AtomicBoolean shouldCollect = new AtomicBoolean(false);

    private static final String TAG = "WearTrainingActivity";

    private int delay         = 1000 * 10; //Shifted from 120 for debugging
    private int maxNumRecords = (delay / 1000) * 20;
    private int recordCount   = 0;
    private static final int SAMPLE_RATE = 50000;

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
        mSensorManager.registerListener(this, mAccelerometer, SAMPLE_RATE);
        mSensorManager.registerListener(this, mGyroscope, SAMPLE_RATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (shouldCollect.get()) {
            long timestamp = System.currentTimeMillis();
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            switch(event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccelerationRecords.add(new AccelerationRecord(x,y,z,timestamp));
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    GyroscopeRecord gyro = new GyroscopeRecord(x,y,z,timestamp);
                    Log.wtf(TAG, "Record is: " + gyro.toString());
                    mGyroscopeRecords.add(gyro);
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
                    ByteArrayOutputStream baosG = null;
                    try {
                        baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(mAccelerationRecords);
                        oos.flush();
                        oos.close();
                        //Handle Gyro
                        baosG = new ByteArrayOutputStream();
                        ObjectOutputStream oosG = new ObjectOutputStream(baosG);
                        oosG.writeObject(mGyroscopeRecords);
                        oosG.flush();
                        oosG.close();
                    } catch (IOException e) {
                        Log.d(TAG, "Something fucky happened: " + e.getMessage());
                    }
                    byte[] data = baos.toByteArray();
                    byte[] gData = baosG.toByteArray();
                    PutDataRequest request = PutDataRequest.create("/data");
                    request.setData(data);
                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(googleApiClient, request);
                    //Do the same for wear
                    PutDataRequest request1 = PutDataRequest.create("/gdata");
                    request1.setData(gData);
                    PendingResult<DataApi.DataItemResult> pendingResult1 =
                            Wearable.DataApi.putDataItem(googleApiClient, request1);

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
