package com.example.mylibraryapps.utils

import android.content.Context
import android.util.Log
import com.example.mylibraryapps.model.Transaction
import com.example.mylibraryapps.utils.SafeFirestoreConverter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PushNotificationHelper(private val context: Context? = null) {
    
    private val db = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val localNotificationHelper = context?.let { LocalNotificationHelper(it) }
    
    companion object {
        private const val TAG = "PushNotificationHelper"
    }
    
    suspend fun sendScheduledNotifications() {
        try {
            // Add timeout to prevent hanging
            withTimeout(20000) { // 20 seconds timeout
                // Get all active transactions
                val activeTransactions = getActiveTransactions()
                
                // Process in batches to prevent memory issues
                activeTransactions.chunked(20).forEach { batch ->
                    batch.forEach { transaction ->
                        try {
                            checkAndSendNotification(transaction)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing transaction ${transaction.id}", e)
                            // Continue with other transactions
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e(TAG, "Send scheduled notifications timed out", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending scheduled notifications", e)
        }
    }
    
    private suspend fun getActiveTransactions(): List<Transaction> {
        return try {
            val snapshot = db.collection("transactions")
                .whereEqualTo("status", "sedang dipinjam") // Use consistent status
                .limit(100) // Limit to prevent large queries
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    SafeFirestoreConverter.documentToTransaction(doc)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing transaction ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active transactions", e)
            emptyList()
        }
    }
    
    private suspend fun checkAndSendNotification(transaction: Transaction) {
        val returnDate = parseDate(transaction.returnDate)
        if (returnDate == null) return
        
        val currentDate = Calendar.getInstance().time
        val daysDifference = calculateDaysDifference(currentDate, returnDate)
        
        // Get user's FCM token
        val userToken = getUserFCMToken(transaction.userId)
        if (userToken.isNullOrEmpty()) return
        
        when (daysDifference) {
            3 -> {
                // 3 days before due date
                if (!notificationAlreadySent(transaction.userId, transaction.id, "3_days")) {
                    sendReminderNotification(
                        token = userToken,
                        transaction = transaction,
                        daysRemaining = 3,
                        returnDate = returnDate
                    )
                    markNotificationSent(transaction.userId, transaction.id, "3_days")
                }
            }
            2 -> {
                // 2 days before due date
                if (!notificationAlreadySent(transaction.userId, transaction.id, "2_days")) {
                    sendReminderNotification(
                        token = userToken,
                        transaction = transaction,
                        daysRemaining = 2,
                        returnDate = returnDate
                    )
                    markNotificationSent(transaction.userId, transaction.id, "2_days")
                }
            }
            1 -> {
                // 1 day before due date
                if (!notificationAlreadySent(transaction.userId, transaction.id, "1_day")) {
                    sendReminderNotification(
                        token = userToken,
                        transaction = transaction,
                        daysRemaining = 1,
                        returnDate = returnDate
                    )
                    markNotificationSent(transaction.userId, transaction.id, "1_day")
                }
            }
            0 -> {
                // Due today
                if (!notificationAlreadySent(transaction.userId, transaction.id, "due_today")) {
                    sendReminderNotification(
                        token = userToken,
                        transaction = transaction,
                        daysRemaining = 0,
                        returnDate = returnDate
                    )
                    markNotificationSent(transaction.userId, transaction.id, "due_today")
                }
            }
            else -> {
                // Overdue
                if (daysDifference < 0) {
                    val daysOverdue = Math.abs(daysDifference)
                    val overdueKey = "overdue_$daysOverdue"
                    
                    if (!notificationAlreadySent(transaction.userId, transaction.id, overdueKey)) {
                        sendOverdueNotification(
                            token = userToken,
                            transaction = transaction,
                            daysOverdue = daysOverdue,
                            returnDate = returnDate
                        )
                        markNotificationSent(transaction.userId, transaction.id, overdueKey)
                    }
                }
            }
        }
    }
    
    private suspend fun sendReminderNotification(
        token: String,
        transaction: Transaction,
        daysRemaining: Int,
        returnDate: Date
    ) {
        val title = when (daysRemaining) {
            3 -> "📚 Reminder: 3 Hari Lagi"
            2 -> "📚 Reminder: 2 Hari Lagi"
            1 -> "📚 Reminder: Besok"
            0 -> "📚 Reminder: Hari Ini"
            else -> "📚 Reminder Pengembalian"
        }
        
        val body = when (daysRemaining) {
            3 -> "Buku \"${transaction.title}\" harus dikembalikan dalam 3 hari (${dateFormat.format(returnDate)})"
            2 -> "Buku \"${transaction.title}\" harus dikembalikan dalam 2 hari (${dateFormat.format(returnDate)})"
            1 -> "Buku \"${transaction.title}\" harus dikembalikan besok (${dateFormat.format(returnDate)})"
            0 -> "Buku \"${transaction.title}\" harus dikembalikan hari ini (${dateFormat.format(returnDate)})"
            else -> "Jangan lupa kembalikan buku \"${transaction.title}\""
        }
        
        val data = mapOf(
            "type" to "return_reminder",
            "title" to title,
            "body" to body,
            "bookTitle" to transaction.title,
            "bookId" to transaction.bookId,
            "transactionId" to transaction.id,
            "returnDate" to dateFormat.format(returnDate),
            "daysRemaining" to daysRemaining.toString()
        )
        
        sendNotificationToToken(token, title, body, data)
        
        // Show local notification for testing
        localNotificationHelper?.showTestNotification(title, body, daysRemaining)
    }
    
    private suspend fun sendOverdueNotification(
        token: String,
        transaction: Transaction,
        daysOverdue: Int,
        returnDate: Date
    ) {
        val title = "⚠️ Buku Terlambat"
        val body = "Buku \"${transaction.title}\" sudah terlambat $daysOverdue hari dari tanggal pengembalian (${dateFormat.format(returnDate)}). Segera kembalikan!"
        
        val data = mapOf(
            "type" to "overdue",
            "title" to title,
            "body" to body,
            "bookTitle" to transaction.title,
            "bookId" to transaction.bookId,
            "transactionId" to transaction.id,
            "returnDate" to dateFormat.format(returnDate),
            "daysOverdue" to daysOverdue.toString()
        )
        
        sendNotificationToToken(token, title, body, data)
        
        // Show local notification for testing
        localNotificationHelper?.showOverdueNotification(title, body)
    }
    
    private suspend fun sendNotificationToToken(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        try {
            // Note: Direct FCM sending from client is not recommended for production
            // This is for demonstration. In production, use a backend server to send notifications
            
            // For now, we'll show local notification for testing
            Log.d(TAG, "Sending local notification instead of FCM")
            Log.d(TAG, "Title: $title")
            Log.d(TAG, "Body: $body")
            Log.d(TAG, "Data: $data")
            
            // Show system notification (Android notification bar)
            localNotificationHelper?.showSystemNotification(title, body, data)
            
            // In production, send this data to your backend server
            // which will then send the push notification using FCM Admin SDK
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
        }
    }
    
    private suspend fun getUserFCMToken(userId: String): String? {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            snapshot.getString("fcmToken")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user FCM token", e)
            null
        }
    }
    
    private suspend fun notificationAlreadySent(userId: String, transactionId: String, type: String): Boolean {
        return try {
            val today = dateFormat.format(Date())
            val snapshot = db.collection("notification_sent")
                .whereEqualTo("userId", userId)
                .whereEqualTo("transactionId", transactionId)
                .whereEqualTo("type", type)
                .whereEqualTo("dateSent", today)
                .get()
                .await()
            
            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification sent status", e)
            false
        }
    }
    
    private suspend fun markNotificationSent(userId: String, transactionId: String, type: String) {
        try {
            val today = dateFormat.format(Date())
            val notificationSent = mapOf(
                "userId" to userId,
                "transactionId" to transactionId,
                "type" to type,
                "dateSent" to today,
                "timestamp" to System.currentTimeMillis()
            )
            
            db.collection("notification_sent")
                .add(notificationSent)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as sent", e)
        }
    }
    
    private fun parseDate(dateString: String): Date? {
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateDaysDifference(currentDate: Date, targetDate: Date): Int {
        val currentCalendar = Calendar.getInstance()
        currentCalendar.time = currentDate
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)
        
        val targetCalendar = Calendar.getInstance()
        targetCalendar.time = targetDate
        targetCalendar.set(Calendar.HOUR_OF_DAY, 0)
        targetCalendar.set(Calendar.MINUTE, 0)
        targetCalendar.set(Calendar.SECOND, 0)
        targetCalendar.set(Calendar.MILLISECOND, 0)
        
        val diffInMillis = targetCalendar.timeInMillis - currentCalendar.timeInMillis
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS).toInt()
    }
}