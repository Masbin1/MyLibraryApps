package com.example.mylibraryapps.ui.login

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mylibraryapps.MainActivity
import com.example.mylibraryapps.R
import com.example.mylibraryapps.ui.register.RegisterActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class LoginActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtDaftar: TextView

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        // Cek apakah user sudah login dan apakah ini adalah instalasi baru
        val isFirstRun = isFirstInstallation()
        
        if (!isFirstRun && auth.currentUser != null) {
            // User sudah login dan bukan instalasi pertama, langsung ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()
        // Binding semua komponen
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtDaftar = findViewById(R.id.txtDaftar)

        // Klik login
        btnLogin.setOnClickListener {
            loginAkun()
        }

        // Klik teks "Belum punya akun? Daftar"
        txtDaftar.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginAkun() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Password harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Authenticate with Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login success, check if email is verified
                    val user = auth.currentUser
                    if (user != null) {
//                        hilangin falsenya jika kamu mau buat login yang email verify
                        if (user.isEmailVerified == false) {
                            // Email verified, get user data from Firestore
                            getUserDataFromFirestore(user.uid)
                        } else {
                            Toast.makeText(
                                this,
                                "Silakan verifikasi email terlebih dahulu!",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Optionally resend verification email
                            // user.sendEmailVerification()
                        }
                    }
                } else {
                    // Login failed
                    Toast.makeText(
                        this,
                        "Login gagal: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun getUserDataFromFirestore(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // User data found, update FCM token and proceed to main activity
                    updateFCMToken(userId)
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Close login activity
                } else {
                    // User data not found in Firestore (shouldn't happen if registration was complete)
                    Toast.makeText(
                        this,
                        "Data pengguna tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                    auth.signOut() // Optional: sign out since data is incomplete
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal mengambil data pengguna: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    
    private fun updateFCMToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("LoginActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("LoginActivity", "FCM Token: $token")

            // Update user document with FCM token
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("LoginActivity", "FCM token updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("LoginActivity", "Failed to update FCM token", e)
                }
        }
    }
    
    /**
     * Checks if this is the first time the app is being installed/run.
     * Uses SharedPreferences to store and retrieve this information.
     * @return true if this is the first run, false otherwise
     */
    private fun isFirstInstallation(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        
        if (isFirstRun) {
            // If this is the first run, update the preference for future runs
            prefs.edit().putBoolean("is_first_run", false).apply()
            return true
        }
        
        return false
    }
}