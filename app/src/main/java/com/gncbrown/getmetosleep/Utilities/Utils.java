package com.gncbrown.getmetosleep.Utilities;

import android.Manifest;
import android.app.ActivityManager; // Added import
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Utils {
    private static final String TAG = "Utils";
    public static final String NOTIFICATION_CHANNEL_ID = "manage_volume_channel";
    private static final int NOTIFICATION_ID = 101; // Unique ID for this notification
    private static final int REQUEST_CODE_DISPLAY_TEXT_PENDING_INTENT = 1; // Unique request code

    private static final String PREFS_NAME = "VolumePrefs";
    private static final String KEY_HOUR = "quietHour";
    private static final String KEY_MINUTE = "quietMinute";
    private static final String KEY_ENABLE_SERVICE = "enableService";

    private static final String KEY_RINGER = "ringer";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_ALARM = "alarm";


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
        Intent displayIntent = new Intent(context, DisplayTextActivity.class);
        displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_TITLE, title);
        displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_CONTENT, message);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(displayIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                REQUEST_CODE_DISPLAY_TEXT_PENDING_INTENT, 
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

    public static void setQuietTime(Context context, int hour, int minute, String label) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_HOUR+"-" + label, hour);
        editor.putInt(KEY_MINUTE+"_"+label, minute);
        editor.apply();
    }

    public static int[] getQuietTime(Context context, String label, int defaultHour, int defaultMinute) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int hour = prefs.getInt(KEY_HOUR+"_" + label, defaultHour);
        int minute = prefs.getInt(KEY_MINUTE+"_" + label, defaultMinute);
        return new int[]{hour, minute};
    }

    public static void setEnableService(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ENABLE_SERVICE, enabled);
        editor.apply();
    }

    public static boolean getEnableService(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLE_SERVICE, false);
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

    public static String saveVolumes(Context context) {
        String message = "Save volumes";
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return message;

        int ringer = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        int media = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int alarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        message += ": ringer=" + ringer + ", media=" + media + ", alarm=" + alarm;
        Log.d(TAG, "saveVolumes: ringer=" + ringer + ", media=" + media);

        Utils.saveVolumes(context, ringer, media, alarm);
        return message;
    }

    public static String restoreVolumes(Context context) {
        StringBuilder message = new StringBuilder("Restore volumes: ");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            message.append("audioManager is null");
            return message.toString();
        }

        int[] savedVolumes = Utils.getSavedVolumes(context);
        int ringer = savedVolumes[0];
        int media = savedVolumes[1];
        int alarm = savedVolumes[2];
        Log.d(TAG, "restoreVolumes: ringer=" + ringer + ", media=" + media + ", alarm=" + alarm + ", alarm=" + alarm);

        try {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, ringer, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, media, 0);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            message.append("ringer=" + ringer + ", media=" + media);
        } catch (Exception e) {
            message.append("error restoring volume; " + e.getMessage());
        }
        return message.toString();
    }

    public static String muteVolumes(Context context) {
        StringBuilder message = new StringBuilder("Mute volumes: ");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            message.append("audioManager is null");
            return message.toString();
        }

        String savedVolumes = Utils.saveVolumes(context);
        message.append(savedVolumes);
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        } catch (Exception e) {
            message.append(", error setting ringer to vibrate; " + e.getMessage());
        }
        message.append(", volume muted");
        return message.toString();
    }

    public static String getAppServices(Context context) {
        StringBuffer message = new StringBuffer("Services:\n");
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();

        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = null;
        if (manager != null) {
            try {
                runningServices = manager.getRunningServices(Integer.MAX_VALUE);
            } catch (Exception e) {
                Log.e(TAG, "Could not get running services list", e);
            }
        }

        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            if (packageInfo.services != null && packageInfo.services.length > 0) {
                Log.i(TAG, "Services declared in this app (" + packageName + "):");
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    String serviceName = serviceInfo.name;
                    message.append("Service Name: ").append(serviceName);

                    boolean isRunning = false;
                    if (runningServices != null) {
                        for (ActivityManager.RunningServiceInfo runningService : runningServices) {
                            if (serviceName.equals(runningService.service.getClassName())) {
                                isRunning = true;
                                break;
                            }
                        }
                    }
                    message.append("  Foreground service type: ").append(foregroundServiceTypeToString(serviceInfo.getForegroundServiceType())).append("\n");
                    message.append("  Running: ").append(isRunning).append("\n");
                    message.append("  Enabled: ").append(serviceInfo.enabled).append("\n");
                    message.append("  Exported: ").append(serviceInfo.exported).append("\n");
                    if (serviceInfo.permission != null) {
                        message.append("  Permission: ").append(serviceInfo.permission).append("\n");
                    }
                    message.append("\n");
                }
            } else {
                Log.i(TAG, "No services declared in this app.");
                message.append("No services declared in this app.\n");
            }
        } catch (PackageManager.NameNotFoundException e) {
            message.append("Could not find package info for ").append(packageName).append(": ").append(e.getMessage()).append("\n");
            Log.e(TAG, "Could not find package info for " + packageName, e);
        }
        return message.toString();
    }

    public static String foregroundServiceTypeToString(int foregroundServiceType) {
        if (foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE) {
            return "NONE";
        }
        if (foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST) {
            return "MANIFEST_DERIVED_OR_DEFAULT";
        }

        List<String> types = new ArrayList<>();

        if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) != 0) {
            types.add("DATA_SYNC");
        }
        if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0) {
            types.add("MEDIA_PLAYBACK");
        }
        if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL) != 0) {
            types.add("PHONE_CALL");
        }
        if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) != 0) {
            types.add("LOCATION");
        }
        if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE) != 0) {
            types.add("CONNECTED_DEVICE");
        }
        if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) != 0) {
            types.add("MEDIA_PROJECTION");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA) != 0) {
                types.add("CAMERA");
            }
            if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) != 0) {
                types.add("MICROPHONE");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { 
            if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE) != 0) {
                types.add("SHORT_SERVICE");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { 
            if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH) != 0) {
                types.add("HEALTH");
            }
            if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING) != 0) {
                types.add("REMOTE_MESSAGING");
            }
            if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED) != 0) {
                types.add("SYSTEM_EXEMPTED");
            }
            if ((foregroundServiceType & ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) != 0) {
                types.add("SPECIAL_USE");
            }
        }

        if (types.isEmpty()) {
            return "UNKNOWN_TYPE_OR_COMBINATION (" + foregroundServiceType + ")";
        }

        return String.join(" | ", types);
    }

    public static boolean isCurrentTimeBetween(int[] startTime, int[] endTime) {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        int startTimeInMinutes = startTime[0] * 60 + startTime[1];
        int endTimeInMinutes = endTime[0] * 60 + endTime[1];
        int currentTimeInMinutes = currentHour * 60 + currentMinute;

        if (startTimeInMinutes <= endTimeInMinutes) {
            return currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes;
        } else {
            return currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutes;
        }
    }
}
