# 🤖 Collaborative Filtering System Guide

## 📋 Overview

Sistem **Collaborative Filtering** yang telah diimplementasikan dalam aplikasi perpustakaan ini menggunakan pendekatan **hybrid** yang menggabungkan beberapa metode rekomendasi untuk memberikan saran buku yang personal dan akurat kepada setiap pengguna.

## 🏗️ Arsitektur Sistem

### 1. **Model Data**
- **`UserBookInteraction`**: Menyimpan semua interaksi pengguna dengan buku
- **`BookRecommendation`**: Objek rekomendasi dengan skor dan alasan
- **`RecommendationType`**: Jenis-jenis algoritma rekomendasi

### 2. **Repository & Service**
- **`InteractionRepository`**: Mengelola data interaksi dengan Firebase
- **`CollaborativeFilteringService`**: Engine utama untuk menghasilkan rekomendasi

### 3. **UI Components**
- **`RecommendationAdapter`**: Menampilkan daftar rekomendasi
- **`BookRatingDialog`**: Dialog untuk memberikan rating buku
- **Chip "⭐ Rekomendasi"**: Filter khusus untuk menampilkan rekomendasi

## 🔄 Algoritma Rekomendasi

### 1. **Collaborative Filtering (User-Based)**
```kotlin
// Menghitung kesamaan antara user menggunakan Cosine Similarity
private fun calculateCosineSimilarity(
    user1Interactions: Map<String, Float>,
    user2Interactions: Map<String, Float>
): Float
```

**Cara Kerja:**
- Mencari pengguna dengan preferensi serupa
- Menggunakan rumus Cosine Similarity untuk menghitung kesamaan
- Merekomendasikan buku yang disukai oleh pengguna serupa

### 2. **Content-Based Filtering**
```kotlin
suspend fun generateContentBasedRecommendations(
    userId: String,
    allBooks: List<Book>,
    limit: Int = 10
): List<BookRecommendation>
```

**Cara Kerja:**
- Menganalisis genre dan author favorit pengguna
- Merekomendasikan buku dengan konten serupa
- Memberikan bobot berdasarkan frekuensi interaksi

### 3. **Popularity-Based Recommendations**
```kotlin
private fun generatePopularRecommendations(
    allBooks: List<Book>,
    allInteractions: List<UserBookInteraction>,
    limit: Int = 10
): List<BookRecommendation>
```

**Cara Kerja:**
- Menghitung popularitas berdasarkan total interaksi
- Memberikan bobot berbeda untuk setiap jenis interaksi
- Fallback untuk pengguna baru yang belum memiliki histori

### 4. **Hybrid Approach**
```kotlin
suspend fun generateHybridRecommendations(
    userId: String,
    allBooks: List<Book>,
    limit: Int = 15
): List<BookRecommendation>
```

**Cara Kerja:**
- Menggabungkan hasil dari semua metode
- Menghilangkan duplikasi berdasarkan skor tertinggi
- Memberikan hasil yang lebih akurat dan beragam

## 📊 Sistem Scoring

### Bobot Interaksi:
- **⭐ Rating**: `rating` (1.0 - 5.0)
- **📖 Pinjam Buku**: `4.0`
- **❤️ Favorit**: `5.0`
- **👁️ Lihat Buku**: `max(duration/60000, 3.0)` (berdasarkan durasi)
- **🔍 Pencarian**: `2.0`
- **📤 Kembalikan**: `3.5`

### Normalisasi Skor:
- Skor akhir dinormalisasi antara `0.0` - `1.0`
- Ditampilkan sebagai persentase (85%, 92%, dst.)

## 🎯 Fitur Utama

### 1. **Tracking Interaksi Otomatis**
```kotlin
// Otomatis track saat user melihat buku
homeViewModel.trackBookView(userId, book)

// Otomatis track saat user melakukan pencarian
homeViewModel.trackBookSearch(userId, query, foundBooks)

// Manual track saat user memberi rating
homeViewModel.rateBook(userId, book, rating)
```

### 2. **Real-time Recommendations**
- Rekomendasi diperbarui setelah setiap interaksi penting
- Loading state yang informatif
- Refresh otomatis saat user login

