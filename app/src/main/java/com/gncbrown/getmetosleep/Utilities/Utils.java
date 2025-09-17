package com.gncbrown.getmetosleep.Utilities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.gncbrown.getmetosleep.MainActivity;
import com.gncbrown.getmetosleep.R;

import java.util.List;

public class Utils {
    private static final String TAG = "Utils";
    private static final String NOTIFICATION_CHANNEL_ID = "volume_muted_channel";
    private static final int NOTIFICATION_ID = 101; // Unique ID for this notification
    private static final int REQUEST_CODE_DISPLAY_TEXT_PENDING_INTENT = 1; // Unique request code

    private static final String PREFS_NAME = "VolumePrefs";
    private static final String KEY_HOUR = "quietHour";
    private static final String KEY_MINUTE = "quietMinute";
    private static final String KEY_POWER_CONNECTED = "powerConnected";

    private static final String KEY_RINGER = "ringer";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_ALARM = "alarm";

    // New preference key for monitoring mode
    private static final String KEY_MONITORING_MODE = "monitoringMode";

    // Monitoring mode constants
    public static final int MONITORING_MODE_SCHEDULER = 0;                        // Nightly scheduler, respects quiet time
    public static final int MONITORING_MODE_POWER_RECEIVER_WITH_QUIET_TIME = 1; // Power receiver, respects quiet time
    public static final int MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE = 2;  // Power receiver, immediate, ignores quiet time


    public static void showDialog(Context context, String title, String message, int iconId) {
        if (context == null) {
            Log.e(TAG, "showDialog: context is null, title: " + title + ", message: " + message);
            return;
        }
        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View dialogView = inflater.inflate(R.layout.dialog_scrollable_message, null);
            TextView messageTextView = dialogView.findViewById(R.id.scrollable_message);
            messageTextView.setText(message);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.setTitle(title);
            dialogBuilder.setIcon(iconId);
            dialogBuilder.setView(dialogView);
            Dialog dialog = dialogBuilder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "showAlertDialog: ", e);
            throw new RuntimeException(e);
        }
    }

    public static void showAlertDialog(Context context, String title, String message) {
        if (context == null) {
            Log.e(TAG, "showAlertDialog: context is null, title: " + title + ", message: " + message);
            return;
        }
        showDialog(context, title, message, android.R.drawable.ic_dialog_alert);
    }

    public static List<ResolveInfo> getRegisteredReceivers(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryBroadcastReceivers(intent, 0);
        if (resolveInfoList != null && !resolveInfoList.isEmpty()) {
            Log.i(TAG, "Found " + resolveInfoList.size() + " receiver(s) for action: " + intent.getAction());
            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (resolveInfo.activityInfo != null) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    String className = resolveInfo.activityInfo.name;
                    Log.d(TAG, "Receiver: " + packageName + "/" + className);
                }
            }
        } else {
            Log.w(TAG, "No receivers found for action: " + intent.getAction());
        }
        return resolveInfoList;
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Volume Mute Alert Channel";
            String description = "Notifications for when volume is muted while charging";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created/updated.");
            } else {
                Log.e(TAG, "NotificationManager not available for creating channel.");
            }
        }
    }

    public static void showNotification(Context context, String title, String message) {
        // Intent to launch DisplayTextActivity when the notification is tapped
        Intent displayIntent = new Intent(context, DisplayTextActivity.class);
        displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_TITLE, title);
        displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_CONTENT, message);
        // IMPORTANT: No custom flags like NEW_TASK or CLEAR_TASK here if using TaskStackBuilder for this purpose.

        // Create a TaskStackBuilder to build the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the parent activity (MainActivity) to the back stack.
        // This intent should be a simple intent to launch MainActivity.
        Intent mainActivityIntent = new Intent(context, MainActivity.class);
        mainActivityIntent.setAction(Intent.ACTION_MAIN); // Common action for main activities
        mainActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER); // Common category
        // It's often good to clear top or reorder to front if MainActivity already exists in a task.
        // However, for TaskStackBuilder, addNextIntentWithParentStack handles parent definition.
        // If MainActivity itself needs specific launch modes defined in manifest, those will be respected.

        // Create an intent for the parent stack (MainActivity)
        // This intent will point to MainActivity
        // addParentStack ensures that MainActivity is in the back stack.
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the DisplayTextActivity intent to the top of the stack
        stackBuilder.addNextIntent(displayIntent);

        // Get the PendingIntent containing the entire back stack
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                REQUEST_CODE_DISPLAY_TEXT_PENDING_INTENT, // Unique request code
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
                return;
            }
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "Notification shown with TaskStackBuilder.");
    }

    public static void setQuietTime(Context context, int hour, int minute) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_HOUR, hour);
        editor.putInt(KEY_MINUTE, minute);
        editor.apply();
    }

    public static int getQuietHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_HOUR, 23);
    }

    public static int getQuietMinute(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MINUTE, 0);
    }

    public static void setPowerConnected(Context context, boolean powerConnected) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_POWER_CONNECTED, powerConnected);
        editor.apply();
    }

    public static boolean getPowerConnected(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_POWER_CONNECTED, false);
    }

    public static void saveVolumes(Context context, int ringer, int media, int alarm) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_RINGER, ringer);
        editor.putInt(KEY_MEDIA, media);
        editor.putInt(KEY_ALARM, alarm);
        editor.apply();
    }

    public static int[] getSavedVolumes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int ringer = prefs.getInt(KEY_RINGER, 0);
        int media = prefs.getInt(KEY_MEDIA, 0);
        int alarm = prefs.getInt(KEY_ALARM, 0);
        return new int[]{ringer, media, alarm};
    }

    // --- Monitoring Mode Preferences ---
    public static void setMonitoringMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_MONITORING_MODE, mode);
        editor.apply();
    }

    public static int getMonitoringMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Default to MONITORING_MODE_SCHEDULER if no preference is set
        return prefs.getInt(KEY_MONITORING_MODE, MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE);
    }

    public static String getMonitoringModeString(Context context) {
        int mode = getMonitoringMode(context);
        switch (mode) {
            case MONITORING_MODE_SCHEDULER:
                return "Scheduler";
            case MONITORING_MODE_POWER_RECEIVER_WITH_QUIET_TIME:
                return "Power Receiver with Quiet Time";
            case MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE:
                return "Power Receiver Only (Immediate)";
            default:
                return "Unknown";
        }
    }

    public static String saveVolumes(Context context) {
        String message = "Save volumes";
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return message;

        int ringer = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        int media = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int alarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        message += ": ringer=" + ringer + ", media=" + media + ", alarm=" + alarm;
        Log.d(TAG, "saveVolumes: ringer=" + ringer + ", media=" + media + ", alarm=" + alarm);

        // Save current volumes
        Utils.saveVolumes(context, ringer, media, alarm);
        return message;
    }
}
