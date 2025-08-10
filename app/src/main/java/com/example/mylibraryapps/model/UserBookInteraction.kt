package com.example.mylibraryapps.model

import com.google.firebase.Timestamp

data class UserBookInteraction(
    val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val interactionType: InteractionType = InteractionType.VIEW,
    val rating: Float = 0f, // 0-5 stars
    val timestamp: Timestamp = Timestamp.now(),
    val duration: Long = 0, // viewing duration in milliseconds
    val genre: String = "",
    val author: String = ""
)

enum class InteractionType {
    VIEW, BORROW, RETURN, RATE, SEARCH, FAVORITE
}