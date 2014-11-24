package edu.fordham.cis.wisdm.actipebble;

/**
 * Created by luigi on 9/5/14.
 */
public class Constants {

    /**
     * I am the name of the directory that encapsulates all app files.
     */
    public static final String APP_DIR = "WISDM";


    public static String activity = "";

    /**
     * I am the name of the file that stores the raw data.
     */
    public static final String RAW_FILE = ".txt";

    /**
     * I am the name of the file that stores the steps.
     */
    public static final String PEBBLE_FILE = "_pebble.txt";

    /**
     * I am an arbitrary unique id for the service status-bar notification.
     */
    public static final int NOTIFICATION_ID = 1;

    /**
     * I am the interval after which sampling should begin once the screen is turned off.
     */
    public static final long SCREEN_WAIT = 5000;

    /**
     * I am the interval between samples.
     */
    public static final long SAMPLING_RATE = 20;

    /**
     * I am the amount of records that will be queued before transferring to file.
     */
    public static final int QUEUE_MAX = 200;
}
