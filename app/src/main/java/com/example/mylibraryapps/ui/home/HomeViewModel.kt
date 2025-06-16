package com.example.mylibraryapps.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.mylibraryapps.MyLibraryApplication
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.User

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as MyLibraryApplication).repository
    
    // Expose repository LiveData
    val books: LiveData<List<Book>> = repository.books
    val isLoading: LiveData<Boolean> = repository.isLoading
    val errorMessage: LiveData<String?> = repository.errorMessage
    
    // Transform user data to just the name
    val userName: LiveData<String> = repository.userData.map { user ->
        user?.nama ?: "Anggota"
    }
    
    // Check if user is admin
    val isAdmin: LiveData<Boolean> = repository.userData.map { user ->
        val isAdmin = user?.is_admin ?: false
        Log.d("HomeViewModel", "User admin status: $isAdmin, User: $user")
        isAdmin
    }

    init {
        // Data is preloaded in the repository, but we can refresh it here if needed
        refreshData()
    }

    fun refreshData() {
        repository.loadBooks()
    }

    fun loadUserData(userId: String) {
        repository.loadUserData(userId)
    }

    fun filterBooksByGenre(genre: String) {
        repository.filterBooksByGenre(genre)
    }

    fun clearErrorMessage() {
        repository.clearErrorMessage()
    }
}