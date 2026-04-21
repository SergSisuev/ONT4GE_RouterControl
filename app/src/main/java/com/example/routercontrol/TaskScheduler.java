package com.example.routercontrol;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {

    private static final String UNIQUE_WORK_NAME = "routercontrol_background_task";

    /**
     * The method creates the task for the given time
     */
    public static void scheduleTask(Context context, Date planningTime, boolean showNotification) {
        try {
            WorkManager workManager = WorkManager.getInstance(context);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(planningTime);

            Log.d("scheduleDailyTask", "The task has been scheduled to:" + calendar.getTime());
            AppLogger.addLog(context, "SUCCESS", "The task has been scheduled to: " +
                    calendar.getTime());
            AppLogger.addLog(context, "SUCCESS", "Is WEB should be enabled after this operation: " +
                    RouterState.isWebShouldBeEnabled());

            long delayInMillis = calendar.getTimeInMillis() - System.currentTimeMillis();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TaskWorker.class)
                    .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                    .build();

            workManager.enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
            );
            RouterState.setRestrictionPlanned(true);
            if (showNotification)
                Toast.makeText(context, "Задача запланирована на " + calendar.getTime(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("scheduleTask", "scheduleTask. Task schedule failed", e);
            AppLogger.addLog(context, "FAILED", "scheduleTask. Task schedule failed: " +
                    e.getMessage());
        }
    }

    public static String getScheduledTime (Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        ListenableFuture<List<WorkInfo>> workInfos = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME);
        try {
            List<WorkInfo> workInfoList = workInfos.get();
            Log.d("scheduleDailyTask", "workInfoList size: " + workInfoList.size());
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                Log.d("scheduleDailyTask", "workInfo state: " + workInfo.getState());
                if (state == WorkInfo.State.ENQUEUED) {
                    UUID workerId = workInfo.getId();
                    long nextScheduled = workInfo.getNextScheduleTimeMillis();
                    String scheduledTime = getTimeFromMillis(nextScheduled);
                    Log.d(UNIQUE_WORK_NAME, "found enqueued work with id " + workerId + " scheduled to run next at " + scheduledTime);
                    return scheduledTime;
                }
            }
        } catch (ExecutionException e) {
            Log.e("ExecutionException", Objects.requireNonNull(e.getMessage()));
        } catch (InterruptedException e) {
            Log.e("InterruptedException", Objects.requireNonNull(e.getMessage()));
        }
        return "";
    }

    /**
     * Отменить задачу
     */
    public static void cancelTask(Context context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
            AppLogger.addLog(context, "SUCCESS", "The task has been canceled");
            RouterState.setRestrictionPlanned(false);
            Toast.makeText(context, "Задача отменена", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("cancelTask", "cancelTask. Cancel task schedule failed", e);
            AppLogger.addLog(context, "FAILED", "cancelTask. Cancel task schedule failed: " +
                    e.getMessage());
        }
    }

    private static String getTimeFromMillis(Long millis) {
        Instant instant = Instant.ofEpochMilli(millis);

        ZoneId zoneId = ZoneId.systemDefault(); // Use the system default time zone
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDateTime = localDateTime.format(formatter);
        Log.d("getTimeFromMilis", "millis: "+ millis + " converted to: " +formattedDateTime);
        return formattedDateTime;
    }
}