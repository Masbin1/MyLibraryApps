package com.example.mylibraryapps.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mylibraryapps.MyLibraryApplication
import com.example.mylibraryapps.databinding.ActivitySplashBinding
import com.example.mylibraryapps.ui.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            supportActionBar?.hide()
            super.onCreate(savedInstanceState)
            binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Get repository from Application with error handling
            val repository = try {
                (application as MyLibraryApplication).repository
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error getting repository", e)
                // Proceed to login without preloading data
                proceedToLogin(2000)
                return
            }
            
            // Preload data in background
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d("SplashActivity", "Starting data preload...")
                    // Ensure data is preloaded
                    repository.preloadData()
                    Log.d("SplashActivity", "Data preload completed")
                    
                    // Delay for at least 1.5 seconds to show splash screen
                    withContext(Dispatchers.Main) {
                        proceedToLogin(1500)
                    }
                } catch (e: Exception) {
                    Log.e("SplashActivity", "Error during data preload", e)
                    // If there's an error, still proceed to login after 2 seconds
                    withContext(Dispatchers.Main) {
                        proceedToLogin(2000)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Critical error in onCreate", e)
            // Fallback: proceed to login immediately
            proceedToLogin(1000)
        }
    }
    
    private fun proceedToLogin(delayMs: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error starting LoginActivity", e)
                // If we can't start LoginActivity, there's a serious problem
                // Let the app crash so we can see the error
                throw e
            }
        }, delayMs)
    }
}
