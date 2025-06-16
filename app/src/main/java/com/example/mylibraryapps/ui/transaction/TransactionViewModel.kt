package com.example.mylibraryapps.ui.transaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.mylibraryapps.MyLibraryApplication
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.model.Transaction

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as MyLibraryApplication).repository
    
    // Expose repository LiveData
    val transactions: LiveData<List<Transaction>> = repository.transactions
    val isLoading: LiveData<Boolean> = repository.isLoading
    val errorMessage: LiveData<String?> = repository.errorMessage
    
    // Check if user is admin
    val isAdmin: LiveData<Boolean> = repository.userData.map { user ->
        user?.is_admin ?: false
    }
    
    init {
        // Refresh transactions data
        refreshTransactions()
    }
    
    fun refreshTransactions(statusFilter: String? = null) {
        repository.loadTransactions(statusFilter)
    }
    
    fun clearErrorMessage() {
        repository.clearErrorMessage()
    }
}