package com.example.mylibraryapps.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.mylibraryapps.MyLibraryApplication
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.data.InteractionRepository
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.BookRecommendation
import com.example.mylibraryapps.model.User
import com.example.mylibraryapps.model.UserBookInteraction
import com.example.mylibraryapps.model.InteractionType
import com.example.mylibraryapps.service.CollaborativeFilteringService
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as MyLibraryApplication).repository
    private val interactionRepository = InteractionRepository()
    private val collaborativeFilteringService = CollaborativeFilteringService(interactionRepository)
    
    // Expose repository LiveData
    val books: LiveData<List<Book>> = repository.books
    val isLoading: LiveData<Boolean> = repository.isLoading
    val errorMessage: LiveData<String?> = repository.errorMessage
    
    // Transform user data to just the name
    val userName: LiveData<String> = repository.userData.map { user ->
        user?.nama ?: "Anggota"
    }
    
    // Check if user is admin
    val isAdmin: LiveData<Boolean> = repository.userData.map { user ->
        val isAdmin = user?.is_admin ?: false
        Log.d("HomeViewModel", "User admin status: $isAdmin, User: $user")
        isAdmin
    }
    
    // Collaborative Filtering Data
    private val _recommendedBooks = MutableLiveData<List<BookRecommendation>>()
    val recommendedBooks: LiveData<List<BookRecommendation>> = _recommendedBooks
    
    private val _isLoadingRecommendations = MutableLiveData<Boolean>()
    val isLoadingRecommendations: LiveData<Boolean> = _isLoadingRecommendations
    
    private val _currentFilterType = MutableLiveData<String>("Semua")
    val currentFilterType: LiveData<String> = _currentFilterType

    init {
        // Data is preloaded in the repository, but we can refresh it here if needed
        refreshData()
    }

    fun refreshData() {
        Log.d("HomeViewModel", "Refreshing data...")
        repository.loadBooks()
    }
    
    fun forceRefreshData() {
        Log.d("HomeViewModel", "Force refreshing data...")
        repository.forceLoadBooks()
    }
    
    fun checkFirebaseConnection() {
        Log.d("HomeViewModel", "Checking Firebase connection...")
        repository.checkFirebaseConnection()
    }

    fun loadUserData(userId: String) {
        repository.loadUserData(userId)
    }

    fun clearErrorMessage() {
        repository.clearErrorMessage()
    }
    
    // =============== COLLABORATIVE FILTERING METHODS ===============
    
    fun loadRecommendations(userId: String) {
        viewModelScope.launch {
            try {
                _isLoadingRecommendations.value = true
                Log.d("HomeViewModel", "🔍 Loading recommendations for user: $userId")
                
                val allBooks = books.value ?: emptyList()
                if (allBooks.isEmpty()) {
                    Log.d("HomeViewModel", "No books available for recommendations")
                    _recommendedBooks.value = emptyList()
                    return@launch
                }
                
                val recommendations = collaborativeFilteringService.generateHybridRecommendations(
                    userId = userId,
                    allBooks = allBooks,
                    limit = 15
                )
                
                _recommendedBooks.value = recommendations
                Log.d("HomeViewModel", "✅ Loaded ${recommendations.size} recommendations")
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error loading recommendations", e)
                _recommendedBooks.value = emptyList()
            } finally {
                _isLoadingRecommendations.value = false
            }
        }
    }
    
    fun trackBookView(userId: String, book: Book, viewDuration: Long = 0) {
        viewModelScope.launch {
            try {
                val interaction = UserBookInteraction(
                    userId = userId,
                    bookId = book.id,
                    interactionType = InteractionType.VIEW,
                    timestamp = Timestamp.now(),
                    duration = viewDuration,
                    genre = book.genre,
                    author = book.author
                )
                
                interactionRepository.saveInteraction(interaction)
                Log.d("HomeViewModel", "📊 Tracked book view: ${book.title}")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error tracking book view", e)
            }
        }
    }
    
    fun trackBookSearch(userId: String, searchQuery: String, foundBooks: List<Book>) {
        viewModelScope.launch {
            try {
                foundBooks.forEach { book ->
                    val interaction = UserBookInteraction(
                        userId = userId,
                        bookId = book.id,
                        interactionType = InteractionType.SEARCH,
                        timestamp = Timestamp.now(),
                        genre = book.genre,
                        author = book.author
                    )
                    
                    interactionRepository.saveInteraction(interaction)
                }
                
                Log.d("HomeViewModel", "📊 Tracked search: '$searchQuery' -> ${foundBooks.size} books")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error tracking search", e)
            }
        }
    }
    
    fun rateBook(userId: String, book: Book, rating: Float) {
        viewModelScope.launch {
            try {
                val interaction = UserBookInteraction(
                    userId = userId,
                    bookId = book.id,
                    interactionType = InteractionType.RATE,
                    rating = rating,
                    timestamp = Timestamp.now(),
                    genre = book.genre,
                    author = book.author
                )
                
                interactionRepository.saveInteraction(interaction)
                Log.d("HomeViewModel", "⭐ Book rated: ${book.title} -> $rating stars")
                
                // Refresh recommendations setelah rating
                loadRecommendations(userId)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error rating book", e)
            }
        }
    }
    
    // Filter books dengan tracking
    fun filterBooksByGenre(genre: String) {
        _currentFilterType.value = genre
        repository.filterBooksByGenre(genre)
    }
    
    // Search books dengan tracking
    fun searchBooks(query: String) {
        repository.searchBooks(query)
        
        // Track search setelah hasil didapat
        books.value?.let { bookList ->
            val filteredBooks = if (query.isBlank()) {
                bookList
            } else {
                bookList.filter { book ->
                    book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true) ||
                    book.genre.contains(query, ignoreCase = true)
                }
            }
            
            // Track search untuk user yang sedang login
            repository.userData.value?.uid?.let { userId ->
                trackBookSearch(userId, query, filteredBooks)
            }
        }
    }
}