package com.example.mylibraryapps.ui.transaction

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentTransactionListBinding
import com.example.mylibraryapps.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionListFragment : Fragment() {

    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter
    private lateinit var viewModel: TransactionViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_filter_all -> {
                    viewModel.refreshTransactions()
                    true
                }
                R.id.action_filter_borrow -> {
                    viewModel.refreshTransactions("sedang dipinjam")
                    true
                }
                R.id.action_filter_return -> {
                    viewModel.refreshTransactions("sudah dikembalikan")
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

    private fun setupObservers() {
        // Show/hide progress bar based on loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Handle error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showError(it)
                viewModel.clearErrorMessage()
            }
        }
        
        // Update transaction list
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isEmpty()) {
                showEmptyState(null)
            } else {
                val sortedList = transactions.sortedByDescending { parseDate(it.borrowDate) }
                adapter.submitList(sortedList)
                binding.tvEmpty.visibility = View.GONE
            }
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

    private fun navigateToDetail(transaction: Transaction) {
        val bundle = Bundle().apply {
            putParcelable("transaction", transaction)
        }
        
        try {
            // Navigate directly to the destination fragment
            findNavController().navigate(
                R.id.transactionDetailFragment,
                bundle
            )
        } catch (e: Exception) {
            Log.e("TransactionList", "Navigation error", e)
            showToast("Navigasi gagal: ${e.message}")
        }
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
