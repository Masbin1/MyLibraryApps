# Firebase Storage Rules - Solusi Error Permission

## Masalah Saat Ini
Rules Storage Anda menggunakan batasan waktu:
```javascript
allow read, write: if request.time < timestamp.date(2025, 8, 8);
```

Ini berarti upload hanya diizinkan sampai **8 Agustus 2025**. Jika sudah melewati atau mendekati tanggal tersebut, upload akan ditolak.

## Solusi - Ganti dengan Rules Permanen

### Opsi 1: Rules Aman untuk Production (Direkomendasikan)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Izinkan authenticated users untuk upload gambar buku
    match /book_covers/{allPaths=**} {
      allow read: if true; // Semua orang bisa lihat gambar
      allow write: if request.auth != null; // Hanya user login yang bisa upload
    }
    
    // Izinkan authenticated users untuk upload gambar lainnya
    match /{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

### Opsi 2: Rules Permisif untuk Testing (Tidak Aman)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if true; // Semua orang bisa upload/download
    }
  }
}
```

### Opsi 3: Rules dengan Admin Control (Paling Aman)
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Function untuk cek admin
    function isAdmin() {
      return request.auth != null && 
        firestore.get(/databases/(default)/documents/users/$(request.auth.uid)).data.is_admin == true;
    }
    
    // Gambar buku - admin bisa upload, semua bisa lihat
    match /book_covers/{allPaths=**} {
      allow read: if true;
      allow write: if isAdmin();
    }
    
    // File lainnya - hanya admin
    match /{allPaths=**} {
      allow read: if true;
      allow write: if isAdmin();
    }
  }
}
```

## Cara Mengupdate Rules

1. Buka **Firebase Console**
2. Pilih project **mylibraryappsskripsi**
3. Klik **Storage** di menu kiri
4. Klik tab **Rules**
5. Hapus rules lama dan paste salah satu rules di atas
6. Klik **Publish**

## Rekomendasi

Gunakan **Opsi 1** karena:
- ✅ Aman untuk production
- ✅ Mengizinkan user login untuk upload
- ✅ Semua orang bisa melihat gambar buku
- ✅ Tidak ada batasan waktu

Setelah mengupdate rules, coba upload gambar lagi. Masalah seharusnya teratasi.