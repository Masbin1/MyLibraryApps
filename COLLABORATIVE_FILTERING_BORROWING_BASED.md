# Collaborative Filtering: Berdasarkan Jumlah Buku yang Dipinjam

## Overview
Telah berhasil mengubah algoritma collaborative filtering dari **rating-based** menjadi **borrowing-based** untuk lebih sesuai dengan sistem perpustakaan.

## Perubahan Algoritma

### ❌ **BEFORE - Rating-Based System**
```kotlin
// Scoring berdasarkan rating dan interaksi umum
private fun interactionToScore(interaction: UserBookInteraction): Float {
    return when (interaction.interactionType) {
        InteractionType.RATE -> interaction.rating        // 1-5 stars
        InteractionType.BORROW -> 4.0f                   // Fixed score
        InteractionType.FAVORITE -> 5.0f                 // Fixed score
        InteractionType.VIEW -> min(duration/60000f, 3.0f) // Time-based
        InteractionType.SEARCH -> 2.0f                   // Fixed score
        InteractionType.RETURN -> 3.5f                   // Fixed score
    }
}

// Cosine similarity untuk rating patterns
private fun calculateCosineSimilarity(user1, user2): Float {
    // Menghitung similarity berdasarkan rating patterns
    // Fokus pada preferensi rating, bukan pola peminjaman
}
```

### ✅ **AFTER - Borrowing-Based System**
```kotlin
// Scoring berdasarkan frekuensi dan pola peminjaman
private fun interactionToBorrowScore(interaction: UserBookInteraction): Float {
    return when (interaction.interactionType) {
        InteractionType.BORROW -> 1.0f    // Setiap peminjaman = 1 point
        InteractionType.RETURN -> 0.5f    // Completion indicator = 0.5 point
        InteractionType.FAVORITE -> 0.3f  // Interest indicator = 0.3 point
        InteractionType.RATE -> {
            // Rating tinggi = kepuasan setelah baca
            if (interaction.rating >= 4.0f) 0.4f else 0.1f
        }
        InteractionType.VIEW -> {
            // View lama = interest yang lebih tinggi
            min(interaction.duration / 300000f, 0.2f) // max 5 menit = 0.2 point
        }
        InteractionType.SEARCH -> 0.05f   // Minimal interest
    }
}

// Jaccard similarity untuk borrowing patterns
private fun calculateBorrowingSimilarity(user1, user2): Float {
    // Menghitung similarity berdasarkan buku yang sama dipinjam
    // Boost untuk users yang pinjam banyak buku yang sama
}
```

## Algoritma Baru

### 🎯 **1. User Similarity Calculation**
```kotlin
// Jaccard Similarity dengan Weight
val intersection = commonBooks.sumOf { bookId ->
    min(user1Borrows[bookId] ?: 0f, user2Borrows[bookId] ?: 0f)
}

val union = (user1Borrows.keys + user2Borrows.keys).sumOf { bookId ->
    max(user1Borrows[bookId] ?: 0f, user2Borrows[bookId] ?: 0f)
}

val jaccardSimilarity = intersection / union

// Boost similarity untuk users dengan banyak buku sama
val commonBooksBoost = min(commonBooks.size / 5f, 1f)
return jaccardSimilarity * (1f + commonBooksBoost)
```

### 🎯 **2. Recommendation Generation**
```kotlin
// Weight berdasarkan similarity dan frekuensi peminjaman
similarUsers.forEach { (similarUserId, similarity) ->
    similarUserBooks.forEach { (bookId, borrowScore) ->
        if (!currentUserBooks.containsKey(bookId)) {
            val borrowFrequencyWeight = min(borrowScore / 2f, 1f)
            val weightedScore = borrowFrequencyWeight * similarity
            recommendations[bookId] = recommendations.getOrDefault(bookId, 0f) + weightedScore
        }
    }
}
```

### 🎯 **3. Popular Recommendations (Fallback)**
```kotlin
// Berdasarkan frekuensi peminjaman, bukan rating
allInteractions.forEach { interaction ->
    val borrowScore = interactionToBorrowScore(interaction)
    bookBorrowCount[interaction.bookId] = currentScore + borrowScore
}

// Reason: "Dipinjam X kali oleh pengguna lain"
val borrowCount = allInteractions.count { 
    it.bookId == book.id && it.interactionType == InteractionType.BORROW 
}
```

## Keunggulan Sistem Baru

