package com.gncbrown.getmetosleep.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.gncbrown.getmetosleep.MainActivity;

public class NoopService extends Service {
    public static final String TAG = "NoopService";
    public static final String ACTION_ENQUEUE_WORKERS = "com.gncbrown.ACTION_ENQUEUE_WORKERS";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_ENQUEUE_WORKERS.equals(intent.getAction())) {
            MainActivity.enqueueChargingWorker();
            MainActivity.enqueueNotChargingWorker();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
