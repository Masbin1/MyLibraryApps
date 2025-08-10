package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.example.mylibraryapps.utils.FirestoreConverters
import java.text.SimpleDateFormat
import java.util.Locale

@Parcelize
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    @get:PropertyName("purchaseDate")
    @set:PropertyName("purchaseDate")
    var purchaseDate: Timestamp = Timestamp.now(), // Firebase Timestamp
    val specifications: String = "",
    val material: String = "",
    val quantity: Long = 0,
    val genre: String = "",
    val coverUrl: String = ""
) : Parcelable {
    
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", Timestamp.now(), "", "", 0, "", "")
    
    // Custom setter for safe deserialization
    fun setPurchaseDateSafe(value: Any?) {
        purchaseDate = FirestoreConverters.convertToTimestamp(value)
    }
    
    // Tambahkan fungsi untuk format tanggal
    fun getFormattedDate(): String {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.format(purchaseDate.toDate())
        } catch (e: Exception) {
            "Invalid Date"
        }
    }
}