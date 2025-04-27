package com.example.mylibraryapps.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mylibraryapps.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var edtNama: EditText
    private lateinit var spinnerKelas: Spinner
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtKonfirmasiPassword: EditText
    private lateinit var btnBuatAkun: Button
    private lateinit var txtLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Binding semua komponen
        edtNama = findViewById(R.id.edtNama)
        spinnerKelas = findViewById(R.id.spinnerKelas)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtKonfirmasiPassword = findViewById(R.id.edtKonfirmasiPassword)
        btnBuatAkun = findViewById(R.id.btnBuatAkun)
        txtLogin = findViewById(R.id.txtLogin)

        // Klik daftar akun
        btnBuatAkun.setOnClickListener {
            daftarAkun()
        }

        // Klik teks "Sudah punya akun? Login"
        txtLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun daftarAkun() {
        val nama = edtNama.text.toString()
        val kelas = spinnerKelas.selectedItem.toString()
        val email = edtEmail.text.toString()
        val password = edtPassword.text.toString()
        val konfirmasiPassword = edtKonfirmasiPassword.text.toString()

        if (nama.isEmpty() || email.isEmpty() || password.isEmpty() || konfirmasiPassword.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != konfirmasiPassword) {
            Toast.makeText(this, "Password tidak cocok!", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()

        // Setelah daftar, bisa arahkan ke login
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

