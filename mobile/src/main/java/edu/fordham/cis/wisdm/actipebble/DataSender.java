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
 * Sends contents of a JSON file to the appropriate server
 */
public class DataSender implements Runnable {
    private static final String TAG = "DataSender";
    private static final String HOSTNAME = "tartarus.cis.fordham.edu";
    private static final int PORT = 1234;
    private Context context;
    private String filename;

    /**
     * Provides arguments so the thread can send data appropriately
     * @param context //TODO: i don't know what context means
     * @param filename The filename containing the user's JSON data
     */
    public DataSender(Context context, String filename) {
        this.context = context;
        this.filename = filename;
    }

    /**
     * Reads JSON file contents into a string
     * @return The string containing JSON representation of user's data
     */
    private String getJSON() throws Exception {
        File file = new File(context.getFilesDir(), filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    /**
     * Method called when the runnable is initiated in the thread.
     * This sends a JSON string representation of the user's data to the server at HOSTNAME & PORT.
     */
    @Override
    public void run() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            Socket echoSocket = new Socket(HOSTNAME, PORT);
            PrintWriter writer = new PrintWriter(echoSocket.getOutputStream(), true);
            writer.println(getJSON());
            writer.close();
            context.deleteFile(filename);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
