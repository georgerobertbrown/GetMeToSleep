package com.gncbrown.getmetosleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

import java.util.Calendar;

public class ChargingReceiver extends BroadcastReceiver {
    private static final String TAG = "ChargingReceiver";

    private static final String PREFS_NAME = "VolumePrefs";
    private static final String KEY_RINGER = "ringer";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_ALARM = "alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);
        if (intent == null) return;
        Log.d(TAG, "onReceive: " + intent.getAction());

        String action = intent.getAction();
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            handlePowerConnected(context);
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            handlePowerDisconnected(context);
        }
    }

    private void handlePowerConnected(Context context) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        Log.d(TAG, "handlePowerConnected: " + hour + ":" + minute);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int quietHour = prefs.getInt("quietHour", 23);
        int quietMinute = prefs.getInt("quietMinute", 0);
        Log.d(TAG, "handlePowerConnected: quietHour=" + quietHour + ", quietMinute=" + quietMinute);

        if (hour > quietHour || (hour == quietHour && minute >= quietMinute)) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) return;

            // Save current volumes
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_RINGER, audioManager.getStreamVolume(AudioManager.STREAM_RING));
            editor.putInt(KEY_MEDIA, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            editor.putInt(KEY_ALARM, audioManager.getStreamVolume(AudioManager.STREAM_ALARM));
            editor.apply();

            // Set to vibrate and zero volumes
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
        }
    }

    private void handlePowerDisconnected(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;
        Log.d(TAG, "handlePowerDisconnected");

        // Restore saved volumes
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int ringer = prefs.getInt(KEY_RINGER, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2);
        int media = prefs.getInt(KEY_MEDIA, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2);
        int alarm = prefs.getInt(KEY_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) / 2);

        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, ringer, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, media, 0);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarm, 0);
    }
}
