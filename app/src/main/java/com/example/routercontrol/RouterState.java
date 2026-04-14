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
    private static boolean webEnabled = true;
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

    static void setOperationStatus(int operationStatus) {
        RouterState.operationStatus = operationStatus;
        if (operationStatus == 2 && currentOperation == RestrictionOperations.ENABLE_WEB)
            webEnabled = true;
        if (operationStatus == 2 && currentOperation == RestrictionOperations.DISABLE_WEB)
            webEnabled = false;
        saveState();
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
        saveState();
    }

    static RestrictionOperations nextRestictionOperation;
    static RestrictionOperations currentOperation;
    static int operationStatus; // 0 - waiting, 1 - started, 2 - success, 3 - failure

    static String getName() {
        return name;
    }

    static void setName(String name) {
        RouterState.name = name;
        saveState();
    }

    static String getPassword() {
        return password;
    }
    static void setPassword(String password) {
        RouterState.password = password;
        saveState();
    }

    static void setRestrictionPlanned(boolean restrictionPlanned) {
        RouterState.restrictionPlanned = restrictionPlanned;
        saveState();
        updateIndicatorsPanel();
    }

    static void setRestrictionApplied(boolean restrictionAppliedValue) {
        restrictionApplied = restrictionAppliedValue;
        saveState();
        updateIndicatorsPanel();
    }

    static String getRestrictionStartTime() {
        return restrictionStartTime;
    }

    static void setRestrictionStartTime(String restrictionStartTime) {
        RouterState.restrictionStartTime = restrictionStartTime;
        saveState();
    }

    static String getRestrictionEndTime() {
        return restrictionEndTime;
    }

    static void setRestrictionEndTime(String restrictionEndTime) {
        RouterState.restrictionEndTime = restrictionEndTime;
        saveState();
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
        saveState();
        updateIndicatorsPanel();
    }

    static void setRestrictionDisableTime(String restrictionDisableTime) {
        RouterState.restrictionDisableTime = getLocalTimeFromString(restrictionDisableTime, true);
        saveState();
        updateIndicatorsPanel();
    }

    static void setWebShouldBeDisabled() {
        nextRestictionOperation = RestrictionOperations.DISABLE_WEB;
    }

    static boolean isWebShouldBeEnabled() {
        return (nextRestictionOperation == RestrictionOperations.ENABLE_WEB);
    }

    static void setTaskRepeatPeriod(int taskRepeatPeriod) {
        RouterState.taskRepeatPeriod = taskRepeatPeriod;
        saveState();
    }

    static int getTaskRepeatPeriod() {
        return RouterState.taskRepeatPeriod;
    }

    static void setRouterContext(Context appContext) {
        context = appContext;
    }

    private static void saveState() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("webEnabled", webEnabled);
        editor.putBoolean("restrictionPlanned", restrictionPlanned);
        editor.putString("restrictionStartTime", restrictionStartTime);
        editor.putString("restrictionEndTime", restrictionEndTime);
        editor.putString("restrictionPlannedTime", dateTimeToString(restrictionPlannedTime));
        editor.putString("restrictionDisableTime", dateTimeToString(restrictionDisableTime));
        editor.putString("name", name);
        editor.putString("password", password);
        editor.putString("mainHttpAddress", mainHttpAddress);
        editor.putInt("taskRepeatPeriod", taskRepeatPeriod);
        editor.apply(); // асинхронно и быстрее, чем commit()
    }

    static void loadState(Context appContext) {
        context = appContext;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        webEnabled = prefs.getBoolean("webEnabled", false);
        restrictionPlanned = prefs.getBoolean("restrictionPlanned", false);
        restrictionStartTime = prefs.getString("restrictionStartTime", "");
        restrictionEndTime = prefs.getString("restrictionEndTime", "");
        restrictionPlannedTime = stringToDateTime(prefs.getString("restrictionPlannedTime", null));
        restrictionDisableTime = stringToDateTime(prefs.getString("restrictionDisableTime", null));
        name = prefs.getString("name", "");
        password = prefs.getString("password", "");
        mainHttpAddress = prefs.getString("mainHttpAddress", "");
        taskRepeatPeriod = prefs.getInt("taskRepeatPeriod", 30);
        Log.d("loadState", "loadState. name value: " + name);
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
        ZoneId zoneId = ZoneId.systemDefault ( );
        ZoneRules rules = zoneId.getRules ( );
        Instant now = Instant.now ( );
        ZoneOffset offset = rules.getOffset ( now );
        LocalDateTime currentDate = java.time.LocalDateTime.now();
        String[] endTimeParts = Objects.requireNonNull(plannedTime.split(":"));
        LocalDateTime endDateTime = LocalDateTime.of(currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth(),
                Integer.parseInt(endTimeParts[0]), Integer.parseInt(endTimeParts[1]), 0);
        if (System.currentTimeMillis() > endDateTime.toInstant(offset).toEpochMilli() && addDay) {
            endDateTime = endDateTime.plusDays(1);
        }
        return endDateTime;
    }

    static void CalculateNextPlannedTime() {
        LocalDateTime nextPlannedDate = restrictionPlannedTime.plusMinutes(taskRepeatPeriod);
        Log.d("CalculateNextPlannedTime", "Restriction plannded time:" + restrictionPlannedTime.toString());
        Log.d("CalculateNextPlannedTime", "Restriction disable time:" + restrictionDisableTime.toString());
        Log.d("CalculateNextPlannedTime", "Restriction next planned time:" + nextPlannedDate.toString());
        // Check if restriction should be finished
        if (nextPlannedDate.isAfter(restrictionDisableTime) || nextPlannedDate.isEqual(restrictionDisableTime)) {
            nextPlannedDate = restrictionDisableTime;
            nextRestictionOperation = RestrictionOperations.ENABLE_WEB;
        } else {
            nextRestictionOperation = RestrictionOperations.DISABLE_WEB;
        }
        restrictionPlannedTime = nextPlannedDate;
        Log.d("CalculateNextPlannedTime", "Restriction restrictionPlannedTime:" + restrictionPlannedTime.toString());
        Log.d("CalculateNextPlannedTime", "nextRestictionOperation:" + nextRestictionOperation);
        updateIndicatorsPanel();
    }

    private static void updateIndicatorsPanel() {
        Log.d("updateIndicatorsPanel", "updateIndicatorsPanel executed");
        if (appActive && dotScheduled != null) {
            if (restrictionPlanned)
                dotScheduled.setBackgroundResource(R.drawable.circle_yellow);
            else {
                dotScheduled.setBackgroundResource(R.drawable.circle_gray);
                dotActiveUntil.setBackgroundResource(R.drawable.circle_gray);
            }
            if (webEnabled) {
                dotWebEnabled.setBackgroundResource(R.drawable.circle_green);
                MainActivity.webEnabled.setText("да");
            } else {
                dotWebEnabled.setBackgroundResource(R.drawable.circle_orange);
                MainActivity.webEnabled.setText("нет");
            }
            if (restrictionApplied && nextRestictionOperation == RestrictionOperations.ENABLE_WEB)
                dotActiveUntil.setBackgroundResource(R.drawable.circle_yellow);
            else
                dotActiveUntil.setBackgroundResource(R.drawable.circle_gray);
            if (restrictionPlannedTime != null && restrictionPlanned) {
                scheduledTime.setText(getTimeFromLocalDate(restrictionPlannedTime));
            } else {
                scheduledTime.setText("");
            }
            if (restrictionDisableTime != null && restrictionPlanned) {
                activeUntil.setText(getTimeFromLocalDate(restrictionDisableTime));
            } else {
                activeUntil.setText("");
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
