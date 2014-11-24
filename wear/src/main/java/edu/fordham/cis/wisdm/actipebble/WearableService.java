package edu.fordham.cis.wisdm.actipebble;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by andrew on 11/11/14.
 */
public class WearableService extends WearableListenerService {

    private static final String TAG = "WearableService";
    private static final String START_TRAINING = "/start-training";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.wtf(TAG, "Wear Service started");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message received for path " + messageEvent.getPath());
        if (messageEvent.getPath().equals(START_TRAINING)) {
            Intent i = new Intent(getBaseContext(), WearTrainingActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(i);
        }
    }
}
