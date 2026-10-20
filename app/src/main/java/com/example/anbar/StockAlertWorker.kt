package com.example.anbar

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** هشدار روزانه کمبود و انقضا */
class StockAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = InventoryRepository(applicationContext)
            val low = repo.getInventoryRows().filter { it.quantity <= 5 }
            val exp = repo.getExpiringItems(14)
            val parts = mutableListOf<String>()
            if (low.isNotEmpty()) {
                parts.add("${low.size} قلم کمبود (≤۵): " +
                    low.take(3).joinToString { it.productName })
            }
            if (exp.isNotEmpty()) {
                parts.add("${exp.size} قلم نزدیک انقضا: " +
                    exp.take(3).joinToString { it.productName })
            }
            if (parts.isNotEmpty()) {
                NotificationHelper.show(
                    applicationContext,
                    NotificationHelper.CHANNEL_ALERTS,
                    1001,
                    "⚠️ هشدار انبار",
                    parts.joinToString("\n")
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK = "anbar_stock_alerts"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<StockAlertWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK, ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
