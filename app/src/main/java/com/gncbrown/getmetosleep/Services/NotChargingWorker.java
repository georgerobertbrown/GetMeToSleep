package com.gncbrown.getmetosleep.Services;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.gncbrown.getmetosleep.Utilities.Utils;

import java.util.concurrent.TimeUnit;

public class NotChargingWorker extends Worker {
    private static final String TAG = "NotChargingWorker";

    public NotChargingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d(TAG, "NotChargingWorker created.");
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        boolean powerConnected = Utils.getPowerConnected(context);
        if (!powerConnected) return Result.success();
        Utils.setPowerConnected(context, false);

        String message = Utils.restoreVolumes(context);
        Utils.showNotification(context, "Not Charging", message);

        // Re-enqueue a future ChargingWorker to handle next unplug event (delay to avoid loops)
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(false)
                .build();

        OneTimeWorkRequest chargingRequest = new OneTimeWorkRequest.Builder(ChargingWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueue(chargingRequest);
        Log.d(TAG, "NotChargingWorker re-enqueued ChargingWorker.");

        return Result.success();
    }
}
