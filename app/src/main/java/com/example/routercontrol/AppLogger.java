package com.example.routercontrol;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AppLogger {

    private static final String PREF_NAME = "task_logs";
    private static final String KEY_LOGS = "logs_list";
    private static final int MAX_LOGS = 100;   // храним максимум 100 записей

    private static final Gson gson = new Gson();

    public static void addLog(Context context, String status, String message) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LOGS, null);

        Type type = new TypeToken<ArrayList<LogEntry>>(){}.getType();
        ArrayList<LogEntry> logs = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        logs.add(0, new LogEntry(status, message));   // новая запись сверху

        // Ограничиваем количество записей
        if (logs.size() > MAX_LOGS) {
            logs = new ArrayList<>(logs.subList(0, MAX_LOGS));
        }

        String updatedJson = gson.toJson(logs);
        prefs.edit().putString(KEY_LOGS, updatedJson).apply();
    }

    public static List<LogEntry> getAllLogs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LOGS, null);

        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<ArrayList<LogEntry>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public static void clearLogs(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }
}