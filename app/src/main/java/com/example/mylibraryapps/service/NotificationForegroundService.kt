package com.example.mylibraryapps.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mylibraryapps.MainActivity
import com.example.mylibraryapps.R
import com.example.mylibraryapps.utils.PushNotificationHelper
import kotlinx.coroutines.*
import java.util.*

class NotificationForegroundService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationTimer: Timer? = null
    private var isServiceRunning = false
    
    companion object {
        private const val TAG = "NotificationForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "notification_service_channel"
        private const val CHANNEL_NAME = "Library Notification Service"
        
        fun startService(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Notification service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Notification service started")
        
        if (!isServiceRunning) {
            // Start foreground service - WAJIB untuk foreground service
//            startForeground(NOTIFICATION_ID, createServiceNotification())
            
            // Start periodic notification checking
            startPeriodicNotificationCheck()
            
            isServiceRunning = true
        }
        
        return START_STICKY // Service will be restarted if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN // Ubah ke IMPORTANCE_MIN agar lebih tersembunyi
            ).apply {
                description = "Service untuk mengecek notifikasi perpustakaan"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Library App")
            .setContentText("Berjalan di background")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN) // Priority minimal
            .setOngoing(true)
            .setSilent(true) // Tidak ada suara
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Tersembunyi di lock screen
            .build()
    }
    
    private fun startPeriodicNotificationCheck() {
        // Cancel existing timer
        notificationTimer?.cancel()
        
        // Create new timer that checks every 4 hours (reduced frequency)
        notificationTimer = Timer("NotificationTimer", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    // Check if service is still running
                    if (isServiceRunning) {
                        serviceScope.launch {
                            try {
                                checkAndSendNotifications()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in periodic notification check", e)
                                // Don't crash the service, just log the error
                            }
                        }
                    }
                }
            }, 0, 4 * 60 * 60 * 1000) // Check every 4 hours (reduced from 6)
        }
        
        Log.d(TAG, "Periodic notification check started (every 4 hours)")
    }
    
    private suspend fun checkAndSendNotifications() {
        try {
            Log.d(TAG, "Checking notifications in background...")
            
            // Add timeout to prevent hanging
            withTimeout(30000) { // 30 seconds timeout
                val notificationService = NotificationService()
                val pushNotificationHelper = PushNotificationHelper(this@NotificationForegroundService)
                
                // Check and create notifications
                notificationService.checkAndCreateNotifications()
                
                // Send push notifications
                pushNotificationHelper.sendScheduledNotifications()
            }
            
            Log.d(TAG, "Background notification check completed")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Notification check timed out", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error in background notification check", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        
        // Cancel timer safely
        try {
            notificationTimer?.cancel()
            notificationTimer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling timer", e)
        }
        
        // Cancel coroutine scope
        try {
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling coroutine scope", e)
        }
        
        Log.d(TAG, "Notification service destroyed")
    }
}