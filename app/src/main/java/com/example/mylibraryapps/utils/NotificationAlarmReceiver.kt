package com.example.mylibraryapps.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mylibraryapps.service.NotificationWorker

class NotificationAlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "NotificationAlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification alarm received")
        
        try {
            // Start work manager task
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            
            Log.d(TAG, "Notification work enqueued from alarm")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification alarm", e)
        }
    }
}