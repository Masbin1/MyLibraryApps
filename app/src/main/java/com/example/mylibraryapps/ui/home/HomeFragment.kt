package com.example.mylibraryapps.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentHomeBinding
import com.example.mylibraryapps.model.Notification
import com.example.mylibraryapps.ui.book.BookAdapter
import com.example.mylibraryapps.ui.notification.NotificationAdapter
import com.example.mylibraryapps.ui.notification.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var bookAdapter: BookAdapter
    private lateinit var notificationAdapter: NotificationAdapter
    private var notificationPopup: PopupWindow? = null
    private var notificationBadge: FrameLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupFilterButtons()
        setupAddBookButton()
        setupNotificationButton()
        setupDebugFeatures()
        
        // Dapatkan user ID dari Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            homeViewModel.loadUserData(currentUser.uid)
            // Load notifications untuk show badge count
            notificationViewModel.loadNotifications(currentUser.uid)
        }
    }

    // Setup Add Book button based on admin status
    private fun setupAddBookButton() {
        // Default: hide the button until we know the admin status
        binding.fabAddBook.visibility = View.GONE
        
        // Observe admin status from ViewModel
        homeViewModel.isAdmin.observe(viewLifecycleOwner) { isAdmin ->
            Log.d("HomeFragment", "Admin status from ViewModel: $isAdmin")
            binding.fabAddBook.visibility = if (isAdmin) View.VISIBLE else View.GONE
            
            if (isAdmin) {
                binding.fabAddBook.setOnClickListener {
                    // Gunakan ID yang sesuai dengan nav_graph.xml
                    findNavController().navigate(R.id.action_homeFragment_to_addBookFragment)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(emptyList()) { selectedBook ->
            selectedBook?.let {
                val bundle = Bundle().apply {
                    putParcelable("book", it)
                }
                findNavController().navigate(R.id.bookDetailFragment, bundle)
            }
        }

        binding.rvBooks.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
            setHasFixedSize(true)
        }
    }

    @SuppressLint("SetTextI18s")
    private fun setupObservers() {
        homeViewModel.books.observe(viewLifecycleOwner) { books ->
            books?.let {
                Log.d("HomeFragment", "Books received: ${it.size} items")
                it.forEachIndexed { index, book ->
                    Log.d("HomeFragment", "Book $index: ${book.title}")
                }
                bookAdapter.updateBooks(it)
            }
        }
        
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }

        homeViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Using Snackbar instead of Toast for better UX
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    it,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
                homeViewModel.clearErrorMessage()
            }
        }

        homeViewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvGreeting.text = "Halo, $name"
        }
        
        // Observe notification count
        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            updateNotificationBadge(count)
        }
    }
    
    private fun setupNotificationButton() {
        // Create notification badge
        createNotificationBadge()
        
        // Set click listener for notification icon
        binding.ivNotification.setOnClickListener {
            showNotificationPopup()
        }
    }
    
    private fun createNotificationBadge() {
        // Create a badge view to show unread count
        val parent = binding.ivNotification.parent as ViewGroup
        
        // Remove existing badge if any
        if (notificationBadge != null) {
            parent.removeView(notificationBadge)
        }
        
        // Create new badge
        notificationBadge = FrameLayout(requireContext()).apply {
            val size = resources.getDimensionPixelSize(R.dimen.notification_badge_size)
            val params = FrameLayout.LayoutParams(size, size)
            params.gravity = Gravity.TOP or Gravity.END
            params.setMargins(0, 0, 0, 0)
            layoutParams = params
            
            background = ContextCompat.getDrawable(requireContext(), R.drawable.notification_badge)
            visibility = View.GONE
            
            // Add text view for count
            val textView = TextView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            
            addView(textView)
        }
        
        // Add badge to parent layout
        (binding.ivNotification.parent as ViewGroup).addView(notificationBadge)
        
        // Position badge relative to notification icon
        val iconLocation = IntArray(2)
        binding.ivNotification.getLocationInWindow(iconLocation)
        
        val badgeParams = notificationBadge?.layoutParams as? ViewGroup.MarginLayoutParams
        badgeParams?.let {
            it.leftMargin = iconLocation[0] + binding.ivNotification.width - it.width / 2
            it.topMargin = iconLocation[1] - it.height / 2
            notificationBadge?.layoutParams = it
        }
    }
    
    private fun updateNotificationBadge(count: Int) {
        notificationBadge?.let { badge ->
            val textView = badge.getChildAt(0) as? TextView
            
            if (count > 0) {
                textView?.text = if (count > 99) "99+" else count.toString()
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        }
    }
    
    private fun showNotificationPopup() {
        // Dismiss existing popup if any
        notificationPopup?.dismiss()
        
        // Inflate popup layout
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_notifications, null)
        
        // Create popup window
        notificationPopup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.transparent))
            isOutsideTouchable = true
        }
        
        // Setup RecyclerView
        val rvNotifications = popupView.findViewById<RecyclerView>(R.id.rvNotifications)
        val progressBar = popupView.findViewById<View>(R.id.progressBar)
        val tvEmptyNotifications = popupView.findViewById<TextView>(R.id.tvEmptyNotifications)
        val tvMarkAllRead = popupView.findViewById<TextView>(R.id.tvMarkAllRead)
        
        // Setup adapter
        notificationAdapter = NotificationAdapter { notification ->
            handleNotificationClick(notification)
        }
        
        rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
        
        // Observe notifications
        notificationViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            notificationAdapter.submitList(notifications)
            
            if (notifications.isEmpty()) {
                tvEmptyNotifications.visibility = View.VISIBLE
                rvNotifications.visibility = View.GONE
            } else {
                tvEmptyNotifications.visibility = View.GONE
                rvNotifications.visibility = View.VISIBLE
            }
        }
        
        // Observe loading state
        notificationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Mark all as read button
        tvMarkAllRead.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                notificationViewModel.markAllAsRead(userId)
            }
        }
        
        // Show popup
        notificationPopup?.showAsDropDown(binding.ivNotification, 0, 10)
    }
    
    private fun handleNotificationClick(notification: Notification) {
        // Mark notification as read
        notificationViewModel.markAsRead(notification.id)
        
        // Close popup first
        notificationPopup?.dismiss()
        
        // Handle navigation based on notification type
        when (notification.type) {
            "return_reminder", "overdue" -> {
                // Navigate to transaction detail if we have the ID
                if (notification.relatedItemId.isNotEmpty()) {
                    // You would need to implement this navigation
                    // findNavController().navigate(...)
                    
                    // For now, just show a message
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "Buku: ${notification.relatedItemTitle}",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    // Show notification message
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        notification.message,
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            else -> {
                // Show notification message for other types
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    notification.message,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupFilterButtons() {
        val genres = resources.getStringArray(R.array.book_types)

        for (genre in genres) {
            val chip = com.google.android.material.chip.Chip(requireContext(), null, R.style.CustomChipStyle).apply {
                text = genre
                isCheckable = true
                isClickable = true
                setOnClickListener {
                    homeViewModel.filterBooksByGenre(genre)
                }
            }
            binding.chipGroupGenre.addView(chip)

        }

    }
    
    private fun setupDebugFeatures() {
        // Long press pada progress bar untuk force refresh
        binding.progressBar.setOnLongClickListener {
            Log.d("HomeFragment", "Force refreshing data...")
            homeViewModel.forceRefreshData()
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "Force refresh data...",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            true
        }
        
        // Long press pada greeting untuk refresh
        binding.tvGreeting.setOnLongClickListener {
            Log.d("HomeFragment", "Refreshing data...")
            homeViewModel.refreshData()
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "Refreshing data...",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            true
        }
        
        // Double tap pada recyclerview untuk check Firebase connection
        var lastTapTime: Long = 0
        binding.rvBooks.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < 500) {
                Log.d("HomeFragment", "Checking Firebase connection...")
                homeViewModel.checkFirebaseConnection()
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Checking Firebase connection...",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
            lastTapTime = currentTime
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        notificationPopup?.dismiss()
        notificationPopup = null
        _binding = null
    }

}