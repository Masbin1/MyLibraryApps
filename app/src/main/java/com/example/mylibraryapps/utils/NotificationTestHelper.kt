package com.example.mylibraryapps.utils

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mylibraryapps.service.NotificationWorker
import com.example.mylibraryapps.utils.SafeFirestoreConverter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationTestHelper(private val context: Context) {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
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
        val message = "Notifikasi ini muncul di notification bar Android seperti WhatsApp! 🎉\n\nTap untuk membuka aplikasi."
        val data = mapOf(
            "type" to "system_test",
            "title" to title,
            "body" to message,
            "timestamp" to System.currentTimeMillis().toString()
        )
        
        localNotificationHelper.showSystemNotification(title, message, data)
        Log.d(TAG, "✅ Direct system notification sent to Android notification bar")
        Log.d(TAG, "📱 Check your notification panel now!")
        
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
     * Test WhatsApp-style notifications with different types
     * This version also saves to Firebase
     */
    fun testWhatsAppStyleNotifications() {
        val localNotificationHelper = LocalNotificationHelper(context)
        
        // Test 1: Welcome notification
        val welcomeData = mapOf(
            "type" to "system_test",
            "title" to "🎉 Selamat Datang!",
            "body" to "Aplikasi MyLibrary siap digunakan. Notifikasi aktif!",
            "timestamp" to System.currentTimeMillis().toString()
        )
        localNotificationHelper.showSystemNotification(
            "🎉 Selamat Datang!",
            "Aplikasi MyLibrary siap digunakan. Notifikasi aktif!",
            welcomeData
        )
        // Save to Firebase
        saveNotificationToFirebase(
            "🎉 Selamat Datang!",
            "Aplikasi MyLibrary siap digunakan. Notifikasi aktif!",
            "system_test"
        )
        
        // Test 2: Reminder notification (delay 2 seconds)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val reminderData = mapOf(
                "type" to "return_reminder",
                "title" to "📚 Pengingat Pengembalian",
                "body" to "Buku 'Android Development Guide' harus dikembalikan dalam 2 hari (25/01/2024)",
                "bookTitle" to "Android Development Guide",
                "daysRemaining" to "2"
            )
            localNotificationHelper.showSystemNotification(
                "📚 Pengingat Pengembalian",
                "Buku 'Android Development Guide' harus dikembalikan dalam 2 hari (25/01/2024)",
                reminderData
            )
            // Save to Firebase
            saveNotificationToFirebase(
                "📚 Pengingat Pengembalian",
                "Buku 'Android Development Guide' harus dikembalikan dalam 2 hari (25/01/2024)",
                "return_reminder",
                mapOf(
                    "bookTitle" to "Android Development Guide",
                    "daysRemaining" to "2"
                )
            )
        }, 2000)
        
        // Test 3: Overdue notification (delay 4 seconds)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val overdueData = mapOf(
                "type" to "overdue",
                "title" to "⚠️ Buku Terlambat!",
                "body" to "Buku 'Kotlin Programming' sudah terlambat 1 hari. Segera kembalikan untuk menghindari denda!",
                "bookTitle" to "Kotlin Programming",
                "daysOverdue" to "1"
            )
            localNotificationHelper.showSystemNotification(
                "⚠️ Buku Terlambat!",
                "Buku 'Kotlin Programming' sudah terlambat 1 hari. Segera kembalikan untuk menghindari denda!",
                overdueData
            )
            // Save to Firebase
            saveNotificationToFirebase(
                "⚠️ Buku Terlambat!",
                "Buku 'Kotlin Programming' sudah terlambat 1 hari. Segera kembalikan untuk menghindari denda!",
                "overdue",
                mapOf(
                    "bookTitle" to "Kotlin Programming",
                    "daysOverdue" to "1"
                )
            )
        }, 4000)
        
        Log.d(TAG, "🚀 WhatsApp-style notifications sent! Check your notification panel.")
        Log.d(TAG, "📱 You should see 3 notifications appearing with 2-second intervals")
        Log.d(TAG, "💾 Notifications also saved to Firebase 'notifications' collection")
        Log.d(TAG, "🔴 Check app icon for notification badge!")
    }
    
    /**
     * Test notification that directly saves to database (simulates FCM)
     */
    fun testDatabaseNotificationDirect() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "No user logged in")
            return
        }
        
        // Simulate FCM notification data
        val title = "📚 Test Database Notification"
        val message = "This notification is saved directly to Firebase database for testing badge functionality!"
        val type = "test_database"
        
        // Save to Firebase (simulating what FCM Service does)
        val notificationData = mapOf(
            "userId" to currentUser.uid,
            "title" to title,
            "message" to message,
            "type" to type,
            "isRead" to false,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "timestamp" to System.currentTimeMillis(),
            "source" to "test" // Mark as test notification
        )
        
        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "✅ Test notification saved to database with ID: ${documentReference.id}")
                Log.d(TAG, "🔴 Check app for notification badge update!")
                
                // Send broadcast to update badge (simulating FCM Service)
                val intent = android.content.Intent("com.example.mylibraryapps.NOTIFICATION_RECEIVED")
                context.sendBroadcast(intent)
                Log.d(TAG, "📡 Badge update broadcast sent")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error saving test notification to database", e)
            }
        
        // Also show local notification
        val localNotificationHelper = LocalNotificationHelper(context)
        localNotificationHelper.showSystemNotification(title, message, mapOf("type" to type))
    }
    
    /**
     * Save notification to Firebase notifications collection
     */
    private fun saveNotificationToFirebase(
        title: String,
        message: String,
        type: String,
        extraData: Map<String, String> = emptyMap()
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "No user logged in, cannot save notification to Firebase")
            return
        }
        
        val notificationData = mutableMapOf(
            "userId" to currentUser.uid,
            "title" to title,
            "message" to message,
            "type" to type,
            "isRead" to false,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "timestamp" to System.currentTimeMillis()
        )
        
        // Add extra data
        notificationData.putAll(extraData)
        
        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "✅ Notification saved to Firebase with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error saving notification to Firebase", e)
            }
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
                "status" to "sedang dipinjam",
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
                .whereEqualTo("status", "sedang dipinjam")
                .get()
                .await()
            
            Log.d(TAG, "=== ACTIVE TRANSACTIONS ===")
            snapshot.documents.forEach { doc ->
                val transaction = SafeFirestoreConverter.documentToTransaction(doc)
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
     * Update user's FCM token for testing (using real FCM token)
     */
    suspend fun updateFCMToken() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return
            }
            
            // Get real FCM token
            val token = FirebaseMessaging.getInstance().token.await()
            
            db.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .await()
            
            Log.d(TAG, "Real FCM token updated: $token")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
        }
    }
    
    /**
     * Debug Firebase Functions - Check transaction data
     */
    suspend fun debugFirebaseTransactions() {
        try {
            withContext(Dispatchers.IO) {
                val projectId = FirebaseApp.getInstance().options.projectId
                val functionUrl = "https://asia-southeast2-$projectId.cloudfunctions.net/debugTransactions"
                
                Log.d(TAG, "🔍 Calling Debug Firebase Functions...")
                Log.d(TAG, "📡 Project ID: $projectId")
                Log.d(TAG, "📡 URL: $functionUrl")
                
                val url = URL(functionUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.setRequestProperty("Content-Type", "application/json")
                
                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: "No error message"
                }
                
                Log.d(TAG, "🔍 Debug Functions Response Code: $responseCode")
                Log.d(TAG, "📝 Debug Functions Response: $response")
                
                if (responseCode == 200) {
                    Log.d(TAG, "✅ Debug Functions executed successfully!")
                    Log.d(TAG, "📋 Check Firebase Console for detailed transaction data")
                } else {
                    Log.e(TAG, "❌ Debug Functions failed with code: $responseCode")
                }
                
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calling Debug Firebase Functions", e)
        }
    }
    
    /**
     * Test Firebase Functions manual trigger
     */
    suspend fun testFirebaseFunctionsManualTrigger() {
        try {
            withContext(Dispatchers.IO) {
                // Get Firebase project ID automatically
                val projectId = FirebaseApp.getInstance().options.projectId
                val functionUrl = "https://asia-southeast2-$projectId.cloudfunctions.net/manualBookReminderCheck"
                
                Log.d(TAG, "🔥 Calling Firebase Functions...")
                Log.d(TAG, "📡 Project ID: $projectId")
                Log.d(TAG, "📡 URL: $functionUrl")
                
                val url = URL(functionUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 30000 // Increase timeout
                connection.readTimeout = 30000
                connection.setRequestProperty("Content-Type", "application/json")
                
                val responseCode = connection.responseCode
                val response = if (responseCode == 200) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: "No error message"
                }
                
                Log.d(TAG, "🔥 Firebase Functions Response Code: $responseCode")
                Log.d(TAG, "📝 Firebase Functions Response: $response")
                
                if (responseCode == 200) {
                    Log.d(TAG, "✅ Firebase Functions executed successfully!")
                    Log.d(TAG, "📱 Check your device for push notifications")
                    Log.d(TAG, "📋 Check Firebase Console for function logs")
                } else {
                    Log.e(TAG, "❌ Firebase Functions failed with code: $responseCode")
                    Log.e(TAG, "💡 Make sure:")
                    Log.e(TAG, "   1. Functions are deployed: firebase deploy --only functions")
                    Log.e(TAG, "   2. Project ID is correct in the URL")
                    Log.e(TAG, "   3. Functions have proper permissions")
                }
                
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calling Firebase Functions", e)
            Log.e(TAG, "💡 Possible issues:")
            Log.e(TAG, "   1. Network connection problem")
            Log.e(TAG, "   2. Functions not deployed")
            Log.e(TAG, "   3. Wrong project ID in URL")
            Log.e(TAG, "   4. Functions region mismatch")
        }
    }
    
    /**
     * Check notifications collection in Firestore
     */
    suspend fun checkNotificationsInFirestore() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return
            }
            
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
            
            Log.d(TAG, "=== 📋 NOTIFICATIONS IN FIREBASE ===")
            Log.d(TAG, "📊 Found ${snapshot.documents.size} notifications")
            Log.d(TAG, "")
            
            if (snapshot.documents.isEmpty()) {
                Log.d(TAG, "❌ No notifications found in Firebase")
                Log.d(TAG, "💡 Try clicking 'Test WhatsApp-Style Notifications' first")
            } else {
                snapshot.documents.forEachIndexed { index, doc ->
                    val data = doc.data
                    Log.d(TAG, "📱 Notification #${index + 1}")
                    Log.d(TAG, "🆔 ID: ${doc.id}")
                    Log.d(TAG, "📝 Title: ${data?.get("title")}")
                    Log.d(TAG, "💬 Message: ${data?.get("message")}")
                    Log.d(TAG, "🏷️ Type: ${data?.get("type")}")
                    Log.d(TAG, "👁️ Is Read: ${data?.get("isRead")}")
                    Log.d(TAG, "📅 Created At: ${data?.get("createdAt")}")
                    Log.d(TAG, "⏰ Timestamp: ${data?.get("timestamp")}")
                    
                    // Show extra data if available
                    val bookTitle = data?.get("bookTitle")
                    val daysRemaining = data?.get("daysRemaining")
                    val daysOverdue = data?.get("daysOverdue")
                    
                    if (bookTitle != null) Log.d(TAG, "📚 Book: $bookTitle")
                    if (daysRemaining != null) Log.d(TAG, "📅 Days Remaining: $daysRemaining")
                    if (daysOverdue != null) Log.d(TAG, "⚠️ Days Overdue: $daysOverdue")
                    
                    Log.d(TAG, "---")
                }
            }
            Log.d(TAG, "=== END NOTIFICATIONS ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notifications in Firestore", e)
        }
    }
    
    /**
     * Clear all notifications from Firebase
     */
    suspend fun clearNotificationsFromFirebase() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return
            }
            
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
            
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            Log.d(TAG, "✅ All ${snapshot.documents.size} notifications cleared from Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing notifications from Firebase", e)
        }
    }
    
    /**
     * Create test transaction with proper borrowDate calculation for Firebase Functions
     */
    suspend fun createTestTransactionForFirebaseFunctions(daysFromBorrow: Int): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "No user logged in")
                return null
            }
            
            // Calculate borrowDate (days ago from today)
            val borrowCalendar = Calendar.getInstance()
            borrowCalendar.add(Calendar.DAY_OF_YEAR, -daysFromBorrow) // Negative for days ago
            val borrowDate = dateFormat.format(borrowCalendar.time)
            
            // Calculate returnDate (7 days from borrowDate)
            val returnCalendar = Calendar.getInstance()
            returnCalendar.time = borrowCalendar.time
            returnCalendar.add(Calendar.DAY_OF_YEAR, 7)
            val returnDate = dateFormat.format(returnCalendar.time)
            
            val testTransaction = mapOf(
                "userId" to currentUser.uid,
                "nameUser" to (currentUser.displayName ?: "Test User"),
                "title" to "Test Book - Borrowed $daysFromBorrow days ago",
                "author" to "Test Author",
                "borrowDate" to borrowDate,
                "returnDate" to returnDate,
                "status" to "sedang dipinjam",
                "coverUrl" to "",
                "bookId" to "test_book_${System.currentTimeMillis()}",
                "genre" to "Test Genre",
                "publisher" to "Test Publisher"
            )
            
            val docRef = db.collection("transactions").add(testTransaction).await()
            
            val daysSinceBorrow = daysFromBorrow
            val daysRemaining = 7 - daysSinceBorrow
            
            Log.d(TAG, "=== TEST TRANSACTION CREATED ===")
            Log.d(TAG, "Transaction ID: ${docRef.id}")
            Log.d(TAG, "Borrow Date: $borrowDate")
            Log.d(TAG, "Return Date: $returnDate")
            Log.d(TAG, "Days Since Borrow: $daysSinceBorrow")
            Log.d(TAG, "Days Remaining: $daysRemaining")
            Log.d(TAG, "Expected Notification Type: ${getExpectedNotificationType(daysRemaining)}")
            Log.d(TAG, "================================")
            
            docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating test transaction for Firebase Functions", e)
            null
        }
    }
    
    private fun getExpectedNotificationType(daysRemaining: Int): String {
        return when {
            daysRemaining < 0 -> "overdue (${Math.abs(daysRemaining)} days late)"
            daysRemaining == 3 -> "return_reminder (3 days left)"
            daysRemaining == 2 -> "return_reminder (2 days left)"
            daysRemaining == 1 -> "return_reminder (1 day left)"
            else -> "no notification (${daysRemaining} days remaining)"
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