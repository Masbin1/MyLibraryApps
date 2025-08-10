package com.example.mylibraryapps.service

import android.util.Log
import com.example.mylibraryapps.data.InteractionRepository
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.BookRecommendation
import com.example.mylibraryapps.model.RecommendationType
import com.example.mylibraryapps.model.UserBookInteraction
import com.example.mylibraryapps.model.InteractionType
import kotlinx.coroutines.*
import kotlin.math.*

class CollaborativeFilteringService(
    private val interactionRepository: InteractionRepository
) {
    private val TAG = "CollaborativeFiltering"
    
    // Hitung cosine similarity antara dua user
    private fun calculateCosineSimilarity(
        user1Interactions: Map<String, Float>,
        user2Interactions: Map<String, Float>
    ): Float {
        val commonBooks = user1Interactions.keys.intersect(user2Interactions.keys)
        
        if (commonBooks.isEmpty()) return 0f
        
        val dotProduct = commonBooks.sumOf { bookId ->
            ((user1Interactions[bookId] ?: 0f) * (user2Interactions[bookId] ?: 0f)).toDouble()
        }.toFloat()
        
        val norm1 = sqrt(user1Interactions.values.sumOf { (it * it).toDouble() }).toFloat()
        val norm2 = sqrt(user2Interactions.values.sumOf { (it * it).toDouble() }).toFloat()
        
        return if (norm1 == 0f || norm2 == 0f) 0f else dotProduct / (norm1 * norm2)
    }
    
    // Konversi interaksi ke score (rating-based)
    private fun interactionToScore(interaction: UserBookInteraction): Float {
        return when (interaction.interactionType) {
            InteractionType.RATE -> interaction.rating
            InteractionType.BORROW -> 4.0f
            InteractionType.FAVORITE -> 5.0f
            InteractionType.VIEW -> {
                // Score berdasarkan durasi view (max 3.0)
                val durationScore = min(interaction.duration / 60000f, 3.0f) // max 1 menit = 3.0 score
                durationScore
            }
            InteractionType.SEARCH -> 2.0f
            InteractionType.RETURN -> 3.5f
        }
    }
    
    // Generate rekomendasi collaborative filtering
    suspend fun generateCollaborativeRecommendations(
        userId: String,
        allBooks: List<Book>,
        limit: Int = 10
    ): List<BookRecommendation> = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "🔍 Generating collaborative recommendations for user: $userId")
            
            val allInteractions = interactionRepository.getAllInteractions()
            val userInteractions = interactionRepository.getUserInteractions(userId)
            
            if (userInteractions.isEmpty()) {
                Log.d(TAG, "No user interactions found, returning popular books")
                return@withContext generatePopularRecommendations(allBooks, allInteractions, limit)
            }
            
            // Buat user-book matrix dengan scores
            val userBookMatrix = mutableMapOf<String, MutableMap<String, Float>>()
            
            allInteractions.forEach { interaction ->
                val userMap = userBookMatrix.getOrPut(interaction.userId) { mutableMapOf() }
                val currentScore = userMap.getOrDefault(interaction.bookId, 0f)
                val interactionScore = interactionToScore(interaction)
                
                // Agregasi score (rata-rata weighted)
                userMap[interaction.bookId] = (currentScore + interactionScore) / 2f
            }
            
            val currentUserBooks = userBookMatrix[userId] ?: emptyMap()
            val recommendations = mutableMapOf<String, Float>()
            
            // Find similar users
            val similarUsers = userBookMatrix.entries
                .filter { it.key != userId }
                .map { (otherUserId, otherUserBooks) ->
                    val similarity = calculateCosineSimilarity(currentUserBooks, otherUserBooks)
                    Pair(otherUserId, similarity)
                }
                .filter { it.second > 0.1f } // minimum similarity threshold
                .sortedByDescending { it.second }
                .take(20) // top 20 similar users
            
            Log.d(TAG, "Found ${similarUsers.size} similar users")
            
            // Generate recommendations from similar users
            similarUsers.forEach { (similarUserId, similarity) ->
                val similarUserBooks = userBookMatrix[similarUserId] ?: emptyMap()
                
                similarUserBooks.forEach { (bookId, score) ->
                    // Hanya recommend buku yang belum pernah diinteraksi user
                    if (!currentUserBooks.containsKey(bookId)) {
                        val weightedScore = score * similarity
                        recommendations[bookId] = recommendations.getOrDefault(bookId, 0f) + weightedScore
                    }
                }
            }
            
            // Convert to BookRecommendation objects
            val bookRecommendations = recommendations.entries
                .sortedByDescending { it.value }
                .take(limit)
                .mapNotNull { (bookId, score) ->
                    val book = allBooks.find { it.id == bookId }
                    book?.let {
                        BookRecommendation(
                            book = it,
                            score = min(score, 1.0f), // normalize score
                            recommendationType = RecommendationType.COLLABORATIVE,
                            reason = "Direkomendasikan berdasarkan preferensi pengguna serupa"
                        )
                    }
                }
            
            Log.d(TAG, "✅ Generated ${bookRecommendations.size} collaborative recommendations")
            bookRecommendations
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating collaborative recommendations", e)
            emptyList()
        }
    }
    
    // Generate rekomendasi content-based
    suspend fun generateContentBasedRecommendations(
        userId: String,
        allBooks: List<Book>,
        limit: Int = 10
    ): List<BookRecommendation> = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "🎯 Generating content-based recommendations for user: $userId")
            
            val userGenres = interactionRepository.getUserFavoriteGenres(userId)
            val userAuthors = interactionRepository.getUserFavoriteAuthors(userId)
            val userInteractions = interactionRepository.getUserInteractions(userId)
            val interactedBookIds = userInteractions.map { it.bookId }.toSet()
            
            if (userGenres.isEmpty() && userAuthors.isEmpty()) {
                Log.d(TAG, "No user preferences found")
                return@withContext emptyList()
            }
            
            val recommendations = allBooks
                .filter { !interactedBookIds.contains(it.id) } // Exclude already interacted books
                .map { book ->
                    var score = 0f
                    var reason = ""
                    
                    // Score based on favorite genres
                    val genreScore = userGenres[book.genre]?.let { count ->
                        val normalizedScore = count.toFloat() / userGenres.values.sum()
                        reason += "Genre yang Anda sukai (${book.genre})"
                        normalizedScore * 0.7f
                    } ?: 0f
                    
                    // Score based on favorite authors
                    val authorScore = userAuthors[book.author]?.let { count ->
                        val normalizedScore = count.toFloat() / userAuthors.values.sum()
                        if (reason.isNotEmpty()) reason += ", "
                        reason += "Author favorit (${book.author})"
                        normalizedScore * 0.3f
                    } ?: 0f
                    
                    score = genreScore + authorScore
                    
                    BookRecommendation(
                        book = book,
                        score = score,
                        recommendationType = RecommendationType.CONTENT_BASED,
                        reason = reason.ifEmpty { "Berdasarkan preferensi konten Anda" }
                    )
                }
                .filter { it.score > 0f }
                .sortedByDescending { it.score }
                .take(limit)
            
            Log.d(TAG, "✅ Generated ${recommendations.size} content-based recommendations")
            recommendations
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating content-based recommendations", e)
            emptyList()
        }
    }
    
    // Generate rekomendasi popular (fallback)
    private fun generatePopularRecommendations(
        allBooks: List<Book>,
        allInteractions: List<UserBookInteraction>,
        limit: Int = 10
    ): List<BookRecommendation> {
        try {
            Log.d(TAG, "📊 Generating popular recommendations")
            
            val bookPopularity = mutableMapOf<String, Int>()
            
            allInteractions.forEach { interaction ->
                val currentCount = bookPopularity.getOrDefault(interaction.bookId, 0)
                val weight = when (interaction.interactionType) {
                    InteractionType.BORROW -> 3
                    InteractionType.RATE -> 2
                    InteractionType.VIEW -> 1
                    InteractionType.FAVORITE -> 4
                    else -> 1
                }
                bookPopularity[interaction.bookId] = currentCount + weight
            }
            
            val recommendations = allBooks
                .map { book ->
                    val popularity = bookPopularity.getOrDefault(book.id, 0)
                    BookRecommendation(
                        book = book,
                        score = popularity.toFloat() / (bookPopularity.values.maxOrNull() ?: 1).toFloat(),
                        recommendationType = RecommendationType.POPULAR,
                        reason = "Buku populer di perpustakaan"
                    )
                }
                .sortedByDescending { it.score }
                .take(limit)
            
            Log.d(TAG, "✅ Generated ${recommendations.size} popular recommendations")
            return recommendations
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating popular recommendations", e)
            return emptyList()
        }
    }
    
    // Generate hybrid recommendations (kombinasi semua metode)
    suspend fun generateHybridRecommendations(
        userId: String,
        allBooks: List<Book>,
        limit: Int = 15
    ): List<BookRecommendation> = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "🔄 Generating hybrid recommendations for user: $userId")
            
            val collaborativeRecs = async { 
                generateCollaborativeRecommendations(userId, allBooks, limit / 2) 
            }
            val contentBasedRecs = async { 
                generateContentBasedRecommendations(userId, allBooks, limit / 2) 
            }
            
            val allRecs = collaborativeRecs.await() + contentBasedRecs.await()
            
            // Combine and remove duplicates, prioritizing higher scores
            val combinedRecs = allRecs
                .groupBy { it.book.id }
                .map { (_, recommendations) ->
                    recommendations.maxByOrNull { it.score } ?: recommendations.first()
                }
                .sortedByDescending { it.score }
                .take(limit)
            
            Log.d(TAG, "✅ Generated ${combinedRecs.size} hybrid recommendations")
            combinedRecs
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating hybrid recommendations", e)
            emptyList()
        }
    }
}