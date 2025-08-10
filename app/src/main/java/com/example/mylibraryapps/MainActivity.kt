package com.example.mylibraryapps

import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mylibraryapps.data.AppRepository
import com.example.mylibraryapps.databinding.ActivityMainBinding
import com.example.mylibraryapps.ui.login.LoginActivity
import com.example.mylibraryapps.ui.test.NotificationTestActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.BuildConfig
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var repository: AppRepository
    private lateinit var db: FirebaseFirestore
    
    // Flag to prevent rapid navigation
    private var isNavigating = false
    
    // BroadcastReceiver for notification updates
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.mylibraryapps.NOTIFICATION_RECEIVED") {
                Log.d(TAG, "📡 Notification update broadcast received")
                checkUnreadNotifications()
            }
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Show action bar for menu access (only in debug mode)
            if (BuildConfig.DEBUG) {
                supportActionBar?.show()
            } else {
                supportActionBar?.hide()
            }

            // Get repository from Application
            repository = (application as MyLibraryApplication).repository

            // Initialize Firebase Auth and Firestore
            auth = Firebase.auth
            db = FirebaseFirestore.getInstance()

            // Check if user is logged in
            if (auth.currentUser == null) {
                redirectToLogin()
                return
            }

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupNavigation()
            setupDebugFeatures()
            
            // Update FCM token for current user
            updateFCMToken()
            
            // Check for unread notifications
            checkUnreadNotifications()
            
            // Observe repository error messages
            repository.errorMessage.observe(this) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    repository.clearErrorMessage()
                }
            }
        } catch (e: Exception) {
            // Log error and redirect to login if something goes wrong
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            Toast.makeText(this, "Terjadi kesalahan saat memuat aplikasi", Toast.LENGTH_LONG).show()
            redirectToLogin()
        }
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        navController.addOnDestinationChangedListener(this)

        // Configure top level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_list_transaction,
                R.id.navigation_account
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Custom navigation handling with debounce
        navView.setOnItemSelectedListener { item ->
            if (isNavigating) {
                return@setOnItemSelectedListener true
            }
            
            when (item.itemId) {
                R.id.navigation_home -> {
                    safeNavigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_list_transaction -> {
                    when (navController.currentDestination?.id) {
                        R.id.transactionDetailFragment -> {
                            navController.popBackStack(R.id.navigation_list_transaction, false)
                            true
                        }
                        R.id.navigation_list_transaction -> true
                        else -> {
                            safeNavigate(R.id.navigation_list_transaction)
                            true
                        }
                    }
                }
                R.id.navigation_account -> {
                    safeNavigate(R.id.navigation_account)
                    true
                }
                else -> false
            }
        }

        // Set initial selection
        navView.selectedItemId = R.id.navigation_home

        // Add auth state listener
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                redirectToLogin()
            }
        }
    }
    
    private fun setupDebugFeatures() {
        // Show FAB only in debug mode
        if (BuildConfig.DEBUG) {
            binding.fabTestNotification.visibility = android.view.View.VISIBLE
            binding.fabTestNotification.setOnClickListener {
                startActivity(Intent(this, NotificationTestActivity::class.java))
            }
        }
    }
    
    private fun safeNavigate(destinationId: Int) {
        if (navController.currentDestination?.id != destinationId && !isNavigating) {
            isNavigating = true
            
            try {
                navController.navigate(destinationId)
            } catch (e: Exception) {
                Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            
            // Reset navigation flag after delay
            lifecycleScope.launch {
                delay(300) // Prevent rapid navigation
                isNavigating = false
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    private fun updateFCMToken() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                Log.d("MainActivity", "FCM Token: $token")

                // Update user document with FCM token
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(currentUser.uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("MainActivity", "FCM token updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Failed to update FCM token", e)
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Double check auth state when activity resumes
        if (auth.currentUser == null) {
            redirectToLogin()
        } else {
            // Refresh notification count when app comes to foreground
            checkUnreadNotifications()
        }
        
        // Register broadcast receiver for notification updates
        val filter = IntentFilter("com.example.mylibraryapps.NOTIFICATION_RECEIVED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationReceiver, filter)
        }
        Log.d(TAG, "📡 Notification broadcast receiver registered")
    }
    
    override fun onStop() {
        super.onStop()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(notificationReceiver)
            Log.d(TAG, "📡 Notification broadcast receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unregistering broadcast receiver", e)
        }
    }
    
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // Reset navigation flag when destination changes
        isNavigating = false
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Add test menu only in debug mode
        if (BuildConfig.DEBUG) {
            menuInflater.inflate(R.menu.main_menu, menu)
        }
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_test_notifications -> {
                startActivity(Intent(this, NotificationTestActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Check for unread notifications and update UI badge
     */
    private fun checkUnreadNotifications() {
        val currentUser = auth.currentUser ?: return
        
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                
                val unreadCount = snapshot.documents.size
                Log.d(TAG, "📊 Unread notifications: $unreadCount")
                
                // Send notification count to HomeFragment via broadcast
                sendNotificationCountBroadcast(unreadCount)
                
                if (unreadCount > 0) {
                    Log.d(TAG, "🔔 You have $unreadCount unread notifications")
                    // You can show a toast or update UI here
                    // Toast.makeText(this@MainActivity, "You have $unreadCount unread notifications", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking unread notifications", e)
            }
        }
    }
    
    /**
     * Send notification count to HomeFragment via broadcast
     */
    private fun sendNotificationCountBroadcast(count: Int) {
        try {
            val intent = Intent("com.example.mylibraryapps.NOTIFICATION_COUNT_UPDATE")
            intent.putExtra("unread_count", count)
            sendBroadcast(intent)
            Log.d(TAG, "📡 Notification count broadcast sent: $count")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification count broadcast", e)
        }
    }
}