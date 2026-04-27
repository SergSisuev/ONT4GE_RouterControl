package com.example.routercontrol;

import static com.example.routercontrol.MainActivity.activeUntil;
import static com.example.routercontrol.MainActivity.dotActiveUntil;
import static com.example.routercontrol.MainActivity.dotScheduled;
import static com.example.routercontrol.MainActivity.dotWebEnabled;
import static com.example.routercontrol.MainActivity.scheduledTime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRules;
import java.util.Date;
import java.util.Objects;

class RouterState {
    private static final String PREF_NAME = "RouterStatePrefs";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static boolean restrictionPlanned = false;
    private static boolean restrictionApplied = false;
    private static String restrictionStartTime = "";
    private static String restrictionEndTime = "";
    private static LocalDateTime restrictionPlannedTime;
    private static LocalDateTime restrictionDisableTime;
    private static String name = "admin";
    private static String password = "";
    private static String mainHttpAddress = "http://192.168.1.1";
    private static int taskRepeatPeriod = 30;
    private static boolean appActive = true;
    private static int operationStatus; // 0 - waiting, 1 - started, 2 - success, 3 - failure
    private static RestrictionOperations nextRestictionOperation;
    private static RestrictionOperations currentOperation;
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    enum RestrictionOperations {
        DISABLE_WEB,
        ENABLE_WEB
    }

    static void setCurrentOperation(RestrictionOperations currentOperation) {
        RouterState.currentOperation = currentOperation;
        // Set operationStatus to started
        operationStatus = 1;
        updateIndicatorsPanel();
    }

    static RestrictionOperations getCurrentOperation() {
        return currentOperation;
    }

    static void setOperationStatus(int operationStatus) {
        RouterState.operationStatus = operationStatus;
        if (operationStatus == 2 && currentOperation == RestrictionOperations.ENABLE_WEB)
            restrictionApplied = false;
        if (operationStatus == 2 && currentOperation == RestrictionOperations.DISABLE_WEB)
            restrictionApplied = true;
        updateIndicatorsPanel();
    }

    static void setAppActive(boolean appActive) {
        RouterState.appActive = appActive;
        if (RouterState.appActive)
            updateIndicatorsPanel();
    }

    static String getMainHttpAddress() {
        return mainHttpAddress;
    }

    static void setMainHttpAddress(String mainHttpAddress) {
        RouterState.mainHttpAddress = mainHttpAddress;
    }

    static String getName() {
        return name;
    }

    static void setName(String name) {
        RouterState.name = name;
    }

    static String getPassword() {
        return password;
    }

    static void setPassword(String password) {
        RouterState.password = password;
    }

    static void setRestrictionPlanned(boolean restrictionPlanned) {
        RouterState.restrictionPlanned = restrictionPlanned;
        if (!restrictionPlanned) {
            RouterState.restrictionPlannedTime = null;
            RouterState.restrictionDisableTime = null;
        }
        updateIndicatorsPanel();
    }

    static void setRestrictionApplied(boolean restrictionAppliedValue) {
        restrictionApplied = restrictionAppliedValue;
        updateIndicatorsPanel();
    }

    static boolean isRestrictionApplied() {
        return restrictionApplied;
    }

    static String getRestrictionStartTime() {
        return restrictionStartTime;
    }

    static void setRestrictionStartTime(String restrictionStartTime) {
        RouterState.restrictionStartTime = restrictionStartTime;
    }

    static String getRestrictionEndTime() {
        return restrictionEndTime;
    }

    static void setRestrictionEndTime(String restrictionEndTime) {
        RouterState.restrictionEndTime = restrictionEndTime;
    }

    static Date getRestrictionPlannedTime() {
        ZonedDateTime zdt = restrictionPlannedTime.atZone(ZoneId.systemDefault());
        Date output = Date.from(zdt.toInstant());
        return output;
    }

    static void setRestrictionPlannedTime(String restrictionPlannedTime) {
        RouterState.restrictionPlannedTime = getLocalTimeFromString(restrictionPlannedTime, true);
        // Set nextRestictionOperation
        nextRestictionOperation = RestrictionOperations.DISABLE_WEB;
        updateIndicatorsPanel();
    }

    static void setRestrictionDisableTime(String restrictionDisableTime) {
        RouterState.restrictionDisableTime = getLocalTimeFromString(restrictionDisableTime, true);
        updateIndicatorsPanel();
    }

    static void setWebShouldBeDisabled() {
        nextRestictionOperation = RestrictionOperations.DISABLE_WEB;
    }

    static boolean isWebShouldBeEnabled() {
        return (nextRestictionOperation == RestrictionOperations.ENABLE_WEB);
    }

    static RestrictionOperations getNextRestictionOperation() {
        return nextRestictionOperation;
    }

    static void setTaskRepeatPeriod(int taskRepeatPeriod) {
        RouterState.taskRepeatPeriod = taskRepeatPeriod;
    }

    static int getTaskRepeatPeriod() {
        return RouterState.taskRepeatPeriod;
    }

    static void saveState(Context appContext) {
        try {
            Log.d("saveState", "saveState. Save state executed");
            if (context == null) context = appContext;
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("restrictionApplied", restrictionApplied);
            editor.putBoolean("restrictionPlanned", restrictionPlanned);
            editor.putString("restrictionStartTime", restrictionStartTime);
            editor.putString("restrictionEndTime", restrictionEndTime);
            editor.putString("restrictionPlannedTime", dateTimeToString(restrictionPlannedTime));
            editor.putString("restrictionDisableTime", dateTimeToString(restrictionDisableTime));
            editor.putString("name", name);
            editor.putString("password", password);
            editor.putString("mainHttpAddress", mainHttpAddress);
            editor.putInt("taskRepeatPeriod", taskRepeatPeriod);
            editor.putString("nextRestictionOperation", String.valueOf(nextRestictionOperation));
            editor.apply(); // асинхронно и быстрее, чем commit()
        } catch (Exception e) {
            Log.e("saveState", "Failed to save current state: ", e);
        }
    }

