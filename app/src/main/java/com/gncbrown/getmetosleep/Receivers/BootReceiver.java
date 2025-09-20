package com.gncbrown.getmetosleep.Receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.gncbrown.getmetosleep.MainActivity;
import com.gncbrown.getmetosleep.Services.ChargingService;
import com.gncbrown.getmetosleep.Services.NoopService;
import com.gncbrown.getmetosleep.Utilities.Utils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            int chargingMode = Utils.getMonitoringMode(context);
            Log.d(TAG, "Boot completed. Charging mode: " + Utils.getMonitoringModeString(context));
            if (chargingMode == Utils.MONITORING_MODE_BROADCAST_RECEIVER) {
                try {
                    ComponentName serviceComponent = new ComponentName(context, ChargingService.class);
                    PackageManager pm = context.getPackageManager();
                    int componentEnabledState = pm.getComponentEnabledSetting(serviceComponent);
                    String stateString = "UNKNOWN (" + componentEnabledState + ")";
                    switch (componentEnabledState) {
                        case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
                            stateString = "DEFAULT (manifest)";
                            break;
                        case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                            stateString = "ENABLED";
                            break;
                        case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                            stateString = "DISABLED";
                            break;
                        case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                            stateString = "DISABLED_USER";
                            break;
                        case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED:
                            stateString = "DISABLED_UNTIL_USED";
                            break;
                    }
                    Log.i(TAG, "Pre-start check: ChargingService component enabled state: " + stateString);
                } catch (Exception e) {
                    Log.e(TAG, "Error checking component enabled state for ChargingService", e);
                }

                Intent serviceIntent = new Intent(context, ChargingService.class);
                // For Android O and above, startForegroundService is required
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }
                } catch (Exception e) {
                    Log.e("BootReceiver", "Failed to start ChargingService: " + e.getMessage());
                }
            } else {
                // Re-enqueue initial workers after boot
                if (context instanceof MainActivity) {
                    MainActivity.enqueueChargingWorker();
                    MainActivity.enqueueNotChargingWorker();
                } else {
                    // Fallback: start a service to trigger enqueuing
                    Intent i = new Intent(context, NoopService.class);
                    i.setAction(NoopService.ACTION_ENQUEUE_WORKERS);
                    context.startService(i);
                }
            }
        }


//        if (intent != null && (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
//                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()))) {
//            Log.d(TAG, "Boot completed or quickboot. Rescheduling nightly check.");
//            NightlyScheduler.scheduleNextNightlyCheck(context);
//        }
    }
}