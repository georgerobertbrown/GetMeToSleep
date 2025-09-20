package com.gncbrown.getmetosleep.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.gncbrown.getmetosleep.Utilities.DisplayTextActivity;
import com.gncbrown.getmetosleep.Utilities.Utils;

public class ChargingService extends Service {
    private static final String TAG = "ChargingService";

    public static final String NOTIFICATION_CHANNEL_ID = "manage_volume_channel";


    private BroadcastReceiver powerReceiver;

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate - VERY FIRST LINE IN SERVICE"); 
        super.onCreate();

        Log.d(TAG, "onCreate: Initializing ChargingService...");

        //Utils.createNotificationChannel(this);
        createNotificationChannelIfNeeded();

        try {
            Log.d(TAG, "onCreate: Attempting to call startForeground...");
            Notification notification = buildNotification();
            if (notification == null) {
                Log.e(TAG, "onCreate: buildNotification() returned null!");
                // Consider stopping the service if the notification is essential
                stopSelf(); 
                return; 
            }
            startForeground(1, notification);
            Log.d(TAG, "onCreate: Successfully called startForeground.");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: EXCEPTION during startForeground call!", e);
            // This catch block might not always catch ForegroundServiceStartNotAllowedException
            // on newer Android versions if the conditions for startForeground aren't met,
            // as those often lead to an ANR or direct process termination.
            stopSelf(); // Stop the service if startForeground fails critically
            return;
        }

        Log.d(TAG, "onCreate: Proceeding to register powerReceiver.");

        powerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                Log.d(TAG, "onReceive: action=" + intent.getAction());

                if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                    Utils.setPowerConnected(ChargingService.this, true);
                    String message = Utils.muteVolumes(ChargingService.this);
                    Utils.showNotification(ChargingService.this, "Charging", message);
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
                    Utils.setPowerConnected(ChargingService.this, false);
                    String message = Utils.restoreVolumes(ChargingService.this);
                    Utils.showNotification(ChargingService.this, "Not Charging", message);
                } else {
                    Log.w(TAG, "Unknown intent action: " + intent.getAction());
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(powerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(powerReceiver, filter);
        }
        Log.d(TAG, "onCreate: PowerReceiver registered.");
        Log.d(TAG, "onCreate: ChargingService initialization complete.");
    }

    private void createNotificationChannelIfNeeded() { // This method seems redundant if Utils.createNotificationChannel is used
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Charging Monitor", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        // Utils.createNotificationChannel(this) should be called before this, e.g., in onCreate
        // createNotificationChannelIfNeeded(); // Redundant if called from onCreate via Utils

        Intent displayIntent = new Intent(this, DisplayTextActivity.class);
        displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_TITLE, "Charging Service Status");
        displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_CONTENT,
                "This service is actively monitoring your device\'s power connection to manage audio settings. " +
                "It runs in the foreground to ensure reliability.");

        PendingIntent resultPendingIntent = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(displayIntent)
                .getPendingIntent(
                        (int) System.currentTimeMillis(),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Charging Monitor")
                .setContentText("Monitoring power connection state. Tap for details.")
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(false) 
                .setOngoing(true) 
                .build();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ChargingService being destroyed."); // Added log
        super.onDestroy();
        try {
            if (powerReceiver != null) { // Check if receiver was registered
                unregisterReceiver(powerReceiver);
                Log.d(TAG, "onDestroy: PowerReceiver unregistered.");
            }
        } catch (IllegalArgumentException e) { // More specific exception catch
            Log.w(TAG, "onDestroy: PowerReceiver was not registered or already unregistered.", e);
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Exception during unregisterReceiver.", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called, returning null."); // Added log
        return null;
    }
}
