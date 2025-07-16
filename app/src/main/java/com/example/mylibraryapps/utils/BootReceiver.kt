package com.example.mylibraryapps.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mylibraryapps.service.NotificationForegroundService

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot completed or app updated, restarting notification service")
                
                // Restart notification service
                NotificationForegroundService.startService(context)
                
                // Reschedule alarms
                val alarmScheduler = AlarmScheduler(context)
                alarmScheduler.scheduleNotificationAlarm()
                
                Log.d(TAG, "Notification service and alarms restarted")
            }
        }
    }
}