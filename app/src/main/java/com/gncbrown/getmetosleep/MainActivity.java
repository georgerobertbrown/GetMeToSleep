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

import com.gncbrown.getmetosleep.Receivers.BootReceiver;
import com.gncbrown.getmetosleep.Services.ChargingService;
import com.gncbrown.getmetosleep.Utilities.DisplayTextActivity;
import com.gncbrown.getmetosleep.Utilities.Utils;

import java.util.List;
import java.util.Locale;

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
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar); 
        setSupportActionBar(toolbar);

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

        startTimeDisplay = findViewById(R.id.startTimeDisplay);
        Button setStartTimeBtn = findViewById(R.id.setStartTimeButton);
        int[] savedTimes = Utils.getQuietTime(context, "start", savedStartHour, savedStartMinute);
        savedStartHour = savedTimes[0]; //Utils.getQuietHour(context, "start");
        savedStartMinute = savedTimes[1]; //Utils.getQuietMinute(context, "start");
        updateTimeDisplay(startTimeDisplay, savedStartHour, savedStartMinute, "Quiet Start Time: ");

        endTimeDisplay = findViewById(R.id.endTimeDisplay);
        Button setEndTimeBtn = findViewById(R.id.setEndTimeButton);
        savedTimes = Utils.getQuietTime(context, "end", savedEndHour, savedEndMinute);
        savedEndHour = savedTimes[0]; //Utils.getQuietHour(context, "end");
        savedEndMinute = savedTimes[1]; //Utils.getQuietMinute(context, "end");
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

            String services = Utils.getAppServices(context);

            Intent intent = new Intent(MainActivity.this, DisplayTextActivity.class);
            intent.putExtra(DisplayTextActivity.EXTRA_TEXT_CONTENT, powerReceiversString
                    .append("\n")
                    .append(powerDisconnectedReceiversString)
                    .append("\n")
                    .append(services)
                    .toString());
            intent.putExtra(DisplayTextActivity.EXTRA_TEXT_TITLE, "Receivers");
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
            });

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
}
