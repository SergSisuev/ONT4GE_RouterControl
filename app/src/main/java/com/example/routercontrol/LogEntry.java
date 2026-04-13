package com.example.routercontrol;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogEntry {
    private final long timestamp;
    private final String status;      // "SUCCESS", "FAILED", "STARTED"
    private final String message;

    public LogEntry(String status, String message) {
        this.timestamp = System.currentTimeMillis();
        this.status = status;
        this.message = message;
    }

    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}