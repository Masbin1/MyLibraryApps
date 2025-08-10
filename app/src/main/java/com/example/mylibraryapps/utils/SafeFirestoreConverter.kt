package com.example.mylibraryapps.utils

import android.util.Log
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.Notification
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp

object SafeFirestoreConverter {
    private const val TAG = "SafeFirestoreConverter"
    
    /**
     * Safely convert DocumentSnapshot to Book
     */
    fun documentToBook(document: DocumentSnapshot): Book? {
        return try {
            val book = Book(
                id = document.getString("id") ?: document.id,
                title = document.getString("title") ?: "",
                author = document.getString("author") ?: "",
                publisher = document.getString("publisher") ?: "",
                purchaseDate = FirestoreConverters.convertToTimestamp(document.get("purchaseDate")),
                specifications = document.getString("specifications") ?: "",
                material = document.getString("material") ?: "",
                quantity = FirestoreConverters.convertToLong(document.get("quantity")),
                genre = document.getString("genre") ?: "",
                coverUrl = document.getString("coverUrl") ?: ""
            )
            Log.d(TAG, "Successfully converted document to Book: ${book.id}")
            book
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to Book: ${document.id}", e)
            null
        }
    }
    
    /**
     * Safely convert DocumentSnapshot to Notification
     */
    fun documentToNotification(document: DocumentSnapshot): Notification? {
        return try {
            val notification = Notification(
                id = document.getString("id") ?: document.id,
                userId = document.getString("userId") ?: "",
                title = document.getString("title") ?: "",
                message = document.getString("message") ?: "",
                timestamp = FirestoreConverters.convertToDate(document.get("timestamp")),
                isRead = FirestoreConverters.convertToBoolean(document.get("isRead")),
                type = document.getString("type") ?: "general",
                relatedItemId = document.getString("relatedItemId") ?: "",
                relatedItemTitle = document.getString("relatedItemTitle") ?: "",
                transactionId = document.getString("transactionId") ?: ""
            )
            Log.d(TAG, "Successfully converted document to Notification: ${notification.id}")
            notification
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to Notification: ${document.id}", e)
            null
        }
    }
    
    /**
     * Convert Book to Map for Firestore
     */
    fun bookToMap(book: Book): Map<String, Any> {
        return mapOf(
            "id" to book.id,
            "title" to book.title,
            "author" to book.author,
            "publisher" to book.publisher,
            "purchaseDate" to book.purchaseDate,
            "specifications" to book.specifications,
            "material" to book.material,
            "quantity" to book.quantity,
            "genre" to book.genre,
            "coverUrl" to book.coverUrl
        )
    }
    
    /**
     * Safely convert DocumentSnapshot to Transaction
     */
    fun documentToTransaction(document: DocumentSnapshot): Transaction? {
        return try {
            val transaction = Transaction(
                id = document.getString("id") ?: document.id,
                nameUser = document.getString("nameUser") ?: "",
                title = document.getString("title") ?: "",
                author = document.getString("author") ?: "",
                borrowDate = document.getString("borrowDate") ?: "",
                returnDate = document.getString("returnDate") ?: "",
                status = document.getString("status") ?: "",
                coverUrl = document.getString("coverUrl") ?: "",
                userId = document.getString("userId") ?: "",
                bookId = document.getString("bookId") ?: "",
                genre = document.getString("genre") ?: "",
                publisher = document.getString("publisher") ?: "",
                remainingDays = FirestoreConverters.convertToLong(document.get("remainingDays")).toInt(),
                stability = FirestoreConverters.convertToLong(document.get("stability")).toInt()
            )
            Log.d(TAG, "Successfully converted document to Transaction: ${transaction.id}")
            transaction
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to Transaction: ${document.id}", e)
            null
        }
    }

    /**
     * Convert Notification to Map for Firestore
     */
    fun notificationToMap(notification: Notification): Map<String, Any> {
        return mapOf(
            "id" to notification.id,
            "userId" to notification.userId,
            "title" to notification.title,
            "message" to notification.message,
            "timestamp" to Timestamp(notification.timestamp),
            "isRead" to notification.isRead,
            "type" to notification.type,
            "relatedItemId" to notification.relatedItemId,
            "relatedItemTitle" to notification.relatedItemTitle,
            "transactionId" to notification.transactionId
        )
    }
    
    /**
     * Convert Transaction to Map for Firestore
     */
    fun transactionToMap(transaction: Transaction): Map<String, Any> {
        return mapOf(
            "id" to transaction.id,
            "nameUser" to transaction.nameUser,
            "title" to transaction.title,
            "author" to transaction.author,
            "borrowDate" to transaction.borrowDate,
            "returnDate" to transaction.returnDate,
            "status" to transaction.status,
            "coverUrl" to transaction.coverUrl,
            "userId" to transaction.userId,
            "bookId" to transaction.bookId,
            "genre" to transaction.genre,
            "publisher" to transaction.publisher,
            "remainingDays" to transaction.remainingDays,
            "stability" to transaction.stability
        )
    }
}