### 3. **Filter Rekomendasi**
- Chip **"⭐ Rekomendasi"** untuk menampilkan hanya rekomendasi
- Toggle antara mode normal dan mode rekomendasi
- Integrasi dengan sistem filter genre yang ada

### 4. **Rating System**
- Dialog rating dengan UI yang menarik
- Star rating 1-5 dengan deskripsi
- Feedback real-time untuk user

## 🎨 UI/UX Features

### 1. **Recommendation Card Design**
- **Badge Type**: Menunjukkan jenis rekomendasi (Kolaboratif, Konten, Populer)
- **Score Badge**: Menampilkan confidence score (85%, 92%)
- **Reason Text**: Penjelasan mengapa buku direkomendasikan

### 2. **Responsive Layout**
- Horizontal scrollable untuk rekomendasi
- Card design yang konsisten
- Loading states yang smooth

### 3. **Color Coding**
- **🟣 Kolaboratif**: Purple (`#9C27B0`)
- **🟢 Konten**: Teal (`#009688`) 
- **🟠 Populer**: Orange (`#FF9800`)
- **🔴 Trending**: Red (`#F44336`)
- **🔵 Personal**: Indigo (`#3F51B5`)

## 🔧 Implementasi Teknis

### 1. **Firebase Collections**
```
user_interactions/
├── userId: string
├── bookId: string
├── interactionType: enum
├── rating: float
├── timestamp: Timestamp
├── duration: long
├── genre: string
└── author: string
```

### 2. **ViewModels Integration**
```kotlin
class HomeViewModel {
    fun loadRecommendations(userId: String)
    fun trackBookView(userId: String, book: Book)
    fun trackBookSearch(userId: String, query: String, books: List<Book>)
    fun rateBook(userId: String, book: Book, rating: Float)
}
```

### 3. **Performance Optimizations**
- Async processing dengan Coroutines
- Caching recommendations di memory
- Batching interactions untuk efisiensi
- Lazy loading untuk UI components

## 🚀 Cara Menggunakan

### 1. **Untuk Pengguna**
1. **Jelajahi Buku**: Semakin sering melihat buku, semakin akurat rekomendasinya
2. **Beri Rating**: Gunakan fitur rating di halaman detail buku
3. **Lihat Rekomendasi**: Klik chip "⭐ Rekomendasi" di home page
4. **Interact Naturally**: Sistem akan belajar dari semua aktivitas Anda

### 2. **Untuk Developer**
1. **Track Custom Interactions**:
   ```kotlin
   homeViewModel.trackBookView(userId, book, duration)
   ```

2. **Customize Recommendation Types**:
   ```kotlin
   collaborativeFilteringService.generateContentBasedRecommendations(userId, books)
   ```

3. **Modify Scoring Weights**:
   ```kotlin
   private fun interactionToScore(interaction: UserBookInteraction): Float
   ```

## 📈 Metrics & Analytics

### Trackable Metrics:
- **Click-through Rate**: Seberapa sering rekomendasi diklik
- **Diversity Score**: Seberapa beragam rekomendasi yang diberikan
- **Accuracy**: Persentase rekomendasi yang mendapat rating tinggi
- **User Engagement**: Peningkatan interaksi setelah implementasi

### Logging:
```kotlin
Log.d("CollaborativeFiltering", "Generated ${recommendations.size} recommendations")
Log.d("HomeFragment", "🔍 Recommendations loading: $isLoading")
Log.d("BookDetailFragment", "User $userId rated book ${book.id} with $rating stars")
```

## 🔮 Future Enhancements

### 1. **Advanced Algorithms**
- Matrix Factorization (SVD)
- Deep Learning recommendations
- Time-decay untuk interaksi lama
- Seasonal trending analysis

### 2. **Social Features**
- Rekomendasi dari teman
- Social proof indicators
- Community ratings aggregation

### 3. **Personalization**
- Reading goals tracking
- Subject matter expertise detection
- Reading difficulty preferences
- Time-based recommendations

---

## 🎉 Kesimpulan

Sistem Collaborative Filtering ini memberikan pengalaman yang personal dan cerdas kepada pengguna perpustakaan. Dengan menggabungkan multiple algoritma dan tracking yang komprehensif, sistem ini akan semakin akurat seiring waktu dan meningkatkan engagement pengguna dengan koleksi buku perpustakaan.

**Happy Reading! 📚✨**