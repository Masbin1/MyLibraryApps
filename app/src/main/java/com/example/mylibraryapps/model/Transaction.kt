package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: String = "",
    val nameUser: String = "",
    val title: String = "",
    val author: String = "",
    val borrowDate: String = "",
    val returnDate: String = "",
    val status: String = "",
    val coverUrl: String = "",
    val userId: String = "",
    val bookId: String = "",
    val genre: String = "",
    val publisher: String = "",
    val remainingDays: Int = 0,
    val stability: Int = 0,
    val lateDays: Int = 0,        // Hari keterlambatan (disimpan saat pengembalian dikonfirmasi)
    val fine: Int = 0             // Denda total (rupiah), 1000/hari keterlambatan
) : Parcelable