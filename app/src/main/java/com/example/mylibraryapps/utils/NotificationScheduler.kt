package com.example.mylibraryapps.utils

import android.content.Context
import androidx.work.*
import com.example.mylibraryapps.service.NotificationWorker
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {
    
    companion object {
        private const val DAILY_NOTIFICATION_WORK = "daily_notification_work"
        private const val PERIODIC_NOTIFICATION_WORK = "periodic_notification_work"
    }
    
    fun scheduleDailyNotificationCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        // Schedule daily notification check at 9 AM
        val dailyWork = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(DAILY_NOTIFICATION_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWork
        )
    }
    
    fun schedulePeriodicNotificationCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        // Schedule periodic notification check every 6 hours
        val periodicWork = PeriodicWorkRequestBuilder<NotificationWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(PERIODIC_NOTIFICATION_WORK)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWork
        )
    }
    
    fun scheduleImmediateNotificationCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val immediateWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateWork)
    }
    
    fun cancelAllScheduledWork() {
        WorkManager.getInstance(context).cancelAllWorkByTag(DAILY_NOTIFICATION_WORK)
        WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_NOTIFICATION_WORK)
    }
}