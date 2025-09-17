package com.gncbrown.getmetosleep.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.gncbrown.getmetosleep.Schedulers.NightlyScheduler;
import com.gncbrown.getmetosleep.Utilities.Utils; // Your existing Utils for notifications

public class NightlyPowerCheckReceiver extends BroadcastReceiver {

    private static final String TAG = "NightlyPowerCheck";
    public static final String ACTION_NIGHTLY_POWER_CHECK = "com.gncbrown.getmetosleep.ACTION_NIGHTLY_POWER_CHECK";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e(TAG, "Context or Intent is null, cannot proceed.");
            return;
        }

        if (ACTION_NIGHTLY_POWER_CHECK.equals(intent.getAction())) {
            Log.d(TAG, "Nightly power check triggered at 11:00 PM.");

            // Check if power is connected
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            // We need to register a null receiver to get the sticky broadcast for battery status.
            // This is a one-time check, so registering and unregistering is fine here.
            // Alternatively, if this receiver is also registered for battery events in the manifest,
            // that might simplify, but this direct check is also reliable for a point-in-time query.
            Intent batteryStatusIntent = context.registerReceiver(null, ifilter);

            if (batteryStatusIntent == null) {
                Log.e(TAG, "Failed to get battery status intent.");
                Utils.showNotification(context, "Nightly Power Check", "Could not determine power status.");
                return;
            }

            // Are we charging / charged?
            int status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            // How are we charging?
            int chargePlug = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            boolean wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;


            String message;
            if (isCharging) {
                String chargeType = "Unknown";
                if (usbCharge) chargeType = "USB";
                else if (acCharge) chargeType = "AC";
                else if (wirelessCharge) chargeType = "Wireless";
                message = "Power is connected (Charging via " + chargeType + "). Status: " + getStatusString(status);
                Log.d(TAG, message);
            } else {
                message = "Power is NOT connected at 11:00 PM. Status: " + getStatusString(status);
                Log.d(TAG, message);
                // You might want to take specific action here if power is NOT connected,
                // e.g., remind the user, or trigger your existing ChargingReceiver logic
                // if it's supposed to mute volumes when power connects *during quiet hours*.
            }

            // Show a notification with the result (optional)
            Utils.showNotification(context, "Nightly Power Check", message);

            // IMPORTANT: Reschedule for the next day if using inexact repeating alarms
            // or if it's a one-time alarm that needs to be manually set again.
            // If using AlarmManager.setRepeating, this might not be needed unless exactness drift is a concern.
            // For setExactAndAllowWhileIdle, you MUST reschedule it manually.
            NightlyScheduler.scheduleNextNightlyCheck(context); // Reschedule for the next day

        }
    }

    private String getStatusString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not Charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default: return "Unknown";
        }
    }
}
