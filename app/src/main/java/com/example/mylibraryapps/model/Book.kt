package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val id: String = "", // Add document ID
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val purchaseDate: String = "",
    val specifications: String = "",
    val material: String = "",
    val quantity: Int = 0,
    val genre: String = "",
    val coverUrl: String = "" // For remote images
) : Parcelable