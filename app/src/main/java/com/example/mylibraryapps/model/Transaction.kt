package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: String = "",
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val genre: String = "",
    val coverUrl: String = "",
    val userId: String = "",
    val borrowDate: String = "",
    val returnDate: String = "",
    val status: String = "menunggu konfirmasi pinjam",
    val remainingDays: Int = 0
) : Parcelable