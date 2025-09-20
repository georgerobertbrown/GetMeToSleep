package com.gncbrown.getmetosleep;

import static androidx.core.app.ServiceCompat.startForeground;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.gncbrown.getmetosleep.Receivers.BootReceiver;
import com.gncbrown.getmetosleep.Receivers.ChargingReceiver;
import com.gncbrown.getmetosleep.Schedulers.NightlyScheduler;
import com.gncbrown.getmetosleep.Services.ChargingService;
import com.gncbrown.getmetosleep.Services.ChargingWorker;
import com.gncbrown.getmetosleep.Services.NotChargingWorker;
import com.gncbrown.getmetosleep.Utilities.DisplayTextActivity;
import com.gncbrown.getmetosleep.Utilities.Utils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BootReceiver bootReceiverInstance;
    private IntentFilter bootIntentFilter;
    private ChargingReceiver chargingReceiverInstance;
    private IntentFilter powerIntentFilter;

    private TextView timeDisplay;
    private int savedHour = 23;   // default 11 PM
    private int savedMinute = 0;  // default 00

    public static Context context;
    private BroadcastReceiver powerReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "MainActivity onCreate - VERY FIRST LINE - From Notification Flow?"); // ADDED THIS LINE
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main); // Redundant line removed
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar); // Or androidx.appcompat.widget.Toolbar
        setSupportActionBar(toolbar);

        int chargingMode = Utils.getMonitoringMode(context);
        RadioButton radioButtonUseScheduler = findViewById(R.id.radioButtonUseScheduler);
        radioButtonUseScheduler.setChecked(chargingMode == Utils.MONITORING_MODE_SCHEDULER);
        radioButtonUseScheduler.setOnClickListener(v -> {
            Utils.setMonitoringMode(context, Utils.MONITORING_MODE_SCHEDULER);
        });
        RadioButton radioButtonUseReceiver = findViewById(R.id.radioButtonUseReceiver);
        radioButtonUseReceiver.setChecked(chargingMode == Utils.MONITORING_MODE_POWER_RECEIVER_WITH_QUIET_TIME);
        radioButtonUseReceiver.setOnClickListener(v -> {
            Utils.setMonitoringMode(context, Utils.MONITORING_MODE_POWER_RECEIVER_WITH_QUIET_TIME);
        });
        RadioButton radioButtonUseReceiverOnly = findViewById(R.id.radioButtonUseReceiverOnly);
        radioButtonUseReceiverOnly.setChecked(chargingMode == Utils.MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE);
        radioButtonUseReceiverOnly.setOnClickListener(v -> {
            Utils.setMonitoringMode(context, Utils.MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE);
        });
        RadioButton radioButtonUseWorker = findViewById(R.id.radioButtonUseWorker);
        radioButtonUseWorker.setChecked(chargingMode == Utils.MONITORING_MODE_WORKER);
        radioButtonUseWorker.setOnClickListener(v -> {
            Utils.setMonitoringMode(context, Utils.MONITORING_MODE_WORKER);
        });
        RadioButton radioButtonUseBroadcastReceiver = findViewById(R.id.radioButtonUseBroadcastReceiver);
        radioButtonUseBroadcastReceiver.setChecked(chargingMode == Utils.MONITORING_MODE_BROADCAST_RECEIVER);
        radioButtonUseBroadcastReceiver.setOnClickListener(v -> {
            Utils.setMonitoringMode(context, Utils.MONITORING_MODE_BROADCAST_RECEIVER);
            try {
                ComponentName serviceComponent = new ComponentName(context, ChargingService.class);
                PackageManager pm = getPackageManager();
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
                    "Alarm: " + savedVolumes[2];
            Utils.showDialog(context, "Saved Volumes", savedVolumesString, android.R.drawable.ic_dialog_info);
        });

        timeDisplay = findViewById(R.id.timeDisplay);
        Button setTimeBtn = findViewById(R.id.setTimeButton);

        // Load saved time
        savedHour = Utils.getQuietHour(context);
        savedMinute = Utils.getQuietMinute(context);

        updateTimeDisplay();

        setTimeBtn.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(
                    MainActivity.this,
                    (TimePicker view, int hourOfDay, int minute) -> {
                        savedHour = hourOfDay;
                        savedMinute = minute;

                        // Save to prefs
                        Utils.setQuietTime(context, savedHour, savedMinute);
                        updateTimeDisplay();

                        NightlyScheduler.scheduleNextNightlyCheck(getApplicationContext());
                    },
                    savedHour, savedMinute, true
            );
            picker.show();
        });

        // Prepare for dynamic registration
        Log.d(TAG, "MainActivity onCreate. Charging mode: " + Utils.getMonitoringModeString(context));
        if (chargingMode == Utils.MONITORING_MODE_POWER_RECEIVER_ONLY_IMMEDIATE) {
            chargingReceiverInstance = new ChargingReceiver();
            powerIntentFilter = new IntentFilter();
            powerIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            powerIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        }

        bootReceiverInstance = new BootReceiver();
        bootIntentFilter = new IntentFilter();
        bootIntentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);

        // Permissions Chain
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            checkAndRequestPostNotificationsPermission(); // Chains to Exact Alarm, then to DND
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31, 32
            checkAndRequestScheduleExactAlarmPermission(); // Chains to DND
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23 (M) to API 30 (R)
            checkAndRequestDndPermission(); // Check DND directly
        }

        Utils.createNotificationChannel(context);

        Log.d(TAG, "MainActivity onCreate completed. Receiver instance created.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        if (chargingReceiverInstance != null && powerIntentFilter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { 
                registerReceiver(chargingReceiverInstance, powerIntentFilter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(chargingReceiverInstance, powerIntentFilter);
            }
            Log.d(TAG, "ChargingReceiver dynamically registered in onResume.");
        } else {
            Log.e(TAG, "Could not register ChargingReceiver: instance or filter is null.");
        }
         */

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
        /*
        if (chargingReceiverInstance != null) {
            try {
                unregisterReceiver(chargingReceiverInstance);
                Log.d(TAG, "ChargingReceiver dynamically unregistered in onPause.");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "ChargingReceiver was not registered or already unregistered.", e);
            }
        }
         */
        if (bootReceiverInstance != null) {
            try {
                unregisterReceiver(bootReceiverInstance);
                Log.d(TAG, "BootReceiver dynamically unregistered in onPause.");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "BootReceiver was not registered or already unregistered.", e);
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
            Intent powerConnectedIntent = new Intent(Intent.ACTION_POWER_CONNECTED);
            List<ResolveInfo> powerReceivers =  Utils.getRegisteredReceivers(context, powerConnectedIntent);
            StringBuilder powerReceiversString = new StringBuilder();
            powerReceiversString.append("Power Connected Receivers:\n");
            for (ResolveInfo resolveInfo : powerReceivers) {
                if (resolveInfo.activityInfo != null) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    String className = resolveInfo.activityInfo.name;
                    powerReceiversString.append(packageName).append("/").append(className).append("\n");
                }
            }

            Intent powerDisconnectedIntent = new Intent(Intent.ACTION_POWER_DISCONNECTED);
            List<ResolveInfo> powerDisconnectedReceivers =  Utils.getRegisteredReceivers(context, powerDisconnectedIntent);
            StringBuilder powerDisconnectedReceiversString = new StringBuilder();
            powerDisconnectedReceiversString.append("Power Disconnected Receivers:\n");
            for (ResolveInfo resolveInfo : powerDisconnectedReceivers) {
                if (resolveInfo.activityInfo != null) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    String className = resolveInfo.activityInfo.name;
                    powerDisconnectedReceiversString.append(packageName).append("/").append(className).append("\n");
                }
            }

            Intent intent = new Intent(MainActivity.this, DisplayTextActivity.class);
            intent.putExtra(DisplayTextActivity.EXTRA_TEXT_CONTENT, powerReceiversString
                    .append("\n")
                    .append(powerDisconnectedReceiversString)
                    .toString());
            intent.putExtra(DisplayTextActivity.EXTRA_TEXT_TITLE, "Receivers");
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_cancel_scheduler) {
            NightlyScheduler.cancelNightlyCheck(context);
            Toast.makeText(context, "Scheduler cancelled", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateTimeDisplay() {
        String formatted = String.format(Locale.getDefault(), "%02d:%02d", savedHour, savedMinute);
        timeDisplay.setText("Quiet Time Start: " + formatted);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void checkAndRequestPostNotificationsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "POST_NOTIFICATIONS permission already granted.");
            checkAndRequestScheduleExactAlarmPermission(); 
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
                        checkAndRequestScheduleExactAlarmPermission(); 
                    })
                    .create().show();
        } else {
            Log.d(TAG, "Requesting POST_NOTIFICATIONS permission directly.");
            postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private final ActivityResultLauncher<String> postNotificationsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted by user.");
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission denied by user.");
                    Toast.makeText(this, "Post Notification permission denied. Some features might be unavailable.", Toast.LENGTH_LONG).show();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // For TIRAMISU and S, S_V2
                     checkAndRequestScheduleExactAlarmPermission();
                }
            });

    @RequiresApi(api = Build.VERSION_CODES.S) 
    private void checkAndRequestScheduleExactAlarmPermission() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
            Log.i(TAG, "SCHEDULE_EXACT_ALARM permission not granted. Requesting user to grant it.");
            new AlertDialog.Builder(this)
                    .setTitle("Exact Alarm Permission Needed")
                    .setMessage("This app uses exact alarms to schedule nightly checks for power connection status reliably. Please grant the 'Alarms & reminders' permission for this app in the system settings.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Could not open SCHEDULE_EXACT_ALARM settings", e);
                            Toast.makeText(this, "Could not open settings. Please enable 'Alarms & reminders' manually.", Toast.LENGTH_LONG).show();
                        }
                        // Note: DND check will happen when user returns and resumes app, or on next app start.
                        // Or we can call it after the negative button.
                    })
                    .setNegativeButton("Later", (dialog, which) -> {
                        dialog.dismiss();
                        Toast.makeText(this, "Exact alarm permission denied. Nightly checks may not function as expected.", Toast.LENGTH_LONG).show();
                        checkAndRequestDndPermission(); // Proceed to DND check
                    })
                    .create().show();
        } else {
            if (alarmManager == null) {
                 Log.w(TAG, "AlarmManager service is null. Cannot check SCHEDULE_EXACT_ALARM permission.");
            } else {
                 Log.d(TAG, "SCHEDULE_EXACT_ALARM permission already granted or not applicable on this Android version prior to S.");
            }
            // Proceed to DND check if applicable for this OS version
            checkAndRequestDndPermission();
        }
    }

    private void checkAndRequestDndPermission() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && !notificationManager.isNotificationPolicyAccessGranted()) {
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

    public static void enqueueChargingWorker() {
        Log.d(TAG, "enqueueChargingWorker");
        Constraints chargingConstraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        OneTimeWorkRequest chargingRequest = new OneTimeWorkRequest.Builder(ChargingWorker.class)
                .setConstraints(chargingConstraints)
                // add a small delay to avoid immediate re-execution loops
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueue(chargingRequest);
    }

    public static void enqueueNotChargingWorker() {
        Log.d(TAG, "enqueueNotChargingWorker");
        Constraints notChargingConstraints = new Constraints.Builder()
                .setRequiresCharging(false)
                .build();

        OneTimeWorkRequest notChargingRequest = new OneTimeWorkRequest.Builder(NotChargingWorker.class)
                .setConstraints(notChargingConstraints)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(context).enqueue(notChargingRequest);
    }
}
