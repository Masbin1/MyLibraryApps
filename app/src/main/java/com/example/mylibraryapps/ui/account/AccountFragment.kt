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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
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
    }

    private fun setupClickListeners() {
        binding.btnAccountSettings.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_accountSettingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
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

                        // Display user data with modern formatting
                        binding.tvUserName.text = name
                        binding.tvEmail.text = buildString {
                            append(currentUser.email ?: "")
                            if (nis.isNotEmpty()) append("\nNIS: $nis")
                            if (kelas.isNotEmpty()) append("\nKelas: $kelas")
                        }
                    } else {
                        binding.tvUserName.text = currentUser.email?.substringBefore("@") ?: "Pengguna"
                    }
                }
                .addOnFailureListener {
                    binding.tvUserName.text = currentUser.email?.substringBefore("@") ?: "Pengguna"
                }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Keluar Akun")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                // Sign out dari Firebase
                auth.signOut()

                // Clear semua activity dan kembali ke LoginActivity
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}