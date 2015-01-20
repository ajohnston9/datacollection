package edu.fordham.cis.wisdm.actipebble;

/**
 * Created by andrew on 1/14/15.
 */
public abstract class AbstractRecord {

    protected long timestamp;
    protected float x;
    protected float y;
    protected float z;

    private static final long serialVersionUID = 2345673456543874764L;

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
