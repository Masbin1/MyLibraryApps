package com.example.mylibraryapps.model
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val imageResId: Int,
    val title: String,
    val category: String,
    val author: String,
    val description: String,
) : Parcelable

