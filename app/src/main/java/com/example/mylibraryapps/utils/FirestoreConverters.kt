package com.example.mylibraryapps.utils

import com.google.firebase.Timestamp
import java.util.*

object FirestoreConverters {
    
    /**
     * Convert various timestamp formats to Date
     */
    fun convertToDate(value: Any?): Date {
        return when (value) {
            is Date -> value
            is Timestamp -> value.toDate()
            is Long -> Date(value)
            is String -> {
                try {
                    // Try to parse as long first
                    Date(value.toLong())
                } catch (e: NumberFormatException) {
                    // If not a number, return current date as fallback
                    Date()
                }
            }
            else -> Date() // Fallback to current date
        }
    }
    
    /**
     * Convert various timestamp formats to Firebase Timestamp
     */
    fun convertToTimestamp(value: Any?): Timestamp {
        return when (value) {
            is Timestamp -> value
            is Date -> Timestamp(value)
            is Long -> Timestamp(Date(value))
            is String -> {
                try {
                    // Try to parse as long first
                    Timestamp(Date(value.toLong()))
                } catch (e: NumberFormatException) {
                    // If not a number, return current timestamp as fallback
                    Timestamp.now()
                }
            }
            else -> Timestamp.now() // Fallback to current timestamp
        }
    }
    
    /**
     * Safely convert to Long
     */
    fun convertToLong(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is String -> {
                try {
                    value.toLong()
                } catch (e: NumberFormatException) {
                    0L
                }
            }
            else -> 0L
        }
    }
    
    /**
     * Safely convert to String
     */
    fun convertToString(value: Any?): String {
        return value?.toString() ?: ""
    }
    
    /**
     * Safely convert to Boolean
     */
    fun convertToBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.lowercase() == "true"
            else -> false
        }
    }
}