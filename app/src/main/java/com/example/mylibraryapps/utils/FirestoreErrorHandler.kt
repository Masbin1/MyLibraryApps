package com.example.mylibraryapps.utils

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Utility class to handle Firestore errors consistently across the app
 */
object FirestoreErrorHandler {
    private const val TAG = "FirestoreErrorHandler"

    /**
     * Get user-friendly error message based on the exception
     */
    fun getErrorMessage(e: Exception, operation: String): String {
        Log.e(TAG, "Error during $operation", e)
        
        return when {
            // Permission denied errors
            e.message?.contains("PERMISSION_DENIED") == true -> {
                "Tidak memiliki izin untuk $operation. Pastikan Anda sudah login dan memiliki izin yang cukup."
            }
            
            // Missing or unapplied index
            e.message?.contains("FAILED_PRECONDITION") == true && 
            e.message?.contains("index") == true -> {
                // Log the full error message to help developers find the index creation URL
                Log.w(TAG, "Index error detected. Full error: ${e.message}")
                
                // Extract the index creation URL if available
                val indexUrlRegex = "https://console\\.firebase\\.google\\.com[^\\s\"]+".toRegex()
                val indexUrl = indexUrlRegex.find(e.message ?: "")?.value
                
                if (indexUrl != null) {
                    // If we found a URL, log it specifically
                    Log.w(TAG, "Index creation URL: $indexUrl")
                }
                
                // Return a more helpful message
                "Terjadi masalah dengan database. Aplikasi sedang dalam penyesuaian, silakan coba lagi dalam beberapa menit."
            }
            
            // Network errors
            e.message?.contains("UNAVAILABLE") == true || 
            e.message?.contains("DEADLINE_EXCEEDED") == true -> {
                "Koneksi ke server gagal. Periksa koneksi internet Anda dan coba lagi."
            }
            
            // Authentication errors
            e.message?.contains("UNAUTHENTICATED") == true -> {
                "Sesi login Anda telah berakhir. Silakan login kembali."
            }
            
            // Not found errors
            e.message?.contains("NOT_FOUND") == true -> {
                "Data yang diminta tidak ditemukan. Mungkin telah dihapus atau dipindahkan."
            }
            
            // Rate limiting
            e.message?.contains("RESOURCE_EXHAUSTED") == true -> {
                "Terlalu banyak permintaan. Silakan coba lagi nanti."
            }
            
            // Default error message
            else -> {
                "Gagal melakukan $operation: ${e.message ?: "Unknown error"}"
            }
        }
    }
    
    /**
     * Handle Firestore exception and return appropriate error message
     */
    fun handleException(e: Exception, operation: String, logTag: String? = null): String {
        val errorMessage = getErrorMessage(e, operation)
        
        // Log the error if tag is provided
        logTag?.let {
            Log.e(it, "Error during $operation", e)
        }
        
        // Check if we need to handle specific error types
        when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                        // Could trigger re-authentication here
                    }
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        // Could log analytics event for permission issues
                    }
                    else -> { /* Handle other specific codes */ }
                }
            }
        }
        
        return errorMessage
    }
}