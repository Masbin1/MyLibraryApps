package com.example.mylibraryapps.data

import com.example.mylibraryapps.model.Notification
import com.example.mylibraryapps.utils.SafeFirestoreConverter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        android.util.Log.d("NotificationRepository", "🔍 Setting up listener for user: $userId")
        
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            // Sementara tanpa ordering untuk testing
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationRepository", "❌ Error getting notifications", error)
                    close(error)
                    return@addSnapshotListener
                }

                android.util.Log.d("NotificationRepository", "📡 Received snapshot with ${snapshot?.documents?.size ?: 0} documents")

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    android.util.Log.d("NotificationRepository", "📄 Processing document: ${doc.id}")
                    android.util.Log.d("NotificationRepository", "📄 Document data: ${doc.data}")
                    SafeFirestoreConverter.documentToNotification(doc)
                } ?: emptyList()

                android.util.Log.d("NotificationRepository", "✅ Converted ${notifications.size} notifications")

                // Log setiap notification untuk debugging
                notifications.forEachIndexed { index, notification ->
                    android.util.Log.d("NotificationRepository", "📋 Notification $index: ID=${notification.id}, Title=${notification.title}, Type=${notification.type}")
                }

                // Tidak filter apapun dulu, kirim semua notifications
                android.util.Log.d("NotificationRepository", "🎯 Sending ALL ${notifications.size} notifications (no filtering)")
                trySend(notifications)
            }

        awaitClose { 
            android.util.Log.d("NotificationRepository", "🔚 Removing listener for user: $userId")
            listener.remove() 
        }
    }

    suspend fun addNotification(notification: Notification): Boolean {
        return try {
            android.util.Log.d("NotificationRepository", "➕ Adding notification: ${notification.title}")
            
            // Create a copy with proper ID
            val docRef = notificationsCollection.document()
            val notificationWithId = notification.copy(id = docRef.id)
            
            // Convert to map dengan field yang sesuai struktur Firebase
            val notificationMap = mapOf(
                "id" to notificationWithId.id,
                "userId" to notificationWithId.userId,
                "title" to notificationWithId.title,
                "message" to notificationWithId.message,
                "createdAt" to notificationWithId.timestamp, // Gunakan createdAt
                "isRead" to notificationWithId.isRead,
                "type" to notificationWithId.type,
                "transactionId" to notificationWithId.transactionId
            )
            
            docRef.set(notificationMap).await()
            android.util.Log.d("NotificationRepository", "✅ Notification added with ID: ${docRef.id}")
            android.util.Log.d("NotificationRepository", "✅ Data saved: $notificationMap")
            true
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "❌ Error adding notification", e)
            false
        }
    }

    suspend fun markAsRead(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAllAsRead(userId: String): Boolean {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun deleteNotification(notificationId: String): Boolean {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createReturnReminder(
        userId: String,
        bookTitle: String,
        bookId: String,
        transactionId: String,
        returnDate: String,
        daysRemaining: Int
    ): Boolean {
        val notification = Notification(
            userId = userId,
            title = "Pengingat Pengembalian Buku",
            message = "Buku \"$bookTitle\" harus dikembalikan dalam $daysRemaining hari (tanggal $returnDate).",
            timestamp = Date(),
            type = "return_reminder",
            relatedItemId = transactionId,
            relatedItemTitle = bookTitle,
            isRead = false
        )
        return addNotification(notification)
    }

    suspend fun createOverdueNotification(
        userId: String,
        bookTitle: String,
        bookId: String,
        transactionId: String,
        returnDate: String,
        daysOverdue: Int
    ): Boolean {
        val notification = Notification(
            userId = userId,
            title = "Buku Terlambat Dikembalikan",
            message = "Buku \"$bookTitle\" sudah terlambat dikembalikan $daysOverdue hari dari tanggal $returnDate.",
            timestamp = Date(),
            type = "overdue",
            relatedItemId = transactionId,
            relatedItemTitle = bookTitle,
            isRead = false
        )
        return addNotification(notification)
    }
}