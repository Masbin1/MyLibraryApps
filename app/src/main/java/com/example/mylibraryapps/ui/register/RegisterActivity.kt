package com.example.mylibraryapps.ui.register

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mylibraryapps.R
import com.example.mylibraryapps.model.User
import com.example.mylibraryapps.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var edtNama: EditText
    private lateinit var edtNis: EditText
    private lateinit var spinnerKelas: Spinner
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtKonfirmasiPassword: EditText
    private lateinit var btnBuatAkun: Button
    private lateinit var txtLogin: TextView

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        edtNama = findViewById(R.id.edtNama)
        edtNis = findViewById(R.id.edtNis)
        spinnerKelas = findViewById(R.id.spinnerKelas)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtKonfirmasiPassword = findViewById(R.id.edtKonfirmasiPassword)
        btnBuatAkun = findViewById(R.id.btnBuatAkun)
        txtLogin = findViewById(R.id.txtLogin)

        // Setup spinner kelas
        val kelasList = listOf("SMP KELAS 1", "SMP KELAS 2","SMP KELAS 3",)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kelasList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKelas.adapter = adapter

        database = FirebaseDatabase.getInstance().reference

        txtLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnBuatAkun.setOnClickListener {
            val nama = edtNama.text.toString().trim()
            val nis = edtNis.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val konfirmasiPassword = edtKonfirmasiPassword.text.toString().trim()
            val kelas = spinnerKelas.selectedItem.toString()

            if (nama.isEmpty() || nis.isEmpty() || email.isEmpty() || password.isEmpty() || konfirmasiPassword.isEmpty()) {
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != konfirmasiPassword) {
                Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = User(nama, nis, email, password, kelas)
            database.child("users").child(nis).setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan data: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

