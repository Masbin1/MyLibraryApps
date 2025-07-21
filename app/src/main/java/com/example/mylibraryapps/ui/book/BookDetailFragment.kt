package com.example.mylibraryapps.ui.book

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mylibraryapps.MyLibraryApplication
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
        Log.d("BookDetailFragment", "Starting borrow process for book: ${book.title} (ID: ${book.id}, Quantity: ${book.quantity})")
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Anda harus login terlebih dahulu")
            return
        }

        // Check if book ID is valid
        if (book.id.isEmpty()) {
            Log.e("BookDetailFragment", "Book ID is empty")
            showToast("ID buku tidak valid")
            return
        }

        // Check if book is available
        if (book.quantity <= 0) {
            Log.w("BookDetailFragment", "Book quantity is ${book.quantity}, not available for borrowing")
            showToast("Buku tidak tersedia untuk dipinjam")
            return
        }

        // Ambil data user dari Firestore
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                // Get user data if available, otherwise use defaults
                val userData = document.data
                val userName = if (document.exists()) {
                    userData?.get("nama") as? String ?: currentUser.displayName ?: "Pengguna"
                } else {
                    currentUser.displayName ?: "Pengguna"
                }
                val userClass = if (document.exists()) {
                    userData?.get("kelas") as? String ?: ""
                } else {
                    ""
                }
                val userNis = if (document.exists()) {
                    userData?.get("nis") as? String ?: ""
                } else {
                    ""
                }

                // If user document doesn't exist, create it
                if (!document.exists()) {
                    val newUser = hashMapOf(
                        "nama" to (currentUser.displayName ?: "Pengguna"),
                        "email" to (currentUser.email ?: ""),
                        "kelas" to "",
                        "nis" to "",
                        "is_admin" to false
                    )
                    
                    db.collection("users").document(currentUser.uid)
                        .set(newUser)
                        .addOnSuccessListener {
                            Log.d("BookDetailFragment", "User document created successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("BookDetailFragment", "Error creating user document", e)
                        }
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
                        Log.e("BookDetailFragment", "Error checking existing transactions", e)
                    }
            }
            .addOnFailureListener { e ->
                showToast("Gagal mengambil data pengguna: ${e.message}")
                Log.e("BookDetailFragment", "Error fetching user data", e)
                
                // Even if we can't get user data, try to borrow with default values
                val userName = currentUser.displayName ?: "Pengguna"
                processBorrowTransaction(currentUser, userName, "", "")
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
            remainingDays = 7
        )

        // Create transaction with auto-generated ID
        val transactionRef = db.collection("transactions").document()
        val transactionWithId = transaction.copy(id = transactionRef.id)
        
        // Try to create the transaction first
        transactionRef.set(transactionWithId)
            .addOnSuccessListener {
                Log.d("BookDetailFragment", "Transaction created successfully with ID: ${transactionRef.id}")
                
                // Skip book quantity update for now - bypass
                Log.d("BookDetailFragment", "Skipping book quantity update (bypassed)")
                showToast("Permintaan peminjaman berhasil diajukan. Menunggu konfirmasi admin.")
            }
            .addOnFailureListener { e ->
                Log.e("BookDetailFragment", "Failed to create transaction", e)
                showToast("Gagal mengajukan peminjaman: ${e.message}")
            }
    }

    private fun checkIfUserIsAdmin() {
        // Default visibility: hide edit button, show borrow button
        binding.btnEdit.visibility = View.GONE
        binding.btnPinjam.visibility = View.VISIBLE
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If user is not logged in, keep default visibility
            Log.d("BookDetailFragment", "User not logged in, hiding edit button, showing borrow button")
            return
        }

        // Get repository from Application
        val repository = (requireActivity().application as MyLibraryApplication).repository
        
        // Observe user data from repository
        repository.userData.observe(viewLifecycleOwner) { user ->
            Log.d("BookDetailFragment", "User admin status: ${user?.is_admin}, User: $user")
            
            if (user != null) {
                val isAdmin = user.is_admin
                
                // If user is admin:
                // - Show edit button
                // - Hide borrow button
                if (isAdmin) {
                    binding.btnEdit.visibility = View.VISIBLE
                    binding.btnPinjam.visibility = View.GONE
                    Log.d("BookDetailFragment", "Admin user: showing edit button, hiding borrow button")
                } 
                // If user is not admin:
                // - Hide edit button
                // - Show borrow button
                else {
                    binding.btnEdit.visibility = View.GONE
                    binding.btnPinjam.visibility = View.VISIBLE
                    Log.d("BookDetailFragment", "Regular user: hiding edit button, showing borrow button")
                }
            } else {
                // If user data is null, use default visibility
                binding.btnEdit.visibility = View.GONE
                binding.btnPinjam.visibility = View.VISIBLE
                Log.d("BookDetailFragment", "User data null, hiding edit button, showing borrow button")
            }
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