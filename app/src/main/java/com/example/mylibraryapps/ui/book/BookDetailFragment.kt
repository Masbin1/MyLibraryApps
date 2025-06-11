package com.example.mylibraryapps.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentBookDetailBinding
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
            
            // Set up listeners
            binding.btnBack.setOnClickListener {
                requireActivity().onBackPressed()
            }

            binding.btnPinjam.setOnClickListener {
                borrowBook()
            }

            binding.btnEdit.setOnClickListener {
                openEditBookFragment()
            }
            
            // Initially hide the edit button, will show it only for admin users
            binding.btnEdit.visibility = View.GONE
            
            // Check if current user is admin
            checkIfUserIsAdmin()
            
            // Update UI with book data
            updateUIWithBookData()
            
            // Observe for book updates from EditBookFragment
            findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Book>("updated_book")?.observe(
                viewLifecycleOwner
            ) { updatedBook ->
                book = updatedBook
                updateUIWithBookData()
            }
        }
    }

    private fun updateUIWithBookData() {
        binding.tvBookTitle.text = book.title
        binding.tvAuthor.text = book.author
        binding.tvPublisher.text = book.publisher
        binding.tvPurchaseDate.text = book.getFormattedDate()
        binding.tvSpecifications.text = book.specifications
        binding.tvMaterial.text = book.material
        binding.tvQuantity.text = book.quantity.toString()
        binding.tvGenre.text = book.genre

        if (book.coverUrl.isNotEmpty()) {
            Glide.with(this)
                .load(book.coverUrl)
                .into(binding.ivCover)
        }
    }

    private fun openEditBookFragment() {
        val bundle = Bundle().apply {
            putParcelable("book", book)
        }
        findNavController().navigate(R.id.editBookFragment, bundle)
    }

    private fun borrowBook() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Anda harus login terlebih dahulu")
            return
        }

        // Ambil data user dari Firestore
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    showToast("Data pengguna tidak ditemukan")
                    return@addOnSuccessListener
                }

                val userData = document.data
                val userName = userData?.get("nama") as? String ?: currentUser.displayName ?: "Pengguna"
                val userClass = userData?.get("kelas") as? String ?: ""
                val userNis = userData?.get("nis") as? String ?: ""

                if (book.quantity <= 0) {
                    showToast("Buku tidak tersedia untuk dipinjam")
                    return@addOnSuccessListener
                }

                // Validasi peminjaman ganda
                db.collection("transactions")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("bookId", book.id)
                    .whereIn("status", listOf("menunggu konfirmasi pinjam", "dipinjam"))
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            showToast("Anda sudah meminjam buku ini dan belum mengembalikannya")
                            return@addOnSuccessListener
                        }

                        // Proses peminjaman
                        processBorrowTransaction(currentUser, userName, userClass, userNis)
                    }
                    .addOnFailureListener { e ->
                        showToast("Gagal memeriksa riwayat peminjaman: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showToast("Gagal mengambil data pengguna: ${e.message}")
            }
    }

    private fun processBorrowTransaction(
        currentUser: FirebaseUser,
        userName: String,
        userClass: String,
        userNis: String
    ) {
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
            nameUser = userName,
            userId = currentUser.uid,
            borrowDate = currentDate,
            returnDate = returnDate,
            status = "menunggu konfirmasi pinjam",
            remainingDays = 7,      // Tambahkan field NIS user
        )

        // Batch write untuk atomic operation
        val batch = db.batch()

        val transactionRef = db.collection("transactions").document()
        batch.set(transactionRef, transaction)

        val bookRef = db.collection("books").document(book.id)
        batch.update(bookRef, "quantity", book.quantity - 1)

        batch.commit()
            .addOnSuccessListener {
                showToast("Permintaan peminjaman berhasil diajukan. Menunggu konfirmasi admin.")
            }
            .addOnFailureListener { e ->
                showToast("Gagal mengajukan peminjaman: ${e.message}")
            }
    }

    private fun checkIfUserIsAdmin() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.btnEdit.visibility = View.GONE
            return
        }

        // Check if user is admin in Firestore
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isAdmin = document.getBoolean("is_admin") ?: false
                    binding.btnEdit.visibility = if (isAdmin) View.VISIBLE else View.GONE
                } else {
                    binding.btnEdit.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                binding.btnEdit.visibility = View.GONE
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