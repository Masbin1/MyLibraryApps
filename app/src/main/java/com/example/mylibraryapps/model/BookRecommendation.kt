package com.example.mylibraryapps.model

data class BookRecommendation(
    val book: Book,
    val score: Float, // 0.0 - 1.0
    val recommendationType: RecommendationType,
    val reason: String = ""
)

enum class RecommendationType {
    COLLABORATIVE, // Based on similar users
    CONTENT_BASED, // Based on book content
    POPULAR,       // Based on popularity
    TRENDING,      // Based on recent trends
    PERSONAL       // Based on user's history
}