package com.example.mylibraryapps.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mylibraryapps.model.UserBookInteraction
import com.example.mylibraryapps.model.InteractionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class InteractionRepository {
    private val TAG = "InteractionRepository"
    private val db = FirebaseFirestore.getInstance()
    private val interactionsCollection = db.collection("user_interactions")
    
    private val _userInteractions = MutableLiveData<List<UserBookInteraction>>()
    val userInteractions: LiveData<List<UserBookInteraction>> = _userInteractions
    
    // Simpan interaksi user dengan buku
    suspend fun saveInteraction(interaction: UserBookInteraction) {
        try {
            val interactionData = hashMapOf(
                "userId" to interaction.userId,
                "bookId" to interaction.bookId,
                "interactionType" to interaction.interactionType.name,
                "rating" to interaction.rating,
                "timestamp" to interaction.timestamp,
                "duration" to interaction.duration,
                "genre" to interaction.genre,
                "author" to interaction.author
            )
            
            interactionsCollection.add(interactionData).await()
            Log.d(TAG, "Interaction saved: ${interaction.interactionType} for book ${interaction.bookId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving interaction", e)
        }
    }
    
    // Ambil semua interaksi user
    suspend fun getUserInteractions(userId: String): List<UserBookInteraction> {
        return try {
            val snapshot = interactionsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    UserBookInteraction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        bookId = doc.getString("bookId") ?: "",
                        interactionType = InteractionType.valueOf(
                            doc.getString("interactionType") ?: "VIEW"
                        ),
                        rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        duration = doc.getLong("duration") ?: 0L,
                        genre = doc.getString("genre") ?: "",
                        author = doc.getString("author") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing interaction document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user interactions", e)
            emptyList()
        }
    }
    
    // Ambil semua interaksi untuk collaborative filtering
    suspend fun getAllInteractions(): List<UserBookInteraction> {
        return try {
            val snapshot = interactionsCollection.get().await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    UserBookInteraction(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        bookId = doc.getString("bookId") ?: "",
                        interactionType = InteractionType.valueOf(
                            doc.getString("interactionType") ?: "VIEW"
                        ),
                        rating = doc.getDouble("rating")?.toFloat() ?: 0f,
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        duration = doc.getLong("duration") ?: 0L,
                        genre = doc.getString("genre") ?: "",
                        author = doc.getString("author") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing interaction document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all interactions", e)
            emptyList()
        }
    }
    
    // Ambil genre favorit user berdasarkan interaksi
    suspend fun getUserFavoriteGenres(userId: String): Map<String, Int> {
        val interactions = getUserInteractions(userId)
        val genreCounts = mutableMapOf<String, Int>()
        
        interactions.forEach { interaction ->
            if (interaction.genre.isNotEmpty()) {
                genreCounts[interaction.genre] = genreCounts.getOrDefault(interaction.genre, 0) + 1
            }
        }
        
        return genreCounts.toList().sortedByDescending { it.second }.toMap()
    }
    
    // Ambil author favorit user
    suspend fun getUserFavoriteAuthors(userId: String): Map<String, Int> {
        val interactions = getUserInteractions(userId)
        val authorCounts = mutableMapOf<String, Int>()
        
        interactions.forEach { interaction ->
            if (interaction.author.isNotEmpty()) {
                authorCounts[interaction.author] = authorCounts.getOrDefault(interaction.author, 0) + 1
            }
        }
        
        return authorCounts.toList().sortedByDescending { it.second }.toMap()
    }
}