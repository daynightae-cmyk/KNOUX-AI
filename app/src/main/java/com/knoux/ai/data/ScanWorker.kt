package com.knoux.ai.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Scheduled Smart Clean — **scan only**.
 *
 * The contract forbids undeclared background deletion, and rightly so: a periodic
 * job cannot obtain per-item customer consent, and the Android media trash/delete
 * dialogs require a foreground Activity. This worker therefore discovers and
 * records, and does nothing destructive. It returns failure without retry when the
 * schedule was switched off, so a disabled schedule stops costing battery.
 */
class ScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        KnouxData.init(applicationContext)
        val settings = KnouxData.settings

        val enabled = settings.scheduleEnabled.firstOrNull() ?: false
        if (!enabled) return Result.failure()

        val scopes = settings.selectedScopes.firstOrNull() ?: setOf(AccessScope.APP_CACHE)

        return try {
            val result = KnouxData.smartClean.scan(scopes)
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "knoux_smart_clean_scan"

        /** Enqueues (or replaces) the periodic scan. Never a delete job. */
        fun schedule(context: Context, intervalMinutes: Long) {
            val request = PeriodicWorkRequestBuilder<ScanWorker>(
                intervalMinutes.coerceAtLeast(15L), TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
