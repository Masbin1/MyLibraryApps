package com.example.mylibraryapps

import android.content.Intent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var repository: AppRepository
    
    // Flag to prevent rapid navigation
    private var isNavigating = false

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

            // Initialize Firebase Auth
            auth = Firebase.auth

            // Check if user is logged in
            if (auth.currentUser == null) {
                redirectToLogin()
                return
            }

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupNavigation()
            setupDebugFeatures()
            
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

    override fun onStart() {
        super.onStart()
        // Double check auth state when activity resumes
        if (auth.currentUser == null) {
            redirectToLogin()
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
}