package edu.fordham.cis.wisdm.actipebble;

import android.util.Log;

import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by andrew on 1/14/15.
 */
public class GyroscopeRecord extends AbstractRecord implements Serializable {



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


}
