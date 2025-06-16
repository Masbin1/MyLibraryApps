package com.example.mylibraryapps.ui.notification

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.mylibraryapps.MyLibraryApplication
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.model.Notification
import com.google.firebase.auth.FirebaseAuth

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository = (application as MyLibraryApplication).repository
    private val auth = FirebaseAuth.getInstance()
    
    // Expose repository LiveData
    val notifications: LiveData<List<Notification>> = repository.notifications
    val unreadCount: LiveData<Int> = repository.unreadNotificationsCount
    val isLoading: LiveData<Boolean> = repository.isLoading
    
    init {
        refreshNotifications()
    }
    
    fun refreshNotifications() {
        val currentUser = auth.currentUser ?: return
        repository.loadNotifications(currentUser.uid)
    }
    
    fun markAsRead(notification: Notification) {
        repository.markNotificationAsRead(
            notification.id,
            onSuccess = {
                Log.d("NotificationVM", "Notification marked as read: ${notification.id}")
            },
            onFailure = { e ->
                Log.e("NotificationVM", "Failed to mark notification as read", e)
            }
        )
    }
    
    fun markAllAsRead() {
        repository.markAllNotificationsAsRead(
            onSuccess = {
                Log.d("NotificationVM", "All notifications marked as read")
            },
            onFailure = { e ->
                Log.e("NotificationVM", "Failed to mark all notifications as read", e)
            }
        )
    }
    
    fun createTestNotification() {
        val currentUser = auth.currentUser ?: return
        
        val notification = Notification(
            userId = currentUser.uid,
            title = "Test Notification",
            message = "This is a test notification to verify the notification system is working.",
            timestamp = java.util.Date(),
            isRead = false,
            type = "test"
        )
        
        repository.createNotification(
            notification,
            onSuccess = {
                Log.d("NotificationVM", "Test notification created")
            },
            onFailure = { e ->
                Log.e("NotificationVM", "Failed to create test notification", e)
            }
        )
    }
}