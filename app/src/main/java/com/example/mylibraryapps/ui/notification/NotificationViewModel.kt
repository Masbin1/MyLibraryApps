package com.example.mylibraryapps.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylibraryapps.data.NotificationRepository
import com.example.mylibraryapps.model.Notification
import com.example.mylibraryapps.service.NotificationService
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val notificationRepository = NotificationRepository()
    private val notificationService = NotificationService()
    
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications
    
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun loadNotifications(userId: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            // First, check for new notifications based on due dates
            notificationService.checkNotificationsForUser(userId)
            
            // Then load all notifications for the user
            notificationRepository.getNotifications(userId)
                .catch { exception ->
                    _isLoading.value = false
                    // Handle error
                }
                .collect { notificationList ->
                    _notifications.value = notificationList
                    _unreadCount.value = notificationList.count { !it.isRead }
                    _isLoading.value = false
                }
        }
    }
    
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }
    
    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
        }
    }
    
    fun createTestNotification(userId: String) {
        viewModelScope.launch {
            val notification = Notification(
                userId = userId,
                title = "Test Notification",
                message = "This is a test notification to verify the notification system is working.",
                timestamp = java.util.Date(),
                isRead = false,
                type = "test"
            )
            
            notificationRepository.addNotification(notification)
        }
    }
}