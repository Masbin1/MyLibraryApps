package com.example.mylibraryapps.data

import com.example.mylibraryapps.model.Notification
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
        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Filter out notifications for completed transactions
                val activeNotifications = notifications.filter { notification ->
                    // Keep general notifications and notifications for active transactions
                    notification.type == "general" || notification.relatedItemId.isNotEmpty()
                }

                trySend(activeNotifications)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addNotification(notification: Notification): Boolean {
        return try {
            notificationsCollection.add(notification).await()
            true
        } catch (e: Exception) {
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