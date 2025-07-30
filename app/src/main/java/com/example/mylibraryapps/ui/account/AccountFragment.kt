package com.example.mylibraryapps.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentAccountBinding
import com.example.mylibraryapps.ui.login.LoginActivity
import com.example.mylibraryapps.ui.test.NotificationTestActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.BuildConfig
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AccountFragment : Fragment() {
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserData()
        setupDebugFeatures()
    }

    private fun setupClickListeners() {
        binding.btnAccountSettings.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_accountSettingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }


        binding.btnTestNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationTestActivity::class.java))
        }

    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set email from Auth
            binding.tvEmail.text = currentUser.email

            // Get additional user data from Firestore
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("nama") ?: "Siswa"
                        val nis = document.getString("nis") ?: ""
                        val kelas = document.getString("kelas") ?: ""
                        val isAdmin = document.getBoolean("is_admin") ?: false


                        // Display user data with modern formatting
                        binding.tvUserName.text = name
                        binding.tvEmail.text = buildString {
                            append(currentUser.email ?: "")
                            if (nis.isNotEmpty()) append("\nNIS: $nis")
                            if (kelas.isNotEmpty()) append("\nKelas: $kelas")
                        }
                    } else {
                        binding.tvUserName.text =
                            currentUser.email?.substringBefore("@") ?: "Pengguna"
                    }
                }
                .addOnFailureListener {
                    binding.tvUserName.text = currentUser.email?.substringBefore("@") ?: "Pengguna"
                }
        }
    }

    private fun showLogoutConfirmationDialog() {
        val options = arrayOf(
            "Keluar (tetap ingat akun)",
            "Keluar dan hapus data login"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Keluar Akun")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Logout biasa
                        performLogout()
                    }

                    1 -> {
                        // Logout dan hapus data login
                        performLogoutAndClearData()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performLogout() {
        // Sign out dari Firebase
        auth.signOut()

        // Kembali ke LoginActivity
        navigateToLogin()
    }

    private fun performLogoutAndClearData() {
        // Sign out dari Firebase
        auth.signOut()

        // Hapus data cache aplikasi
        try {
            // Hapus shared preferences yang mungkin menyimpan data login
            val sharedPrefs = requireActivity().getSharedPreferences("com.example.mylibraryapps", 0)
            sharedPrefs.edit().clear().apply()

            // Hapus cache Firebase Authentication
            requireContext().getSharedPreferences("com.google.firebase.auth.api.Store", 0)
                .edit().clear().apply()

            // Hapus cache lainnya
            requireContext().cacheDir.deleteRecursively()
        } catch (e: Exception) {
            // Abaikan error
        }

        // Kembali ke LoginActivity
        navigateToLogin()
    }

    private fun setupDebugFeatures() {
        // Show debug features only in debug mode
        if (BuildConfig.DEBUG) {
            // Show test notification button in debug mode
            binding.btnTestNotification.visibility = View.VISIBLE
        } else {
            // Hide test notification button in release mode
            binding.btnTestNotification.visibility = View.VISIBLE
        }
    }

    private fun navigateToLogin() {
        // Clear semua activity dan kembali ke LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}