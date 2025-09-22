package com.gncbrown.getmetosleep.Services;

import android.app.Notification;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChargingService extends Service {
    private static final String TAG = "ChargingService";

    public static final String NOTIFICATION_CHANNEL_ID = "manage_volume_channel";

    private BroadcastReceiver powerReceiver;

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate - VERY FIRST LINE IN SERVICE"); 
        super.onCreate();

        Log.d(TAG, "onCreate: Initializing ChargingService...");

        Utils.createNotificationChannel(this); // Ensure channel is created first

        try {
            Log.d(TAG, "onCreate: Attempting to call startForeground...");
            Notification notification = buildNotification();
            if (notification == null) {
                Log.e(TAG, "onCreate: buildNotification() returned null!");
                stopSelf(); 
                return; 
            }
            startForeground(1, notification);
            Log.d(TAG, "onCreate: Successfully called startForeground.");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: EXCEPTION during startForeground call!", e);
            stopSelf(); 
            return;
        }

        Log.d(TAG, "onCreate: Proceeding to register powerReceiver.");

        powerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) { 
                if (intent == null || intent.getAction() == null) return;

                int[] startTimes = Utils.getQuietTime(context, "start", 23, 0);
                int[] endTimes = Utils.getQuietTime(context, "end", 7, 0);
                boolean isBetween = Utils.isCurrentTimeBetween(startTimes, endTimes);
                Log.d(TAG, "onReceive: action=" + intent.getAction() + ", isBetween=" + isBetween);

                String message = "";
                if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
                    // Always want to restore volumes if charging is disconnected
                    message = Utils.restoreVolumes(ChargingService.this);
                    Utils.showNotification(ChargingService.this, "Not Charging", message);
                } else if (!isBetween) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm", Locale.getDefault());
                    Date currentDate = new Date();
                    message = "Current time: " + sdf.format(currentDate) + " is not between quiet times "
                            + String.format(Locale.getDefault(), "%02d:%02d", startTimes[0], startTimes[1]) + " and "
                            + String.format(Locale.getDefault(), "%02d:%02d", endTimes[0], endTimes[1]) + ".";
                    Utils.showNotification(ChargingService.this, "ChargingService", message);
                } else if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                    message = Utils.muteVolumes(ChargingService.this);
                    Utils.showNotification(ChargingService.this, "Charging", message);
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand - Service is starting/restarting.");
        Log.d(TAG, "onStartCommand: Intent action: " + (intent != null ? intent.getAction() : "null intent"));
        Log.d(TAG, "onStartCommand: Flags: " + flags);
        Log.d(TAG, "onStartCommand: Start ID: " + startId);
        return START_STICKY;
    }

    // Removed createNotificationChannelIfNeeded() from here as it's called by Utils in onCreate

    private Notification buildNotification() {
        // Utils.createNotificationChannel(this) is now solely responsible for channel creation in onCreate.

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
        Log.d(TAG, "onDestroy: ChargingService being destroyed."); 
        super.onDestroy();
        try {
            if (powerReceiver != null) { 
                unregisterReceiver(powerReceiver);
                Log.d(TAG, "onDestroy: PowerReceiver unregistered.");
            }
        } catch (IllegalArgumentException e) { 
            Log.w(TAG, "onDestroy: PowerReceiver was not registered or already unregistered.", e);
        } catch (Exception e) {
            Log.e(TAG, "onDestroy: Exception during unregisterReceiver.", e);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called, returning null."); 
        return null;
    }
}
