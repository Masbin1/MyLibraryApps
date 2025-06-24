# Panduan Firestore untuk MyLibraryApps

Dokumen ini berisi panduan untuk mengelola Firebase Firestore dalam aplikasi MyLibraryApps.

## Struktur Database

Aplikasi menggunakan beberapa koleksi utama:

1. **users** - Menyimpan data pengguna
2. **books** - Menyimpan data buku
3. **transactions** - Menyimpan data peminjaman
4. **notifications** - Menyimpan notifikasi pengguna

## Aturan Keamanan

Aturan keamanan Firestore didefinisikan dalam file `firestore.rules`. Aturan ini memastikan:

- Pengguna hanya dapat mengakses data mereka sendiri
- Admin memiliki akses lebih luas
- Buku dapat dibaca oleh semua pengguna yang terautentikasi
- Hanya admin yang dapat menambah/mengubah/menghapus buku

### Cara Memperbarui Aturan

1. Edit file `firestore.rules`
2. Deploy menggunakan Firebase CLI:
   ```
   firebase deploy --only firestore:rules
   ```

## Indeks

Indeks Firestore didefinisikan dalam file `firestore.indexes.json`. Indeks ini diperlukan untuk query yang menggunakan kombinasi filter dan pengurutan.

### Indeks yang Digunakan

1. **notifications** - userId (ASC) + timestamp (DESC)
2. **transactions** - status (ASC) + borrowDate (DESC)
3. **transactions** - userId (ASC) + borrowDate (DESC)
4. **books** - genre (ASC) + title (ASC)

### Cara Memperbarui Indeks

1. Edit file `firestore.indexes.json`
2. Deploy menggunakan Firebase CLI:
   ```
   firebase deploy --only firestore:indexes
   ```

## Penanganan Error

Aplikasi menggunakan `FirestoreErrorHandler` untuk menangani error Firestore secara konsisten. Kelas ini menyediakan:

- Pesan error yang ramah pengguna
- Logging yang konsisten
- Penanganan khusus untuk berbagai jenis error

### Cara Menggunakan

```kotlin
// Contoh penggunaan di repository
.addOnFailureListener { e ->
    _errorMessage.value = FirestoreErrorHandler.handleException(e, "mengakses data", TAG)
}
```

## Praktik Terbaik

1. **Selalu Gunakan Caching**
   - Aplikasi mengimplementasikan caching untuk mengurangi beban Firestore
   - Pastikan untuk memperbarui cache saat data berubah

2. **Batasi Jumlah Pembacaan**
   - Gunakan filter untuk membatasi jumlah dokumen yang dibaca
   - Hindari membaca seluruh koleksi jika tidak diperlukan

3. **Gunakan Batch untuk Operasi Atomik**
   - Untuk operasi yang memerlukan konsistensi (seperti peminjaman buku), gunakan batch

4. **Uji dengan Emulator**
   - Gunakan Firebase Emulator Suite untuk pengujian lokal
   - Ini membantu mengidentifikasi masalah indeks dan aturan sebelum deploy

## Pemecahan Masalah

### Error "Missing Index"

Jika Anda melihat error "FAILED_PRECONDITION: The query requires an index":

1. Klik link yang disediakan dalam error
2. Atau tambahkan indeks ke `firestore.indexes.json` dan deploy

### Error "Permission Denied"

Jika Anda melihat error "PERMISSION_DENIED":

1. Pastikan pengguna sudah login
2. Periksa aturan keamanan di `firestore.rules`
3. Pastikan pengguna memiliki izin yang sesuai untuk operasi tersebut

## Referensi

- [Dokumentasi Firestore](https://firebase.google.com/docs/firestore)
- [Aturan Keamanan Firestore](https://firebase.google.com/docs/firestore/security/get-started)
- [Indeks Firestore](https://firebase.google.com/docs/firestore/query-data/indexing)
- [Firebase CLI](https://firebase.google.com/docs/cli)