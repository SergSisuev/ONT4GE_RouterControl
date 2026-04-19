package com.example.routercontrol;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DailyTaskWorker extends Worker {

    private Context context;

    public DailyTaskWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("DailyTaskWorker", "The task is started");
        AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is started at: " +
                new java.util.Date());

        RouterState.setCurrentOperation(RouterState.isWebShouldBeEnabled() ?
                RouterState.RestrictionOperations.ENABLE_WEB : RouterState.RestrictionOperations.DISABLE_WEB);
        AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is going to be started");
        int result = executeRouterAction(RouterState.isWebShouldBeEnabled());
        AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is executed");

        if (result == 1) {
            AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is finished successfully");
            if (RouterState.isWebShouldBeEnabled()) {
                RouterState.setRestrictionApplied(false);
                RouterState.setRestrictionPlanned(false);
            } else {
                RouterState.setRestrictionApplied(true);
                RouterState.CalculateNextPlannedTime();
                TaskScheduler.scheduleTask(getApplicationContext(), RouterState.getRestrictionPlannedTime(), false);
            }
            RouterState.setOperationStatus(2);
            return Result.success();
        } else {
            AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is failed");
            RouterState.CalculateNextPlannedTime();
            TaskScheduler.scheduleTask(context, RouterState.getRestrictionPlannedTime(), false);
            RouterState.setOperationStatus(3);
            return Result.success();
        }

    }

    private int executeRouterAction(boolean enableWeb) {
        try {
            // Get required result of the work
            Log.d("executeRouterAction", "is web should be enabled? " + enableWeb);
            AppLogger.addLog(context, "SUCCESS", "executeRouterAction. is web should be enabled?" + enableWeb);
            // Reload router state if it's already cleaned
            RouterState.loadState(this.getApplicationContext());
            final CountDownLatch latch = new CountDownLatch(1);
            new RouterAction(latch, context).execute(enableWeb ? 1 : 0, false);
            // Wait for the result
            try {
                if (latch.await(30, TimeUnit.SECONDS)) {
                    return 1;
                } else {
                    AppLogger.addLog(context, "FAILED", "DailyTaskWorker. The task execution timeout");
                    return 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                AppLogger.addLog(context, "FAILED", "DailyTaskWorker. The task execution interrupted");
                return 0;
            }
        } catch (Exception e) {
            Log.e("executeYourTask", Objects.requireNonNull(e.getMessage()));
            e.printStackTrace();
            AppLogger.addLog(context, "FAILED", "DailyTaskWorker. The task execution failed");
            return 0;
        }
    }
}