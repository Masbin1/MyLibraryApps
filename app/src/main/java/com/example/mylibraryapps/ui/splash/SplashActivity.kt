package com.example.mylibraryapps.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get repository from Application
        val repository = (application as MyLibraryApplication).repository
        
        // Preload data in background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Ensure data is preloaded
                repository.preloadData()
                
                // Delay for at least 1.5 seconds to show splash screen
                withContext(Dispatchers.Main) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                    }, 1500)
                }
            } catch (e: Exception) {
                // If there's an error, still proceed to login after 2 seconds
                withContext(Dispatchers.Main) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                    }, 2000)
                }
            }
        }
    }
}
