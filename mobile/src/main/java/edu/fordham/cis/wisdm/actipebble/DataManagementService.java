package edu.fordham.cis.wisdm.actipebble;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Mananges the collection and saving of data from both the phone and smartwatch
 * @author Andrew H. Johnston <a href="mailto:ajohnston9@fordham.edu">ajohnston9@fordham.edu</a>
 * @version 1.0STABLE
 */
public class DataManagementService extends WearableListenerService implements SensorEventListener {

    /**
     * The debugging tag for the class
     */
    private static final String TAG = "DataManagementService";


    /**
     * The container for user's data
     */
    BiometricSignature signature;


    /**
     * The sampling rate in microseconds to collect acceleration records at (this is 20Hz)
     */
    private int SAMPLE_RATE = 50000;

    /**
     * Flag that instructs the class whether to save new acceleration records
     */
    private boolean shouldSample = true;

    /**
     * I am used in determining and manipulating the state of the screen.
     */
    private PowerManager powerManager = null;

    /**
     * I am responsible for keeping the device partly awake while collecting data.
     */
    private PowerManager.WakeLock wakeLock = null;

    /**
     * Handles the instantiation of the accelerometer
     */
    private SensorManager mSensorManager;

    /**
     * Represents the physical acclerometer
     */
    private Sensor mAccelerometer;

    /**
     * Represents the gyroscope
     */
    private Sensor mGyroscope;

    /**
     * The list of acceleration records from the watch
     */
    private ArrayList<AccelerationRecord> mWatchAccelerationRecords = new ArrayList<AccelerationRecord>();
    /**
     * The list of gyroscopic records from the watch
     */
    private ArrayList<GyroscopeRecord> mWatchGyroRecords = new ArrayList<GyroscopeRecord>();

    /**
     * The list of acceleration records from the phone
     */
    private ArrayList<AccelerationRecord> mPhoneAccelerationRecords = new ArrayList<AccelerationRecord>();
    /**
     * The list of gyroscopic records from the phone
     */
    private ArrayList<GyroscopeRecord> mPhoneGyroRecords = new ArrayList<GyroscopeRecord>();


    /**
     * Flag that signals the end of data transmission from the watch
     */
    private static final String DATA_COLLECTION_DONE = "/thats-all-folks";

    /**
     * This is the equivalent of onCreate() but for Services. Allows for instantiating a service with arguments
     * @param intent The intent that carries all arguments
     * @param flags Any special flags for the class
     * @param startId The ID of the service
     * @return A code used by Android internals
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        signature.setName(intent.getStringExtra("NAME"));
        signature.setEmail(intent.getStringExtra("EMAIL"));
        signature.setSex(intent.getCharExtra("SEX", 'M'));

        powerManager = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //wakeLock.acquire();
        mSensorManager.registerListener(this, mAccelerometer, SAMPLE_RATE);
        mSensorManager.registerListener(this, mGyroscope, SAMPLE_RATE);
        shouldSample = true;

        //Don't restart when app is shut down and reopened
        return START_NOT_STICKY;
    }

    /**
     * Service clean up.
     *
     * Release the wake lock and un registers the accelerometer and sensor listeners when
     * collection is over.
     */
    @Override
    public void onDestroy() {
        if(wakeLock.isHeld()) {
            wakeLock.release();
        }
        mSensorManager.unregisterListener(this);
        Log.i(TAG, "Un registering phone sensor listeners.");
        super.onDestroy();
    }

    /**
     * This method acquires the wake lock and registers the accelerometer and sensor listeners at
     * the chosen SAMPLE_RATE.
     *
     */
    private void registerSensorListeners(){
        wakeLock.acquire();
        mSensorManager.registerListener(this, mAccelerometer, SAMPLE_RATE);
        mSensorManager.registerListener(this, mGyroscope, SAMPLE_RATE);
        shouldSample = true;
    }


