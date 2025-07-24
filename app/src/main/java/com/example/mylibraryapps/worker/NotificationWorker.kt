package com.example.mylibraryapps.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mylibraryapps.service.NotificationService
import com.example.mylibraryapps.utils.PushNotificationHelper
import kotlinx.coroutines.withTimeout

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationWorker"
        private const val MAX_RETRY_COUNT = 3
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting background notification check...")
            
            // Add timeout to prevent hanging
            withTimeout(60000) { // 60 seconds timeout
                val notificationService = NotificationService()
                val pushNotificationHelper = PushNotificationHelper(applicationContext)
                
                // Check and create notifications
                notificationService.checkAndCreateNotifications()
                
                // Send push notifications
                pushNotificationHelper.sendScheduledNotifications()
            }
            
            Log.d(TAG, "Background notification check completed successfully")
            Result.success()
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e(TAG, "Notification worker timed out", e)
            // Don't retry on timeout, just fail
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Error in background notification check", e)
            
            // Only retry if we haven't exceeded max retry count
            val retryCount = inputData.getInt("retry_count", 0)
            if (retryCount < MAX_RETRY_COUNT) {
                Log.d(TAG, "Retrying notification worker (attempt ${retryCount + 1})")
                Result.retry()
            } else {
                Log.e(TAG, "Max retry count reached, failing notification worker")
                Result.failure()
            }
        }
    }
}