package com.example.mylibraryapps.ui.book

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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentEditBookBinding
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.data.BookRepository
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EditBookFragment : Fragment() {

    private var _binding: FragmentEditBookBinding? = null
    private val binding get() = _binding!!
    private lateinit var book: Book
    private val db = FirebaseFirestore.getInstance()
    private val bookRepository = BookRepository()
    private var selectedImageUri: Uri? = null
    private var hasImageChanged = false
    
    // Launcher untuk memilih gambar dari galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (validateImageFile(uri)) {
                    selectedImageUri = uri
                    hasImageChanged = true
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
                    Snackbar.make(binding.root, "Cover berhasil dipilih", Snackbar.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Format gambar tidak didukung", Toast.LENGTH_LONG).show()
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

    companion object {
        fun newInstance(book: Book): EditBookFragment {
            val fragment = EditBookFragment()
            val args = Bundle()
            args.putParcelable("book", book)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        book = arguments?.getParcelable("book") ?: return
        populateFormWithBookData()

        binding.btnSave.setOnClickListener {
            updateBook()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.etPurchaseDate.setOnClickListener {
            showDatePicker()
        }
        
        // Listener untuk cover image
        binding.cardCover.setOnClickListener {
            showImageOptionsDialog()
        }
    }

    private fun populateFormWithBookData() {
        binding.etTitle.setText(book.title)
        binding.etAuthor.setText(book.author)
        binding.etPublisher.setText(book.publisher)
        binding.etPurchaseDate.setText(book.getFormattedDate())
        binding.etSpecifications.setText(book.specifications)
        binding.etMaterial.setText(book.material)
        binding.etQuantity.setText(book.quantity.toString())
        binding.etGenre.setText(book.genre)
        
        // Load existing cover if available
        if (book.coverUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(book.coverUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_book_cover_placeholder)
                .error(R.drawable.ic_book_cover_placeholder)
                .into(binding.ivBookCover)
                
            binding.layoutAddCover.visibility = View.GONE
            binding.overlay.visibility = View.VISIBLE
            binding.ivEditIcon.visibility = View.VISIBLE
        } else {
            // Show add cover layout if no cover exists
            binding.layoutAddCover.visibility = View.VISIBLE
            binding.overlay.visibility = View.GONE
            binding.ivEditIcon.visibility = View.GONE
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = book.purchaseDate.toDate()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                binding.etPurchaseDate.setText("$dayOfMonth/${month + 1}/$year")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    
    private fun showImageOptionsDialog() {
        val hasExistingCover = book.coverUrl.isNotEmpty()
        val options = if (hasExistingCover) {
            arrayOf("Ganti Gambar", "Hapus Gambar", "Batal")
        } else {
            arrayOf("Tambah Gambar", "Batal")
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Pilih Aksi")
            .setItems(options) { _, which ->
                when {
                    !hasExistingCover && which == 0 -> checkPermissionAndOpenImagePicker() // Tambah gambar
                    hasExistingCover && which == 0 -> checkPermissionAndOpenImagePicker() // Ganti gambar
                    hasExistingCover && which == 1 -> removeSelectedImage() // Hapus gambar
                    // Batal - tidak melakukan apa-apa
                }
            }
            .show()
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
    
    private fun removeSelectedImage() {
        selectedImageUri = null
        hasImageChanged = true
        binding.ivBookCover.setImageResource(R.drawable.ic_book_cover_placeholder)
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

    private fun updateBook() {
        // Validate inputs
        if (binding.etTitle.text.isNullOrEmpty() ||
            binding.etAuthor.text.isNullOrEmpty() ||
            binding.etQuantity.text.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Judul, Penulis, dan Jumlah harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Parse date
            val dateParts = binding.etPurchaseDate.text.toString().split("/")
            val day = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val year = dateParts[2].toInt()

            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)
            val purchaseDate = Timestamp(calendar.time)

            // Create updated book (tanpa coverUrl dulu)
            val updatedBookBase = book.copy(
                title = binding.etTitle.text.toString(),
                author = binding.etAuthor.text.toString(),
                publisher = binding.etPublisher.text.toString(),
                purchaseDate = purchaseDate,
                specifications = binding.etSpecifications.text.toString(),
                material = binding.etMaterial.text.toString(),
                quantity = binding.etQuantity.text.toString().toLong(),
                genre = binding.etGenre.text.toString()
            )

            // Disable button to prevent multiple submissions
            binding.btnSave.isEnabled = false
            
            // Check if image has changed
            if (hasImageChanged) {
                if (selectedImageUri != null) {
                    // Update with new image
                    bookRepository.updateBookWithCover(
                        bookId = book.id,
                        imageUri = selectedImageUri!!,
                        updatedBook = updatedBookBase,
                        onSuccess = {
                            Toast.makeText(requireContext(), "Buku berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            
                            // Get updated book with new cover URL
                            val finalUpdatedBook = updatedBookBase.copy(coverUrl = "")
                            findNavController().previousBackStackEntry?.savedStateHandle?.set("updated_book", finalUpdatedBook)
                            findNavController().navigateUp()
                        },
                        onFailure = { e ->
                            binding.btnSave.isEnabled = true
                            Toast.makeText(requireContext(), "Gagal memperbarui buku: ${e.message}", Toast.LENGTH_SHORT).show()
                        },
                        onProgress = { progress ->
                            // Optional: show progress
                        }
                    )
                } else {
                    // Remove image (set empty coverUrl)
                    val updatedBook = updatedBookBase.copy(coverUrl = "")
                    bookRepository.updateBook(
                        bookId = book.id,
                        updatedBook = updatedBook,
                        onSuccess = {
                            Toast.makeText(requireContext(), "Buku berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            findNavController().previousBackStackEntry?.savedStateHandle?.set("updated_book", updatedBook)
                            findNavController().navigateUp()
                        },
                        onFailure = { e ->
                            binding.btnSave.isEnabled = true
                            Toast.makeText(requireContext(), "Gagal memperbarui buku: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                // No image change, keep existing coverUrl
                val updatedBook = updatedBookBase.copy(coverUrl = book.coverUrl)
                bookRepository.updateBook(
                    bookId = book.id,
                    updatedBook = updatedBook,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Buku berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        findNavController().previousBackStackEntry?.savedStateHandle?.set("updated_book", updatedBook)
                        findNavController().navigateUp()
                    },
                    onFailure = { e ->
                        binding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), "Gagal memperbarui buku: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } catch (e: Exception) {
            binding.btnSave.isEnabled = true
            Toast.makeText(requireContext(), "Format tanggal salah: gunakan DD/MM/YYYY", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}