package com.example.mylibraryapps.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

class AlarmScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    companion object {
        private const val TAG = "AlarmScheduler"
        private const val ALARM_REQUEST_CODE = 1001
    }
    
    fun scheduleNotificationAlarm() {
        try {
            // Request permission for exact alarms on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Request permission
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            }
            
            val intent = Intent(context, NotificationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule alarm to repeat every 1 hour
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.MINUTE, 1) // Start after 1 minute
            }
            
            // Use setRepeating for regular intervals
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                1 * 60 * 60 * 1000, // 1 hour in milliseconds
                pendingIntent
            )
            
            Log.d(TAG, "Notification alarm scheduled for every 1 hour")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification alarm", e)
        }
    }
    
    fun cancelNotificationAlarm() {
        try {
            val intent = Intent(context, NotificationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Notification alarm cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification alarm", e)
        }
    }
}