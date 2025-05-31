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

class LoginActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtDaftar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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
        val email = edtEmail.text.toString()
        val password = edtPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Password harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Simulasi login sukses
        Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
        // Pindah ke MainActivity setelah login berhasil
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Menutup LoginActivity agar tidak bisa kembali
//        finish()
        // TODO: Setelah login sukses, pindah ke dashboard
    }
}

