package com.example.mylibraryapps.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object GooglePlayServicesErrorHandler {
    private const val TAG = "GooglePlayServicesErrorHandler"
    
    /**
     * Check if Google Play Services is available and handle errors
     */
    fun checkGooglePlayServices(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        
        return when (resultCode) {
            ConnectionResult.SUCCESS -> {
                Log.d(TAG, "Google Play Services is available")
                true
            }
            ConnectionResult.SERVICE_MISSING -> {
                Log.e(TAG, "Google Play Services is missing")
                false
            }
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                Log.e(TAG, "Google Play Services needs to be updated")
                false
            }
            ConnectionResult.SERVICE_DISABLED -> {
                Log.e(TAG, "Google Play Services is disabled")
                false
            }
            else -> {
                Log.e(TAG, "Google Play Services error: $resultCode")
                false
            }
        }
    }
    
    /**
     * Handle specific Google Play Services authentication errors
     */
    fun handleAuthenticationError(exception: Exception): String {
        val message = exception.message ?: ""
        
        return when {
            message.contains("Unknown calling package name") -> {
                Log.e(TAG, "Google Play Services authentication error detected!")
                Log.e(TAG, "This usually means the SHA-1 fingerprint is not added to Firebase Console")
                Log.e(TAG, "Required SHA-1: 5B:E5:BD:4F:8D:22:90:2B:73:EB:14:85:C5:CB:15:0F:FA:FE:5E:5B")
                "Authentication error: Please add SHA-1 fingerprint to Firebase Console"
            }
            message.contains("SIGN_IN_REQUIRED") -> {
                Log.e(TAG, "Google Play Services sign-in required")
                "Sign-in required for Google Play Services"
            }
            message.contains("NETWORK_ERROR") -> {
                Log.e(TAG, "Google Play Services network error")
                "Network error with Google Play Services"
            }
            message.contains("INTERNAL_ERROR") -> {
                Log.e(TAG, "Google Play Services internal error")
                "Internal error with Google Play Services"
            }
            else -> {
                Log.e(TAG, "Unknown Google Play Services error: $message")
                "Google Play Services error: $message"
            }
        }
    }
    
    /**
     * Check if error is related to Google Play Services
     */
    fun isGooglePlayServicesError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return message.contains("google") || 
               message.contains("gms") || 
               message.contains("play services") ||
               message.contains("unknown calling package name")
    }
}