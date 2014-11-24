package edu.fordham.cis.wisdm.actipebble;

import android.util.Log;

import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
        * This class acts as a bin for acceleration records and allows for easy serialization of the data
        * @author Andrew H. Johnston
        * @verison 0.01ALPHA
        */
public class AccelerationRecord implements Serializable {

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

    private static final long serialVersionUID = 2345673456543874764L;

    public AccelerationRecord(float _x, float _y, float _z, long _time) {
        x         = _x;
        y         = _y;
        z         = _z;
        timestamp = _time;
    }

    private void writeObject(ObjectOutputStream outputStream) {
        try {
            outputStream.defaultWriteObject();
        } catch (Exception e) {
            Log.wtf("AccelerationRecord", e.getMessage());
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
}