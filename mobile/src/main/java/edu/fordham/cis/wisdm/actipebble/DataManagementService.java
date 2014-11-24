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

//import com.getpebble.android.kit.PebbleKit;
//import com.getpebble.android.kit.util.PebbleDictionary;


/**
 * Mananges the collection and saving of data from both the phone and smartwatch
 * @author <a href="mailto:ajohnston@fordham.edu">Andrew H. Johnston</a>
 * @version 1.0ALPHA
 */
public class DataManagementService extends WearableListenerService implements SensorEventListener {

    private static final String TAG = "DataManagementService";

    private String  name;
    private char    activity;

    private int    SAMPLE_RATE = 50000; //Sample at 20Hz (Once every 50,000 microseconds)
    private boolean shouldSample = true;
    private SensorManager mSensorManager;
    private Sensor        mAccelerometer;

    private ArrayList<AccelerationRecord> mWatchAccelerationRecords;
    private ArrayList<AccelerationRecord> mPhoneAccelerationRecords = new ArrayList<AccelerationRecord>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.wtf(TAG, "DataManagementService Started");
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
                Log.wtf(TAG, "Records is of size: " + mWatchAccelerationRecords.size());
                shouldSample = false;
                writeToFile(mWatchAccelerationRecords, name +"_"+activity+"_watch.txt");
                writeToFile(mPhoneAccelerationRecords, name +"_"+activity+"_phone.txt");
                new Thread(
                        new SendData("wisdm.gaitlab@gmail.com", "WiSdM403!",
                                name +"_"+activity+"_watch.txt",
                                name +"_"+activity+"_phone.txt")).start();
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

    class SendData implements Runnable {
        private String user;
        private String pass;
        private File watch;
        private File phone;

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
                //TODO: "New Data" gets used everywhere
                sender.sendMail("Data for " + name, "This is the data", "wisdm.gaitlab@gmail.com", "ahjohnston25@gmail.com", attach);
            } catch (Exception e) {
                Log.wtf(TAG, e.getMessage());
            }
        }

    }
}
