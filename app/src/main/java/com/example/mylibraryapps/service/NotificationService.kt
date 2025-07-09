package com.example.mylibraryapps.service

import com.example.mylibraryapps.data.NotificationRepository
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationService {
    private val db = FirebaseFirestore.getInstance()
    private val notificationRepository = NotificationRepository()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun checkAndCreateNotifications() {
        try {
            // Get all active transactions (borrowed books)
            val activeTransactions = getActiveTransactions()
            
            for (transaction in activeTransactions) {
                checkTransactionForNotifications(transaction)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getActiveTransactions(): List<Transaction> {
        return try {
            val snapshot = db.collection("transactions")
                .whereEqualTo("status", "Dipinjam")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun checkTransactionForNotifications(transaction: Transaction) {
        val returnDate = parseDate(transaction.returnDate)
        if (returnDate == null) return

        val currentDate = Calendar.getInstance().time
        val daysDifference = calculateDaysDifference(currentDate, returnDate)

        when {
            daysDifference == 3 -> {
                // 3 days before due date - create reminder
                createReminderIfNotExists(transaction, returnDate, 3)
            }
            daysDifference == 2 -> {
                // 2 days before due date - create reminder
                createReminderIfNotExists(transaction, returnDate, 2)
            }
            daysDifference == 1 -> {
                // 1 day before due date - create urgent reminder
                createReminderIfNotExists(transaction, returnDate, 1)
            }
            daysDifference == 0 -> {
                // Due today - create today reminder
                createReminderIfNotExists(transaction, returnDate, 0)
            }
            daysDifference < 0 -> {
                // Overdue - create overdue notification
                createOverdueNotificationIfNotExists(transaction, returnDate, Math.abs(daysDifference))
            }
        }
    }

    private suspend fun createReminderIfNotExists(transaction: Transaction, returnDate: Date, daysRemaining: Int) {
        // Check if reminder already exists
        if (!reminderExists(transaction.userId, transaction.id, "return_reminder")) {
            val message = when (daysRemaining) {
                3 -> "Buku \"${transaction.title}\" harus dikembalikan dalam 3 hari (${dateFormat.format(returnDate)})."
                2 -> "Buku \"${transaction.title}\" harus dikembalikan dalam 2 hari (${dateFormat.format(returnDate)})."
                1 -> "Buku \"${transaction.title}\" harus dikembalikan besok (${dateFormat.format(returnDate)})."
                0 -> "Buku \"${transaction.title}\" harus dikembalikan hari ini (${dateFormat.format(returnDate)})."
                else -> "Buku \"${transaction.title}\" harus dikembalikan dalam $daysRemaining hari (${dateFormat.format(returnDate)})."
            }

            notificationRepository.createReturnReminder(
                userId = transaction.userId,
                bookTitle = transaction.title,
                bookId = transaction.bookId,
                transactionId = transaction.id,
                returnDate = dateFormat.format(returnDate),
                daysRemaining = daysRemaining
            )
        }
    }

    private suspend fun createOverdueNotificationIfNotExists(transaction: Transaction, returnDate: Date, daysOverdue: Int) {
        // Check if overdue notification already exists
        if (!reminderExists(transaction.userId, transaction.id, "overdue")) {
            notificationRepository.createOverdueNotification(
                userId = transaction.userId,
                bookTitle = transaction.title,
                bookId = transaction.bookId,
                transactionId = transaction.id,
                returnDate = dateFormat.format(returnDate),
                daysOverdue = daysOverdue
            )
        }
    }

    private suspend fun reminderExists(userId: String, transactionId: String, type: String): Boolean {
        return try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("relatedItemId", transactionId)
                .whereEqualTo("type", type)
                .get()
                .await()

            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
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

    suspend fun checkNotificationsForUser(userId: String) {
        try {
            val userTransactions = getUserActiveTransactions(userId)
            for (transaction in userTransactions) {
                checkTransactionForNotifications(transaction)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun getUserActiveTransactions(userId: String): List<Transaction> {
        return try {
            val snapshot = db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Dipinjam")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}