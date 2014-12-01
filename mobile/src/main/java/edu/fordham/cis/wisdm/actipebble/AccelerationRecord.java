package edu.fordham.cis.wisdm.actipebble;

import android.util.Log;

import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Holds an (x,y,z) tuple for a given time. Serializable for easy transmission from watch
 * to phone.
 * @author Andrew H. Johnston <a href="mailto:ajohnston9@fordham.edu">mailto:ajohnston9@fordham.edu</a>
 * @version 1.0STABLE
 */
public class AccelerationRecord implements Serializable{

    /**
     * The debugging tag for the class
     */
    private static final String TAG = "AccelerationRecord";

    /**
     * The acceleration in the x-axis
     * @serial
     */
    private float x;

    /**
     * The acceleration in the y-axis
     * @serial
     */
    private float y;
    /**
     * The acceleration in the z-axis
     * @serial
     */
    private float z;

    /**
     * The timestamp of when the record was taken
     * @serial
     */
    private long timestamp;

    /**
     * Automatically generated serial number for ensuring that a object of this type can be safely
     * deserialized.
     */
    private static final long serialVersionUID = 2345673456543874764L;

    /**
     * Creates a new record
     * @param _x the acceleration in the x-axis (in m/s^2)
     * @param _y the acceleration in the y-axis (in m/s^2)
     * @param _z the acceleration in the z-axis (in m/s^2)
     * @param _time the UNIX time of the acceleration record
     */
    public AccelerationRecord(float _x, float _y, float _z, long _time) {
        x         = _x;
        y         = _y;
        z         = _z;
        timestamp = _time;
    }

    /**
     * Used for serialization of the class
     * @param outputStream the output stream to write to
     */
    private void writeObject(ObjectOutputStream outputStream) {
        try {
            outputStream.defaultWriteObject();
        } catch (Exception e) {
            Log.w(TAG, "Caught exception in AccelerationRecord.writeObject(): " + e.getMessage());
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return timestamp + "," +x+","+y+","+z;
    }
}
