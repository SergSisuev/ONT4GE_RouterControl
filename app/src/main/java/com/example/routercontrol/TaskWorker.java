package com.example.routercontrol;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TaskWorker extends Worker {

    private Context context;

    public TaskWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("DailyTaskWorker", "The task is started");
        AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is started at: " +
                new java.util.Date());
        // Reload router state if it's already cleaned
        RouterState.loadState(this.getApplicationContext());

        RouterState.setCurrentOperation(RouterState.getNextRestictionOperation());
        AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is going to be started");
        int result = executeRouterAction();
        AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is executed");

        if (result == 1) {
            AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is finished successfully");
            if (RouterState.isWebShouldBeEnabled()) {
                RouterState.setRestrictionApplied(false);
                RouterState.setRestrictionPlanned(false);
            } else {
                RouterState.setRestrictionApplied(true);
                RouterState.CalculateNextPlannedTime(false);
                TaskScheduler.scheduleTask(getApplicationContext(), RouterState.getRestrictionPlannedTime(), false);
            }
        } else {
            AppLogger.addLog(context, "SUCCESS", "DailyTaskWorker. The task is failed");
            RouterState.CalculateNextPlannedTime(true);
            TaskScheduler.scheduleTask(context, RouterState.getRestrictionPlannedTime(), false);
            RouterState.setOperationStatus(3);
        }
        RouterState.saveState(this.getApplicationContext());
        return Result.success();
    }

    private int executeRouterAction() {
        try {
            // Get required result of the work
            Log.d("executeRouterAction", "Executing router operation: " + RouterState.getCurrentOperation());
            AppLogger.addLog(context, "SUCCESS", "eExecuting router operation: " + RouterState.getCurrentOperation());
            final CountDownLatch latch = new CountDownLatch(1);
            final int enableFilter = RouterState.getCurrentOperation() == RouterState.RestrictionOperations.DISABLE_WEB ? 1 : 0;
            new RouterAction(latch, context).execute(enableFilter, false);
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