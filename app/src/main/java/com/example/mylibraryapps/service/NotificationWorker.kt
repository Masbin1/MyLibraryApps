package com.example.mylibraryapps.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mylibraryapps.utils.PushNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Check and send push notifications for all users
                val notificationService = NotificationService()
                val pushNotificationHelper = PushNotificationHelper(context)
                
                // Get all active transactions and send push notifications
                notificationService.checkAndCreateNotifications()
                
                // Send push notifications for reminders
                pushNotificationHelper.sendScheduledNotifications()
                
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }
}