package com.gncbrown.getmetosleep.Receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.gncbrown.getmetosleep.Services.ChargingService;
import com.gncbrown.getmetosleep.Utilities.FileLogger;
import com.gncbrown.getmetosleep.Utilities.Utils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive - VERY FIRST LINE IN BootReceiver");
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "BootReceiver onReceive: Action: " + intent.getAction());
            FileLogger.getInstance().i(TAG, "Received BOOT_COMPLETED intent, action="
                    + intent.getAction());
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Log.i(TAG, "BOOT_COMPLETED received by BootReceiver.");
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
                    Log.i(TAG, "Pre-start check from BootReceiver: ChargingService component enabled state: " + stateString);
                } catch (Exception e) {
                    Log.e(TAG, "Error checking component enabled state for ChargingService in BootReceiver", e);
                }

                // Check your app's setting for whether the service should be enabled
                // This depends on how you store this preference (e.g., SharedPreferences via a Utils method)
                // For this example, let's assume Utils.getEnableService(context) reads this preference.
                // boolean shouldStartService = Utils.getEnableService(context); // You need to implement this
                boolean shouldStartService = Utils.getEnableService(context);
                Log.d(TAG, "BootReceiver: shouldStartService determined as: " + shouldStartService);
                FileLogger.getInstance().d(TAG, "BootReceiver: shouldStartService determined as: " + shouldStartService);
                if (shouldStartService) {
                    Intent serviceIntent = new Intent(context, ChargingService.class);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent);
                        } else {
                            context.startService(serviceIntent);
                        }
                        Log.d(TAG, "BOOT_COMPLETED: Call to start ChargingService completed.");
                        FileLogger.getInstance().d(TAG, "BOOT_COMPLETED: Call to start ChargingService completed.");
                    } catch (Exception e) {
                        Log.e(TAG, "BOOT_COMPLETED: Failed to start ChargingService: " + e.getMessage(), e);
                        FileLogger.getInstance().e(TAG, "BOOT_COMPLETED: Failed to start ChargingService: " + e.getMessage());
                    }
                } else {
                    Log.d(TAG, "BOOT_COMPLETED: ChargingService disabled.");
                    FileLogger.getInstance().d(TAG, "BOOT_COMPLETED: ChargingService disabled.");
                    Utils.showNotification(context, "BootReceiver", "Charging service disabled.");
                }
            } else {
                Log.w(TAG, "BootReceiver onReceive: Received action: " + intent.getAction() + ", not BOOT_COMPLETED.");
                FileLogger.getInstance().w(TAG, "BootReceiver onReceive: Received action: " + intent.getAction() + ", not BOOT_COMPLETED.");
            }
        } else {
            Log.w(TAG, "BootReceiver onReceive: Intent or action is null.");
            FileLogger.getInstance().w(TAG, "BootReceiver onReceive: Intent or action is null.");
        }
    }
}
