package com.example.mylibraryapps.model
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val title: String,
    val author: String,
    val year: String,
    val publisher: String,
    val type: String,
    val description: String,
    val coverResId: Int
) : Parcelable

