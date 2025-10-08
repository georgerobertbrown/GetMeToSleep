package com.gncbrown.getmetosleep.Workers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.gncbrown.getmetosleep.Utilities.FileLogger;
import com.gncbrown.getmetosleep.Utilities.Utils; // Assuming this is where handlePowerConnected is

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DailyPowerTaskWorker extends Worker {
    private static final String TAG = "DailyPowerTaskWorker";

    public DailyPowerTaskWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Daily task executing.");

        try {
            // Call your static method, passing the ApplicationContext provided by the Worker
            handlePowerConnected(getApplicationContext());

            Log.i(TAG, "Utilities.handlePowerConnected executed successfully.");
            FileLogger.getInstance().i(TAG, "doWork executed successfully.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error executing Utilities.handlePowerConnected", e);
            FileLogger.getInstance().e(TAG, "Error in DailyPowerTaskWorker", e);
            return Result.failure();
        }
    }

    private static void handlePowerConnected(Context context) {
        String message = "";
        if (isPowerConnected(context)) {
            message = "Power already connected at wakeup time. ";
            Log.d(TAG, "handlePowerConnected: " + message);

            message += Utils.muteVolumes(context);
            FileLogger.getInstance().i("PowerConnected", message);
            Utils.showNotification(context, "Power Already Connected", message);
            return;
        }
        Log.d(TAG, "handlePowerConnected: Power is not connected.");
        FileLogger.getInstance().i("PowerConnected", "Power is not connected.");

        int[] startTimes = Utils.getQuietTime(context, "start", 23, 0);
        int[] endTimes = Utils.getQuietTime(context, "end", 7, 0);
        boolean isBetween = Utils.isCurrentTimeBetween(startTimes, endTimes);
        Log.d(TAG, "handlePowerConnected: isBetween=" + isBetween);

        if (!isBetween && !Utils.getDebugMode(context)) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm", Locale.getDefault());
            Date currentDate = new Date();
            message = "Time is not between quiet times "
                    + String.format(Locale.getDefault(), "%02d:%02d", startTimes[0], startTimes[1]) + " and "
                    + String.format(Locale.getDefault(), "%02d:%02d", endTimes[0], endTimes[1]) + ".";
            FileLogger.getInstance().w(TAG, message);
            Utils.showNotification(context, "Power Connected", message);
        } else {
            message = Utils.muteVolumes(context);
            FileLogger.getInstance().i("PowerConnected", "Power Connected: " + message);
            Utils.showNotification(context, "Power Connected", message);
        }
    }

    private static boolean isPowerConnected(Context context) {
        if (context == null) {
            return false;
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = context.registerReceiver(null, intentFilter);

        if (batteryStatusIntent != null) {
            int status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            // You can also check the plug type if needed
            int chargePlug = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            boolean wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;

            // For simplicity, we consider it connected if it's charging OR if it's plugged in
            // (even if it's full and not actively "charging" but still drawing power).
            // The "isCharging" flag is generally reliable.
            // A device can be "plugged in" but not "charging" if there's an issue or if it's full.
            // A device can also be "full" and plugged in, which counts as connected.
            return isCharging || usbCharge || acCharge || wirelessCharge;
        }
        return false; // Should not happen if ACTION_BATTERY_CHANGED is a sticky broadcast
    }
}
