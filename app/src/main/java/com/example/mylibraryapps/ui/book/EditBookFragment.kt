package com.example.mylibraryapps.ui.book

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mylibraryapps.databinding.FragmentEditBookBinding
import com.example.mylibraryapps.model.Book
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EditBookFragment : Fragment() {

    private var _binding: FragmentEditBookBinding? = null
    private val binding get() = _binding!!
    private lateinit var book: Book
    private val db = FirebaseFirestore.getInstance()

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

            // Create updated book
            val updatedBook = book.copy(
                title = binding.etTitle.text.toString(),
                author = binding.etAuthor.text.toString(),
                publisher = binding.etPublisher.text.toString(),
                purchaseDate = purchaseDate,
                specifications = binding.etSpecifications.text.toString(),
                material = binding.etMaterial.text.toString(),
                quantity = binding.etQuantity.text.toString().toLong(),
                genre = binding.etGenre.text.toString()
            )

            // Update to Firestore
            db.collection("books").document(book.id)
                .set(updatedBook)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Buku berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    
                    // Pass updated book back to BookDetailFragment
                    val bundle = Bundle().apply {
                        putParcelable("book", updatedBook)
                    }
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("updated_book", updatedBook)
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Gagal memperbarui buku: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Format tanggal salah: gunakan DD/MM/YYYY", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}