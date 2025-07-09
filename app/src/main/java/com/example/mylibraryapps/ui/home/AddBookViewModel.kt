package com.example.mylibraryapps.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mylibraryapps.data.BookRepository
import com.example.mylibraryapps.model.Book
import com.google.firebase.Timestamp

class AddBookViewModel(private val repository: BookRepository = BookRepository()) : ViewModel() {
    private val TAG = "AddBookViewModel"
    
    private val _addBookSuccess = MutableLiveData<Boolean>()
    val addBookSuccess: LiveData<Boolean> = _addBookSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> = _errorMessage
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress

    fun addBook(
        title: String,
        author: String,
        year: String,
        publisher: String,
        genre: String,
        description: String,
        quantity: Int,
        material: String,
        specifications: String,
        purchaseDate: String,
    ) {
        _isLoading.value = true
        
        val book = Book(
            title = title,
            author = author,
            publisher = publisher,
            genre = genre,
            specifications = specifications,
            quantity = quantity.toLong(),
            material = material,
            purchaseDate = com.google.firebase.Timestamp.now()
        )

        repository.addBook(book,
            onSuccess = { 
                _addBookSuccess.postValue(true)
                _isLoading.postValue(false)
            },
            onFailure = { e -> 
                _errorMessage.postValue("Gagal menambahkan buku: ${e.message}")
                _isLoading.postValue(false)
            }
        )
    }
    
    fun uploadBookCoverAndAddBook(
        imageUri: Uri,
        title: String,
        author: String,
        year: String,
        publisher: String,
        genre: String,
        description: String,
        quantity: Int,
        material: String,
        specifications: String,
        purchaseDate: String,
    ) {
        Log.d(TAG, "Memulai proses upload gambar dan tambah buku")
        Log.d(TAG, "Image URI: $imageUri")
        
        _isLoading.value = true
        _uploadProgress.value = 0
        
        val book = Book(
            title = title,
            author = author,
            publisher = publisher,
            genre = genre,
            specifications = specifications,
            quantity = quantity.toLong(),
            material = material,
            purchaseDate = com.google.firebase.Timestamp.now()
        )
        
        Log.d(TAG, "Book object created: $book")
        
        try {
            repository.uploadBookCoverAndAddBook(
                imageUri = imageUri,
                book = book,
                onSuccess = { 
                    Log.d(TAG, "Upload berhasil!")
                    _addBookSuccess.postValue(true)
                    _isLoading.postValue(false)
                },
                onFailure = { e -> 
                    Log.e(TAG, "Upload gagal: ${e.message}", e)
                    val errorMsg = when {
                        e.message?.contains("Object does not exist at location") == true -> 
                            "Gagal mengunggah gambar: Pastikan Firebase Storage diatur dengan benar dan aturan keamanan mengizinkan upload."
                        e.message?.contains("Permission denied") == true -> 
                            "Gagal mengunggah gambar: Izin ditolak. Periksa aturan keamanan Firebase Storage."
                        e.message?.contains("Network") == true -> 
                            "Gagal mengunggah gambar: Masalah jaringan. Periksa koneksi internet Anda."
                        e.message?.contains("storage bucket") == true ->
                            "Gagal mengunggah gambar: Storage bucket tidak ditemukan. Periksa konfigurasi Firebase."
                        else -> "Gagal mengunggah gambar: ${e.message ?: "Error tidak diketahui"}"
                    }
                    _errorMessage.postValue(errorMsg)
                    _isLoading.postValue(false)
                },
                onProgress = { progress ->
                    Log.d(TAG, "Upload progress: $progress%")
                    _uploadProgress.postValue(progress)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat memulai upload", e)
            _errorMessage.postValue("Terjadi kesalahan: ${e.message}")
            _isLoading.postValue(false)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * Method untuk check storage status
     */
    fun checkStorageStatus() {
        Log.d(TAG, "Checking storage status")
        repository.checkStorageStatus { result ->
            Log.d(TAG, "Storage status: $result")
            _errorMessage.postValue("STORAGE STATUS: $result")
        }
    }

    /**
     * Method untuk test upload sederhana
     */
    fun testSimpleUpload(imageUri: Uri) {
        Log.d(TAG, "Testing simple upload")
        _isLoading.value = true
        
        repository.simpleUploadTest(
            imageUri = imageUri,
            onSuccess = { downloadUrl ->
                Log.d(TAG, "Simple upload berhasil: $downloadUrl")
                _errorMessage.postValue("TEST BERHASIL: Upload berhasil! URL: $downloadUrl")
                _isLoading.postValue(false)
            },
            onFailure = { e ->
                Log.e(TAG, "Simple upload gagal", e)
                _errorMessage.postValue("TEST GAGAL: ${e.message}")
                _isLoading.postValue(false)
            }
        )
    }
}
