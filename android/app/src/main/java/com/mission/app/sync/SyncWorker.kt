package com.mission.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mission.app.MissionApp
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? MissionApp ?: return Result.success()
        return try {
            val status = app.container.syncManager.sync()
            if (status.state == "error") Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_NAME = "mission-sync-periodic"
        private const val ONCE_NAME = "mission-sync-once"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun requestNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONCE_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}