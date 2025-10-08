package com.gncbrown.getmetosleep; // Your main package

import android.app.Application;
import android.util.Log;

import com.gncbrown.getmetosleep.Utilities.FileLogger; // Adjust if FileLogger is elsewhere
import com.gncbrown.getmetosleep.Utilities.Utils;
import com.gncbrown.getmetosleep.Workers.DailyPowerTaskWorker; // Import your worker

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainApplication extends Application {
    private static final String TAG = "MainApplication";

    private String uniqueWorkName = "dailyPowerConnectWork";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate - Application STARTING"); // For easy Logcat check
        FileLogger.initialize(this);
        FileLogger.getInstance().i(TAG, "FileLogger initialized from Application.onCreate");

        // Schedule the daily task
        if (Utils.getEnableService(this))
            scheduleDailyPowerTask();
        else
            FileLogger.getInstance().w(TAG, "Daily power task not enabled in preferences.");
    }

    public void scheduleDailyPowerTask() {
        String message = "";
        Log.i(TAG, "Scheduling daily power task");
        int[] startTimes = Utils.getQuietTime(this, "start", 23, 0);

        // Calculate the delay
        Calendar calendar = Calendar.getInstance();
        long nowMillis = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, startTimes[0]);
        calendar.set(Calendar.MINUTE, startTimes[1]);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long scheduleTimeInMillis = calendar.getTimeInMillis();

        // If 11 PM today has already passed, schedule for 11 PM tomorrow
        if (scheduleTimeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            scheduleTimeInMillis = calendar.getTimeInMillis(); // Recalculate for tomorrow
        }

        long initialDelay = scheduleTimeInMillis - nowMillis;
        FileLogger.getInstance().i(TAG, "Initial delay for daily power task: " + initialDelay);

        // Create the periodic work request
        PeriodicWorkRequest dailyWorkRequest =
                new PeriodicWorkRequest.Builder(DailyPowerTaskWorker.class, 24, TimeUnit.HOURS)
                        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                        .addTag("daily_power_task") // Optional: Tag for easier management
                        .build();

        // Enqueue the work as unique periodic work
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                uniqueWorkName, // A unique name for this work
                ExistingPeriodicWorkPolicy.REPLACE, // REPLACE ensures the task is updated if re-scheduled
                dailyWorkRequest
        );

        // Convert delay to a readable format for logging (optional)
        long hours = TimeUnit.MILLISECONDS.toHours(initialDelay);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(initialDelay) % 60;
        message = String.format("Daily power task scheduled for %02d:%02d. Initial delay: %dh %dm ("
                + "%dms)", startTimes[0], startTimes[1], hours, minutes, initialDelay);
        Log.i(TAG, message);
        FileLogger.getInstance().i(TAG, message);
    }

    public void cancelDailyPowerTask() {
        // Cancel the unique periodic work by its name
        WorkManager.getInstance(getApplicationContext()).cancelUniqueWork(uniqueWorkName);

        String message = "Attempted to cancel daily power task with unique name: " + uniqueWorkName;
        Log.i(TAG, message);
        FileLogger.getInstance().i(TAG, message);
    }
}
