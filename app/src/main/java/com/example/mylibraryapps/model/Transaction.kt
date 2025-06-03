package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: String = "",
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
    val stability: Int = 0
) : Parcelable