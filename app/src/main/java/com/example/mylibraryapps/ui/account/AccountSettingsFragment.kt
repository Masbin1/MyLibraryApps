package com.example.mylibraryapps.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentAccountSettingsBinding
import com.example.mylibraryapps.model.User
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class AccountSettingsFragment : Fragment() {

    private var _binding: FragmentAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth
        currentUserId = auth.currentUser?.uid

        setupKelasDropdown()
        loadUserData()
        setupSaveButton()
    }

    private fun setupKelasDropdown() {
        val kelasOptions = listOf("SMP KELAS VII", "SMP KELAS VIII", "SMP KELAS IX", "GURU")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, kelasOptions)
        val dropdown: MaterialAutoCompleteTextView = binding.root.findViewById(R.id.autoCompleteTextView_class)
        dropdown.setAdapter(adapter)

        
        // Set dropdown behavior
        binding.autoCompleteTextViewClass.setOnClickListener {
            binding.autoCompleteTextViewClass.showDropDown()
        }
        
        // Handle item selection
        binding.autoCompleteTextViewClass.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = kelasOptions[position]
            binding.autoCompleteTextViewClass.setText(selectedItem, false)
        }
    }

    private fun loadUserData() {
        currentUserId?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        binding.editTextName.setText(user?.nama)
                        binding.editTextEmail.setText(user?.email)
                        user?.kelas?.let { kelas ->
                            binding.autoCompleteTextViewClass.setText(kelas, false)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupSaveButton() {
        binding.buttonSave.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val kelas = binding.autoCompleteTextViewClass.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || kelas.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = currentUserId ?: return@setOnClickListener

            // Update Firestore data
            val userUpdates = mapOf(
                "nama" to name,
                "email" to email,
                "kelas" to kelas
            )

            db.collection("users").document(userId).update(userUpdates)
                .addOnSuccessListener {
                    // Optionally update FirebaseAuth email
                    val currentUser = auth.currentUser
                    if (currentUser != null && currentUser.email != email) {
                        currentUser.updateEmail(email)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                                // Handle password update if needed, then navigate
                                handlePasswordUpdateAndNavigate(currentUser, password)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Email update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                // Still navigate back even if email update fails
                                handlePasswordUpdateAndNavigate(currentUser, password)
                            }
                    } else {
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                        // Handle password update if needed, then navigate
                        handlePasswordUpdateAndNavigate(currentUser, password)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update user", Toast.LENGTH_SHORT).show()
                    // Navigate back even if update fails
                    navigateBack()
                }
        }
    }

    private fun handlePasswordUpdateAndNavigate(currentUser: com.google.firebase.auth.FirebaseUser?, password: String) {
        if (password.isNotEmpty() && currentUser != null) {
            currentUser.updatePassword(password)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Password updated", Toast.LENGTH_SHORT).show()
                    navigateBack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Password update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    navigateBack()
                }
        } else {
            navigateBack()
        }
    }

    private fun navigateBack() {
        // Check if fragment is still attached and has a valid NavController
        if (isAdded && !isDetached && !isRemoving) {
            try {
                findNavController().popBackStack()
            } catch (e: IllegalStateException) {
                // Fallback: if NavController is not available, use activity's onBackPressed
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
