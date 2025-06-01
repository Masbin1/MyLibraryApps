package com.example.mylibraryapps.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mylibraryapps.model.Book
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _books = MutableLiveData<List<Book>>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _errorMessage = MutableLiveData<String?>()

    val books: LiveData<List<Book>> = _books
    val isLoading: LiveData<Boolean> = _isLoading
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadBooks()
    }

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    fun loadUserData(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _userName.value = document.getString("nama") ?: "Anggota"
                } else {
                    _userName.value = "Anggota"
                }
            }
            .addOnFailureListener {
                _userName.value = "Anggota"
                Log.e("HomeVM", "Error loading user data", it)
            }
    }

    fun loadBooks() {
        _isLoading.value = true
        _errorMessage.value = null

        db.collection("books")
            .get()
            .addOnCompleteListener { task ->
                _isLoading.value = false

                if (task.isSuccessful) {
                    val booksList = mutableListOf<Book>()
                    for (document in task.result) {
                        try {
                            val book = document.toObject(Book::class.java).copy(id = document.id)
                            booksList.add(book)
                        } catch (e: Exception) {
                            Log.e("HomeVM", "Error parsing book ${document.id}", e)
                        }
                    }
                    _books.value = booksList
                } else {
                    _errorMessage.value = "Gagal memuat data: ${task.exception?.message ?: "Unknown error"}"
                    Log.e("HomeVM", "Error loading books", task.exception)
                }
            }
    }

    fun filterBooksByGenre(genre: String) {
        val currentList = _books.value ?: return

        if (genre == "Semua") {
            loadBooks()
        } else {
            _books.value = currentList.filter {
                it.genre.equals(genre, ignoreCase = true)
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}