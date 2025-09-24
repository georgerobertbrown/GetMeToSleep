package com.gncbrown.getmetosleep; // Your main package

import android.app.Application;
import android.util.Log;

import com.gncbrown.getmetosleep.Utilities.FileLogger; // Adjust if FileLogger is elsewhere

public class MainApplication extends Application {
    private static final String TAG = "MainApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate - Application STARTING"); // For easy Logcat check
        FileLogger.initialize(this);
        FileLogger.getInstance().i(TAG, "FileLogger initialized from Application.onCreate");
    }
}
