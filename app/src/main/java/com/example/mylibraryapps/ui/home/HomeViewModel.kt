package com.example.mylibraryapps.ui.home

import androidx.lifecycle.ViewModel
import com.example.mylibraryapps.R
import com.example.mylibraryapps.model.Book

class HomeViewModel : ViewModel() {

    fun getAllBooks(): List<Book> {
        return listOf(
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
            Book(R.drawable.ic_book_dummy, "Judul buku", "Kategori","Kategori","Kategori"),
        )
    }
}
