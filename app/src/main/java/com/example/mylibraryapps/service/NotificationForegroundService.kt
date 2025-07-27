package com.example.mylibraryapps.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.mylibraryapps.utils.PushNotificationHelper
import kotlinx.coroutines.*
import java.util.*

class NotificationForegroundService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationTimer: Timer? = null
    private var isServiceRunning = false
    
    companion object {
        private const val TAG = "NotificationForegroundService"
        
        fun startService(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java)
            context.startService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Foreground notification service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Foreground notification service started")
        
        if (!isServiceRunning) {
            // PERINGATAN: Foreground service requires notification
            // untuk menjalankan service requires        Log.w(TAG, "Running as foreground service - requires notification")
            
            // Start periodic notification checking
            startPeriodicNotificationCheck()
            
            isServiceRunning = true
        }
        
        return START_STICKY // Service will be restarted if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    

    

    
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