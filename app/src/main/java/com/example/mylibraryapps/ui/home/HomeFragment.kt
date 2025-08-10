package com.example.mylibraryapps.ui.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.example.mylibraryapps.ui.recommendation.RecommendationAdapter
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var bookAdapter: BookAdapter
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var recommendationAdapter: RecommendationAdapter
    private var notificationPopup: PopupWindow? = null
    private var notificationBadge: FrameLayout? = null
    private var searchTimer: android.os.CountDownTimer? = null
    private var currentFilter: String = "Semua"
    private var bookViewStartTime: Long = 0
    
    // Broadcast receiver untuk menerima update notification count
    private val notificationCountReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.mylibraryapps.NOTIFICATION_COUNT_UPDATE") {
                val unreadCount = intent.getIntExtra("unread_count", 0)
                Log.d("HomeFragment", "📡 Received notification count update: $unreadCount")
                updateNotificationBadge(unreadCount)
            }
        }
    }

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
        setupRecommendationRecyclerView()
        setupObservers()
        setupFilterButtons()
        setupAddBookButton()
        setupNotificationButton()
        setupSearchBar()
        setupDebugFeatures()
        setupNotificationCountReceiver()
        
        // Dapatkan user ID dari Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            homeViewModel.loadUserData(currentUser.uid)
            // Load notifications untuk show badge count
            notificationViewModel.loadNotifications(currentUser.uid)
            // Load recommendations untuk collaborative filtering
            homeViewModel.loadRecommendations(currentUser.uid)
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
                // Track book view interaction
                val currentUser = FirebaseAuth.getInstance().currentUser
                currentUser?.uid?.let { userId ->
                    bookViewStartTime = System.currentTimeMillis()
                    homeViewModel.trackBookView(userId, it)
                }
                
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
            // Disable nested scrolling karena sudah ada NestedScrollView sebagai parent
            isNestedScrollingEnabled = false
        }
    }
    
    private fun setupRecommendationRecyclerView() {
        recommendationAdapter = RecommendationAdapter { recommendation ->
            // Track recommendation interaction
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                homeViewModel.trackBookView(userId, recommendation.book)
            }
            
            val bundle = Bundle().apply {
                putParcelable("book", recommendation.book)
            }
            findNavController().navigate(R.id.bookDetailFragment, bundle)
        }

        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendationAdapter
            setHasFixedSize(true)
            // Disable nested scrolling untuk horizontal RecyclerView
            isNestedScrollingEnabled = false
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
        
        // Observe notification count directly from ViewModel as backup
        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            Log.d("HomeFragment", "📊 Direct unread count observer: $count")
            updateNotificationBadge(count)
        }
        
        // Observe recommendations
        homeViewModel.recommendedBooks.observe(viewLifecycleOwner) { recommendations ->
            Log.d("HomeFragment", "🔍 Received ${recommendations.size} recommendations")
            
            if (recommendations.isEmpty()) {
                binding.layoutRecommendations.visibility = View.GONE
                binding.dividerRecommendations.visibility = View.GONE
            } else {
                binding.layoutRecommendations.visibility = View.VISIBLE
                binding.dividerRecommendations.visibility = View.VISIBLE
                recommendationAdapter.submitList(recommendations)
                
                Log.d("HomeFragment", "✨ Recommendations section is now STICKY - will remain visible when scrolling!")
            }
        }
        
        // Observe recommendations loading state
        homeViewModel.isLoadingRecommendations.observe(viewLifecycleOwner) { isLoading ->
            binding.progressRecommendations.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d("HomeFragment", "🔍 Recommendations loading: $isLoading")
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
        // Wait for layout to be ready
        binding.ivNotification.post {
            // Get the FrameLayout container that holds the notification icon
            val container = binding.ivNotification.parent as FrameLayout
            
            // Remove existing badge if any
            notificationBadge?.let { 
                container.removeView(it)
                notificationBadge = null
            }
            
            // Create new badge
            notificationBadge = FrameLayout(requireContext()).apply {
                val size = resources.getDimensionPixelSize(R.dimen.notification_badge_size)
                val params = FrameLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = 0
                    rightMargin = 0
                }
                
                layoutParams = params
                background = ContextCompat.getDrawable(requireContext(), R.drawable.notification_badge)
                visibility = View.GONE
                elevation = 8f
                
                // Add text view for count
                val textView = TextView(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    textSize = 10f
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                
                addView(textView)
            }
            
            // Add badge to container
            container.addView(notificationBadge)
            Log.d("HomeFragment", "🎯 Notification badge created and positioned")
        }
    }
    
    private fun updateNotificationBadge(count: Int) {
        notificationBadge?.let { badge ->
            val textView = badge.getChildAt(0) as? TextView
            
            if (count > 0) {
                textView?.text = if (count > 99) "99+" else count.toString()
                badge.visibility = View.VISIBLE
                Log.d("HomeFragment", "🔴 Badge updated: $count notifications")
            } else {
                badge.visibility = View.GONE
                Log.d("HomeFragment", "✅ Badge hidden: no unread notifications")
            }
        }
    }
    
    private fun setupNotificationCountReceiver() {
        val filter = IntentFilter("com.example.mylibraryapps.NOTIFICATION_COUNT_UPDATE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(notificationCountReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            requireContext().registerReceiver(notificationCountReceiver, filter)
        }
        Log.d("HomeFragment", "📡 Notification count receiver registered")
    }
    
    private fun showNotificationPopup() {
        // Dismiss existing popup if any
        notificationPopup?.dismiss()
        
        // Reload notifications when popup is opened
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
        
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
        
        // Observe notifications - use removeObservers to avoid duplicate observers
        notificationViewModel.notifications.removeObservers(viewLifecycleOwner)
        notificationViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            Log.d("HomeFragment", "📋 Popup received ${notifications.size} notifications")
            notificationAdapter.submitList(notifications)
            
            if (notifications.isEmpty()) {
                tvEmptyNotifications.visibility = View.VISIBLE
                rvNotifications.visibility = View.GONE
                Log.d("HomeFragment", "📋 Showing empty state")
            } else {
                tvEmptyNotifications.visibility = View.GONE
                rvNotifications.visibility = View.VISIBLE
                Log.d("HomeFragment", "📋 Showing ${notifications.size} notifications")
            }
        }
        
        // Observe loading state
        notificationViewModel.isLoading.removeObservers(viewLifecycleOwner)
        notificationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d("HomeFragment", "📋 Loading state: $isLoading")
        }
        
        // Mark all as read button
        tvMarkAllRead.setOnClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                Log.d("HomeFragment", "📋 Marking all notifications as read for user: $userId")
                notificationViewModel.markAllAsRead(userId)
                // Reload notifications after marking as read
                notificationViewModel.loadNotifications(userId)
            }
        }
        
        // Show popup
        try {
            notificationPopup?.showAsDropDown(binding.ivNotification, 0, 10)
            Log.d("HomeFragment", "📋 Notification popup shown")
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error showing notification popup", e)
        }
    }
    
    private fun handleNotificationClick(notification: Notification) {
        Log.d("HomeFragment", "📋 Notification clicked: ${notification.title}")
        
        // Mark notification as read
        notificationViewModel.markAsRead(notification.id)
        
        // Reload notifications to update badge count
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
        
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

        // Configure ChipGroup for single selection
        binding.chipGroupGenre.apply {
            isSingleSelection = true
            isSelectionRequired = true
        }

        // Add "Semua" chip first
        val allChip = com.google.android.material.chip.Chip(requireContext(), null, R.style.CustomChipStyle).apply {
            text = "Semua"
            isCheckable = true
            isClickable = true
            id = View.generateViewId() // Generate unique ID for proper selection handling
        }
        binding.chipGroupGenre.addView(allChip)

        // Add "Rekomendasi" chip with special styling
        val recommendationChip = com.google.android.material.chip.Chip(requireContext(), null, R.style.CustomChipStyle).apply {
            text = "⭐ Rekomendasi"
            isCheckable = true
            isClickable = true
            id = View.generateViewId()
            // Add special styling for recommendation chip
            chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.orange_500)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
        binding.chipGroupGenre.addView(recommendationChip)

        // Add genre chips
        for (genre in genres) {
            val chip = com.google.android.material.chip.Chip(requireContext(), null, R.style.CustomChipStyle).apply {
                text = genre
                isCheckable = true
                isClickable = true
                id = View.generateViewId() // Generate unique ID for proper selection handling
            }
            binding.chipGroupGenre.addView(chip)
        }

        // Set "Semua" as default selected
        binding.chipGroupGenre.check(allChip.id)

        // Set up selection listener
        setupChipGroupListener()
    }
    
    private fun setupChipGroupListener() {
        binding.chipGroupGenre.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChip = group.findViewById<com.google.android.material.chip.Chip>(checkedIds.first())
                val selectedGenre = selectedChip.text.toString()
                
                Log.d("HomeFragment", "Filter selected: $selectedGenre")
                
                // Update current filter
                currentFilter = selectedGenre
                
                // Clear search when filter is selected
                binding.etSearch.setText("")
                
                // Apply filter based on selection
                when (selectedGenre) {
                    "⭐ Rekomendasi" -> {
                        // Show recommendations view and hide regular books
                        showRecommendationsOnly()
                    }
                    else -> {
                        // Show regular books and hide recommendations view for specific filters
                        showRegularBooks()
                        homeViewModel.filterBooksByGenre(selectedGenre)
                    }
                }
            }
        }
    }
    
    private fun resetFilterToAll() {
        // Update current filter
        currentFilter = "Semua"
        
        // Show regular books view
        showRegularBooks()
        
        // Find the "Semua" chip and select it without triggering listener
        for (i in 0 until binding.chipGroupGenre.childCount) {
            val chip = binding.chipGroupGenre.getChildAt(i) as? com.google.android.material.chip.Chip
            if (chip?.text == "Semua") {
                // Temporarily remove listener to avoid recursive calls
                binding.chipGroupGenre.setOnCheckedStateChangeListener(null)
                binding.chipGroupGenre.check(chip.id)
                // Restore listener
                setupChipGroupListener()
                // Apply "Semua" filter to show all books
                homeViewModel.filterBooksByGenre("Semua")
                break
            }
        }
    }
    
    private fun setupSearchBar() {
        // Setup search functionality with debouncing
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().trim()
                
                // Cancel previous timer
                searchTimer?.cancel()
                
                // Start new timer with 300ms delay
                searchTimer = object : android.os.CountDownTimer(300, 300) {
                    override fun onTick(millisUntilFinished: Long) {}
                    
                    override fun onFinish() {
                        if (query.isNotEmpty()) {
                            // Reset filter to "Semua" when searching
                            resetFilterToAll()
                            homeViewModel.searchBooks(query)
                        } else {
                            // When search is cleared, apply current filter
                            homeViewModel.filterBooksByGenre(currentFilter)
                        }
                    }
                }.start()
            }
        })
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
        
        // Long press pada notification icon untuk create test notification
        binding.ivNotification.setOnLongClickListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                Log.d("HomeFragment", "Creating test notification...")
                notificationViewModel.createTestNotification(userId)
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "Test notification created!",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
            true
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        searchTimer?.cancel()
        searchTimer = null
        notificationPopup?.dismiss()
        notificationPopup = null
        
        // Unregister broadcast receiver
        try {
            requireContext().unregisterReceiver(notificationCountReceiver)
            Log.d("HomeFragment", "📡 Notification count receiver unregistered")
        } catch (e: Exception) {
            Log.e("HomeFragment", "❌ Error unregistering notification count receiver", e)
        }
        
        _binding = null
    }
    
    // =============== FILTER HELPER METHODS ===============
    
    private fun showRecommendationsOnly() {
        // Hide regular books RecyclerView
        binding.rvBooks.visibility = View.GONE
        
        // Show recommendations section
        binding.layoutRecommendations.visibility = View.VISIBLE
        binding.dividerRecommendations.visibility = View.VISIBLE
        
        // Reload recommendations if needed
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            homeViewModel.loadRecommendations(userId)
        }
        
        Log.d("HomeFragment", "🌟 Showing recommendations only")
    }
    
    private fun showRegularBooks() {
        // Show regular books RecyclerView
        binding.rvBooks.visibility = View.VISIBLE
        
        // Keep recommendations section visible but in normal mode
        val recommendations = homeViewModel.recommendedBooks.value
        if (recommendations.isNullOrEmpty()) {
            binding.layoutRecommendations.visibility = View.GONE
            binding.dividerRecommendations.visibility = View.GONE
        } else {
            binding.layoutRecommendations.visibility = View.VISIBLE
            binding.dividerRecommendations.visibility = View.VISIBLE
        }
        
        Log.d("HomeFragment", "📚 Showing regular books")
    }

}