package edu.fordham.cis.wisdm.actipebble;

/**
 * edu.fordham.cis.wisdm.biometrics.server.Tuple stores either an acceleration or a gyroscope record as a POJO.
 * @author Susanne George <a href="mailto:sgeorge15@fordham.edu">sgeorge15@fordham.edu</a>
 * @author Andrew Johnston <a href="mailto:ajohnston9@fordham.edu">ajohnston9@fordham.edu</a>
 * @version 0.01ALPHA
 */
public class Tuple {
    private long timestamp;
    private double X;
    private double Y;
    private double Z;

    public Tuple () {}

    public Tuple (AccelerationRecord accel) {
        this.timestamp = accel.getTimestamp();
        this.X = accel.getX();
        this.Y = accel.getY();
        this.Z = accel.getZ();
    }

    public Tuple (GyroscopeRecord accel) {
        this.timestamp = accel.getTimestamp();
        this.X = accel.getX();
        this.Y = accel.getY();
        this.Z = accel.getZ();
    }

    public Tuple(long timestamp, double x, double y, double z) {
        this.timestamp = timestamp;
        X = x;
        Y = y;
        Z = z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getX() { return X; }

    public double getY() {
        return Y;
    }

    public double getZ() {
        return Z;
    }

}

