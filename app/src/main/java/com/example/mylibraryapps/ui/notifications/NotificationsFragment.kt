package com.example.mylibraryapps.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylibraryapps.databinding.FragmentNotificationsBinding
import com.example.mylibraryapps.data.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var notificationAdapter: NotificationAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        
        setupRecyclerView()
        setupRepository()
        loadNotifications()
        
        return binding.root
    }
    
    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            // Handle notification click
            markAsRead(notification.id)
        }
        
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }
    
    private fun setupRepository() {
        notificationRepository = NotificationRepository()
    }
    
    private fun loadNotifications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyState("Silakan login untuk melihat notifikasi")
            return
        }
        
        lifecycleScope.launch {
            try {
                notificationRepository.getNotifications(currentUser.uid).collect { notifications ->
                    if (notifications.isEmpty()) {
                        showEmptyState("Tidak ada notifikasi")
                    } else {
                        showNotifications(notifications)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationsFragment", "Error loading notifications", e)
                showEmptyState("Gagal memuat notifikasi")
            }
        }
    }
    
    private fun showNotifications(notifications: List<com.example.mylibraryapps.model.Notification>) {
        binding.tvEmptyNotifications.visibility = View.GONE
        binding.rvNotifications.visibility = View.VISIBLE
        notificationAdapter.submitList(notifications)
    }
    
    private fun showEmptyState(message: String) {
        binding.tvEmptyNotifications.visibility = View.VISIBLE
        binding.rvNotifications.visibility = View.GONE
        binding.tvEmptyNotifications.text = message
    }
    
    private fun markAsRead(notificationId: String) {
        lifecycleScope.launch {
            try {
                val success = notificationRepository.markAsRead(notificationId)
                if (!success) {
                    Toast.makeText(context, "Gagal menandai notifikasi sebagai dibaca", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NotificationsFragment", "Error marking notification as read", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}