package com.example.mylibraryapps.utils

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mylibraryapps.service.NotificationWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class NotificationTestHelper(private val context: Context) {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private const val TAG = "NotificationTestHelper"
    }
    
    /**
     * Test immediate notification check
     */
    fun testImmediateNotificationCheck() {
        Log.d(TAG, "Starting immediate notification check...")
        
        val immediateWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateWork)
        
        Log.d(TAG, "Immediate notification work enqueued")
        
        // Also show a test system notification immediately
        showTestSystemNotification()
    }
    
    /**
     * Show test system notification immediately
     */
    private fun showTestSystemNotification() {
        val localNotificationHelper = LocalNotificationHelper(context)
        
        val title = "📚 Test Notification"
        val message = "Ini adalah test notifikasi sistem yang akan muncul di notification bar Android"
        val data = mapOf(
            "type" to "test",
            "title" to title,
            "body" to message
        )
        
        localNotificationHelper.showSystemNotification(title, message, data)
        Log.d(TAG, "Test system notification shown")
    }
    
    /**
     * Test system notification directly (for button click)
     */
    fun testSystemNotificationDirect() {
        val localNotificationHelper = LocalNotificationHelper(context)
        
        val title = "📲 MyLibrary App"
        val message = "Notifikasi ini muncul di notification bar Android seperti WhatsApp! 🎉"
        val data = mapOf(
            "type" to "system_test",
            "title" to title,
            "body" to message,
            "timestamp" to System.currentTimeMillis().toString()
        )
        
        localNotificationHelper.showSystemNotification(title, message, data)
        Log.d(TAG, "Direct system notification sent to Android notification bar")
        
        // Show multiple notifications for testing
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Show a reminder notification
            val reminderData = mapOf(
                "type" to "return_reminder",
                "title" to "📚 Reminder: 2 Hari Lagi",
                "body" to "Jangan lupa kembalikan buku 'Android Development' dalam 2 hari",
                "bookTitle" to "Android Development",
                "daysRemaining" to "2"
            )
            
            localNotificationHelper.showSystemNotification(
                "📚 Reminder: 2 Hari Lagi",
                "Jangan lupa kembalikan buku 'Android Development' dalam 2 hari",
                reminderData
            )
            
            // Show an overdue notification
            val overdueData = mapOf(
                "type" to "overdue",
                "title" to "⚠️ Buku Terlambat",
                "body" to "Buku 'Kotlin Programming' sudah terlambat 1 hari. Segera kembalikan!",
                "bookTitle" to "Kotlin Programming",
                "daysOverdue" to "1"
            )
            
            localNotificationHelper.showSystemNotification(
                "⚠️ Buku Terlambat",
                "Buku 'Kotlin Programming' sudah terlambat 1 hari. Segera kembalikan!",
                overdueData
            )
        }
        
        Log.d(TAG, "Multiple test notifications sent")
    }
    
    /**
     * Create test transaction with specific return date for testing
     */
    suspend fun createTestTransaction(daysFromNow: Int): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return null
            }
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, daysFromNow)
            val returnDate = dateFormat.format(calendar.time)
            
            val borrowCalendar = Calendar.getInstance()
            borrowCalendar.add(Calendar.DAY_OF_YEAR, daysFromNow - 7) // 7 days ago
            val borrowDate = dateFormat.format(borrowCalendar.time)
            
            val testTransaction = mapOf(
                "userId" to currentUser.uid,
                "nameUser" to (currentUser.displayName ?: "Test User"),
                "title" to "Test Book - Return in $daysFromNow days",
                "author" to "Test Author",
                "borrowDate" to borrowDate,
                "returnDate" to returnDate,
                "status" to "Dipinjam",
                "coverUrl" to "",
                "bookId" to "test_book_${System.currentTimeMillis()}",
                "genre" to "Test Genre",
                "publisher" to "Test Publisher",
                "remainingDays" to daysFromNow,
                "stability" to 1
            )
            
            val docRef = db.collection("transactions").add(testTransaction).await()
            Log.d(TAG, "Test transaction created with ID: ${docRef.id}")
            Log.d(TAG, "Return date: $returnDate")
            
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating test transaction", e)
            null
        }
    }
    
    /**
     * Delete test transaction
     */
    suspend fun deleteTestTransaction(transactionId: String) {
        try {
            db.collection("transactions").document(transactionId).delete().await()
            Log.d(TAG, "Test transaction deleted: $transactionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting test transaction", e)
        }
    }
    
    /**
     * Show current user's active transactions
     */
    suspend fun showActiveTransactions() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return
            }
            
            val snapshot = db.collection("transactions")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", "Dipinjam")
                .get()
                .await()
            
            Log.d(TAG, "=== ACTIVE TRANSACTIONS ===")
            snapshot.documents.forEach { doc ->
                val transaction = doc.toObject(com.example.mylibraryapps.model.Transaction::class.java)
                if (transaction != null) {
                    val returnDate = parseDate(transaction.returnDate)
                    val daysRemaining = if (returnDate != null) {
                        calculateDaysDifference(Date(), returnDate)
                    } else 0
                    
                    Log.d(TAG, "ID: ${doc.id}")
                    Log.d(TAG, "Title: ${transaction.title}")
                    Log.d(TAG, "Return Date: ${transaction.returnDate}")
                    Log.d(TAG, "Days Remaining: $daysRemaining")
                    Log.d(TAG, "---")
                }
            }
            Log.d(TAG, "=== END ACTIVE TRANSACTIONS ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing active transactions", e)
        }
    }
    
    /**
     * Clear all notification sent records for testing
     */
    suspend fun clearNotificationSentRecords() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return
            }
            
            val snapshot = db.collection("notification_sent")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
            
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            Log.d(TAG, "All notification sent records cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notification sent records", e)
        }
    }
    
    /**
     * Show notification sent records
     */
    suspend fun showNotificationSentRecords() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return
            }
            
            val snapshot = db.collection("notification_sent")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
            
            Log.d(TAG, "=== NOTIFICATION SENT RECORDS ===")
            snapshot.documents.forEach { doc ->
                val data = doc.data
                Log.d(TAG, "Transaction ID: ${data?.get("transactionId")}")
                Log.d(TAG, "Type: ${data?.get("type")}")
                Log.d(TAG, "Date Sent: ${data?.get("dateSent")}")
                Log.d(TAG, "---")
            }
            Log.d(TAG, "=== END NOTIFICATION SENT RECORDS ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification sent records", e)
        }
    }
    
    /**
     * Update user's FCM token for testing
     */
    suspend fun updateFCMToken() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return
            }
            
            // Generate dummy FCM token for testing
            val dummyToken = "test_fcm_token_${System.currentTimeMillis()}"
            
            db.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", dummyToken)
                .await()
            
            Log.d(TAG, "FCM token updated: $dummyToken")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
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
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
}