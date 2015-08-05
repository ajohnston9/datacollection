package edu.fordham.cis.wisdm.actipebble;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Takes the data files and sends them to the appropriate emails
 */
public class DataSender implements Runnable {
    private static final String TAG = "DataSender";
    private static final String HOSTNAME = "tartarus.cis.fordham.edu";
    private static final int PORT = 1234;
    private File datafile;

    /**
     * Provides arguments so the thread can send email appropriately
     * @param datafile The filename for the user's JSON data
     */
    public DataSender(Context ctxt, String datafile) {
        this.datafile = new File(ctxt.getFilesDir(), datafile);
    }

    /**
     * Method called when the runnable is initiated in the thread. This sends an email
     * containing the sensor data in another Thread.
     */
    @Override
    public void run() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Socket echoSocket = new Socket(HOSTNAME, PORT);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datafile)));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((reader.readLine()) != null) {
                builder.append(line);
            }
            out.println(builder.toString());
            out.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

        }
    }

}
