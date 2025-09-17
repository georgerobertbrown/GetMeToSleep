package com.gncbrown.getmetosleep.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class TestPowerReceiver extends BroadcastReceiver {
    private static final String TAG = "TestPowerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = (intent != null) ? intent.getAction() : "null intent";
        Log.d(TAG, "TestPowerReceiver onReceive triggered! Action: " + action);
        // Toast for immediate visual feedback, especially if logs are hard to catch when app is closed.
        // Make sure to run this on a device/emulator where you can see Toasts from background apps.
        Toast.makeText(context, "TestPowerReceiver: " + action, Toast.LENGTH_LONG).show();
    }
}
