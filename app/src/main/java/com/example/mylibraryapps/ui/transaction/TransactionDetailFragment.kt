package com.example.mylibraryapps.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mylibraryapps.databinding.FragmentTransactionDetailBinding
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var transaction: Transaction

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Admin status management
    private var adminStatus: Boolean = false
    private var adminCheckCompleted: Boolean = false
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { bundle ->
            transaction = bundle.getParcelable("transaction") ?: run {
                showErrorAndClose("Transaksi tidak valid")
                return@let
            }

            if (transaction.id.isEmpty()) {
                showErrorAndClose("ID transaksi tidak valid")
                return@let
            }

            checkAdminStatus() // First check admin status
            setupInitialViews() // Setup views that don't need admin status
        } ?: run {
            showErrorAndClose("Data transaksi tidak ditemukan")
        }
    }

    private fun checkAdminStatus() {
        uiScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val document = db.collection("users")
                    .document(userId)
                    .get()
                    .await()

                adminStatus = document.getBoolean("is_admin") ?: false
            } catch (e: Exception) {
                adminStatus = false
                showToast("Gagal memeriksa status admin")
            } finally {
                adminCheckCompleted = true
                setupAdminDependentViews() // Setup views that need admin status
            }
        }
    }

    private fun setupInitialViews() {
        // Views that don't need admin status
        binding.tvBookTitle.text = transaction.title.ifEmpty { "Judul Tidak Tersedia" }
        binding.tvUserName.text = transaction.nameUser.ifEmpty { "Judul Tidak Tersedia" }
        binding.tvAuthor.text = transaction.author.ifEmpty { "Penulis Tidak Diketahui" }
        binding.tvBorrowDate.text = formatDate(transaction.borrowDate)
        binding.tvReturnDate.text = formatDate(transaction.returnDate)
        binding.tvStatus.text = getStatusText(transaction.status)
        binding.tvGenre.text = transaction.genre.takeIf { !it.isNullOrEmpty() } ?: "Genre Tidak Tersedia"
        binding.tvPublisher.text = transaction.publisher.takeIf { !it.isNullOrEmpty() } ?: "Penerbit Tidak Tersedia"
    }

    private fun setupAdminDependentViews() {
        if (!adminCheckCompleted) return

        // Only setup views that depend on admin status
        if (adminStatus) {
            when (transaction.status) {
                "menunggu konfirmasi pinjam" -> {
                    binding.btnConfirm.visibility = View.VISIBLE
                    binding.btnConfirm.text = "Konfirmasi Peminjaman"
                    binding.btnConfirm.setOnClickListener { confirmBorrow() }
                }
                "menunggu konfirmasi pengembalian" -> {
                    binding.btnConfirm.visibility = View.VISIBLE
                    binding.btnConfirm.text = "Konfirmasi Pengembalian"
                    binding.btnConfirm.setOnClickListener { confirmReturn() }
                }
                else -> {
                    binding.btnConfirm.visibility = View.GONE
                }
            }
        } else {
            binding.btnConfirm.visibility = View.GONE
        }
    }

    private fun confirmBorrow() {
        if (transaction.id.isEmpty()) {
            showToast("ID transaksi tidak valid")
            return
        }

        uiScope.launch {
            try {
                db.collection("transactions").document(transaction.id)
                    .update("status", "sedang dipinjam")
                    .await()

                // Update book quantity if needed
                if (transaction.bookId.isNotEmpty()) {
                    val bookDoc = db.collection("books").document(transaction.bookId).get().await()
                    val currentQuantity = bookDoc.getLong("quantity")?.toInt() ?: 0
                    if (currentQuantity > 0) {
                        db.collection("books").document(transaction.bookId)
                            .update("quantity", currentQuantity - 1)
                            .await()
                    }
                }

                showToast("Peminjaman berhasil dikonfirmasi")
                requireActivity().onBackPressed()
            } catch (e: Exception) {
                showToast("Gagal mengkonfirmasi: ${e.message}")
            }
        }
    }

    private fun confirmReturn() {
        if (transaction.id.isEmpty()) {
            showToast("ID transaksi tidak valid")
            return
        }

        uiScope.launch {
            try {
                // Update transaction status first
                db.collection("transactions").document(transaction.id)
                    .update("status", "sudah dikembalikan")
                    .await()

                // Then update book quantity
                if (transaction.bookId.isNotEmpty()) {
                    val bookDoc = db.collection("books").document(transaction.bookId).get().await()
                    val currentQuantity = bookDoc.getLong("quantity")?.toInt() ?: 0
                    db.collection("books").document(transaction.bookId)
                        .update("quantity", currentQuantity + 1)
                        .await()
                }

                showToast("Pengembalian berhasil dikonfirmasi")
                requireActivity().onBackPressed()
            } catch (e: Exception) {
                showToast("Gagal mengkonfirmasi: ${e.message}")
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "menunggu konfirmasi pinjam" -> "Menunggu Konfirmasi Peminjaman"
            "sedang dipinjam" -> "Sedang Dipinjam"
            "menunggu konfirmasi pengembalian" -> "Menunggu Konfirmasi Pengembalian"
            "sudah dikembalikan" -> "Sudah Dikembalikan"
            else -> status
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorAndClose(message: String) {
        showToast(message)
        requireActivity().onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModelJob.cancel()
        _binding = null
    }
}