package com.example.mylibraryapps

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.Observer
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.utils.NetworkMonitor
import com.example.mylibraryapps.utils.NotificationScheduler
import com.example.mylibraryapps.utils.AlarmScheduler
import com.example.mylibraryapps.service.NotificationForegroundService
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class MyLibraryApplication : Application() {
    
    // Lazy initialization of repository
    val repository: AppRepository by lazy { AppRepository() }
    
    // Network monitor
    lateinit var networkMonitor: NetworkMonitor
    
    // Notification scheduler
    lateinit var notificationScheduler: NotificationScheduler
    
    // Alarm scheduler
    lateinit var alarmScheduler: AlarmScheduler
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            
            // Configure Firestore for better offline support
            configureFirestore()
            
            // Initialize network monitor
            setupNetworkMonitoring()
            
            // Initialize FCM
            setupFCM()
            
            // Initialize notification scheduler
            setupNotificationScheduler()
            
            // Setup background services (delayed to reduce startup load)
            setupBackgroundServices()
            
            // Run initial notification check (delayed)
            runInitialNotificationCheck()
            
            // Preload data (delayed to reduce startup time)
            // repository.preloadData() // Commented out to reduce startup load
            
        } catch (e: Exception) {
            Log.e("MyLibraryApplication", "Error during application initialization", e)
            // Don't crash the app, just log the error
        }
    }
    
    /**
     * Configure Firestore settings for better offline support
     */
    private fun configureFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)  // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)  // Unlimited cache size
            .build()
        
        firestore.firestoreSettings = settings
    }
    
    /**
     * Setup network monitoring
     */
    private fun setupNetworkMonitoring() {
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        
        // Observe network changes with proper lifecycle management
        val networkObserver = Observer<Boolean> { isConnected ->
            if (!isConnected) {
                // Show toast when connection is lost
                Toast.makeText(
                    this,
                    "Koneksi internet terputus. Beberapa fitur mungkin tidak tersedia.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        networkMonitor.isConnected.observeForever(networkObserver)
    }
    
    /**
     * Setup Firebase Cloud Messaging
     */
    private fun setupFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            
            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Registration Token: $token")
            
            // TODO: Send token to server if user is logged in
            // This will be handled in the MyFirebaseMessagingService
        }
    }
    
    /**
     * Setup notification scheduler
     */
    private fun setupNotificationScheduler() {
        notificationScheduler = NotificationScheduler(this)
        
        // Schedule periodic notification checks
        notificationScheduler.scheduleDailyNotificationCheck()
        notificationScheduler.schedulePeriodicNotificationCheck()
        
        Log.d("NotificationScheduler", "Notification scheduling initialized")
    }
    
    /**
     * Setup background services for persistent notifications
     */
    private fun setupBackgroundServices() {
        // Start foreground service for background notifications
        NotificationForegroundService.startService(this)
        
        // Setup alarm scheduler
        alarmScheduler = AlarmScheduler(this)
        alarmScheduler.scheduleNotificationAlarm()
        
        Log.d("BackgroundServices", "Background services initialized")
    }
    
    /**
     * Run initial notification check when app starts
     */
    private fun runInitialNotificationCheck() {
        // Use the existing NotificationScheduler to run immediate check
        notificationScheduler.scheduleImmediateNotificationCheck()
        Log.d("NotificationCheck", "Initial notification check triggered")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Stop network monitoring
        networkMonitor.stopMonitoring()
        
        // Cancel scheduled notifications
        notificationScheduler.cancelAllScheduledWork()
        
        // Stop background services
        NotificationForegroundService.stopService(this)
        alarmScheduler.cancelNotificationAlarm()
    }
}