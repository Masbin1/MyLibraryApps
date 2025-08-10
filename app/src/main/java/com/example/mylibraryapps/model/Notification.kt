package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.PropertyName
import com.example.mylibraryapps.utils.FirestoreConverters
import java.util.*

@Parcelize
data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Date = Date(),
    val isRead: Boolean = false,
    val type: String = "general", // general, return_reminder, overdue, etc.
    val relatedItemId: String = "", // ID of related book or transaction
    val relatedItemTitle: String = "", // Title of related book
    val transactionId: String = "" // ID of related transaction
) : Parcelable {
    
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", Date(), false, "general", "", "", "")
    
    // Custom setter for safe deserialization
    fun setTimestampSafe(value: Any?) {
        timestamp = FirestoreConverters.convertToDate(value)
    }
}