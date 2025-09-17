package com.gncbrown.getmetosleep.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gncbrown.getmetosleep.Schedulers.NightlyScheduler;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()))) {
            Log.d(TAG, "Boot completed or quickboot. Rescheduling nightly check.");
            NightlyScheduler.scheduleNextNightlyCheck(context);
        }
    }
}