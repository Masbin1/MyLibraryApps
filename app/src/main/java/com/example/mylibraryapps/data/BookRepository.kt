package com.example.mylibraryapps.data

import com.example.mylibraryapps.model.Book
import com.google.firebase.firestore.FirebaseFirestore

class BookRepository {
    private val db = FirebaseFirestore.getInstance()

    fun addBook(book: Book, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("books")
            .add(book)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}
