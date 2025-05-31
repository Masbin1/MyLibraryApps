package com.example.mylibraryapps.ui.login

import android.content.Intent
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

        // Cek apakah user sudah login
        auth.currentUser?.let {
            // User sudah login, langsung ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
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
                        if (user.isEmailVerified) {
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
                    // User data found, proceed to main activity
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
}