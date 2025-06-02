package com.example.mylibraryapps.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mylibraryapps.databinding.FragmentBookDetailBinding
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var book: Book
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            book = it.getParcelable("book") ?: return@let

            binding.tvBookTitle.text = book.title
            binding.tvAuthor.text = book.author
            binding.tvPublisher.text = book.publisher
            binding.tvPurchaseDate.text = book.purchaseDate.toString()
            binding.tvSpecifications.text = book.specifications
            binding.tvMaterial.text = book.material
            binding.tvQuantity.text = book.quantity.toString()
            binding.tvGenre.text = book.genre

            if (book.coverUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(book.coverUrl)
                    .into(binding.ivCover)
            }

            binding.btnBack.setOnClickListener {
                requireActivity().onBackPressed()
            }

            binding.btnPinjam.setOnClickListener {
                borrowBook()
            }
        }
    }

    private fun borrowBook() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Anda harus login terlebih dahulu")
            return
        }

        if (book.quantity <= 0) {
            showToast("Buku tidak tersedia untuk dipinjam")
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val returnDate = dateFormat.format(calendar.time)

        val transaction = Transaction(
            bookId = book.id,
            title = book.title,
            author = book.author,
            publisher = book.publisher,
            genre = book.genre,
            coverUrl = book.coverUrl,
            userId = currentUser.uid,
            borrowDate = currentDate,
            returnDate = returnDate,
            status = "menunggu konfirmasi pinjam",
            remainingDays = 7
        )

        db.collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                db.collection("books").document(book.id)
                    .update("quantity", book.quantity - 1)
                    .addOnSuccessListener {
                        showToast("Permintaan peminjaman berhasil diajukan. Menunggu konfirmasi admin.")
                    }
                    .addOnFailureListener { e ->
                        showToast("Gagal memperbarui stok buku: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showToast("Gagal mengajukan peminjaman: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}