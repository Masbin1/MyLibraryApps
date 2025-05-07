package com.example.mylibraryapps.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

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

        // Set click listeners
        binding.btnAccountSettings.setOnClickListener {
            // Navigate to account settings
            findNavController().navigate(R.id.action_accountFragment_to_accountSettingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            // Handle logout logic
            showLogoutConfirmationDialog()
        }

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        // Example data - replace with actual user data
        binding.tvUserName.text = "John Doe"
        binding.tvEmail.text = "john.doe@example.com"
    }

    private fun showLogoutConfirmationDialog() {
        // Implement logout confirmation dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}