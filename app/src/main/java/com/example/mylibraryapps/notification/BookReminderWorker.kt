package com.example.mylibraryapps.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mylibraryapps.MyLibraryApplication
import com.example.mylibraryapps.model.Transaction
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BookReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "BookReminderWorker"
        const val WORK_NAME = "book_reminder_work"
        
        // Konfigurasi hari
        const val WARNING_DAYS_BEFORE = 2 // Peringatan 2 hari sebelum jatuh tempo
        const val LOAN_PERIOD_DAYS = 7 // Periode peminjaman 7 hari
    }

    private val repository = (applicationContext as MyLibraryApplication).repository
    private val notificationHelper = NotificationHelper(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting book reminder check...")
            
            // Ambil semua transaksi yang sedang dipinjam
            val transactions = getActiveTransactions()
            Log.d(TAG, "Found ${transactions.size} active transactions")
            
            if (transactions.isNotEmpty()) {
                checkForOverdueAndWarnings(transactions)
            }
            
            Log.d(TAG, "Book reminder check completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in book reminder worker", e)
            Result.retry()
        }
    }

    private suspend fun getActiveTransactions(): List<Transaction> {
        return try {
            val snapshot = repository.firestore
                .collection("transactions")
                .whereEqualTo("status", "sedang dipinjam")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { document ->
                try {
                    Transaction(
                        id = document.id,
                        nameUser = document.getString("nameUser") ?: "",
                        title = document.getString("title") ?: "",
                        author = document.getString("author") ?: "",
                        borrowDate = document.getString("borrowDate") ?: "",
                        returnDate = document.getString("returnDate") ?: "",
                        status = document.getString("status") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing transaction document: ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions", e)
            emptyList()
        }
    }

    private fun checkForOverdueAndWarnings(transactions: List<Transaction>) {
        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        var overdueCount = 0
        var warningCount = 0
        
        transactions.forEach { transaction ->
            try {
                val borrowDate = dateFormat.parse(transaction.borrowDate)
                if (borrowDate != null) {
                    val borrowCalendar = Calendar.getInstance().apply { time = borrowDate }
                    
                    // Hitung hari sejak peminjaman
                    val daysSinceBorrow = TimeUnit.MILLISECONDS.toDays(
                        currentDate.timeInMillis - borrowCalendar.timeInMillis
                    ).toInt()
                    
                    when {
                        daysSinceBorrow > LOAN_PERIOD_DAYS -> {
                            // Buku terlambat
                            val daysOverdue = daysSinceBorrow - LOAN_PERIOD_DAYS
                            Log.d(TAG, "Overdue book: ${transaction.title} by ${transaction.nameUser}, $daysOverdue days overdue")
                            
                            notificationHelper.showOverdueNotification(
                                transaction.title,
                                transaction.nameUser,
                                daysOverdue
                            )
                            overdueCount++
                            
                        }
                        daysSinceBorrow >= (LOAN_PERIOD_DAYS - WARNING_DAYS_BEFORE) -> {
                            // Peringatan akan jatuh tempo
                            val daysLeft = LOAN_PERIOD_DAYS - daysSinceBorrow
                            Log.d(TAG, "Warning book: ${transaction.title} by ${transaction.nameUser}, $daysLeft days left")
                            
                            notificationHelper.showWarningNotification(
                                transaction.title,
                                transaction.nameUser,
                                daysLeft
                            )
                            warningCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing transaction: ${transaction.id}", e)
            }
        }
        
        // Jika ada banyak buku terlambat, kirim notifikasi ringkasan
        if (overdueCount > 3) {
            notificationHelper.showMultipleOverdueNotification(overdueCount)
        }
        
        Log.d(TAG, "Notification summary - Overdue: $overdueCount, Warnings: $warningCount")
    }
}