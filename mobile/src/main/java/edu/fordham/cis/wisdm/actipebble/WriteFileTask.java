package edu.fordham.cis.wisdm.actipebble;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by luigi on 9/5/14.
 */
public class WriteFileTask extends AsyncTask<Void, Void, Void> {

    /**
     * Tag for logging purposes
     */
    private final String TAG = "WriteFileTask";

    /**
     * I am a shared resource between the sampler and the sender, holding the raw data.
     */
    private File rawFile = null;


    /**
     * I hold sampled records until I am full enough to be dumped into the file.
     */
    private LinkedBlockingQueue<String> records = null;


    /**
     * I am responsible for dumping the data from the record queue into the file.
     */
    private FileWriter rawWriter = null;


    /**
     * I pass references to the file and record queue to this class.
     * @param f
     * @param q
     */
    public WriteFileTask(File f, LinkedBlockingQueue<String> q){
        records = q;
        rawFile = f;

        try {
            rawWriter = new FileWriter(rawFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try{
            String r = null;
            for(int i = 0; i < Constants.QUEUE_MAX; i++) {
                r = records.poll();
                rawWriter.write(r + "\n");
            }
            rawWriter.close();

        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }


}