    /**
     * This method is called each time new sensor data is available. Checks for the sensor type and
     * then adds the data to the appropriate list of records
     *
     * @param event - data structure containing the sensor data
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (shouldSample) {
            long time = System.currentTimeMillis();
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            switch(event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mPhoneAccelerationRecords.add(new AccelerationRecord(x,y,z,time));
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    Log.wtf(TAG, "Gyro data");
                    mPhoneGyroRecords.add(new GyroscopeRecord(x,y,z,time));
            }


        }
    }

    /**
     * This method is called once the phone receives a message from the watch.
     * The data is added to the appropriate lists and then finalizeDataCollection is called.
     *
     * @param dataEvents - the sensor data from the watch
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        //Once the watch starts sending data its time to stop collecting
        shouldSample = false;

        try {
            for (DataEvent event: dataEvents) {
                String path = event.getDataItem().getUri().getPath();

                if (path.matches("/accel-data")) {

                    DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    ArrayList<AccelerationRecord> accelTmp = (ArrayList<AccelerationRecord>)
                            (new ObjectInputStream(
                                    new ByteArrayInputStream(map.getByteArray("/accel"))
                            )
                            ).readObject();
                    mWatchAccelerationRecords.addAll(accelTmp);
                    Log.i(TAG, "Received acceleration list is of size: " + accelTmp.size());

                } else if (path.matches("/gyro-data")) {

                    DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    ArrayList<GyroscopeRecord> gyroTmp = (ArrayList<GyroscopeRecord>)
                            (new ObjectInputStream(
                                    new ByteArrayInputStream(map.getByteArray("/gyro"))
                            )
                            ).readObject();
                    mWatchGyroRecords.addAll(gyroTmp);
                    Log.i(TAG, "Received gyroscope list is of size: " + gyroTmp.size());

                    if (map.getString("/done").matches(DATA_COLLECTION_DONE)) {
                        finalizeDataCollection();
                    }

                } else {
                    Log.e(TAG, "Received unexpected data with path " + path);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in onDataChanged: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * This method is called after the gyroscope sensor data from the watch is received and added
     * to the appropriate data structure in memory.
     */
    private void finalizeDataCollection() {

        // Sort the lists in ascending order of timestamp
		Collections.sort(mWatchAccelerationRecords);
		Collections.sort(mWatchGyroRecords);

        Log.i(TAG, "Watch Acceleration List size is " + mWatchAccelerationRecords.size());
        Log.i(TAG, "Watch Gyro List size is " + mWatchGyroRecords.size());

        signature.setConvertedWatchAccel(mWatchAccelerationRecords);
        signature.setConvertedWatchGyro(mWatchGyroRecords);
        signature.setConvertedPhoneAccel(mPhoneAccelerationRecords);
        signature.setConvertedPhoneGyro(mPhoneGyroRecords);

        String filename = signature.getName() + ".txt";

        // Write the sensor records to files on the phone's disk
        writeToFile(filename);

        // Email all 4 files as attachments
        new Thread(new SendData(signature, filename)).start();

        // Vibrate half a second for the user's sake
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500l);
    }

    /**
     * This method write the BiometricSignature to a JSON file on the phone.
     *
     * @param filename
     */
    private void writeToFile(String filename) {
        File file = new File(getFilesDir(), filename);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            Gson gson = new Gson();
            gson.toJson(signature, writer);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "File location is: " + file.getAbsolutePath());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Not actually needed but must be overridden
    }

    /**
     * Takes the data files and sends them to the appropriate emails
     */
    class SendData implements Runnable {
        private static final String HOSTNAME = "tartarus.cis.fordham.edu";
        private static final int PORT = 1234;
        private File datafile;
        private BiometricSignature signature;

        /**
         * Provides arguments so the thread can send email appropriately
         * @param datafile The filename for the user's JSON data
         */
        public SendData(BiometricSignature signature, String datafile) {
            this.datafile = new File(getFilesDir(), datafile);
            this.signature = signature;
        }

        /**
         * Method called when the runnable is initiated in the thread. This sends an email
         * containing the sensor data in another Thread.
         *
         */
        @Override
        public void run() {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try {
                Socket echoSocket = new Socket(HOSTNAME, PORT);
                PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
                Gson gson = new Gson();
                gson.toJson(signature, out);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());

            }
        }

    }
}
