package com.example.mylibraryapps.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentAddBookBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddBookFragment : Fragment() {
    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddBookViewModel by viewModels()
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGenreDropdown()
        setupDatePicker()
        setupListeners()
        setupObservers()
    }

    private fun setupGenreDropdown() {
        val genres = resources.getStringArray(R.array.book_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genres)
        binding.actvGenre.setAdapter(adapter)
        binding.actvGenre.setOnClickListener { binding.actvGenre.showDropDown() }
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            updateDateInView()
        }

        binding.etPurchaseDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateInView() {
        val myFormat = "dd MMMM yyyy 'at' HH:mm:ss z"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding.etPurchaseDate.setText(sdf.format(calendar.time))
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAddBook.setOnClickListener {
            validateAndAddBook()
        }
    }

    private fun validateAndAddBook() {
        val title = binding.etTitle.text.toString().trim()
        val author = binding.etAuthor.text.toString().trim()
        val genre = binding.actvGenre.text.toString().trim()
        val publisher = binding.etPublisher.text.toString().trim()
        val material = binding.etMaterial.text.toString().trim()
        val purchaseDate = binding.etPurchaseDate.text.toString().trim()
        val quantity = binding.etQuantity.text.toString().toIntOrNull() ?: 0
        val specifications = binding.etSpecifications.text.toString().trim()
        val year = "" // Adding the missing year parameter with an empty default value

        if (validateInput(title, author, genre, publisher, material, purchaseDate, specifications)) {
            viewModel.addBook(
                title = title,
                author = author,
                publisher = publisher,
                genre = genre,
                description = specifications, 
                material = material,
                quantity = quantity,
                purchaseDate = purchaseDate,
                year = year,
                specifications = specifications, 
            )
        }
    }

    private fun validateInput(
        title: String,
        author: String,
        genre: String,
        publisher: String,
        material: String,
        purchaseDate: String,
        specifications: String,

    ): Boolean {
        var isValid = true

        if (title.isEmpty()) {
            binding.etTitle.error = "Judul buku harus diisi"
            isValid = false
        }
        if (author.isEmpty()) {
            binding.etAuthor.error = "Penulis harus diisi"
            isValid = false
        }
        if (genre.isEmpty()) {
            binding.actvGenre.error = "Jenis buku harus dipilih"
            isValid = false
        }
        if (publisher.isEmpty()) {
            binding.etPublisher.error = "Penerbit harus diisi"
            isValid = false
        }
        if (material.isEmpty()) {
            binding.etMaterial.error = "Material harus diisi"
            isValid = false
        }
        if (purchaseDate.isEmpty()) {
            binding.etPurchaseDate.error = "Tanggal pembelian harus diisi"
            isValid = false
        }
        if (specifications.isEmpty()) {
            binding.etSpecifications.error = "Spesifikasi harus diisi"
            isValid = false
        }

        return isValid
    }

    private fun setupObservers() {
        viewModel.addBookSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "Buku berhasil ditambahkan", Snackbar.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}