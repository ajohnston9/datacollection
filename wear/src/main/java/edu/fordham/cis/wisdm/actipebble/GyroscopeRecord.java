package edu.fordham.cis.wisdm.actipebble;

import android.util.Log;

import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by andrew on 1/14/15.
 */
public class GyroscopeRecord implements Serializable, Comparable<GyroscopeRecord> {

    /**
     * @serial
     */
    protected long timestamp;
    /**
     * @serial
     */
    protected float x;
    /**
     * @serial
     */
    protected float y;
    /**
     * @serial
     */
    protected float z;

    static final long serialVersionUID = 2409678382753934076L;

    public GyroscopeRecord(float _x, float _y, float _z, long _time) {
        x = _x;
        y = _y;
        z = _z;
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

    @Override
    public String toString() {
        return timestamp+","+x+","+y+","+z;
    }
    @Override
    public int compareTo(GyroscopeRecord that) {
        if (that == null) {
            throw new NullPointerException();
        }
        long tstmp = that.getTimestamp();
        if (that == this || tstmp == timestamp) {
            return 0;
        } else if (tstmp > timestamp) {
            return 1;
        }
        return -1;
    }

}
