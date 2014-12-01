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
import com.google.android.gms.wearable.WearableListenerService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Mananges the collection and saving of data from both the phone and smartwatch
 * @author <a href="mailto:ajohnston@fordham.edu">Andrew H. Johnston</a>
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
     * The list of acceleration records from the watch
     */
    private ArrayList<AccelerationRecord> mWatchAccelerationRecords;

    /**
     * The list of acceleration records from the phone
     */
    private ArrayList<AccelerationRecord> mPhoneAccelerationRecords = new ArrayList<AccelerationRecord>();

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
        mSensorManager.registerListener(this, mAccelerometer, SAMPLE_RATE);
        return START_STICKY; //Apparently this is what is typically returned
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (shouldSample) {
            long time = System.currentTimeMillis();
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mPhoneAccelerationRecords.add(new AccelerationRecord(x,y,z,time));
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        try {
            for (DataEvent event: dataEvents) {
                byte[] data = event.getDataItem().getData();
                ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                mWatchAccelerationRecords = (ArrayList<AccelerationRecord>) objectInputStream.readObject();
                shouldSample = false;

                String filename = name + "_" + activity;

                writeToFile(mWatchAccelerationRecords, filename+"_watch.txt");
                writeToFile(mPhoneAccelerationRecords, filename+"_phone.txt");
                new Thread(
                        new SendData(emailSender, emailPassword,
                                filename+"_watch.txt",
                                filename+"_phone.txt")).start();
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(500l); //Vibrate for half a second
            }
        } catch (Exception e) {
            Log.wtf(TAG, "Something happened: " +e.getClass().getName() + ": " +e.getMessage());
        }
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
        private File watch;
        private File phone;

        /**
         * Provides arguments so the thread can send email appropriately
         * @param u The email of the sender
         * @param p The password to the sender's email
         * @param w The filename for the watch file
         * @param f The filename for the phone file
         */
        public SendData(String u, String p, String w, String f) {
            user = u;
            pass = p;
            watch = new File(getFilesDir(), w);
            phone = new File(getFilesDir(), f);
        }

        @Override
        public void run() {
            GMailSender sender = new GMailSender(user, pass);
            try {
                File[] attach = {watch, phone};
                sender.sendMail("Data for " + name, "This is the data", user, emailRecipient, attach);
            } catch (Exception e) {
                Log.wtf(TAG, e.getMessage());
            }
        }

    }
}
