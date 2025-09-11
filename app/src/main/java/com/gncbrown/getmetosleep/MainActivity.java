package com.gncbrown.getmetosleep;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.app.TimePickerDialog;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "VolumePrefs";
    private static final String KEY_HOUR = "quietHour";
    private static final String KEY_MINUTE = "quietMinute";

    private TextView timeDisplay;
    private int savedHour = 23;   // default 11 PM
    private int savedMinute = 0;  // default 00

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeDisplay = findViewById(R.id.timeDisplay);
        Button setTimeBtn = findViewById(R.id.setTimeButton);

        // Load saved time
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        savedHour = prefs.getInt(KEY_HOUR, 23);
        savedMinute = prefs.getInt(KEY_MINUTE, 0);

        updateTimeDisplay();

        setTimeBtn.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(
                    MainActivity.this,
                    (TimePicker view, int hourOfDay, int minute) -> {
                        savedHour = hourOfDay;
                        savedMinute = minute;

                        // Save to prefs
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(KEY_HOUR, savedHour);
                        editor.putInt(KEY_MINUTE, savedMinute);
                        editor.apply();

                        updateTimeDisplay();
                    },
                    savedHour, savedMinute, true
            );
            picker.show();
        });
    }

    private void updateTimeDisplay() {
        String formatted = String.format(Locale.getDefault(), "%02d:%02d", savedHour, savedMinute);
        timeDisplay.setText("Quiet Time Start: " + formatted);
    }
}
