package com.example.mylibraryapps

import android.app.Application
import com.example.mylibraryapps.data.AppRepository
import com.google.firebase.FirebaseApp

class MyLibraryApplication : Application() {
    
    // Lazy initialization of repository
    val repository: AppRepository by lazy { AppRepository() }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Preload data
        repository.preloadData()
    }
}