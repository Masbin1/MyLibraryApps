package com.example.mylibraryapps.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.Transaction
import com.example.mylibraryapps.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    
    // Loading and error states
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    /**
     * Preload all data when the application starts
     */
    fun preloadData() {
        loadBooks()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserData(currentUser.uid)
            loadTransactions()
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
                        doc.toObject(Book::class.java)?.copy(id = doc.id)
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
                Log.e(TAG, "Error loading books", e)
                _errorMessage.value = "Failed to load books: ${e.message}"
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
                Log.e(TAG, "Error loading user data", e)
                _errorMessage.value = "Failed to load user data: ${e.message}"
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
                        Log.e(TAG, "Error loading transactions", e)
                        _errorMessage.value = "Failed to load transactions: ${e.message}"
                        _isLoading.value = false
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking admin status", e)
                _errorMessage.value = "Failed to check admin status: ${e.message}"
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
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}