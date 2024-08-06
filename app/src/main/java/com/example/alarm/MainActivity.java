package com.example.alarm;

import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.alarm.R;

public class MainActivity extends AppCompatActivity {
    private int originalBackgroundColor;
    private TextView timeTextView;
    private static final long INITIAL_DELAY = 30000; // 30 seconds
    private static final long INTERVAL = 30000; // 30 seconds
    private static final long DISMISS_INTERVAL = 20000; // 20 seconds
    private Handler handler = new Handler();
    private Set<Integer> dismissedNotifications = new HashSet<>();
    private AlarmDatabaseHelper dbHelper;
    private String csvFilePath;
    private LinearLayout notificationContainer;
    private RelativeLayout mainActivityLayout;
    private int currentAlarmId = 0; // Keeps track of the current alarm ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new AlarmDatabaseHelper(this);
        csvFilePath = getExternalFilesDir(null) + "/alarm_log.csv";
        notificationContainer = findViewById(R.id.notificationContainer);
        mainActivityLayout = findViewById(R.id.main_activity_layout);
        timeTextView = findViewById(R.id.timeTextView);

        originalBackgroundColor = getThemeBackgroundColor();

        enterFullScreenMode();
        initializeCSV();
        startAlarms();
        startClock();
    }

    private void startClock() {
        final Handler clockHandler = new Handler();
        Runnable updateClock = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                timeTextView.setText(currentTime);
                clockHandler.postDelayed(this, 1000); // Update every second
            }
        };
        updateClock.run();
    }

    private int getThemeBackgroundColor() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        return typedValue.data;
    }

    private void resetBackground() {
        View mainActivityLayout = findViewById(R.id.main_activity_layout);
        mainActivityLayout.setBackgroundColor(originalBackgroundColor);
    }

    private void enterFullScreenMode(){
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void initializeCSV() {
        File csvFile = new File(csvFilePath);
        if(!csvFile.exists()){
            try (FileWriter writer = new FileWriter(csvFilePath, false)) {
                writer.append(getString(R.string.csv_header) + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void logToCSV(int alarmId, long timestampMs, String timestampFormatted, String action) {
        try (FileWriter writer = new FileWriter(csvFilePath, true)) {
            writer.append(timestampMs + "," + alarmId + "," + timestampFormatted + "," + action + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logToDatabase(int alarmId, long timestampMs, String timestampFormatted, String action) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AlarmContract.AlarmEntry.COLUMN_ALARM_ID, alarmId);
        values.put(AlarmContract.AlarmEntry.COLUMN_TIMESTAMP_MS, timestampMs);
        values.put(AlarmContract.AlarmEntry.COLUMN_TIMESTAMP, timestampFormatted);
        values.put(AlarmContract.AlarmEntry.COLUMN_ACTION, action);
        db.insert(AlarmContract.AlarmEntry.TABLE_NAME, null, values);
    }

    private void startAlarms() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 8; i++) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNotification();
                        }
                    }, i * INTERVAL);
                }
            }
        }, INITIAL_DELAY);
    }

    private void showNotification() {
        final int alarmId = ++currentAlarmId; // Increment the alarm ID
        final View notificationView = LayoutInflater.from(this).inflate(R.layout.notification, notificationContainer, false);
        Button dismissButton = notificationView.findViewById(R.id.dismiss_button);

        mainActivityLayout.setBackgroundResource(R.drawable.wallpaper);
        timeTextView.setVisibility(View.VISIBLE);

        // Log the notification shown time
        long timestampMs = System.currentTimeMillis();
        String timestampFormatted = getCurrentTimestamp();
        logToCSV(alarmId, timestampMs, timestampFormatted, getString(R.string.log_shown));
        logToDatabase(alarmId, timestampMs, timestampFormatted, getString(R.string.log_shown));

        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationView.setVisibility(View.GONE);
                // Log the dismissal time
                long timestampMs = System.currentTimeMillis();
                String timestampFormatted = getCurrentTimestamp();
                logToCSV(alarmId, timestampMs, timestampFormatted, getString(R.string.log_dismissed));
                logToDatabase(alarmId, timestampMs, timestampFormatted, getString(R.string.log_dismissed));
                // Mark this notification as dismissed by the user
                dismissedNotifications.add(alarmId);
                // Remove notification view and update container visibility
                notificationContainer.removeView(notificationView);
                updateNotificationContainerVisibility();
                resetBackground();
                timeTextView.setVisibility(View.GONE);
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!dismissedNotifications.contains(alarmId)) {
                    notificationView.setVisibility(View.GONE);
                    // Log the auto-dismissal time if not already dismissed
                    long timestampMs = System.currentTimeMillis();
                    String timestampFormatted = getCurrentTimestamp();
                    logToCSV(alarmId, timestampMs, timestampFormatted, getString(R.string.log_auto_dismissed));
                    logToDatabase(alarmId, timestampMs, timestampFormatted, getString(R.string.log_auto_dismissed));
                    // Remove notification view and update container visibility
                    notificationContainer.removeView(notificationView);
                    updateNotificationContainerVisibility();
                    resetBackground();
                    timeTextView.setVisibility(View.GONE);
                }
            }
        }, DISMISS_INTERVAL);

        notificationContainer.addView(notificationView);
        updateNotificationContainerVisibility();
    }

    private void updateNotificationContainerVisibility() {
        if (notificationContainer.getChildCount() == 0) {
            notificationContainer.setVisibility(View.GONE);
        } else {
            notificationContainer.setVisibility(View.VISIBLE);
        }
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        return sdf.format(new Date());
    }

    private static class AlarmDatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "alarmLog.db";
        private static final int DATABASE_VERSION = 1;

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + AlarmContract.AlarmEntry.TABLE_NAME + " (" +
                        AlarmContract.AlarmEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        AlarmContract.AlarmEntry.COLUMN_ALARM_ID + " INTEGER," +
                        AlarmContract.AlarmEntry.COLUMN_TIMESTAMP_MS + " INTEGER," +
                        AlarmContract.AlarmEntry.COLUMN_TIMESTAMP + " TEXT," +
                        AlarmContract.AlarmEntry.COLUMN_ACTION + " TEXT)";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + AlarmContract.AlarmEntry.TABLE_NAME;

        public AlarmDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }

    public static class AlarmContract {
        private AlarmContract() {}

        public static class AlarmEntry {
            public static final String TABLE_NAME = "alarmLog";
            public static final String _ID = "_id";
            public static final String COLUMN_ALARM_ID = "alarmId";
            public static final String COLUMN_TIMESTAMP_MS = "timestampMs";
            public static final String COLUMN_TIMESTAMP = "timestamp";
            public static final String COLUMN_ACTION = "action";
        }
    }
}
