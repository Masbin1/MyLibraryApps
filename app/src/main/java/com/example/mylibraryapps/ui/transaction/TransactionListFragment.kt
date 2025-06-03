package com.example.mylibraryapps.ui.transaction

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        setupRecyclerView()
        loadInitialData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
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
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { transaction ->
            if (transaction.id.isNotEmpty()) {
                navigateToDetail(transaction)
            } else {
                showToast("Transaksi tidak valid")
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
        loadTransactions()
    }

    private fun loadTransactions(statusFilter: String? = null) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            showError("User tidak terautentikasi")
            return
        }

        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                handleSuccess(snapshot, statusFilter)
            }
            .addOnFailureListener { e ->
                handleError(e)
            }
    }

    private fun handleSuccess(snapshot: QuerySnapshot, statusFilter: String?) {
        binding.progressBar.visibility = View.GONE

        val transactions = snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                Log.w("TransactionList", "Error parsing doc ${doc.id}", e)
                null
            }
        }

        val processedList = transactions
            .filter { transaction -> statusFilter?.let { it == transaction.status } ?: true }
            .sortedByDescending { parseDate(it.borrowDate) }

        if (processedList.isEmpty()) {
            showEmptyState(statusFilter)
        } else {
            adapter.submitList(processedList)
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)?.time ?: 0
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
        val bundle = Bundle().apply {
            putParcelable("transaction", transaction)
        }
        
        // Navigate directly to the destination fragment
        findNavController().navigate(
            R.id.transactionDetailFragment,
            bundle
        )
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
