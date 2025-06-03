package com.example.mylibraryapps.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mylibraryapps.databinding.FragmentTransactionDetailBinding
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionDetailFragment : Fragment() {

    private var _binding: FragmentTransactionDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var transaction: Transaction
    private val db = FirebaseFirestore.getInstance()

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

            setupViews()
        } ?: run {
            showErrorAndClose("Data transaksi tidak ditemukan")
        }
    }

    private fun setupViews() {
        binding.tvBookTitle.text = transaction.title.ifEmpty { "Judul Tidak Tersedia" }
        binding.tvAuthor.text = transaction.author.ifEmpty { "Penulis Tidak Diketahui" }
        binding.tvBorrowDate.text = formatDate(transaction.borrowDate)
        binding.tvReturnDate.text = formatDate(transaction.returnDate)
        binding.tvStatus.text = getStatusText(transaction.status)

        // Additional fields if needed
        binding.tvGenre.text = transaction.genre.takeIf { !it.isNullOrEmpty() } ?: "Genre Tidak Tersedia"
        binding.tvPublisher.text = transaction.publisher.takeIf { !it.isNullOrEmpty() } ?: "Penerbit Tidak Tersedia"

        if (isAdmin()) {
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

    private fun confirmBorrow() {
        if (transaction.id.isEmpty()) {
            showToast("ID transaksi tidak valid")
            return
        }

        db.collection("transactions").document(transaction.id)
            .update("status", "sedang dipinjam")
            .addOnSuccessListener {
                showToast("Peminjaman berhasil dikonfirmasi")
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                showToast("Gagal mengkonfirmasi: ${e.message}")
            }
    }

    private fun confirmReturn() {
        if (transaction.id.isEmpty()) {
            showToast("ID transaksi tidak valid")
            return
        }

        db.collection("transactions").document(transaction.id)
            .update("status", "sudah dikembalikan")
            .addOnSuccessListener {
                if (transaction.bookId.isNotEmpty()) {
                    db.collection("books").document(transaction.bookId)
                        .get()
                        .addOnSuccessListener { document ->
                            val currentQuantity = document.getLong("quantity")?.toInt() ?: 0
                            db.collection("books").document(transaction.bookId)
                                .update("quantity", currentQuantity + 1)
                                .addOnSuccessListener {
                                    showToast("Pengembalian berhasil dikonfirmasi")
                                    requireActivity().onBackPressed()
                                }
                                .addOnFailureListener { e ->
                                    showToast("Berhasil mengkonfirmasi tapi gagal update stok: ${e.message}")
                                }
                        }
                } else {
                    showToast("Pengembalian berhasil dikonfirmasi")
                    requireActivity().onBackPressed()
                }
            }
            .addOnFailureListener { e ->
                showToast("Gagal mengkonfirmasi: ${e.message}")
            }
    }

    private fun isAdmin(): Boolean {
        // Implement your admin check logic here
        return false
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
        _binding = null
    }
}