    static void loadState(Context appContext) {
        try {
            context = appContext;
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            restrictionApplied = prefs.getBoolean("restrictionApplied", false);
            restrictionPlanned = prefs.getBoolean("restrictionPlanned", false);
            restrictionStartTime = prefs.getString("restrictionStartTime", "");
            restrictionEndTime = prefs.getString("restrictionEndTime", "");
            restrictionPlannedTime = stringToDateTime(prefs.getString("restrictionPlannedTime", null));
            restrictionDisableTime = stringToDateTime(prefs.getString("restrictionDisableTime", null));
            name = prefs.getString("name", "");
            password = prefs.getString("password", "");
            mainHttpAddress = prefs.getString("mainHttpAddress", "");
            taskRepeatPeriod = prefs.getInt("taskRepeatPeriod", 30);
            nextRestictionOperation = RestrictionOperations.valueOf(prefs.getString("nextRestictionOperation", ""));
            Log.d("loadState", "loadState. LoadState executed: " + password);
        } catch (Exception e) {
            Log.e("loadState", "Failed to load saved state: ", e);
        }
    }

    private static String dateTimeToString(LocalDateTime dateTime) {
        return (dateTime == null) ? null : dateTime.format(FORMATTER);
    }

    private static LocalDateTime stringToDateTime(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            return LocalDateTime.parse(str, FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getTimeFromLocalDate(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return localDateTime.format(formatter);
    }

    private static LocalDateTime getLocalTimeFromString(String plannedTime, boolean addDay) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZoneRules rules = zoneId.getRules();
        Instant now = Instant.now();
        ZoneOffset offset = rules.getOffset(now);
        LocalDateTime currentDate = java.time.LocalDateTime.now();
        String[] endTimeParts = Objects.requireNonNull(plannedTime.split(":"));
        LocalDateTime endDateTime = LocalDateTime.of(currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth(),
                Integer.parseInt(endTimeParts[0]), Integer.parseInt(endTimeParts[1]), 0);
        if (System.currentTimeMillis() > endDateTime.toInstant(offset).toEpochMilli() && addDay) {
            endDateTime = endDateTime.plusDays(1);
        }
        return endDateTime;
    }

    static void CalculateNextPlannedTime(boolean Repeat) {
        if (Repeat) {
            // Repeat previous operation
            nextRestictionOperation = currentOperation;
        } else {
            nextRestictionOperation = currentOperation == RestrictionOperations.ENABLE_WEB ?
                    RestrictionOperations.DISABLE_WEB : RestrictionOperations.ENABLE_WEB;
        }
        LocalDateTime currentDate = LocalDateTime.now();
        Log.d("CalculateNextPlannedTime", "Restriction disable time: " + restrictionDisableTime.toString());
        LocalDateTime nextPlannedDate = restrictionDisableTime;
        if (Repeat) {
            nextPlannedDate = currentDate.plusMinutes(taskRepeatPeriod);
        }
        // Check if restriction should be finished
        if (restrictionDisableTime.isBefore(currentDate) || restrictionDisableTime.isEqual(currentDate)) {
            nextRestictionOperation = RestrictionOperations.ENABLE_WEB;
        }
        restrictionPlannedTime = nextPlannedDate;
        Log.d("CalculateNextPlannedTime", "Restriction restrictionPlannedTime:" + restrictionPlannedTime.toString());
        Log.d("CalculateNextPlannedTime", "nextRestictionOperation:" + nextRestictionOperation);
        updateIndicatorsPanel();
    }

    private static void updateIndicatorsPanel() {
        Log.d("updateIndicatorsPanel", "updateIndicatorsPanel executed: " + appActive);
        if (appActive && dotScheduled != null) {
            if (restrictionPlanned && nextRestictionOperation == RestrictionOperations.DISABLE_WEB) {
                dotScheduled.setBackgroundResource(R.drawable.circle_yellow);
                scheduledTime.setText(getTimeFromLocalDate(restrictionPlannedTime));
            } else {
                dotScheduled.setBackgroundResource(R.drawable.circle_gray);
                scheduledTime.setText("");
            }
            if (!restrictionPlanned) {
                activeUntil.setText("");
                dotActiveUntil.setBackgroundResource(R.drawable.circle_gray);
            }
            if (!restrictionApplied) {
                dotWebEnabled.setBackgroundResource(R.drawable.circle_green);
                MainActivity.webEnabled.setText("да");
            } else {
                dotWebEnabled.setBackgroundResource(R.drawable.circle_orange);
                MainActivity.webEnabled.setText("нет");
            }
            if (restrictionDisableTime != null && restrictionApplied) {
                activeUntil.setText(getTimeFromLocalDate(restrictionDisableTime));
                dotActiveUntil.setBackgroundResource(R.drawable.circle_yellow);
            } else {
                activeUntil.setText("");
                dotActiveUntil.setBackgroundResource(R.drawable.circle_gray);
            }
            if (operationStatus == 1 && currentOperation == RestrictionOperations.ENABLE_WEB) {
                dotWebEnabled.setBackgroundResource(R.drawable.circle_yellow);
                MainActivity.webEnabled.setText("включение");
            }
            if (operationStatus == 1 && currentOperation == RestrictionOperations.DISABLE_WEB) {
                dotWebEnabled.setBackgroundResource(R.drawable.circle_yellow);
                MainActivity.webEnabled.setText("выключение");
            }
        }
    }
}
