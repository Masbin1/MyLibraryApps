package com.example.mylibraryapps.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylibraryapps.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadBooksFromFirestore()
    }

    fun loadBooksFromFirestore() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val result = db.collection("books")
                    .get()
                    .await()

                val booksList = result.documents.map { document ->
                    document.toObject(Book::class.java)?.copy(id = document.id) ?: Book()
                }

                _books.value = booksList
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data buku: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterBooksByGenre(genre: String) {
        val currentBooks = _books.value ?: return
        if (genre == "Semua") {
            loadBooksFromFirestore()
        } else {
            _books.value = currentBooks.filter { it.genre.equals(genre, ignoreCase = true) }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
