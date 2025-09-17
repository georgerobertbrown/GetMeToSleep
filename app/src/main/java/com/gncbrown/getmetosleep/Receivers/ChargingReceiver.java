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
                + ", current time is " + hour + ":" + minute);
        if ((monitoringMode == Utils.MONITORING_MODE_POWER_RECEIVER_WITH_QUIET_TIME &&
                (hour > quietHour || (hour == quietHour && minute >= quietMinute)))
                || monitoringMode == Utils.MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) return;

            // Save current volumes
            String savedVolumes = Utils.saveVolumes(context);
            message.append(". ").append(savedVolumes);
            try {
                // Set to vibrate and zero volumes
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);

                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            } catch (Exception e) {
                message.append(", error setting ringer to vibrate; " + e.getMessage());
            }
            message.append(", volume muted");
        } else {
            Log.d(TAG, "handlePowerConnected: not quiet time");
            message.append(", not quiet time");
        }
        Utils.showNotification(context, "Power Connected Alerts", message.toString());
    }

    private void handlePowerDisconnected(Context context) {
        Utils.setPowerConnected(context, false);

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        Log.d(TAG, "handlePowerDisconnected");
        StringBuilder message = new StringBuilder("Power Disconnected. ");
        // Restore saved volumes
        int[] savedVolumes = Utils.getSavedVolumes(context);
        int ringer = savedVolumes[0];
        int media = savedVolumes[1];
        int alarm = savedVolumes[2];
        Log.d(TAG, "handlePowerDisconnected: ringer=" + ringer + ", media=" + media + ", alarm=" + alarm);

        try {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, ringer, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, media, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarm, 0);

            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            message.append("Restored ringer=" + ringer + ", media=" + media + ", alarm=" + alarm);
        } catch (Exception e) {
            message.append(", error restoring volume; " + e.getMessage());
        }
        Utils.showNotification(context, "Power Disconnected Alerts", message.toString());
    }

}
