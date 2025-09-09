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
    
    // Hitung similarity berdasarkan pola peminjaman buku
    private fun calculateBorrowingSimilarity(
        user1Borrows: Map<String, Float>,
        user2Borrows: Map<String, Float>
    ): Float {
        val commonBooks = user1Borrows.keys.intersect(user2Borrows.keys)
        
        if (commonBooks.isEmpty()) return 0f
        
        // Jaccard similarity dengan weight berdasarkan frekuensi peminjaman
        val intersection = commonBooks.sumOf { bookId ->
            min(user1Borrows[bookId] ?: 0f, user2Borrows[bookId] ?: 0f).toDouble()
        }.toFloat()
        
        val union = (user1Borrows.keys + user2Borrows.keys).sumOf { bookId ->
            max(user1Borrows[bookId] ?: 0f, user2Borrows[bookId] ?: 0f).toDouble()
        }.toFloat()
        
        val jaccardSimilarity = if (union == 0f) 0f else intersection / union
        
        // Boost similarity jika ada banyak buku yang sama dipinjam
        val commonBooksBoost = min(commonBooks.size / 5f, 1f) // max boost untuk 5+ buku sama
        
        return min(jaccardSimilarity * (1f + commonBooksBoost), 1f)
    }
    
    // Konversi interaksi ke score berdasarkan peminjaman (borrow-based)
    private fun interactionToBorrowScore(interaction: UserBookInteraction): Float {
        return when (interaction.interactionType) {
            InteractionType.BORROW -> 1.0f  // Setiap peminjaman = 1 point
            InteractionType.RETURN -> 0.5f  // Return menunjukkan completion = 0.5 point
            InteractionType.FAVORITE -> 0.3f // Favorite menunjukkan interest = 0.3 point
            InteractionType.RATE -> {
                // Rating tinggi menunjukkan kepuasan setelah baca = weighted
                if (interaction.rating >= 4.0f) 0.4f else 0.1f
            }
            InteractionType.VIEW -> {
                // View lama menunjukkan interest = scaled
                min(interaction.duration / 300000f, 0.2f) // max 5 menit = 0.2 point
            }
            InteractionType.SEARCH -> 0.05f // Search menunjukkan minimal interest
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
            
            // Buat user-book matrix berdasarkan frekuensi peminjaman
            val userBookMatrix = mutableMapOf<String, MutableMap<String, Float>>()
            
            allInteractions.forEach { interaction ->
                val userMap = userBookMatrix.getOrPut(interaction.userId) { mutableMapOf() }
                val currentScore = userMap.getOrDefault(interaction.bookId, 0f)
                val borrowScore = interactionToBorrowScore(interaction)
                
                // Akumulasi score (total peminjaman + interaksi)
                userMap[interaction.bookId] = currentScore + borrowScore
            }
            
            val currentUserBooks = userBookMatrix[userId] ?: emptyMap()
            val recommendations = mutableMapOf<String, Float>()
            
            // Find similar users berdasarkan pola peminjaman
            val similarUsers = userBookMatrix.entries
                .filter { it.key != userId }
                .map { (otherUserId, otherUserBooks) ->
                    val similarity = calculateBorrowingSimilarity(currentUserBooks, otherUserBooks)
                    Pair(otherUserId, similarity)
                }
                .filter { it.second > 0.05f } // minimum similarity threshold (lebih rendah untuk borrowing)
                .sortedByDescending { it.second }
                .take(15) // top 15 similar users (lebih fokus)
            
            Log.d(TAG, "📚 Found ${similarUsers.size} users with similar borrowing patterns")
            Log.d(TAG, "📖 Current user has borrowed/interacted with ${currentUserBooks.size} books")
            
            // Generate recommendations from similar users
            similarUsers.forEach { (similarUserId, similarity) ->
                val similarUserBooks = userBookMatrix[similarUserId] ?: emptyMap()
                
                similarUserBooks.forEach { (bookId, borrowScore) ->
                    // Hanya recommend buku yang belum pernah dipinjam user
                    if (!currentUserBooks.containsKey(bookId)) {
                        // Weight berdasarkan similarity dan frekuensi peminjaman
                        val borrowFrequencyWeight = min(borrowScore / 2f, 1f) // normalize frequency
                        val weightedScore = borrowFrequencyWeight * similarity
                        
                        recommendations[bookId] = recommendations.getOrDefault(bookId, 0f) + weightedScore
                    }
                }
            }
            
            Log.d(TAG, "🎯 Generated ${recommendations.size} potential recommendations")
            
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
                            reason = "Sering dipinjam oleh pengguna dengan pola peminjaman serupa"
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
    
    // Generate rekomendasi berdasarkan buku yang paling sering dipinjam (fallback)
    private fun generatePopularRecommendations(
        allBooks: List<Book>,
        allInteractions: List<UserBookInteraction>,
        limit: Int = 10
    ): List<BookRecommendation> {
        try {
            Log.d(TAG, "📊 Generating popular recommendations based on borrowing frequency")
            
            val bookBorrowCount = mutableMapOf<String, Float>()
            
            allInteractions.forEach { interaction ->
                val currentScore = bookBorrowCount.getOrDefault(interaction.bookId, 0f)
                val borrowScore = interactionToBorrowScore(interaction)
                bookBorrowCount[interaction.bookId] = currentScore + borrowScore
            }
            
            val maxBorrowScore = bookBorrowCount.values.maxOrNull() ?: 1f
            
            val recommendations = allBooks
                .mapNotNull { book ->
                    val borrowScore = bookBorrowCount.getOrDefault(book.id, 0f)
                    if (borrowScore > 0f) {
                        val borrowCount = allInteractions.count { 
                            it.bookId == book.id && it.interactionType == InteractionType.BORROW 
                        }
                        BookRecommendation(
                            book = book,
                            score = borrowScore / maxBorrowScore,
                            recommendationType = RecommendationType.POPULAR,
                            reason = "Dipinjam ${borrowCount} kali oleh pengguna lain"
                        )
                    } else null
                }
                .sortedByDescending { it.score }
                .take(limit)
            
            Log.d(TAG, "✅ Generated ${recommendations.size} popular recommendations based on borrowing")
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