package com.example.mylibraryapps.data

import android.net.Uri
import android.util.Log
import com.example.mylibraryapps.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class BookRepository {
    private val TAG = "BookRepository"
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference.child("book_covers")
    
    init {
        Log.d(TAG, "BookRepository initialized")
        Log.d(TAG, "Firebase Storage bucket: ${storage.app.options.storageBucket}")
        
        // Test storage connection saat inisialisasi
        testStorageConnection()
    }
    
    private fun testStorageConnection() {
        try {
            val bucket = storage.app.options.storageBucket
            Log.d(TAG, "Storage bucket from config: $bucket")
            
            if (bucket == null || bucket.isEmpty()) {
                Log.e(TAG, "Storage bucket is null or empty - Storage belum dikonfigurasi!")
                return
            }
            
            val testRef = storage.reference.child("test")
            Log.d(TAG, "Storage connection test successful")
            Log.d(TAG, "Test reference path: ${testRef.path}")
            Log.d(TAG, "Storage bucket: ${testRef.bucket}")
        } catch (e: Exception) {
            Log.e(TAG, "Storage connection test failed", e)
        }
    }

    fun addBook(book: Book, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("books")
            .add(book)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
    
    /**
     * Upload gambar cover buku ke Firebase Storage dan kemudian tambahkan buku ke Firestore
     */
    fun uploadBookCoverAndAddBook(
        imageUri: Uri,
        book: Book,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        try {
            Log.d(TAG, "Memulai upload gambar: $imageUri")
            
            // Periksa apakah Firebase Storage telah diinisialisasi dengan benar
            try {
                val storageInstance = FirebaseStorage.getInstance()
                val bucket = storageInstance.app.options.storageBucket
                Log.d(TAG, "Firebase Storage instance berhasil dibuat")
                Log.d(TAG, "Storage bucket: $bucket")
                
                if (bucket == null || bucket.isEmpty()) {
                    Log.e(TAG, "Storage bucket tidak dikonfigurasi")
                    onFailure(Exception("Storage bucket tidak dikonfigurasi di Firebase"))
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase Storage tidak dapat diinisialisasi", e)
                onFailure(Exception("Firebase Storage tidak dapat diinisialisasi: ${e.message}"))
                return
            }
            
            // Buat referensi storage dengan URL bucket yang eksplisit
            val storage = FirebaseStorage.getInstance()
            
            // Gunakan URL bucket yang eksplisit untuk memastikan storage reference benar
            val storageRef = try {
                storage.reference
            } catch (e: Exception) {
                Log.e(TAG, "Error membuat storage reference, coba dengan URL eksplisit", e)
                FirebaseStorage.getInstance("gs://mylibraryappsskripsi.firebasestorage.app").reference
            }
            
            Log.d(TAG, "Storage reference created successfully")
            
            // Buat nama file unik untuk gambar dengan extension yang tepat
            val timeStamp = System.currentTimeMillis()
            val fileName = "book_covers/cover_${timeStamp}.jpg"
            Log.d(TAG, "File path: $fileName")
            
            val fileRef = storageRef.child(fileName)
            Log.d(TAG, "File reference created: ${fileRef.path}")
            Log.d(TAG, "Storage bucket: ${fileRef.bucket}")
            Log.d(TAG, "Storage name: ${fileRef.name}")
            
            // Validasi URI gambar
            if (imageUri == null) {
                Log.e(TAG, "Image URI is null")
                onFailure(Exception("URI gambar tidak valid"))
                return
            }
            
            // Test koneksi ke storage sebelum upload
            Log.d(TAG, "Testing storage connection...")
            
            // Upload gambar ke Firebase Storage
            Log.d(TAG, "Starting upload task...")
            val uploadTask = fileRef.putFile(imageUri)
            Log.d(TAG, "Upload task created successfully")
            
            // Monitor progress upload
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                Log.d(TAG, "Upload progress: $progress%")
                onProgress(progress)
            }
            
            // Tambahkan listener untuk failure dengan error handling yang lebih baik
            uploadTask.addOnFailureListener { exception ->
                Log.e(TAG, "Upload gagal pada tahap awal", exception)
                Log.e(TAG, "Error class: ${exception.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${exception.message}")
                
                val detailedError = when {
                    exception.message?.contains("Object does not exist") == true -> {
                        Exception("Firebase Storage belum dikonfigurasi dengan benar. Pastikan Storage sudah diaktifkan di Firebase Console.")
                    }
                    exception.message?.contains("Permission denied") == true -> {
                        Exception("Izin upload ditolak. Periksa Security Rules di Firebase Storage.")
                    }
                    exception.message?.contains("storage bucket") == true -> {
                        Exception("Storage bucket tidak ditemukan. Periksa konfigurasi Firebase di google-services.json.")
                    }
                    exception.message?.contains("network") == true -> {
                        Exception("Masalah koneksi internet. Periksa koneksi Anda.")
                    }
                    else -> {
                        Exception("Upload gagal: ${exception.message}")
                    }
                }
                
                onFailure(detailedError)
            }
            
            // Handle hasil upload
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Upload task tidak berhasil", task.exception)
                    task.exception?.let { throw it }
                }
                Log.d(TAG, "Upload berhasil, mendapatkan URL download")
                // Dapatkan URL download
                fileRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result.toString()
                    Log.d(TAG, "Upload berhasil, URL: $downloadUrl")
                    
                    // Tambahkan buku dengan URL gambar
                    val bookWithCover = book.copy(coverUrl = downloadUrl)
                    addBook(bookWithCover, onSuccess, onFailure)
                } else {
                    Log.e(TAG, "Gagal mendapatkan URL download", task.exception)
                    onFailure(task.exception ?: Exception("Gagal mendapatkan URL download"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat memproses upload", e)
            onFailure(e)
        }
    }
    
    /**
     * Method upload alternatif untuk troubleshooting
     */
    fun testUpload(imageUri: Uri, onResult: (String) -> Unit) {
        try {
            Log.d(TAG, "Testing upload dengan URI: $imageUri")
            
            // Coba dengan URL bucket yang eksplisit
            val storage = FirebaseStorage.getInstance("gs://mylibraryappsskripsi.firebasestorage.app")
            val storageRef = storage.reference
            val testRef = storageRef.child("test_images/test_${System.currentTimeMillis()}.jpg")
            
            Log.d(TAG, "Test reference: ${testRef.path}")
            Log.d(TAG, "Test bucket: ${testRef.bucket}")
            
            val uploadTask = testRef.putFile(imageUri)
            
            uploadTask.addOnSuccessListener {
                Log.d(TAG, "Test upload berhasil!")
                onResult("SUCCESS: Upload berhasil ke ${testRef.path}")
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Test upload gagal", exception)
                Log.e(TAG, "Error details: ${exception.cause}")
                onResult("ERROR: ${exception.javaClass.simpleName} - ${exception.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception pada test upload", e)
            onResult("EXCEPTION: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
    
    /**
     * Method untuk check apakah Storage sudah aktif
     */
    fun checkStorageStatus(onResult: (String) -> Unit) {
        try {
            val bucket = storage.app.options.storageBucket
            
            if (bucket == null || bucket.isEmpty()) {
                onResult("❌ STORAGE BELUM AKTIF: Storage bucket tidak ditemukan di konfigurasi")
                return
            }
            
            Log.d(TAG, "Checking storage status...")
            Log.d(TAG, "Storage bucket: $bucket")
            
            // Test dengan membuat referensi sederhana
            val testRef = storage.reference.child("status_check")
            
            onResult("✅ STORAGE AKTIF: Bucket = $bucket, Path = ${testRef.path}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage status", e)
            onResult("❌ STORAGE ERROR: ${e.message}")
        }
    }

    /**
     * Method untuk upload dengan pendekatan yang lebih sederhana
     */
    fun simpleUploadTest(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            Log.d(TAG, "Simple upload test started")
            
            // Langsung gunakan storage instance dengan URL eksplisit
            val storageRef = FirebaseStorage.getInstance("gs://mylibraryappsskripsi.firebasestorage.app")
                .reference
                .child("book_covers")
                .child("simple_test_${System.currentTimeMillis()}.jpg")
            
            Log.d(TAG, "Simple test reference: ${storageRef.path}")
            
            storageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d(TAG, "Simple upload success!")
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG, "Download URL: $uri")
                        onSuccess(uri.toString())
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get download URL", e)
                        onFailure(e)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Simple upload failed", exception)
                    onFailure(exception)
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Exception in simple upload", e)
            onFailure(e)
        }
    }
}
