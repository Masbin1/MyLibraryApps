package com.example.mylibraryapps

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.Observer
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.utils.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MyLibraryApplication : Application() {
    
    // Lazy initialization of repository
    val repository: AppRepository by lazy { AppRepository() }
    
    // Network monitor
    lateinit var networkMonitor: NetworkMonitor
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Configure Firestore for better offline support
        configureFirestore()
        
        // Initialize network monitor
        setupNetworkMonitoring()
        
        // Preload data
        repository.preloadData()
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
        
        // Observe network changes
        networkMonitor.isConnected.observeForever { isConnected ->
            if (!isConnected) {
                // Show toast when connection is lost
                Toast.makeText(
                    this,
                    "Koneksi internet terputus. Beberapa fitur mungkin tidak tersedia.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Stop network monitoring
        networkMonitor.stopMonitoring()
    }
}