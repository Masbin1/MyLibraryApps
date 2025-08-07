package com.example.mylibraryapps.notification

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * NotificationScheduler - Legacy WorkManager implementation
 * 
 * NOTE: This class is now deprecated in favor of Firebase Functions cron job.
 * The new system uses Firebase Functions with scheduled triggers that run daily
 * to check for overdue books and send push notifications via FCM.
 * 
 * See: FIREBASE_CRON_NOTIFICATION_GUIDE.md for the new implementation.
 * 
 * This class is kept for backward compatibility and testing purposes.
 */
class NotificationScheduler(private val context: Context) {
    
    companion object {
        const val TAG = "NotificationScheduler"
    }
    
    fun scheduleBookReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        // Jadwalkan pengecekan setiap 12 jam
        val reminderRequest = PeriodicWorkRequestBuilder<BookReminderWorker>(
            12, TimeUnit.HOURS,
            1, TimeUnit.HOURS // Flex interval
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // Delay awal 1 menit
            .addTag(BookReminderWorker.TAG)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BookReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            reminderRequest
        )
    }
    
    fun scheduleImmediateCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val immediateRequest = OneTimeWorkRequestBuilder<BookReminderWorker>()
            .setConstraints(constraints)
            .addTag("immediate_check")
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateRequest)
    }
    
    fun cancelBookReminders() {
        WorkManager.getInstance(context).cancelUniqueWork(BookReminderWorker.WORK_NAME)
    }
    
    fun getWorkStatus(): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(BookReminderWorker.TAG)
    }
}