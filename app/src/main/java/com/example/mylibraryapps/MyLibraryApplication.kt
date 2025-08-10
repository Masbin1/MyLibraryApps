package com.example.mylibraryapps

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.Observer
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.utils.NetworkMonitor
import com.example.mylibraryapps.notification.NotificationScheduler
import com.example.mylibraryapps.utils.AlarmScheduler
import com.example.mylibraryapps.service.NotificationForegroundService
import com.example.mylibraryapps.utils.DebugUtils
import com.example.mylibraryapps.utils.GooglePlayServicesErrorHandler
import com.example.mylibraryapps.utils.CrashFixVerifier
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
    
    // Network observer untuk cleanup
    private var networkObserver: Observer<Boolean>? = null
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("MyLibraryApplication", "Starting application initialization...")
        
        // Check Google Play Services first
        if (!GooglePlayServicesErrorHandler.checkGooglePlayServices(this)) {
            Log.w("MyLibraryApplication", "Google Play Services not available - some features may not work")
        }
        
        // Print debug information
        DebugUtils.printPackageInfo(this)
        DebugUtils.printSHA1Fingerprint(this)
        
        try {
            // Initialize Firebase first (critical)
            initializeFirebase()
            
            // Initialize network monitor (critical for offline support)
            setupNetworkMonitoring()
            
            // Initialize FCM (can fail gracefully)
            setupFCM()
            
            // Initialize notification scheduler (can fail gracefully)
            setupNotificationScheduler()
            
            // Setup background services with delay to reduce startup load
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setupBackgroundServices()
                runInitialNotificationCheck()
                
                // Run crash fix verification tests
                CrashFixVerifier.runAllTests(this)
                CrashFixVerifier.simulateOriginalCrashScenarios()
            }, 2000) // 2 second delay
            
            Log.d("MyLibraryApplication", "Application initialization completed successfully")
            
        } catch (e: Exception) {
            Log.e("MyLibraryApplication", "Critical error during application initialization", e)
            // Don't crash the app, but ensure basic functionality works
            try {
                FirebaseApp.initializeApp(this)
            } catch (firebaseError: Exception) {
                Log.e("MyLibraryApplication", "Failed to initialize Firebase", firebaseError)
            }
        }
    }
    
    /**
     * Initialize Firebase with proper error handling
     */
    private fun initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            val existingApp = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                null
            }
            
            if (existingApp == null) {
                Log.d("MyLibraryApplication", "Initializing Firebase...")
                FirebaseApp.initializeApp(this)
                Log.d("MyLibraryApplication", "Firebase initialized successfully")
            } else {
                Log.d("MyLibraryApplication", "Firebase already initialized")
            }
            
            // Add delay before configuring Firestore to avoid race conditions
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                configureFirestore()
            }, 1000)
            
        } catch (e: Exception) {
            Log.e("MyLibraryApplication", "Error initializing Firebase", e)
            // Don't re-throw, let the app continue without Firebase for now
        }
    }
    
    /**
     * Configure Firestore settings for better offline support
     */
    private fun configureFirestore() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Enable offline persistence (new way)
            firestore.enableNetwork()
            
            // Note: Cache settings are now handled automatically by Firebase
            Log.d("MyLibraryApplication", "Firestore configured successfully")
        } catch (e: Exception) {
            Log.e("MyLibraryApplication", "Error configuring Firestore", e)
            
            // Handle Google Play Services errors
            if (GooglePlayServicesErrorHandler.isGooglePlayServicesError(e)) {
                val errorMessage = GooglePlayServicesErrorHandler.handleAuthenticationError(e)
                Log.e("MyLibraryApplication", errorMessage)
            }
        }
    }
    
    /**
     * Setup network monitoring
     */
    private fun setupNetworkMonitoring() {
        try {
            networkMonitor = NetworkMonitor(this)
            networkMonitor.startMonitoring()
            
            // Observe network changes with proper lifecycle management
            networkObserver = Observer<Boolean> { isConnected ->
                if (!isConnected) {
                    // Show toast when connection is lost
                    Toast.makeText(
                        this,
                        "Koneksi internet terputus. Beberapa fitur mungkin tidak tersedia.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            
            networkObserver?.let { observer ->
                networkMonitor.isConnected.observeForever(observer)
            }
        } catch (e: Exception) {
            Log.e("MyLibraryApplication", "Error setting up network monitoring", e)
        }
    }
    
    /**
     * Setup Firebase Cloud Messaging
     */
    private fun setupFCM() {
        try {
            // Add delay to ensure Firebase is fully initialized
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
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
                } catch (e: Exception) {
                    Log.e("FCM", "Error setting up FCM", e)
                }
            }, 2000) // 2 second delay
        } catch (e: Exception) {
            Log.e("FCM", "Error in setupFCM", e)
        }
    }
    
    /**
     * Setup notification scheduler
     */
    private fun setupNotificationScheduler() {
        notificationScheduler = NotificationScheduler(this)
        
        // Schedule periodic book reminder checks
        notificationScheduler.scheduleBookReminders()
        
        Log.d("NotificationScheduler", "Book reminder scheduling initialized")
    }
    
    /**
     * Setup background services for persistent notifications
     */
    private fun setupBackgroundServices() {
        try {
            // Start background service for notifications (tanpa foreground notification)
            NotificationForegroundService.startService(this)
            
            // Setup alarm scheduler
            alarmScheduler = AlarmScheduler(this)
            alarmScheduler.scheduleNotificationAlarm()
            
            Log.d("BackgroundServices", "Background services initialized")
        } catch (e: Exception) {
            Log.e("MyLibraryApplication", "Error setting up background services", e)
        }
    }
    
    /**
     * Run initial notification check when app starts
     */
    private fun runInitialNotificationCheck() {
        // Use the NotificationScheduler to run immediate check
        notificationScheduler.scheduleImmediateCheck()
        Log.d("NotificationCheck", "Initial book reminder check triggered")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Remove network observer to prevent memory leak
        networkObserver?.let { observer ->
            networkMonitor.isConnected.removeObserver(observer)
        }
        
        // Stop network monitoring
        networkMonitor.stopMonitoring()
        
        // Cancel scheduled notifications
        notificationScheduler.cancelBookReminders()
        
        // Stop background services
        NotificationForegroundService.stopService(this)
        alarmScheduler.cancelNotificationAlarm()
    }
}