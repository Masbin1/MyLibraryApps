package com.example.mylibraryapps.ui.transaction

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentTransactionListBinding
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class TransactionListFragment : Fragment() {

    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            setupToolbar()
            setupRecyclerView()
            loadInitialData()
        } catch (e: Exception) {
            Log.e("TransactionList", "Initialization error", e)
            showError("Gagal memulai aplikasi")
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            try {
                when (menuItem.itemId) {
                    R.id.action_filter_all -> {
                        loadTransactions()
                        true
                    }
                    R.id.action_filter_borrow -> {
                        loadTransactions("sedang dipinjam")
                        true
                    }
                    R.id.action_filter_return -> {
                        loadTransactions("sudah dikembalikan")
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Log.e("TransactionList", "Menu error", e)
                false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            try {
                if (transaction.id.isNotEmpty()) {
                    navigateToDetail(transaction)
                } else {
                    showToast("Transaksi tidak valid")
                }
            } catch (e: Exception) {
                Log.e("TransactionList", "Navigation error", e)
                showToast("Gagal membuka detail")
            }
        }

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransactionListFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadInitialData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        // Load without filter first
        loadTransactions()
    }

    private fun loadTransactions(statusFilter: String? = null) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            showError("User tidak terautentikasi")
            return
        }

        try {
            db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    handleSuccess(snapshot, statusFilter)
                }
                .addOnFailureListener { e ->
                    handleError(e)
                }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun handleSuccess(snapshot: QuerySnapshot, statusFilter: String?) {
        binding.progressBar.visibility = View.GONE

        try {
            val transactions = mutableListOf<Transaction>()
            for (doc in snapshot.documents) {
                try {
                    doc.toObject(Transaction::class.java)?.let { transaction ->
                        transactions.add(transaction.copy(id = doc.id))
                    }
                } catch (e: Exception) {
                    Log.w("TransactionList", "Error parsing doc ${doc.id}", e)
                }
            }

            // Client-side processing
            val processedList = processTransactions(transactions, statusFilter)

            if (processedList.isEmpty()) {
                showEmptyState(statusFilter)
            } else {
                adapter.submitList(processedList)
                binding.tvEmpty.visibility = View.GONE
            }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun processTransactions(transactions: List<Transaction>, statusFilter: String?): List<Transaction> {
        return transactions
            .filter {
                statusFilter?.let { filter -> it.status == filter } ?: true
            }
            .sortedByDescending {
                parseDate(it.borrowDate)
            }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .parse(dateString)?.time ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun showEmptyState(statusFilter: String?) {
        binding.tvEmpty.text = if (statusFilter != null) {
            "Tidak ada transaksi dengan status ini"
        } else {
            "Belum ada transaksi"
        }
        binding.tvEmpty.visibility = View.VISIBLE
    }

    private fun handleError(e: Exception) {
        binding.progressBar.visibility = View.GONE
        Log.e("TransactionList", "Error loading data", e)
        showError("Gagal memuat data: ${e.localizedMessage ?: "Unknown error"}")
    }

    private fun navigateToDetail(transaction: Transaction) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, TransactionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("transaction", transaction)
                }
            })
            .addToBackStack(null)
            .commit()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        binding.tvEmpty.text = message
        binding.tvEmpty.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}