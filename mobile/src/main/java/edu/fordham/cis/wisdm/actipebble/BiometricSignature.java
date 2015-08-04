package edu.fordham.cis.wisdm.actipebble;

import java.util.ArrayList;

/**
 * Wraps connections to the database and provides an easily accessible API.
 * @author Susanne George <a href="mailto:sgeorge15@fordham.edu">sgeorge15@fordham.edu</a>
 * @author Andrew Johnston <a href="mailto:ajohnston9@fordham.edu">ajohnston9@fordham.edu</a>
 * @version 0.01ALPHA
 */
public class BiometricSignature {


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
    private ArrayList<Tuple> phoneAccel;
    /**
     * A list of phone accel records. Filled by Gson automatically, no setters/constructors
     */
    private ArrayList<Tuple> phoneGyro;
    /**
     * A list of phone accel records. Filled by Gson automatically, no setters/constructors
     */
    private ArrayList<Tuple> watchAccel;
    /**
     * A list of phone accel records. Filled by Gson automatically, no setters/constructors
     */
    private ArrayList<Tuple> watchGyro;

    /**
     * Gson requires a default constructor, but there is no initialization to do so it is empty.
     */
    public BiometricSignature () {}

    public String getName() { return name; }

    public String getEmail() {
        return email;
    }

    public char getSex() {
        return sex;
    }

    public ArrayList<Tuple> getPhoneAccel() {
        return phoneAccel;
    }

    public ArrayList<Tuple> getPhoneGyro() {
        return phoneGyro;
    }

    public ArrayList<Tuple> getWatchAccel() {
        return watchAccel;
    }

    public ArrayList<Tuple> getWatchGyro() {
        return watchGyro;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSex(char sex) {
        this.sex = sex;
    }

    public void setPhoneAccel(ArrayList<Tuple> phoneAccel) {
        this.phoneAccel = phoneAccel;
    }

    public void setConvertedPhoneAccel (ArrayList<AccelerationRecord> phoneAccel) {
        this.phoneAccel = new ArrayList<>();
        for (AccelerationRecord accel: phoneAccel) {
            this.phoneAccel.add(new Tuple(accel));
        }
    }

    public void setPhoneGyro(ArrayList<Tuple> phoneGyro) {
        this.phoneGyro = phoneGyro;
    }

    public void setConvertedPhoneGyro (ArrayList<GyroscopeRecord> phoneGyro) {
        this.phoneGyro = new ArrayList<>();
        for (GyroscopeRecord gyro: phoneGyro) {
            this.phoneGyro.add(new Tuple(gyro));
        }
    }

    public void setWatchAccel(ArrayList<Tuple> watchAccel) {
        this.watchAccel = watchAccel;
    }

    public void setConvertedWatchAccel (ArrayList<AccelerationRecord> watchAccel) {
        this.watchAccel = new ArrayList<>();
        for (AccelerationRecord accel: watchAccel) {
            this.watchAccel.add(new Tuple(accel));
        }
    }

    public void setWatchGyro(ArrayList<Tuple> watchGyro) {
        this.watchGyro = watchGyro;
    }

    public void setConvertedWatchGyro (ArrayList<GyroscopeRecord> watchGyro) {
        this.watchGyro = new ArrayList<>();
        for (GyroscopeRecord gyro: watchGyro) {
            this.watchGyro.add(new Tuple(gyro));
        }
    }


}
