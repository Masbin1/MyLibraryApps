package com.example.mylibraryapps

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mylibraryapps.databinding.ActivityMainBinding
import com.example.mylibraryapps.ui.login.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

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
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

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

        // Custom navigation handling
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id != R.id.navigation_home) {
                        navController.navigate(R.id.navigation_home)
                    }
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
                            navController.navigate(R.id.navigation_list_transaction)
                            true
                        }
                    }
                }
                R.id.navigation_account -> {
                    if (navController.currentDestination?.id != R.id.navigation_account) {
                        navController.navigate(R.id.navigation_account)
                    }
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
}