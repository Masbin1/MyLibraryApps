// model/User.kt
package com.example.mylibraryapps.model

data class User(
    val nama: String = "",
    val nis: String = "",
    val email: String = "",
    val kelas: String = "",
    val is_admin: Boolean = false,
)
