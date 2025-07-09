package com.example.mylibraryapps.ui.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
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
    private var selectedImageUri: Uri? = null
    
    // Launcher untuk memilih gambar dari galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (validateImageFile(uri)) {
                    selectedImageUri = uri
                    // Tampilkan gambar yang dipilih
                    Glide.with(requireContext())
                        .load(uri)
                        .centerCrop()
                        .into(binding.ivBookCover)
                    
                    // Sembunyikan layout tambah foto dan tampilkan overlay
                    binding.layoutAddCover.visibility = View.GONE
                    binding.overlay.visibility = View.VISIBLE
                    binding.ivEditIcon.visibility = View.VISIBLE
                    
                    // Tampilkan snackbar sukses
                    Snackbar.make(binding.root, getString(R.string.cover_berhasil_dipilih), Snackbar.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.format_gambar_tidak_didukung), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    // Launcher untuk request permission
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permission diperlukan untuk mengakses galeri", Toast.LENGTH_SHORT).show()
        }
    }

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
        
        // Tambahkan listener untuk memilih gambar
        binding.cardCover.setOnClickListener {
            if (selectedImageUri != null) {
                // Jika sudah ada gambar, beri pilihan untuk mengganti atau menghapus
                showImageOptionsDialog()
            } else {
                checkPermissionAndOpenImagePicker()
            }
        }
        
        // Test upload button (untuk debugging)
        binding.cardCover.setOnLongClickListener {
            if (selectedImageUri != null) {
                Toast.makeText(requireContext(), "Testing upload...", Toast.LENGTH_SHORT).show()
                viewModel.testSimpleUpload(selectedImageUri!!)
            } else {
                Toast.makeText(requireContext(), "Pilih gambar dulu", Toast.LENGTH_SHORT).show()
            }
            true
        }
        
        // Check storage status dengan back button long press
        binding.btnBack.setOnLongClickListener {
            Toast.makeText(requireContext(), "Checking storage status...", Toast.LENGTH_SHORT).show()
            viewModel.checkStorageStatus()
            true
        }
    }
    
    private fun checkPermissionAndOpenImagePicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Tampilkan dialog penjelasan mengapa permission diperlukan
                Snackbar.make(binding.root, "Permission diperlukan untuk mengakses galeri", Snackbar.LENGTH_LONG)
                    .setAction("OK") {
                        requestPermissionLauncher.launch(permission)
                    }
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }
    
    private fun showImageOptionsDialog() {
        val options = arrayOf("Ganti Gambar", "Hapus Gambar", "Batal")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Aksi")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkPermissionAndOpenImagePicker() // Ganti gambar
                    1 -> removeSelectedImage() // Hapus gambar
                    2 -> {} // Batal - tidak melakukan apa-apa
                }
            }
            .show()
    }
    
    private fun removeSelectedImage() {
        selectedImageUri = null
        binding.ivBookCover.setImageDrawable(null)
        binding.layoutAddCover.visibility = View.VISIBLE
        binding.overlay.visibility = View.GONE
        binding.ivEditIcon.visibility = View.GONE
        Snackbar.make(binding.root, "Cover buku dihapus", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun validateImageFile(uri: Uri): Boolean {
        return try {
            val contentResolver = requireContext().contentResolver
            val mimeType = contentResolver.getType(uri)
            
            // Validasi MIME type
            val validMimeTypes = listOf(
                "image/jpeg",
                "image/jpg", 
                "image/png",
                "image/webp"
            )
            
            if (mimeType !in validMimeTypes) {
                return false
            }
            
            // Validasi ukuran file (maksimal 5MB)
            val inputStream = contentResolver.openInputStream(uri)
            val fileSize = inputStream?.available() ?: 0
            inputStream?.close()
            
            if (fileSize > 5 * 1024 * 1024) { // 5MB
                Toast.makeText(requireContext(), "Ukuran gambar terlalu besar (maksimal 5MB)", Toast.LENGTH_LONG).show()
                return false
            }
            
            true
        } catch (e: Exception) {
            false
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
        val year = binding.etYear.text.toString().trim()

        if (validateInput(title, author, genre, publisher, material, purchaseDate, specifications)) {
            // Tampilkan loading
            binding.progressBar.visibility = View.VISIBLE
            binding.btnAddBook.isEnabled = false
            
            if (selectedImageUri != null) {
                // Jika ada gambar yang dipilih, upload gambar terlebih dahulu
                viewModel.uploadBookCoverAndAddBook(
                    imageUri = selectedImageUri!!,
                    title = title,
                    author = author,
                    publisher = publisher,
                    genre = genre,
                    description = specifications, 
                    material = material,
                    quantity = quantity,
                    purchaseDate = purchaseDate,
                    year = year,
                    specifications = specifications
                )
            } else {
                // Jika tidak ada gambar, tambahkan buku tanpa gambar
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
                    specifications = specifications
                )
            }
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
                binding.progressBar.visibility = View.GONE
                binding.btnAddBook.isEnabled = true
                Snackbar.make(binding.root, "Buku berhasil ditambahkan", Snackbar.LENGTH_LONG).show()
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                binding.progressBar.visibility = View.GONE
                binding.btnAddBook.isEnabled = true
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnAddBook.isEnabled = !isLoading
        }
        
        // Observer untuk upload progress
        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            // Bisa ditambahkan progress bar untuk upload progress jika diperlukan
            // Saat ini hanya log untuk debug
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}