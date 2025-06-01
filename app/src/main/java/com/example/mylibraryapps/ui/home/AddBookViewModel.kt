package com.example.mylibraryapps.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mylibraryapps.data.BookRepository
import com.example.mylibraryapps.model.Book
import com.google.firebase.Timestamp

class AddBookViewModel(private val repository: BookRepository = BookRepository()) : ViewModel() {
    private val _addBookSuccess = MutableLiveData<Boolean>()
    val addBookSuccess: LiveData<Boolean> = _addBookSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> = _errorMessage

    fun addBook(
        title: String,
        author: String,
        year: String,
        publisher: String,
        genre: String,
        description: String,
        quantity: Int,
        material: String,
        specifications: String,
        purchaseDate: String,
    ) {
        val book = Book(
            title = title,
            author = author,
            publisher = publisher,
            genre = genre,
            specifications = description,
            quantity = quantity.toLong(),
            material = material,
            purchaseDate = com.google.firebase.Timestamp.now()
        )

        repository.addBook(book,
            onSuccess = { _addBookSuccess.postValue(true) },
            onFailure = { e -> _errorMessage.postValue("Gagal menambahkan buku: ${e.message}") }
        )
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
