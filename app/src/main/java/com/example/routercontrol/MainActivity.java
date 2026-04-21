package com.example.routercontrol;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {
    public static TextInputEditText startTime;
    public static TextInputEditText endTime;
    public static View dotScheduled;
    public static TextView scheduledTime;
    public static TextView activeUntil;
    public static TextView webEnabled;
    public static View dotWebEnabled;
    public static View dotActiveUntil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnLog = findViewById(R.id.btn_view_log);
        Button btnSettings = findViewById(R.id.btn_settings);
        btnLog.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LogActivity.class));
        });
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        // Initialization of UI elements
        dotScheduled = findViewById(R.id.dotScheduled);
        scheduledTime = findViewById(R.id.tvScheduledTime);
        dotWebEnabled = findViewById(R.id.dotWebEnabled);
        dotActiveUntil = findViewById(R.id.dotActiveUntil);
        activeUntil = findViewById(R.id.tvActiveUntil);
        startTime = findViewById(R.id.etStartTime);
        endTime = findViewById(R.id.etEndTime);
        webEnabled = findViewById(R.id.tvWebEnabled);
        startTime.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 5 || RouterState.getRestrictionStartTime() == null)
                    RouterState.setRestrictionStartTime(s.toString());
            }
        });
        endTime.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 5 || RouterState.getRestrictionEndTime() == null)
                    RouterState.setRestrictionEndTime(s.toString());
            }
        });
        startTime.setOnClickListener(v -> showTimePicker(startTime, "Выберите время начала блокировки"));
        endTime.setOnClickListener(v -> showTimePicker(endTime, "Выберите время окончания блокировки"));
        initApplicationState();
    }

    @Override
    protected void onStop() {
        RouterState.setAppActive(false);
        RouterState.saveState(this);
        AppLogger.addLog(this, "SUCCESS", "onStop. Application is stopped");
        Log.d("onPause", "onStop. Application is stopped");
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RouterState.loadState(this);
        RouterState.setAppActive(true);
        AppLogger.addLog(this, "SUCCESS", "onResume. Application is resumed");
        Log.d("onResume", "onResume. Application is resumed");
    }

    @Override
    protected void onDestroy() {
        RouterState.setAppActive(false);
        RouterState.saveState(this);
        Log.d("onDestroy", "onDestroy. Application is destroyed");
        super.onDestroy();
    }

    public void startSchedule(View view) {
        AppLogger.addLog(this, "SUCCESS", "startSchedule. Requested schedule start");
        RouterState.setRestrictionPlannedTime(Objects.requireNonNull(startTime.getText()).toString());
        RouterState.setRestrictionDisableTime(Objects.requireNonNull(endTime.getText()).toString());
        RouterState.setWebShouldBeDisabled();
        TaskScheduler.scheduleTask(this, RouterState.getRestrictionPlannedTime(), true);
        requestIgnoreBatteryOptimization();
        RouterState.saveState(this);
    }

    public void cancelSchedule(View view) {
        AppLogger.addLog(this, "SUCCESS", "cancelSchedule. Requested schedule stop");
        TaskScheduler.cancelTask(getApplicationContext());
        // If restriction is applied - enable WEB
        if (RouterState.isRestrictionApplied()) {
            disableFilter(view);
        }
        RouterState.saveState(this);
    }

    public void enableFilter(View view) {
        AppLogger.addLog(this, "SUCCESS", "request to stop the WEB");
        final CountDownLatch latch = new CountDownLatch(0);
        RouterState.setCurrentOperation(RouterState.RestrictionOperations.DISABLE_WEB);
        new RouterAction(latch, this).execute(1, false);
        RouterState.saveState(this);
    }

    public void disableFilter(View view) {
        AppLogger.addLog(this, "SUCCESS", "request to start the WEB");
        final CountDownLatch latch = new CountDownLatch(0);
        RouterState.setCurrentOperation(RouterState.RestrictionOperations.ENABLE_WEB);
        new RouterAction(latch, this).execute(0, false);
        RouterState.saveState(this);
    }

    public void checkWan(View view) {
        AppLogger.addLog(this, "SUCCESS", "request to check WEB status");
        final CountDownLatch latch = new CountDownLatch(0);
        new RouterAction(latch, this).execute(0, true);
        RouterState.saveState(this);
    }

    private void initApplicationState() {
        // Get saved Router state
        RouterState.loadState(this);
        RouterState.setAppActive(true);
        if (!RouterState.getRestrictionStartTime().isEmpty())
            startTime.setText(RouterState.getRestrictionStartTime());
        if (!RouterState.getRestrictionEndTime().isEmpty())
            endTime.setText(RouterState.getRestrictionEndTime());
        String nextTime = TaskScheduler.getScheduledTime(getApplicationContext());
        if (!nextTime.isEmpty()) {
            RouterState.setRestrictionPlanned(true);
            RouterState.setRestrictionPlannedTime(nextTime);
        }
        // Init AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (!alarmManager.canScheduleExactAlarms()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Разрешение на точные будильники")
                    .setMessage("Для выполнения задачи точно в выбранное время необходимо разрешить \"Будильники и напоминания\".\n\nБез этого задача может сильно опаздывать или не выполняться.")
                    .setPositiveButton("Открыть настройки", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(intent);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    }

    private void showTimePicker(final TextInputEditText editText, String title) {
        // Получаем текущее значение из поля (если есть)
        String currentTime = editText.getText().toString();
        int hour = 22;
        int minute = 0;

        if (currentTime != null && currentTime.length() == 5) {
            try {
                String[] parts = currentTime.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception e) {
                // если формат неверный — используем значение по умолчанию
            }
        }

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)           // 24-часовой формат
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(title)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();

            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            editText.setText(formattedTime);
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private void requestIgnoreBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}