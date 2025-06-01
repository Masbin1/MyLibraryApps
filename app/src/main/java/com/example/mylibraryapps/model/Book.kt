package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Parcelize
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val purchaseDate: Timestamp = Timestamp.now(), // Firebase Timestamp
    val specifications: String = "",
    val material: String = "",
    val quantity: Long = 0,
    val genre: String = "",
    val coverUrl: String = ""
) : Parcelable {
    // Tambahkan fungsi untuk format tanggal
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(purchaseDate.toDate())
    }
}