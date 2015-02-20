package edu.fordham.cis.wisdm.actipebble;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
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
     * The user's name
     */
    private String name;

    /**
     * The label for the activity being performed
     */
    private char activity;

    /**
     * The sampling rate in microseconds to collect acceleration records at (this is 20Hz)
     */
    private int SAMPLE_RATE = 50000;

    /**
     * Flag that instructs the class whether to save new acceleration records
     */
    private boolean shouldSample = true;

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
    private ArrayList<AccelerationRecord> mWatchAccelerationRecords;
    /**
     * The list of gyroscopic records from the watch
     */
    private ArrayList<GyroscopeRecord> mWatchGyroRecords;

    /**
     * The list of acceleration records from the phone
     */
    private ArrayList<AccelerationRecord> mPhoneAccelerationRecords = new ArrayList<AccelerationRecord>();
    /**
     * The list of gyroscopic records from the phone
     */
    private ArrayList<GyroscopeRecord> mPhoneGyroRecords = new ArrayList<GyroscopeRecord>();
    /**
     * The email used to send the data
     */
    private static final String emailSender = "wisdm.gaitlab@gmail.com";

    /**
     * The password for the sender's email
     */
    private static final String emailPassword = "WiSdM403!";

    /**
     * The email to send the data to
     */
    private static final String emailRecipient = "wisdm.gaitlab@gmail.com";

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
        name = intent.getStringExtra("NAME");
        activity = intent.getCharExtra("ACTIVITY", 'A');
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometer, SAMPLE_RATE);
        mSensorManager.registerListener(this, mGyroscope, SAMPLE_RATE);
        return START_NOT_STICKY; //Don't restart when app is shut down and reopened
    }

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
                    mPhoneGyroRecords.add(new GyroscopeRecord(x,y,z,time));
            }


        }
    }

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
                } else if (path.matches("/gyro-data")) {
                    DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    ArrayList<GyroscopeRecord> gyroTmp = (ArrayList<GyroscopeRecord>)
                            (new ObjectInputStream(
                                    new ByteArrayInputStream(map.getByteArray("/gyro"))
                            )
                            ).readObject();
                    mWatchGyroRecords.addAll(gyroTmp);
                } else {
                    Log.wtf(TAG, "Received unexpected data with path " + path);
                }
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Something happened: " +e.getClass().getName() + ": " +e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().matches(DATA_COLLECTION_DONE)) {
            finalizeDataCollection();
        }
    }

    private void finalizeDataCollection() {
		Collections.sort(mWatchAccelerationRecords);
		Collections.sort(mWatchGyroRecords);
        String filename = name + "_accel_" + activity;
        String gyFilename = name + "_gyro_" + activity;
        final String watchFile = "_watch.txt";
        final String phoneFile = "_phone.txt";
        writeToFile(mWatchAccelerationRecords, filename+watchFile);
        writeToFile(mPhoneAccelerationRecords, filename+phoneFile);
        writeToFileGyro(mWatchGyroRecords, gyFilename + watchFile);
        writeToFileGyro(mPhoneGyroRecords, gyFilename+phoneFile);
        new Thread(
                new SendData(emailSender, emailPassword,
                        filename+watchFile,
                        filename+phoneFile,
                        gyFilename + watchFile,
                        gyFilename+phoneFile)).start();
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500l); //Vibrate for half a second
    }


    private void writeToFile(ArrayList<AccelerationRecord> accelerationRecords, String filename) {
        File file = new File(getFilesDir(), filename);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            for (AccelerationRecord record : accelerationRecords) {
                writer.println(record.toString());
            }
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Log.wtf(TAG, "File location is: " + file.getAbsolutePath());
    }

    private void writeToFileGyro(ArrayList<GyroscopeRecord> gyroscopeRecords, String filename) {
        File file = new File(getFilesDir(), filename);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            for (GyroscopeRecord record : gyroscopeRecords) {
                writer.println(record.toString());
            }
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Log.wtf(TAG, "File location is: " + file.getAbsolutePath());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Not actually needed but must be overridden
    }

    /**
     * Takes the data files and sends them to the appropriate emails
     */
    class SendData implements Runnable {
        private String user;
        private String pass;
        private File watchAccel;
        private File phoneAccel;
        private File watchGyro;
        private File phoneGyro;

        /**
         * Provides arguments so the thread can send email appropriately
         * @param u The email of the sender
         * @param p The password to the sender's email
         * @param wA The filename for the watch accel file
         * @param fA The filename for the phone accel file
         * @param wG The filename for the watch gyro file
         * @param pG The filename for the phone accel file
         */
        public SendData(String u, String p, String wA, String fA, String wG, String pG) {
            user = u;
            pass = p;
            watchAccel = new File(getFilesDir(), wA);
            phoneAccel = new File(getFilesDir(), fA);
            watchGyro = new File(getFilesDir(), wG);
            phoneGyro = new File(getFilesDir(), pG);

        }

        @Override
        public void run() {
            GMailSender sender = new GMailSender(user, pass);
            try {
                File[] attach = {watchAccel, phoneAccel, watchGyro,phoneGyro};
                sender.sendMail("Data for " + name, "This is the data", user, emailRecipient, attach);
            } catch (Exception e) {
                Log.wtf(TAG, e.getMessage());
            }
        }

    }
}
