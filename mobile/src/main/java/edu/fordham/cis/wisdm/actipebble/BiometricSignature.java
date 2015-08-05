package edu.fordham.cis.wisdm.actipebble;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;

public class BiometricSignature {

    private static final String TAG = "BiometricSignature";

    /**
     * The person's name
     */
    private String name;
    /**
     * The person's email
     */
    private String email;
    /**
     * The sex (not gender) of the person
     */
    private char sex;
    /**
     * A list of phone accel records. Filled by Gson automatically, no setters/constructors
     */
    private ArrayList<AccelerationRecord> phoneAccel;
    /**
     * A list of phone accel records. Filled by Gson automatically, no setters/constructors
     */
    private ArrayList<GyroscopeRecord> phoneGyro;
    /**
     * A list of phone accel records. Filled by Gson automatically, no setters/constructors
     */
    private ArrayList<AccelerationRecord> watchAccel;
    /**
     * A list of phone accel records. Filled by Gson automatically, no setters/constructors
     */
    private ArrayList<GyroscopeRecord> watchGyro;

    /**
     * Gson requires a default constructor, but there is no initialization to do so it is empty.
     */
    public BiometricSignature () {}


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public char getSex() {
        return sex;
    }

    public void setSex(char sex) {
        this.sex = sex;
    }

    public ArrayList<AccelerationRecord> getPhoneAccel() {
        return phoneAccel;
    }

    public void setPhoneAccel(ArrayList<AccelerationRecord> phoneAccel) {
        this.phoneAccel = phoneAccel;
    }

    public ArrayList<GyroscopeRecord> getPhoneGyro() {
        return phoneGyro;
    }

    public void setPhoneGyro(ArrayList<GyroscopeRecord> phoneGyro) {
        this.phoneGyro = phoneGyro;
    }

    public ArrayList<AccelerationRecord> getWatchAccel() {
        return watchAccel;
    }

    public void setWatchAccel(ArrayList<AccelerationRecord> watchAccel) {
        this.watchAccel = watchAccel;
    }

    public ArrayList<GyroscopeRecord> getWatchGyro() {
        return watchGyro;
    }

    public void setWatchGyro(ArrayList<GyroscopeRecord> watchGyro) {
        this.watchGyro = watchGyro;
    }

    public void pushPhoneAccel(AccelerationRecord record) {
        phoneAccel.add(record);
    }

    public void pushPhoneGyro(GyroscopeRecord record)  {
        phoneGyro.add(record);
    }

    public void pushWatchAccel(byte[] accels) {
        try {
            ArrayList<AccelerationRecord> temp = (ArrayList<AccelerationRecord>) (new ObjectInputStream(new ByteArrayInputStream(accels))).readObject();
            watchAccel.addAll(temp);
            Log.i(TAG, "Received acceleration list is of size: " + temp.size());
        } catch (Exception e) {
            Log.e(TAG, "Exception in pushWatchAccel: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void pushWatchGyro(byte[] gyros) {
        try {
            ArrayList<GyroscopeRecord> temp = (ArrayList<GyroscopeRecord>) (new ObjectInputStream(new ByteArrayInputStream(gyros))).readObject();
            watchGyro.addAll(temp);
            Log.i(TAG, "Received gyroscope list is of size: " + temp.size());
        } catch (Exception e) {
            Log.e(TAG, "Exception in pushWatchGyro: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public void sortWatchRecords() {
        Collections.sort(watchAccel);
        Collections.sort(watchGyro);
    }

}
