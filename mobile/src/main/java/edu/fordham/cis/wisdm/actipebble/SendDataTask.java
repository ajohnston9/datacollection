package edu.fordham.cis.wisdm.actipebble;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;

/**
 * Created by luigi on 9/5/14.
 */
public class SendDataTask extends AsyncTask<Void, Void, Void> {

    private String name;

    @Override
    protected Void doInBackground(Void... params) {

        GMailSender sender = new GMailSender("wisdm.gaitlab@gmail.com", "WiSdM403!");

        try {
            File[] attach = new File[2];
            //attach[0] = MainActivity.outFile;
            //attach[1] = DataManagementService.rawFile;

            sender.sendMail(name,
                    "Email",
                    "wisdm.gaitlab@gmail.com",
                    "wisdm.gaitlab@gmail.com",
                    attach);

        } catch(Exception e){
            Log.d("sendDataTask", e.toString());
        }

        return null;
    }

}