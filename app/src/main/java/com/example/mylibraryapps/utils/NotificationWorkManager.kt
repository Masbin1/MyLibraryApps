package com.example.mylibraryapps.utils

import android.content.Context
import androidx.work.*
import com.example.mylibraryapps.worker.NotificationWorker
import java.util.concurrent.TimeUnit

object NotificationWorkManager {
    
    private const val WORK_NAME = "notification_check_work"
    
    fun startPeriodicNotificationCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val notificationWork = PeriodicWorkRequestBuilder<NotificationWorker>(
            6, TimeUnit.HOURS // Check setiap 6 jam
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            notificationWork
        )
    }
    
    fun stopPeriodicNotificationCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}