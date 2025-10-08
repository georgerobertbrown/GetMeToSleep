package com.gncbrown.getmetosleep;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.gncbrown.getmetosleep.Receivers.BootReceiver;
import com.gncbrown.getmetosleep.Services.ChargingService;
import com.gncbrown.getmetosleep.Utilities.DisplayTextActivity;
import com.gncbrown.getmetosleep.Utilities.FileLogger;
import com.gncbrown.getmetosleep.Utilities.LogViewerActivity;
import com.gncbrown.getmetosleep.Utilities.Utils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BootReceiver bootReceiverInstance;
    private IntentFilter bootIntentFilter;

    private TextView startTimeDisplay;
    private int savedStartHour = 23;   // default 11 PM
    private int savedStartMinute = 0;  // default 00
    private TextView endTimeDisplay;
    private int savedEndHour = 7;   // default 7 AM
    private int savedEndMinute = 0;  // default 00

    public static Context context;
    private BroadcastReceiver powerReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "MainActivity onCreate - VERY FIRST LINE - From Notification Flow?"); 
        super.onCreate(savedInstanceState);
        context = this;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FileLogger.initialize(this);
        FileLogger.getInstance().i(TAG, "App started and FileLogger initialized.");


        Switch switchEnable = findViewById(R.id.switchEnable);
        switchEnable.setChecked(Utils.getEnableService(context));
        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.setEnableService(context, isChecked);
            if (!isChecked) {
                Intent serviceIntent = new Intent(context, ChargingService.class);
                stopService(serviceIntent);
                Utils.showNotification(context, "Charging Service", "Charging service disabled.");
            } else {
                Intent serviceIntent = new Intent(context, ChargingService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Utils.showNotification(context, "Charging Service", "Charging service started.");
            }
        });

        CheckBox alwaysRestoreVolumes = findViewById(R.id.checkBoxAlwaysRestoreVolumes);
        alwaysRestoreVolumes.setChecked(Utils.getAlwaysRestoreVolumes(context));
        alwaysRestoreVolumes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.setAlwaysRestoreVolumes(context, isChecked);
        });
        CheckBox debugMode = findViewById(R.id.checkBoxDebugMode);
        debugMode.setChecked(Utils.getDebugMode(context));
        debugMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.setDebugMode(context, isChecked);
        });

        Button saveButton = findViewById(R.id.buttonSaveVolumes);
        saveButton.setOnClickListener(v -> {
            String savedVolumes = Utils.saveVolumes(context);
            Toast.makeText(context, savedVolumes, Toast.LENGTH_SHORT).show();
        });
        Button showSavedButton = findViewById(R.id.buttonShowSavedVolumes);
        showSavedButton.setOnClickListener(v -> {
            int[] savedVolumes = Utils.getSavedVolumes(context);
            String savedVolumesString = "Saved Volumes:\n" +
                    "Ringer: " + savedVolumes[0] + "\n" +
                    "Media: " + savedVolumes[1] + "\n" +
                    "Alarm: " + savedVolumes[2] + "\n";
            Utils.showDialog(context, "Saved Volumes", savedVolumesString, android.R.drawable.ic_dialog_info);
        });
        Button showTasksButton = findViewById(R.id.buttonShowTasks);
        showTasksButton.setOnClickListener(v -> {
            // IMPORTANT: getScheduledWorkTasks BLOCKS. Run it on a background thread.
            new Thread(() -> {
                final String tasks = getScheduledWorkTasks(context);
                runOnUiThread(() -> {
                    Intent displayIntent = new Intent(MainActivity.this, DisplayTextActivity.class);
                    displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_TITLE, "WorkManager Tasks");
                    displayIntent.putExtra(DisplayTextActivity.EXTRA_TEXT_CONTENT, tasks);
                    startActivity(displayIntent);
                });
            }).start();
        });
        Button killTasksButton = findViewById(R.id.buttonKillTasks);
        killTasksButton.setOnClickListener(v -> {
            MainApplication application = (MainApplication) getApplication();
            application.cancelDailyPowerTask();
            Toast.makeText(this, "Daily power task cancelled!", Toast.LENGTH_SHORT).show();
        });

        startTimeDisplay = findViewById(R.id.startTimeDisplay);
        Button setStartTimeBtn = findViewById(R.id.setStartTimeButton);
        int[] savedTimes = Utils.getQuietTime(context, "start", savedStartHour, savedStartMinute);
        savedStartHour = savedTimes[0]; 
        savedStartMinute = savedTimes[1]; 
        updateTimeDisplay(startTimeDisplay, savedStartHour, savedStartMinute, "Quiet Start Time: ");

        endTimeDisplay = findViewById(R.id.endTimeDisplay);
        Button setEndTimeBtn = findViewById(R.id.setEndTimeButton);
        savedTimes = Utils.getQuietTime(context, "end", savedEndHour, savedEndMinute);
        savedEndHour = savedTimes[0]; 
        savedEndMinute = savedTimes[1]; 
        updateTimeDisplay(endTimeDisplay, savedEndHour, savedEndMinute, "Quiet End Time: ");


        setStartTimeBtn.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(
                    MainActivity.this,
                    (TimePicker view, int hourOfDay, int minute) -> {
                        savedStartHour = hourOfDay;
                        savedStartMinute = minute;

                        Utils.setQuietTime(context, savedStartHour, savedStartMinute, "start");
                        updateTimeDisplay(startTimeDisplay, savedStartHour, savedStartMinute, "Quiet Start Time: ");
                    },
                    savedStartHour, savedStartMinute, true
            );
            picker.show();
        });

        setEndTimeBtn.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(
                    MainActivity.this,
                    (TimePicker view, int hourOfDay, int minute) -> {
                        savedEndHour = hourOfDay;
                        savedEndMinute = minute;

                        Utils.setQuietTime(context, savedEndHour, savedEndMinute, "end");
                        updateTimeDisplay(endTimeDisplay, savedEndHour, savedEndMinute, "Quiet End Time: ");
                    },
                    savedEndHour, savedEndMinute, true
            );
            picker.show();
        });

        bootReceiverInstance = new BootReceiver();
        bootIntentFilter = new IntentFilter();
        bootIntentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { 
            checkAndRequestPostNotificationsPermission(); 
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestDndPermission(); 
        }

        if (Utils.isAppBatteryOptimized(context)) {
            Utils.showNotification(context, "Battery Optimized",
                    "Battery is optimized for this app. Recommend going to Settings > Apps > See all apps > GetMeToSleep > App battery usage and set to Unrestricted.");
        }

        Utils.createNotificationChannel(context);

        Log.d(TAG, "MainActivity onCreate completed. Receiver instance created.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bootReceiverInstance != null && bootIntentFilter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { 
                registerReceiver(bootReceiverInstance, bootIntentFilter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(bootReceiverInstance, bootIntentFilter);
            }
            Log.d(TAG, "BootReceiver dynamically registered in onResume.");
        } else {
            Log.e(TAG, "Could not register BootReceiver: instance or filter is null.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bootReceiverInstance != null) {
            try {
                unregisterReceiver(bootReceiverInstance);
                Log.d(TAG, "BootReceiver dynamically unregistered in onPause.");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "BootReceiver was not registered or already unregistered.", e);
                FileLogger.getInstance().w(TAG, "BootReceiver was not registered or already unregistered.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_about) {
            Utils.showDialog(this, getResources().getString(R.string.app_name),
                    "Version " + com.gncbrown.getmetosleep.BuildConfig.VERSION_NAME, android.R.drawable.ic_dialog_info);
            return true;
        } else if (itemId == R.id.action_receivers) {
            // Run potentially slow operations on a background thread
            new Thread(() -> {
                StringBuilder finalTextBuilder = new StringBuilder();

                Intent powerConnectedIntent = new Intent(Intent.ACTION_POWER_CONNECTED);
                List<ResolveInfo> powerReceivers = Utils.getRegisteredReceivers(context, powerConnectedIntent);
                finalTextBuilder.append("Power Connected Receivers:\n");
                finalTextBuilder.append("---------------------------------\n");
                for (ResolveInfo resolveInfo : powerReceivers) {
                    if (resolveInfo.activityInfo != null) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        String className = resolveInfo.activityInfo.name;
                        finalTextBuilder.append(packageName).append("/").append(className).append("\n");
                    }
                }
                finalTextBuilder.append("\n");

                Intent powerDisconnectedIntent = new Intent(Intent.ACTION_POWER_DISCONNECTED);
                List<ResolveInfo> powerDisconnectedReceivers = Utils.getRegisteredReceivers(context, powerDisconnectedIntent);
                finalTextBuilder.append("Power Disconnected Receivers:\n");
                for (ResolveInfo resolveInfo : powerDisconnectedReceivers) {
                    if (resolveInfo.activityInfo != null) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        String className = resolveInfo.activityInfo.name;
                        finalTextBuilder.append(packageName).append("/").append(className).append("\n");
                    }
                }
                finalTextBuilder.append("\n");

                String services = Utils.getAppServices(context);
                finalTextBuilder.append(services).append("\n");

                // Using Utils.getScheduledWorkInfo which should use the corrected WorkQuery
                String workManagerTasks = getScheduledWorkTasks(context.getApplicationContext());
                finalTextBuilder.append(workManagerTasks);

                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, DisplayTextActivity.class);
                    intent.putExtra(DisplayTextActivity.EXTRA_TEXT_CONTENT, finalTextBuilder.toString());
                    intent.putExtra(DisplayTextActivity.EXTRA_TEXT_TITLE, "Receivers, Services & Tasks");
                    startActivity(intent);
                });
            }).start();
            return true;
        } else if (itemId == R.id.action_logfile) {
            Intent intent = new Intent(MainActivity.this, LogViewerActivity.class);
            startActivity(intent);
            return true;
        }  else if (itemId == R.id.action_close) {
            finish();
            return true;
        } else if (itemId == R.id.action_help) {
            Intent intent = new Intent(MainActivity.this, HelpActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("type", "help");
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_releases) {
            Intent intent = new Intent(MainActivity.this, HelpActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("type", "changeLog");
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateTimeDisplay(TextView timeDisplay, int hour, int minute, String label) {
        String formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        timeDisplay.setText(label + formatted);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void checkAndRequestPostNotificationsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "POST_NOTIFICATIONS permission already granted.");
            // The following FileLogger line might be a leftover or intended for a different context.
            // FileLogger.getInstance().i(TAG, "BootReceiver was not registered or already unregistered."); 
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            Log.i(TAG, "Showing rationale for POST_NOTIFICATIONS permission.");
            new AlertDialog.Builder(this)
                    .setTitle("Notification Permission Needed")
                    .setMessage("This app needs the Post Notification permission to show status updates and reminders. Please grant this permission to ensure full functionality.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    })
                    .setNegativeButton("Later", (dialog, which) -> {
                        dialog.dismiss();
                        Toast.makeText(this, "Post Notification permission denied. Some features might be unavailable.", Toast.LENGTH_LONG).show();
                    })
                    .create().show();
        } else {
            Log.d(TAG, "Requesting POST_NOTIFICATIONS permission directly.");
            FileLogger.getInstance().i(TAG, "Requesting POST_NOTIFICATIONS permission directly.");
            postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private final ActivityResultLauncher<String> postNotificationsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted by user.");
                    FileLogger.getInstance().i(TAG, "POST_NOTIFICATIONS permission granted by user.");
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission denied by user.");
                    FileLogger.getInstance().w(TAG, "POST_NOTIFICATIONS permission denied by user.");
                    Toast.makeText(this, "Post Notification permission denied. Some features might be unavailable.", Toast.LENGTH_LONG).show();
                }
            });

    private void checkAndRequestDndPermission() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Check for null and SDK version before calling isNotificationPolicyAccessGranted
        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            !notificationManager.isNotificationPolicyAccessGranted()) { 
            Log.i(TAG, "Do Not Disturb permission not granted. Requesting user to grant it.");
            new AlertDialog.Builder(this)
                    .setTitle("Do Not Disturb Permission Needed")
                    .setMessage("This app needs permission to modify Do Not Disturb settings to automatically silence your device during quiet hours when charging. Please grant this permission in the system settings.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not open Do Not Disturb settings", e);
                            Toast.makeText(this, "Could not open DND settings. Please find it manually.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Later", (dialog, which) -> {
                        dialog.dismiss();
                        Toast.makeText(this, "Do Not Disturb permission denied. Silencing feature will not work as expected.", Toast.LENGTH_LONG).show();
                    })
                    .create().show();
        } else {
            if (notificationManager == null) {
                Log.w(TAG, "NotificationManager service is null. Cannot check DND permission.");
            } else {
                Log.d(TAG, "Do Not Disturb permission already granted or not applicable on this Android version prior to M.");
            }
        }
    }

    /**
     * Retrieves information about scheduled WorkManager tasks.
     * WARNING: This method BLOCKS THE CALLING THREAD. Do NOT call this on the Main/UI thread.
     * It's called from a background thread in the 'Show Tasks' button's OnClickListener.
     * @param context The application context.
     * @return A string containing formatted information about scheduled tasks.
     */
    public String getScheduledWorkTasks(Context context) {
        StringBuilder sb = new StringBuilder("WorkManager Task Info:\n");
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());

        try {
            // Query for all work (tasks in any state).
            // CORRECTED LINE:
            ListenableFuture<List<WorkInfo>> futureWorkInfos = workManager.getWorkInfos(WorkQuery.fromStates(Arrays.asList(WorkInfo.State.values())));
            List<WorkInfo> workInfos = futureWorkInfos.get(); // This BLOCKS the current thread!

            if (workInfos == null || workInfos.isEmpty()) {
                sb.append("No WorkManager tasks found.\n");
                return sb.toString();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            for (WorkInfo workInfo : workInfos) {
                sb.append("---------------------------------\n");
                sb.append("ID: ").append(workInfo.getId()).append("\n");
                sb.append("State: ").append(workInfo.getState()).append("\n");
                sb.append("Run attempt count: ")
                        .append(workInfo.getRunAttemptCount())
                        .append("\n");
                sb.append("Initial delay: ")
                        .append(sdf.format(workInfo.getInitialDelayMillis()))
                        .append("\n");
                sb.append("Next scheduled time: ")
                        .append(sdf.format(workInfo.getNextScheduleTimeMillis()))
                        .append("\n");

                Set<String> tags = workInfo.getTags();
                if (tags.isEmpty()) {
                    sb.append("Tags: (none)\n");
                } else {
                    sb.append("Tags: ").append(String.join(", ", tags)).append("\n");
                }
                sb.append("\n");
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error fetching WorkManager info", e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            sb.append("Error fetching tasks: ").append(e.getMessage()).append("\n");
        } catch (Exception e) { // Catch any other unexpected exceptions
            Log.e(TAG, "Unexpected error fetching WorkManager info", e);
            sb.append("Unexpected error fetching tasks: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }
}
