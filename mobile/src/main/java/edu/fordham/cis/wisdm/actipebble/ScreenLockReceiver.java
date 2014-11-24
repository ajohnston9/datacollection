package edu.fordham.cis.wisdm.actipebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Class to notify activities of when the screen locks and unlocks so it can be detected in onPause()\
 * @author Andrew H. Johnston
 * @version 0.01ALPHA
 */
public class ScreenLockReceiver extends BroadcastReceiver {

    public static boolean wasScreenOn = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            wasScreenOn = false;
        } else {
            wasScreenOn = true;
        }

    }
}
