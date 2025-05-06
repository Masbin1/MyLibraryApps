package com.example.mylibraryapps.ui.home

import androidx.lifecycle.ViewModel
import com.example.mylibraryapps.R
import com.example.mylibraryapps.model.Book

class HomeViewModel : ViewModel() {

    fun getAllBooks(): List<Book> {
        return listOf(
            Book(
                coverResId = R.drawable.ic_book_dummy,
                title = "Laskar Pelangi",
                author = "Andrea Hirata",
                year = "2005",
                publisher = "Bentang Pustaka",
                type = "Fiksi",
                description = "Novel tentang perjuangan sekelompok anak miskin di Belitung yang bersekolah di SD Muhammadiyah"
            ),
            Book(
                coverResId = R.drawable.ic_book_dummy,
                title = "Bumi Manusia",
                author = "Pramoedya Ananta Toer",
                year = "1980",
                publisher = "Hasta Mitra",
                type = "Fiksi Sejarah",
                description = "Buku pertama dari Tetralogi Buru yang mengisahkan kehidupan Minke di era kolonial"
            ),
            Book(
                coverResId = R.drawable.ic_book_dummy,
                title = "Atomic Habits",
                author = "James Clear",
                year = "2018",
                publisher = "Penguin Random House",
                type = "Pengembangan Diri",
                description = "Buku tentang membangun kebiasaan baik dan menghilangkan kebiasaan buruk"
            ),
            Book(
                coverResId = R.drawable.ic_book_dummy,
                title = "Filosofi Teras",
                author = "Henry Manampiring",
                year = "2018",
                publisher = "Kompas",
                type = "Filosofi",
                description = "Pengenalan praktis tentang filsafat Stoa untuk mengatasi emosi negatif"
            ),
            Book(
                coverResId = R.drawable.ic_book_dummy,
                title = "Cantik Itu Luka",
                author = "Eka Kurniawan",
                year = "2002",
                publisher = "Gramedia Pustaka Utama",
                type = "Fiksi Magis",
                description = "Kisah tentang Dewi Ayu dan ketiga putrinya yang penuh dengan kejadian magis"
            )
        )
    }
}
