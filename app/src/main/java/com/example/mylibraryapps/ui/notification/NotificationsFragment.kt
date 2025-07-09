package com.example.mylibraryapps.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylibraryapps.databinding.FragmentNotificationsBinding
import com.example.mylibraryapps.model.Notification
import com.google.firebase.auth.FirebaseAuth

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationViewModel: NotificationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupViewModel()
        observeViewModel()
        setupClickListeners()
        
        // Load notifications for current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            notificationViewModel.loadNotifications(user.uid)
        }
    }

    private fun setupClickListeners() {
        // This will be used for popup notifications in HomeFragment
        // For now, we'll add a refresh functionality
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            // Handle notification click
            handleNotificationClick(notification)
        }
        
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    private fun setupViewModel() {
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
    }

    private fun observeViewModel() {
        notificationViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isEmpty()) {
                binding.rvNotifications.visibility = View.GONE
                binding.tvEmptyNotifications.visibility = View.VISIBLE
            } else {
                binding.rvNotifications.visibility = View.VISIBLE
                binding.tvEmptyNotifications.visibility = View.GONE
                notificationAdapter.submitList(notifications)
            }
        }
        
        notificationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        // Mark notification as read
        notificationViewModel.markAsRead(notification.id)
        
        // Handle different notification types
        when (notification.type) {
            "return_reminder" -> {
                // Navigate to transaction detail or book detail
                // You can implement navigation here
            }
            "overdue" -> {
                // Navigate to overdue books
            }
            else -> {
                // Handle general notifications
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}