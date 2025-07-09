package com.example.mylibraryapps.data

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.Notification
import com.example.mylibraryapps.model.Transaction
import com.example.mylibraryapps.model.User
import com.example.mylibraryapps.utils.FirestoreErrorHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.Date
import kotlinx.coroutines.tasks.await

/**
 * Central repository for all data in the application.
 * Implements caching and provides LiveData for UI components.
 */
class AppRepository {
    private val TAG = "AppRepository"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Cache for books
    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books
    private var cachedBooks: List<Book> = emptyList()
    
    // Cache for user data
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData
    
    // Cache for transactions
    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions
    private var cachedTransactions: List<Transaction> = emptyList()
    
    // Cache for notifications
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications
    private var cachedNotifications: List<Notification> = emptyList()
    
    // Unread notifications count
    private val _unreadNotificationsCount = MutableLiveData<Int>()
    val unreadNotificationsCount: LiveData<Int> = _unreadNotificationsCount
    
    // Loading and error states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    /**
     * Preload all data when the application starts
     */
    fun preloadData() {
        // Check if user is authenticated before loading any data
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is authenticated, load all data
            Log.d(TAG, "User authenticated, loading data for user: ${currentUser.uid}")
            loadBooks()
            loadUserData(currentUser.uid)
            loadTransactions()
            loadNotifications(currentUser.uid)
        } else {
            // User is not authenticated, only load public data or show message
            Log.d(TAG, "User not authenticated, loading only public data")
            _errorMessage.value = "Silakan login untuk mengakses semua fitur aplikasi"
            // Optionally load public data that doesn't require authentication
            loadBooks()
        }
    }
    
    /**
     * Handle index errors by using a fallback query
     * This is a workaround for missing indexes
     */
    private fun handleIndexError(collection: String, userId: String, operation: String, onSuccess: (List<Any>) -> Unit) {
        Log.w(TAG, "Using fallback query for $collection due to index error")
        
        // Use a simpler query that doesn't require a composite index
        db.collection(collection)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        when (collection) {
                            "notifications" -> {
                                val notification = doc.toObject(Notification::class.java)?.copy(id = doc.id)
                                notification
                            }
                            "transactions" -> {
                                val transaction = doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                                transaction
                            }
                            else -> null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document ${doc.id}", e)
                        null
                    }
                }
                
                // Sort the results manually (since we can't use orderBy in the query)
                val sortedItems = when (collection) {
                    "notifications" -> {
                        @Suppress("UNCHECKED_CAST")
                        (items as List<Notification>).sortedByDescending { it.timestamp }
                    }
                    "transactions" -> {
                        @Suppress("UNCHECKED_CAST")
                        (items as List<Transaction>).sortedByDescending { 
                            try {
                                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                dateFormat.parse(it.borrowDate)?.time ?: 0
                            } catch (e: Exception) {
                                0L
                            }
                        }
                    }
                    else -> items
                }
                
                onSuccess(sortedItems)
            }
            .addOnFailureListener { e ->
                // If even the simple query fails, log the error
                Log.e(TAG, "Fallback query for $collection also failed", e)
                _errorMessage.value = FirestoreErrorHandler.handleException(e, operation, TAG)
            }
    }
    
    /**
     * Load all books from Firestore
     */
    fun loadBooks() {
        _isLoading.value = true
        
        // Return cached data immediately if available
        if (cachedBooks.isNotEmpty()) {
            _books.value = cachedBooks
        }
        
        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val booksList = result.documents.mapNotNull { doc ->
                    try {
                        // Ambil data buku dan pastikan ID dan coverUrl disertakan
                        val book = doc.toObject(Book::class.java)
                        val coverUrl = doc.getString("coverUrl") ?: ""
                        book?.copy(id = doc.id, coverUrl = coverUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing book ${doc.id}", e)
                        null
                    }
                }
                
                cachedBooks = booksList
                _books.value = booksList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _errorMessage.value = FirestoreErrorHandler.handleException(e, "mengakses data buku", TAG)
                _isLoading.value = false
            }
    }
    
    /**
     * Filter books by genre
     */
    fun filterBooksByGenre(genre: String) {
        if (genre == "Semua") {
            _books.value = cachedBooks
            return
        }
        
        _books.value = cachedBooks.filter {
            it.genre.equals(genre, ignoreCase = true)
        }
    }
    
    /**
     * Load user data from Firestore
     */
    fun loadUserData(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        // Log raw data for debugging
                        Log.d(TAG, "User document data: ${document.data}")
                        
                        // Check if is_admin field exists and its value
                        val isAdmin = document.getBoolean("is_admin")
                        Log.d(TAG, "is_admin field value: $isAdmin")
                        
                        // Create user object manually to ensure correct field mapping
                        val user = User(
                            uid = document.id,
                            nama = document.getString("nama") ?: "",
                            nis = document.getString("nis") ?: "",
                            email = document.getString("email") ?: "",
                            kelas = document.getString("kelas") ?: "",
                            is_admin = document.getBoolean("is_admin") ?: false
                        )
                        Log.d(TAG, "Manually created user object: $user")
                        
                        _userData.value = user
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user data", e)
                        _errorMessage.value = "Error loading user data: ${e.message}"
                    }
                } else {
                    _userData.value = null
                    Log.d(TAG, "No user document found for ID: $userId")
                }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = FirestoreErrorHandler.handleException(e, "mengakses data pengguna", TAG)
            }
    }
    
    /**
     * Load transactions from Firestore
     */
    fun loadTransactions(statusFilter: String? = null) {
        _isLoading.value = true
        
        // Return cached data immediately if available
        if (cachedTransactions.isNotEmpty()) {
            filterAndSetTransactions(statusFilter)
        }
        
        val currentUser = auth.currentUser ?: run {
            _errorMessage.value = "User not authenticated"
            _isLoading.value = false
            return
        }
        
        // First check if user is admin
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val isAdmin = userDoc.getBoolean("is_admin") ?: false
                
                // Create base query
                val query = if (isAdmin) {
                    // If admin, get all transactions
                    db.collection("transactions")
                } else {
                    // If not admin, filter by userId
                    db.collection("transactions")
                        .whereEqualTo("userId", currentUser.uid)
                }
                
                // Execute query
                query.get()
                    .addOnSuccessListener { snapshot ->
                        val transactions = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing transaction ${doc.id}", e)
                                null
                            }
                        }
                        
                        cachedTransactions = transactions
                        filterAndSetTransactions(statusFilter)
                        _isLoading.value = false
                    }
                    .addOnFailureListener { e ->
                        _errorMessage.value = FirestoreErrorHandler.handleException(e, "mengakses data transaksi", TAG)
                        _isLoading.value = false
                    }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = FirestoreErrorHandler.handleException(e, "memeriksa status admin", TAG)
                _isLoading.value = false
            }
    }
    
    /**
     * Filter transactions by status and update LiveData
     */
    private fun filterAndSetTransactions(statusFilter: String?) {
        val filteredList = if (statusFilter != null) {
            cachedTransactions.filter { it.status == statusFilter }
        } else {
            cachedTransactions
        }
        
        _transactions.value = filteredList
    }
    
    /**
     * Add a new book to Firestore
     */
    fun addBook(book: Book, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("books")
            .add(book)
            .addOnSuccessListener { 
                // Reload books to update cache
                loadBooks()
                onSuccess() 
            }
            .addOnFailureListener { e -> 
                onFailure(e) 
            }
    }
    
    /**
     * Update a book in Firestore
     */
    fun updateBook(book: Book, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("books").document(book.id)
            .set(book)
            .addOnSuccessListener {
                // Update cache
                val updatedList = cachedBooks.toMutableList()
                val index = updatedList.indexOfFirst { it.id == book.id }
                if (index != -1) {
                    updatedList[index] = book
                    cachedBooks = updatedList
                    _books.value = updatedList
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    
    /**
     * Process a book borrow transaction
     */
    fun borrowBook(
        book: Book,
        transaction: Transaction,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Create batch operation for atomic update
        val batch = db.batch()
        
        // Add transaction
        val transactionRef = db.collection("transactions").document()
        batch.set(transactionRef, transaction)
        
        // Update book quantity
        val bookRef = db.collection("books").document(book.id)
        batch.update(bookRef, "quantity", book.quantity - 1)
        
        // Execute batch
        batch.commit()
            .addOnSuccessListener {
                // Update caches
                loadBooks()
                loadTransactions()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    
    /**
     * Load notifications for a user
     */
    fun loadNotifications(userId: String) {
        _isLoading.value = true
        
        // Return cached data immediately if available
        if (cachedNotifications.isNotEmpty()) {
            _notifications.value = cachedNotifications
            updateUnreadCount()
        }
        
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val notificationsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Convert Firestore timestamp to Date
                        val data = doc.data
                        val timestamp = (data?.get("timestamp") as? com.google.firebase.Timestamp)?.toDate() ?: java.util.Date()
                        
                        Notification(
                            id = doc.id,
                            userId = data?.get("userId") as? String ?: "",
                            title = data?.get("title") as? String ?: "",
                            message = data?.get("message") as? String ?: "",
                            timestamp = timestamp as java.util.Date,
                            isRead = data?.get("isRead") as? Boolean ?: false,
                            type = data?.get("type") as? String ?: "general",
                            relatedItemId = data?.get("relatedItemId") as? String ?: "",
                            relatedItemTitle = data?.get("relatedItemTitle") as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification ${doc.id}", e)
                        null
                    }
                }
                
                cachedNotifications = notificationsList
                _notifications.value = notificationsList
                updateUnreadCount()
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                // Check if this is an index error
                if (e.message?.contains("FAILED_PRECONDITION") == true && 
                    e.message?.contains("index") == true) {
                    
                    Log.w(TAG, "Index error detected in loadNotifications. Full error: ${e.message}")
                    
                    // Use fallback query without the complex ordering
                    handleIndexError("notifications", userId, "mengakses notifikasi") { items ->
                        @Suppress("UNCHECKED_CAST")
                        val notificationsList = items as List<Notification>
                        
                        cachedNotifications = notificationsList
                        _notifications.value = notificationsList
                        updateUnreadCount()
                        _isLoading.value = false
                    }
                } else {
                    // Handle other types of errors
                    _errorMessage.value = FirestoreErrorHandler.handleException(e, "mengakses notifikasi", TAG)
                    _isLoading.value = false
                }
            }
    }
    
    /**
     * Mark a notification as read
     */
    fun markNotificationAsRead(notificationId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                // Update cache
                val updatedList = cachedNotifications.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
                cachedNotifications = updatedList
                _notifications.value = updatedList
                updateUnreadCount()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    
    /**
     * Mark all notifications as read
     */
    fun markAllNotificationsAsRead(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            onFailure(Exception("User not authenticated"))
            return
        }
        
        // Get all unread notifications
        val unreadNotifications = cachedNotifications.filter { !it.isRead }
        
        if (unreadNotifications.isEmpty()) {
            onSuccess()
            return
        }
        
        // Create a batch operation
        val batch = db.batch()
        
        // Add update operations to batch
        unreadNotifications.forEach { notification ->
            val notificationRef = db.collection("notifications").document(notification.id)
            batch.update(notificationRef, "isRead", true)
        }
        
        // Execute batch
        batch.commit()
            .addOnSuccessListener {
                // Update cache
                val updatedList = cachedNotifications.map { notification ->
                    notification.copy(isRead = true)
                }
                cachedNotifications = updatedList
                _notifications.value = updatedList
                _unreadNotificationsCount.value = 0
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    
    /**
     * Create a notification for a user
     */
    fun createNotification(notification: Notification, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Convert Date to Firestore Timestamp
        val firestoreTimestamp = com.google.firebase.Timestamp(notification.timestamp)
        
        // Create notification data
        val notificationData = hashMapOf(
            "userId" to notification.userId,
            "title" to notification.title,
            "message" to notification.message,
            "timestamp" to firestoreTimestamp,
            "isRead" to notification.isRead,
            "type" to notification.type,
            "relatedItemId" to notification.relatedItemId,
            "relatedItemTitle" to notification.relatedItemTitle
        )
        
        // Add to Firestore
        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener { documentReference ->
                // Update cache with new notification
                val newNotification = notification.copy(id = documentReference.id)
                val updatedList = listOf(newNotification) + cachedNotifications
                cachedNotifications = updatedList
                _notifications.value = updatedList
                updateUnreadCount()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    
    /**
     * Create return reminder notifications for all users with books due soon
     */
    fun createReturnReminders() {
        // Get all active transactions
        db.collection("transactions")
            .whereEqualTo("status", "dipinjam")
            .get()
            .addOnSuccessListener { snapshot ->
                val transactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction ${doc.id}", e)
                        null
                    }
                }
                
                // Check each transaction for due date
                val calendar = Calendar.getInstance()
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = dateFormat.format(calendar.time)
                
                transactions.forEach { transaction ->
                    try {
                        val returnDate = dateFormat.parse(transaction.returnDate)
                        val todayDate = dateFormat.parse(today)
                        
                        if (returnDate != null && todayDate != null) {
                            val diff = (returnDate.time - todayDate.time) / (24 * 60 * 60 * 1000)
                            
                            // Create notification for books due in 2 days or overdue
                            if (diff <= 2) {
                                val title = if (diff < 0) "Buku Terlambat" else "Pengingat Pengembalian"
                                val message = if (diff < 0) 
                                    "Buku '${transaction.title}' sudah melewati batas waktu pengembalian. Segera kembalikan untuk menghindari denda." 
                                else 
                                    "Buku '${transaction.title}' harus dikembalikan dalam $diff hari. Jangan lupa untuk mengembalikannya tepat waktu."
                                
                                val notification = Notification(
                                    userId = transaction.userId,
                                    title = title,
                                    message = message,
                                    timestamp = java.util.Date(),
                                    isRead = false,
                                    type = if (diff < 0) "overdue" else "return_reminder",
                                    relatedItemId = transaction.id,
                                    relatedItemTitle = transaction.title
                                )
                                
                                // Check if similar notification already exists
                                val existingNotification = cachedNotifications.find { 
                                    it.relatedItemId == transaction.id && 
                                    it.type == notification.type &&
                                    !it.isRead
                                }
                                
                                if (existingNotification == null) {
                                    createNotification(notification, {}, { e ->
                                        Log.e(TAG, "Failed to create reminder notification", e)
                                    })
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing transaction date", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading transactions for reminders", e)
            }
    }
    
    /**
     * Update unread notifications count
     */
    private fun updateUnreadCount() {
        val unreadCount = cachedNotifications.count { !it.isRead }
        _unreadNotificationsCount.value = unreadCount
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}