package com.example.mylibraryapps.utils

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreDeserializationErrorHandler {
    private const val TAG = "FirestoreDeserializationErrorHandler"
    
    /**
     * Handle Firestore deserialization errors and provide helpful debugging info
     */
    fun handleDeserializationError(
        exception: Exception,
        document: DocumentSnapshot?,
        targetClass: String
    ): String {
        val documentId = document?.id ?: "unknown"
        val documentData = document?.data?.toString() ?: "no data"
        
        Log.e(TAG, "=== FIRESTORE DESERIALIZATION ERROR ===")
        Log.e(TAG, "Target Class: $targetClass")
        Log.e(TAG, "Document ID: $documentId")
        Log.e(TAG, "Document Data: $documentData")
        Log.e(TAG, "Error: ${exception.message}")
        Log.e(TAG, "========================================")
        
        return when {
            exception.message?.contains("Failed to convert value of type java.lang.String to Timestamp") == true -> {
                handleTimestampConversionError(exception, document)
            }
            exception.message?.contains("Failed to convert value of type java.lang.Long to Date") == true -> {
                handleDateConversionError(exception, document)
            }
            exception.message?.contains("Could not deserialize object") == true -> {
                handleGenericDeserializationError(exception, document)
            }
            else -> {
                "Unknown deserialization error: ${exception.message}"
            }
        }
    }
    
    private fun handleTimestampConversionError(exception: Exception, document: DocumentSnapshot?): String {
        Log.e(TAG, "TIMESTAMP CONVERSION ERROR DETECTED")
        Log.e(TAG, "This usually happens when:")
        Log.e(TAG, "1. A String date is stored but Timestamp is expected")
        Log.e(TAG, "2. Date format doesn't match expected format")
        Log.e(TAG, "3. Field contains null or invalid data")
        
        // Extract field name from error message
        val fieldName = extractFieldName(exception.message, "Timestamp")
        if (fieldName != null && document != null) {
            val fieldValue = document.get(fieldName)
            Log.e(TAG, "Problematic field '$fieldName' contains: $fieldValue (${fieldValue?.javaClass?.simpleName})")
        }
        
        return "Timestamp conversion error in field '$fieldName'. Use SafeFirestoreConverter to handle this."
    }
    
    private fun handleDateConversionError(exception: Exception, document: DocumentSnapshot?): String {
        Log.e(TAG, "DATE CONVERSION ERROR DETECTED")
        Log.e(TAG, "This usually happens when:")
        Log.e(TAG, "1. A Long timestamp is stored but Date is expected")
        Log.e(TAG, "2. Timestamp format doesn't match expected format")
        Log.e(TAG, "3. Field contains null or invalid data")
        
        // Extract field name from error message
        val fieldName = extractFieldName(exception.message, "Date")
        if (fieldName != null && document != null) {
            val fieldValue = document.get(fieldName)
            Log.e(TAG, "Problematic field '$fieldName' contains: $fieldValue (${fieldValue?.javaClass?.simpleName})")
        }
        
        return "Date conversion error in field '$fieldName'. Use SafeFirestoreConverter to handle this."
    }
    
    private fun handleGenericDeserializationError(exception: Exception, document: DocumentSnapshot?): String {
        Log.e(TAG, "GENERIC DESERIALIZATION ERROR DETECTED")
        Log.e(TAG, "This usually happens when:")
        Log.e(TAG, "1. Data types don't match model expectations")
        Log.e(TAG, "2. Required fields are missing")
        Log.e(TAG, "3. Field names don't match model properties")
        
        if (document != null) {
            Log.e(TAG, "Available fields in document:")
            document.data?.forEach { (key, value) ->
                Log.e(TAG, "  $key: $value (${value?.javaClass?.simpleName})")
            }
        }
        
        return "Generic deserialization error. Check field types and names."
    }
    
    private fun extractFieldName(errorMessage: String?, expectedType: String): String? {
        if (errorMessage == null) return null
        
        // Try to extract field name from error message like:
        // "Failed to convert value of type java.lang.String to Timestamp (found in field 'purchaseDate')"
        val regex = "\\(found in field '([^']+)'\\)".toRegex()
        val matchResult = regex.find(errorMessage)
        return matchResult?.groupValues?.get(1)
    }
    
    /**
     * Check if an exception is a Firestore deserialization error
     */
    fun isDeserializationError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return message.contains("could not deserialize object") ||
               message.contains("failed to convert value") ||
               message.contains("found in field")
    }
    
    /**
     * Provide suggestions for fixing deserialization errors
     */
    fun getFixSuggestions(exception: Exception): List<String> {
        val suggestions = mutableListOf<String>()
        val message = exception.message ?: ""
        
        when {
            message.contains("Timestamp") -> {
                suggestions.add("Use SafeFirestoreConverter.documentToBook() instead of toObject()")
                suggestions.add("Ensure date fields are stored as Firebase Timestamp")
                suggestions.add("Check if date strings are in correct format")
            }
            message.contains("Date") -> {
                suggestions.add("Use SafeFirestoreConverter.documentToNotification() instead of toObject()")
                suggestions.add("Ensure timestamp fields are stored as Long or Timestamp")
                suggestions.add("Check if date conversion logic is correct")
            }
            else -> {
                suggestions.add("Use SafeFirestoreConverter methods for all document conversions")
                suggestions.add("Check that all model fields have default values")
                suggestions.add("Verify field names match between Firestore and model")
            }
        }
        
        return suggestions
    }
}