package com.gncbrown.getmetosleep.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import com.gncbrown.getmetosleep.Utilities.Utils;

import java.util.Calendar;

public class ChargingReceiver extends BroadcastReceiver {
    private static final String TAG = "ChargingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);
        if (intent == null) return;
        Log.d(TAG, "onReceive: " + intent.getAction());

        if (Utils.getMonitoringMode(context) != Utils.MONITORING_MODE_SCHEDULER) {
            String action = intent.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                handlePowerConnected(context);
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                handlePowerDisconnected(context);
            }
        } else {
            Utils.showNotification(context, "ChargingReceiver", "Scheduler is enabled, skipping charging receiver");
        }
    }

    private void handlePowerConnected(Context context) {
        Utils.setPowerConnected(context, true);

        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        Log.d(TAG, "handlePowerConnected: " + hour + ":" + minute);

        int quietHour = Utils.getQuietHour(context);
        int quietMinute = Utils.getQuietMinute(context);
        Log.d(TAG, "handlePowerConnected: quietHour=" + quietHour + ", quietMinute=" + quietMinute);

        int monitoringMode = Utils.getMonitoringMode(context);
        StringBuilder message = new StringBuilder("Monitoring mode: "
                + Utils.getMonitoringModeString(context)
                + " Quiet Time Start: " + quietHour + ":" + quietMinute
                + ", current time is " + hour + ":" + minute + ". ");
        if ((monitoringMode == Utils.MONITORING_MODE_POWER_RECEIVER_WITH_QUIET_TIME &&
                (hour > quietHour || (hour == quietHour && minute >= quietMinute)))
                || monitoringMode == Utils.MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE) {
            message.append(Utils.muteVolumes(context));
        } else {
            Log.d(TAG, "handlePowerConnected: not quiet time");
            message.append(", not quiet time");
        }
        Utils.showNotification(context, "Power Connected Alerts", message.toString());
    }

    private void handlePowerDisconnected(Context context) {
        Utils.setPowerConnected(context, false);

        Log.d(TAG, "handlePowerDisconnected");
        StringBuilder message = new StringBuilder(Utils.restoreVolumes(context));
        Utils.showNotification(context, "Power Disconnected Alerts", message.toString());
    }

}