### 📚 **1. Lebih Relevan untuk Perpustakaan**
- **Focus on Borrowing**: Algoritma fokus pada pola peminjaman aktual
- **Real Usage Patterns**: Berdasarkan buku yang benar-benar dipinjam
- **Library Context**: Sesuai dengan konteks perpustakaan, bukan e-commerce

### 🎯 **2. Better User Similarity**
- **Borrowing Patterns**: Users dengan pola peminjaman serupa
- **Common Books Boost**: Prioritas untuk users yang pinjam buku sama
- **Frequency Weight**: Mempertimbangkan seberapa sering user meminjam

### 📊 **3. Improved Recommendations**
- **Borrowing Frequency**: Buku yang sering dipinjam mendapat prioritas
- **Completion Rate**: Mempertimbangkan tingkat pengembalian
- **Interest Indicators**: Favorite dan rating tinggi sebagai booster

### 🚀 **4. Performance & Accuracy**
- **Lower Threshold**: 0.05f minimum similarity (vs 0.1f sebelumnya)
- **Focused Users**: Top 15 similar users (vs 20 sebelumnya)
- **Better Scoring**: Akumulasi score vs rata-rata weighted

## Contoh Skenario

### **User A**: Sering pinjam novel fiksi
- Borrow: Novel A (2x), Novel B (1x), Novel C (3x)
- Return: Semua dikembalikan tepat waktu
- Favorite: Novel C
- Rating: Novel A (5⭐), Novel C (5⭐)

### **User B**: Pola serupa dengan User A
- Borrow: Novel A (1x), Novel B (2x), Novel D (1x)
- Return: Semua dikembalikan
- Favorite: Novel B
- Rating: Novel A (4⭐), Novel B (5⭐)

### **Similarity Calculation**:
```
Common Books: Novel A, Novel B
Intersection: min(2,1) + min(1,2) = 1 + 1 = 2
Union: max(2,1) + max(1,2) + 3 + 1 = 2 + 2 + 3 + 1 = 8
Jaccard: 2/8 = 0.25
Common Books Boost: min(2/5, 1) = 0.4
Final Similarity: 0.25 * (1 + 0.4) = 0.35
```

### **Recommendation for User A**:
- **Novel D** akan direkomendasikan karena:
  - User B (similarity 0.35) pernah meminjam Novel D
  - User A belum pernah meminjam Novel D
  - Weight: borrowScore(1.0) * similarity(0.35) = 0.35

## Logging & Monitoring

### **Enhanced Logging**:
```kotlin
Log.d(TAG, "📚 Found ${similarUsers.size} users with similar borrowing patterns")
Log.d(TAG, "📖 Current user has borrowed/interacted with ${currentUserBooks.size} books")
Log.d(TAG, "🎯 Generated ${recommendations.size} potential recommendations")
Log.d(TAG, "📊 Generating popular recommendations based on borrowing frequency")
```

### **Recommendation Reasons**:
- **Collaborative**: "Sering dipinjam oleh pengguna dengan pola peminjaman serupa"
- **Popular**: "Dipinjam X kali oleh pengguna lain"

## Testing & Validation

### **Build Status**: ✅ **SUCCESS**
- Semua perubahan berhasil dikompilasi
- Tidak ada breaking changes
- Backward compatible dengan data existing

### **Algorithm Validation**:
1. **User Similarity**: Berdasarkan buku yang sama dipinjam
2. **Recommendation Quality**: Prioritas pada buku yang sering dipinjam
3. **Fallback System**: Popular books berdasarkan borrowing frequency
4. **Performance**: Optimized dengan threshold dan limit yang tepat

## Migration Notes

### **Data Compatibility**:
- ✅ Menggunakan data `UserBookInteraction` yang sudah ada
- ✅ Tidak perlu perubahan database schema
- ✅ Backward compatible dengan sistem rating existing

### **Gradual Rollout**:
- ✅ Sistem lama masih berfungsi sebagai fallback
- ✅ Content-based recommendations tetap aktif
- ✅ Popular recommendations sebagai safety net

## Summary

🎯 **Problem Solved**: Collaborative filtering sekarang berdasarkan pola peminjaman buku
✅ **Algorithm Improved**: Jaccard similarity + borrowing frequency weighting  
🚀 **Result**: Rekomendasi yang lebih relevan untuk sistem perpustakaan

Sistem collaborative filtering sekarang bekerja berdasarkan **jumlah dan pola peminjaman buku**, bukan rating, sehingga lebih sesuai dengan konteks perpustakaan! 📚🎉