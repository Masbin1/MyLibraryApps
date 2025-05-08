package com.example.mylibraryapps
//testing
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mylibraryapps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_list_book, R.id.navigation_account
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Custom handling for bottom navigation to ensure proper navigation
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (navController.currentDestination?.id != R.id.navigation_home) {
                        navController.navigate(R.id.navigation_home)
                    }
                    true
                }
                R.id.navigation_list_book -> {
                    // If we're in bookDetailFragment, navigate back to list
                    if (navController.currentDestination?.id == R.id.bookDetailFragment) {
                        navController.navigate(R.id.navigation_list_book)
                        true
                    } else if (navController.currentDestination?.id != R.id.navigation_list_book) {
                        navController.navigate(R.id.navigation_list_book)
                        true
                    } else {
                        true
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
        
        // Set the initially selected item
        navView.selectedItemId = R.id.navigation_home
    }
}
