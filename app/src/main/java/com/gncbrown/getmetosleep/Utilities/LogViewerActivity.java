package com.gncbrown.getmetosleep.Utilities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView; // Import SearchView
import androidx.appcompat.widget.Toolbar;

import com.gncbrown.getmetosleep.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale; // For case-insensitive filtering

public class LogViewerActivity extends AppCompatActivity {

    private static final String TAG = "LogViewerActivity";
    private TextView logContentTextView;
    private boolean sortNewestFirst = true;
    private String currentFilterQuery = null; // To store the active filter query

    // Define log level tags for parsing
    private static final String ERROR_TAG = "[E]";
    private static final String WARNING_TAG = "[W]";
    private static final String INFO_TAG = "[I]";
    private static final String DEBUG_TAG = "[D]";
    private static final String VERBOSE_TAG = "[V]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        Toolbar toolbar = findViewById(R.id.log_viewer_toolbar);
        setSupportActionBar(toolbar);
        updateActivityTitle();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        logContentTextView = findViewById(R.id.log_content_textview);
        loadLogFile();
    }

    private void updateActivityTitle() {
        if (getSupportActionBar() != null) {
            String title = "App Logs";
            if (sortNewestFirst) {
                title += " (Newest First)";
            } else {
                title += " (Oldest First)";
            }
            if (currentFilterQuery != null && !currentFilterQuery.isEmpty()) {
                title += " [Filtered]";
            }
            getSupportActionBar().setTitle(title);
        }
    }

    private void loadLogFile() {
        FileLogger fileLogger = null;
        try {
            fileLogger = FileLogger.getInstance();
        } catch (IllegalStateException e) {
            Log.e(TAG, "FileLogger not initialized", e);
            logContentTextView.setText("Error: FileLogger not initialized. Initialize it in your Application class.");
            return;
        }

        File logFile = fileLogger.getLogFile();
        SpannableStringBuilder spannableLog = new SpannableStringBuilder();

        if (logFile != null && logFile.exists() && logFile.length() > 0) {
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Apply filter before adding to the list for processing
                    if (currentFilterQuery == null || currentFilterQuery.isEmpty() ||
                        line.toLowerCase(Locale.getDefault()).contains(currentFilterQuery.toLowerCase(Locale.getDefault()))) {
                        lines.add(line);
                    }
                }

                if (!lines.isEmpty()) {
                    if (sortNewestFirst) {
                        Collections.reverse(lines);
                    }

                    int defaultColor = logContentTextView.getCurrentTextColor();

                    for (String currentLine : lines) {
                        int color = defaultColor;
                        if (currentLine.contains(ERROR_TAG)) {
                            color = Color.RED;
                        } else if (currentLine.contains(WARNING_TAG)) {
                            color = Color.YELLOW; //Color.rgb(255, 165, 0); // Orange
                        } else if (currentLine.contains(INFO_TAG)) {
                            color = Color.GREEN;
                        } else if (currentLine.contains(DEBUG_TAG)) {
                            color = Color.BLUE;
                        } else if (currentLine.contains(VERBOSE_TAG)) {
                            //color = Color.BLACK; //Color.GRAY;
                        }

                        int start = spannableLog.length();
                        spannableLog.append(currentLine).append("\n");
                        int end = spannableLog.length();
                        spannableLog.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    logContentTextView.setText(spannableLog);
                } else {
                    if (currentFilterQuery != null && !currentFilterQuery.isEmpty()) {
                        logContentTextView.setText("No log entries match the filter: \"" + currentFilterQuery + "\"");
                    } else {
                        logContentTextView.setText(getString(R.string.log_file_not_found));
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading log file", e);
                logContentTextView.setText(getString(R.string.error_reading_log_file));
            }
        } else {
             if (currentFilterQuery != null && !currentFilterQuery.isEmpty()) {
                logContentTextView.setText("Log file empty or not found. Cannot apply filter.");
            } else {
                logContentTextView.setText(getString(R.string.log_file_not_found));
            }
        }
        updateActivityTitle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log_viewer_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_filter_logs);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setQueryHint("Filter logs...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // User pressed search button (or enter)
                    currentFilterQuery = query;
                    loadLogFile();
                    searchView.clearFocus(); // Hide keyboard
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Text has changed, apply filter dynamically or on submit
                    // For dynamic filtering (as user types):
                    currentFilterQuery = newText;
                    loadLogFile();
                    return true;
                }
            });

            // Handle the SearchView closing (e.g., user presses X button)
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true; // Return true to allow expansion
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    // SearchView is closing
                    if (currentFilterQuery != null && !currentFilterQuery.isEmpty()) {
                        currentFilterQuery = null; // Clear the filter
                        loadLogFile(); // Reload logs without filter
                    }
                    return true; // Return true to allow collapse
                }
            });

            // Restore filter if it was active
            if (currentFilterQuery != null && !currentFilterQuery.isEmpty()) {
                searchItem.expandActionView();
                searchView.setQuery(currentFilterQuery, true); // true to submit immediately (and filter)
                searchView.clearFocus();
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_sort_newest_first) {
            if (!sortNewestFirst) {
                sortNewestFirst = true;
                loadLogFile();
            }
            return true;
        } else if (itemId == R.id.action_sort_oldest_first) {
            if (sortNewestFirst) {
                sortNewestFirst = false;
                loadLogFile();
            }
            return true;
        } else if (itemId == R.id.action_purge_log) {
            showConfirmPurgeDialog();
            return true;
        }
        // R.id.action_filter_logs is handled by SearchView directly
        return super.onOptionsItemSelected(item);
    }

    private void showConfirmPurgeDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.confirm_purge_log_title))
                .setMessage(getString(R.string.confirm_purge_log_message))
                .setPositiveButton(getString(R.string.action_purge), (dialog, which) -> performPurgeLog())
                .setNegativeButton(getString(R.string.action_cancel), null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performPurgeLog() {
        try {
            FileLogger.getInstance().purgeLogFile();
            Toast.makeText(this, getString(R.string.log_purged_message), Toast.LENGTH_SHORT).show();
            loadLogFile();
        } catch (IllegalStateException e) {
            Log.e(TAG, "FileLogger not initialized, cannot purge.", e);
            Toast.makeText(this, "Error: FileLogger not available.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error during log purge operation.", e);
            Toast.makeText(this, "Error purging log file.", Toast.LENGTH_LONG).show();
        }
    }
}
