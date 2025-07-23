package com.example.mylibraryapps.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mylibraryapps.service.NotificationService
import com.example.mylibraryapps.utils.PushNotificationHelper

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting background notification check...")
            
            val notificationService = NotificationService()
            val pushNotificationHelper = PushNotificationHelper(applicationContext)
            
            // Check and create notifications
            notificationService.checkAndCreateNotifications()
            
            // Send push notifications
            pushNotificationHelper.sendScheduledNotifications()
            
            Log.d(TAG, "Background notification check completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in background notification check", e)
            Result.retry() // Retry jika ada error
        }
    }
}