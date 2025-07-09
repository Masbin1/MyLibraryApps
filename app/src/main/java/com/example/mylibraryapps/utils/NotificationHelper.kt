package com.example.mylibraryapps.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibraryapps.R
import com.example.mylibraryapps.model.Notification
import com.example.mylibraryapps.ui.notification.NotificationAdapter
import com.example.mylibraryapps.ui.notification.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth

class NotificationHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val notificationViewModel: NotificationViewModel
) {
    
    private var popupWindow: PopupWindow? = null
    private var notificationAdapter: NotificationAdapter? = null
    
    fun showNotificationPopup(anchorView: View) {
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            return
        }
        
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.popup_notifications, null)
        
        setupPopupViews(popupView)
        
        popupWindow = PopupWindow(
            popupView,
            context.resources.getDimensionPixelSize(R.dimen.notification_popup_width),
            context.resources.getDimensionPixelSize(R.dimen.notification_popup_height),
            true
        )
        
        popupWindow?.apply {
            isOutsideTouchable = true
            isFocusable = true
            showAsDropDown(anchorView, 0, 0)
        }
        
        // Load notifications
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            notificationViewModel.loadNotifications(user.uid)
        }
    }
    
    private fun setupPopupViews(popupView: View) {
        val rvNotifications = popupView.findViewById<RecyclerView>(R.id.rvNotifications)
        val tvEmptyNotifications = popupView.findViewById<TextView>(R.id.tvEmptyNotifications)
        val tvMarkAllRead = popupView.findViewById<TextView>(R.id.tvMarkAllRead)
        val progressBar = popupView.findViewById<View>(R.id.progressBar)
        
        // Setup RecyclerView
        notificationAdapter = NotificationAdapter { notification ->
            // Handle notification click
            handleNotificationClick(notification)
        }
        
        rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
        
        // Setup Mark All Read click
        tvMarkAllRead.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                notificationViewModel.markAllAsRead(user.uid)
            }
        }
        
        // Observe notifications
        notificationViewModel.notifications.observe(lifecycleOwner) { notifications ->
            if (notifications.isEmpty()) {
                rvNotifications.visibility = View.GONE
                tvEmptyNotifications.visibility = View.VISIBLE
                tvMarkAllRead.visibility = View.GONE
            } else {
                rvNotifications.visibility = View.VISIBLE
                tvEmptyNotifications.visibility = View.GONE
                tvMarkAllRead.visibility = View.VISIBLE
                
                // Show only recent notifications (last 10)
                val recentNotifications = notifications.take(10)
                notificationAdapter?.submitList(recentNotifications)
            }
        }
        
        // Observe loading state
        notificationViewModel.isLoading.observe(lifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun handleNotificationClick(notification: Notification) {
        // Mark as read
        notificationViewModel.markAsRead(notification.id)
        
        // Handle different notification types
        when (notification.type) {
            "return_reminder", "overdue" -> {
                // You can implement navigation to specific transaction or book
                // For now, just dismiss the popup
                popupWindow?.dismiss()
            }
            else -> {
                popupWindow?.dismiss()
            }
        }
    }
    
    fun dismissPopup() {
        popupWindow?.dismiss()
    }
}