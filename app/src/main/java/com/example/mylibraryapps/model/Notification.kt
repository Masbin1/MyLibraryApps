package com.example.mylibraryapps.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val type: String = "general", // general, return_reminder, overdue, etc.
    val relatedItemId: String = "", // ID of related book or transaction
    val relatedItemTitle: String = "", // Title of related book
    val transactionId: String = "" // ID of related transaction
) : Parcelable