package com.gncbrown.getmetosleep.Schedulers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.gncbrown.getmetosleep.Receivers.NightlyPowerCheckReceiver;
import com.gncbrown.getmetosleep.Utilities.Utils;

import java.util.Calendar;

public class NightlyScheduler {

    private static final String TAG = "NightlyScheduler";
    private static final int NIGHTLY_CHECK_REQUEST_CODE = 1001; // Unique request code

    public static void scheduleNextNightlyCheck(Context context) {
        String message = "";
        if (Utils.getMonitoringMode(context) != Utils.MONITORING_MODE_SCHEDULER) {
            message = "Scheduler is not selected. Cannot schedule nightly check.";
            Log.d(TAG, message);
            Utils.showNotification(context, "NightlyScheduler", message);
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            message = "AlarmManager is null. Cannot schedule nightly check.";
            Log.e(TAG, message);
            Utils.showNotification(context, "NightlyScheduler", message);
            return;
        }

        Intent intent = new Intent(context, NightlyPowerCheckReceiver.class);
        intent.setAction(NightlyPowerCheckReceiver.ACTION_NIGHTLY_POWER_CHECK);

        // Use FLAG_IMMUTABLE for PendingIntents targeting Android 12 (API 31) and above
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                NIGHTLY_CHECK_REQUEST_CODE,
                intent,
                pendingIntentFlags
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int quietHour = Utils.getQuietHour(context);
        int quietMinute = Utils.getQuietMinute(context);
        Log.d(TAG, "Scheduling for quiet time: " + quietHour + ":" + quietMinute);
        calendar.set(Calendar.HOUR_OF_DAY, quietHour); // 11 PM
        calendar.set(Calendar.MINUTE, quietMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time today has already passed, schedule for 11 PM tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            message = "Time today has passed, scheduling for tomorrow.";
            Log.d(TAG, message);
            Utils.showNotification(context, "NightlyScheduler", message);
        }

        Log.d(TAG, "Scheduling nightly power check for: " + calendar.getTime().toString());

        // Use setExactAndAllowWhileIdle for precision, but be mindful of battery.
        // This requires SCHEDULE_EXACT_ALARM permission for Android 12+
        // and may still be subject to Doze mode restrictions (though "allowWhileIdle" helps).
        // For less critical tasks, setInexactRepeating or WorkManager would be better.
        // Since this is a once-a-day check, setExactAndAllowWhileIdle is often acceptable.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            message = "Cannot schedule exact alarms. App needs SCHEDULE_EXACT_ALARM permission or user to grant it.";
            Log.w(TAG, message);
        } else {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set( // Less exact for older versions
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
                message = "Nightly check alarm successfully set.";
                Log.d(TAG, message);
            } catch (SecurityException se) {
                message = "SecurityException: Cannot schedule exact alarm. Check SCHEDULE_EXACT_ALARM permission.";
                Log.e(TAG, message, se);
            }
            Utils.showNotification(context, "NightlyScheduler", message);
        }
    }

    public static void cancelNightlyCheck(Context context) {
        String message = "";
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            message = "AlarmManager is null. Cannot cancel nightly check.";
            Log.e(TAG, message);
            Utils.showNotification(context, "NightlyScheduler", message);
            return;
        }
        Intent intent = new Intent(context, NightlyPowerCheckReceiver.class);
        intent.setAction(NightlyPowerCheckReceiver.ACTION_NIGHTLY_POWER_CHECK);

        int pendingIntentFlags = PendingIntent.FLAG_NO_CREATE; // Important: use NO_CREATE to only get existing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                NIGHTLY_CHECK_REQUEST_CODE,
                intent,
                pendingIntentFlags
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel(); // Also cancel the PendingIntent itself
            message = "Nightly check alarm successfully canceled.";
            Log.d(TAG, message);
        } else {
            message = "No nightly check alarm was found to cancel.";
            Log.d(TAG, message);
        }

        Utils.showNotification(context, "NightlyScheduler", message);
    }
}
