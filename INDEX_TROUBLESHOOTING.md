# Panduan Mengatasi Masalah Indeks Firestore

Dokumen ini berisi panduan untuk mengatasi masalah indeks Firestore yang mungkin muncul dalam aplikasi MyLibraryApps.

## Masalah Umum

### Error "FAILED_PRECONDITION: The query requires an index"

Error ini muncul ketika Anda mencoba menjalankan query yang memerlukan indeks komposit, tetapi indeks tersebut belum dibuat di Firebase Console.

### Error "Sistem sedang dalam pemeliharaan"

Pesan ini muncul ketika aplikasi mendeteksi error indeks dan mencoba memberikan pesan yang lebih ramah pengguna.

## Solusi Cepat

1. **Gunakan Fallback Query**
   
   Aplikasi sudah dilengkapi dengan mekanisme fallback yang akan otomatis menggunakan query yang lebih sederhana jika terjadi error indeks. Ini memungkinkan aplikasi tetap berfungsi meskipun indeks belum dibuat.

2. **Restart Aplikasi**
   
   Terkadang, me-restart aplikasi dapat membantu karena:
   - Indeks mungkin sudah dibuat tetapi belum diterapkan sepenuhnya
   - Cache lokal mungkin perlu disegarkan

## Solusi Permanen

### 1. Buat Indeks Melalui Firebase Console

1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Pilih proyek "mylibraryappsskripsi"
3. Di sidebar kiri, klik "Firestore Database"
4. Klik tab "Indexes"
5. Klik "Add Index"
6. Buat indeks sesuai dengan error yang muncul:
   - Collection ID: nama koleksi (misalnya "notifications")
   - Fields: field yang digunakan dalam query (misalnya "userId" ASC, "timestamp" DESC)

### 2. Buat Indeks Melalui URL Error

1. Ketika error indeks muncul, lihat log aplikasi
2. Cari URL yang dimulai dengan "https://console.firebase.google.com/"
3. Buka URL tersebut di browser
4. Ikuti petunjuk untuk membuat indeks

### 3. Deploy Indeks dari File Konfigurasi

Aplikasi sudah dilengkapi dengan file konfigurasi indeks (`firestore.indexes.json`). Untuk men-deploy indeks:

1. Instal Firebase CLI:
   ```
   npm install -g firebase-tools
   ```

2. Login ke Firebase:
   ```
   firebase login
   ```

3. Inisialisasi proyek Firebase (jika belum):
   ```
   firebase init firestore
   ```

4. Deploy indeks:
   ```
   firebase deploy --only firestore:indexes
   ```

## Indeks yang Diperlukan

Berikut adalah daftar indeks yang diperlukan oleh aplikasi:

1. **notifications**
   - userId (ASC), timestamp (DESC)
   - userId (ASC), isRead (ASC), timestamp (DESC)

2. **transactions**
   - status (ASC), borrowDate (DESC)
   - userId (ASC), borrowDate (DESC)
   - userId (ASC), status (ASC), borrowDate (DESC)

3. **books**
   - genre (ASC), title (ASC)
   - quantity (ASC), title (ASC)

## Pemecahan Masalah Lanjutan

### Indeks Sudah Dibuat Tetapi Error Masih Muncul

1. **Periksa Status Indeks**
   
   Di Firebase Console, pastikan status indeks adalah "Enabled" bukan "Building". Pembuatan indeks bisa memakan waktu beberapa menit hingga beberapa jam tergantung ukuran data.

2. **Periksa Query**
   
   Pastikan query di kode aplikasi persis sama dengan indeks yang dibuat:
   - Field harus sama
   - Urutan pengurutan harus sama (ASC/DESC)
   - Filter harus sama

3. **Bersihkan Cache Aplikasi**
   
   Terkadang, cache lokal dapat menyebabkan masalah:
   - Hapus data aplikasi di perangkat
   - Atau gunakan opsi "Clear Cache" di pengaturan aplikasi

### Jika Masalah Berlanjut

Jika setelah mencoba semua solusi di atas masalah masih berlanjut:

1. Periksa log aplikasi untuk error yang lebih spesifik
2. Periksa aturan keamanan Firestore
3. Pastikan pengguna memiliki izin yang cukup untuk mengakses data
4. Hubungi dukungan Firebase jika diperlukan