package com.gncbrown.getmetosleep.Utilities;

import android.content.Context;
import android.util.Log; // Using Android's Log class for TAG consistency if desired

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {
    private static final String TAG = "FileLogger";

    private static FileLogger instance;
    private File logFile;
    private static final String LOG_DIR_NAME = "logs";
    private static final String LOG_FILE_NAME = "app_debug_log.txt";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private final Context appContext; // Store application context

    // Maximum log file size in bytes (e.g., 5MB). Set to 0 for no limit (not recommended for long term).
    private static final long MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    // Whether to backup the old log file when max size is reached, or just clear it.
    private static final boolean BACKUP_OLD_LOG_ON_ROTATE = true;
    private static final String LOG_FILE_BACKUP_NAME = "app_debug_log_old.txt";


    private FileLogger(Context context) {
        this.appContext = context.getApplicationContext(); // Use application context
        setupLogFile();
    }

    private void setupLogFile() {
        try {
            File logDir = new File(appContext.getFilesDir(), LOG_DIR_NAME);
            if (!logDir.exists()) {
                if (logDir.mkdirs()) {
                    Log.i(TAG, "Log directory created: " + logDir.getAbsolutePath());
                } else {
                    Log.e(TAG, "Failed to create log directory.");
                    // Fallback to root filesDir if logs subdir fails
                    logDir = appContext.getFilesDir();
                }
            }
            logFile = new File(logDir, LOG_FILE_NAME);
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    Log.i(TAG, "Log file created: " + logFile.getAbsolutePath());
                }
            } else {
                 Log.i(TAG, "Log file already exists: " + logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error setting up log file", e);
            // Fallback: Try to log to a file in the root internal storage if subdir fails
            try {
                logFile = new File(appContext.getFilesDir(), LOG_FILE_NAME);
                 if (!logFile.exists()) {
                    logFile.createNewFile();
                }
            } catch (IOException ex) {
                 Log.e(TAG, "Fallback log file setup also failed", ex);
                 logFile = null; // Indicate logging is not possible
            }
        }
    }


    /**
     * Initializes the FileLogger. Should be called once, typically in Application.onCreate().
     *
     * @param context The application context.
     */
    public static synchronized void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null for FileLogger initialization");
        }
        if (instance == null) {
            instance = new FileLogger(context.getApplicationContext());
            Log.i(TAG, "FileLogger initialized.");
        } else {
            Log.w(TAG, "FileLogger already initialized.");
        }
    }

    /**
     * Gets the singleton instance of the FileLogger.
     *
     * @return The FileLogger instance.
     * @throws IllegalStateException if initialize() has not been called.
     */
    public static FileLogger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FileLogger not initialized. Call FileLogger.initialize() in your Application class.");
        }
        return instance;
    }

    private void appendLog(String level, String tag, String message) {
        if (logFile == null || !logFile.canWrite()) {
            Log.e(TAG, "Log file is not available or not writable. Cannot log: [" + level + "/" + tag + "] " + message);
            return;
        }

        // Check log file size and rotate if necessary
        checkAndRotateLogFile();

        String logText = dateFormat.format(new Date()) +
                " [" + level + "]" +
                " [" + tag + "] " +
                message;

        // Also print to Android's Logcat for real-time debugging
        switch (level) {
            case "E": Log.e(tag, message); break;
            case "W": Log.w(tag, message); break;
            case "I": Log.i(tag, message); break;
            case "D": Log.d(tag, message); break;
            case "V":
            default:  Log.v(tag, message); break;
        }

        writeToFile(logText);
    }

    private synchronized void writeToFile(String text) {
        if (logFile == null) return;

        try (FileWriter fw = new FileWriter(logFile, true); // true for append mode
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(text);
            bw.newLine();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }

    private synchronized void checkAndRotateLogFile() {
        if (logFile == null || !logFile.exists() || MAX_LOG_SIZE_BYTES <= 0) {
            return;
        }

        if (logFile.length() > MAX_LOG_SIZE_BYTES) {
            Log.i(TAG, "Log file size (" + logFile.length() + " bytes) exceeds limit (" + MAX_LOG_SIZE_BYTES + " bytes). Rotating.");
            File oldLogFile = new File(logFile.getParentFile(), LOG_FILE_BACKUP_NAME);

            if (BACKUP_OLD_LOG_ON_ROTATE) {
                if (oldLogFile.exists()) {
                    if(!oldLogFile.delete()){
                        Log.w(TAG, "Could not delete old backup log file.");
                    }
                }
                if(!logFile.renameTo(oldLogFile)) {
                     Log.w(TAG, "Could not rename current log to backup.");
                } else {
                    Log.i(TAG, "Current log backed up to: " + oldLogFile.getName());
                }
            } else {
                if(!logFile.delete()) {
                     Log.w(TAG, "Could not delete current log for rotation.");
                } else {
                    Log.i(TAG, "Current log deleted due to size limit.");
                }
            }
            // Re-run setup to create a new empty log file and ensure directory structure
             setupLogFile();
        }
    }

    // Public logging methods
    public void v(String tag, String message) {
        appendLog("V", tag, message);
    }

    public void d(String tag, String message) {
        appendLog("D", tag, message);
    }

    public void i(String tag, String message) {
        appendLog("I", tag, message);
    }

    public void w(String tag, String message) {
        appendLog("W", tag, message);
    }

    public void w(String tag, String message, Throwable tr) {
        appendLog("W", tag, message + '\n' + Log.getStackTraceString(tr));
    }

    public void e(String tag, String message) {
        appendLog("E", tag, message);
    }

    public void e(String tag, String message, Throwable tr) {
        appendLog("E", tag, message + '\n' + Log.getStackTraceString(tr));
    }

    /**
     * Gets the current log file.
     * @return The log file, or null if not properly initialized.
     */
    public File getLogFile() {
        return logFile;
    }

    /**
     * Deletes the current log file and creates a new empty one.
     * Also deletes the backup log file if it exists.
     */
    public synchronized void purgeLogFile() {
        Log.i(TAG, "Purge log file requested."); // Added log
        if (logFile != null) {
            if (logFile.exists()) {
                if (logFile.delete()) {
                    Log.i(TAG, "Log file purged: " + logFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "Failed to delete log file: " + logFile.getAbsolutePath());
                }
            }
            // Attempt to delete the backup file as well
            File backupLogFile = new File(logFile.getParentFile(), LOG_FILE_BACKUP_NAME);
            if (backupLogFile.exists()) {
                if (backupLogFile.delete()) {
                    Log.i(TAG, "Backup log file purged: " + backupLogFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "Failed to delete backup log file: " + backupLogFile.getAbsolutePath());
                }
            }
            // Re-run setup to create a new empty log file and ensure directory structure
            setupLogFile();
        } else {
            Log.w(TAG, "Log file was null, cannot purge. Re-initializing setup.");
            // If logFile was null, setupLogFile will attempt to create it.
            setupLogFile(); // Ensure a new log file is ready even if the old one was null
        }
    }
}
