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

public class ChargingWorker extends Worker {
    private static final String TAG = "ChargingWorker";

    public ChargingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d(TAG, "ChargingWorker created.");
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        boolean powerConnected = Utils.getPowerConnected(context);
        if (powerConnected) return Result.success();
        Utils.setPowerConnected(context, true);

        String message = Utils.muteVolumes(context);
        Utils.showNotification(context, "Charging", message);

        // Re-enqueue a future ChargingWorker to handle next charging event (delay to avoid loops)
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        OneTimeWorkRequest notChargingRequest = new OneTimeWorkRequest.Builder(NotChargingWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueue(notChargingRequest);
        Log.d(TAG, "ChargingWorker re-enqueued NotChargingWorker.");

        return Result.success();
    }
}
