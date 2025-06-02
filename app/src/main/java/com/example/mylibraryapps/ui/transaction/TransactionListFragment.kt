package com.example.mylibraryapps.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentTransactionListBinding
import com.example.mylibraryapps.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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

        setupToolbar()
        setupRecyclerView()
        loadTransactions()
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
            val fragment = TransactionDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("transaction", transaction)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransactionListFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadTransactions(statusFilter: String? = null) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        var query = db.collection("transactions")
            .whereEqualTo("userId", auth.currentUser?.uid ?: "")
            .orderBy("borrowDate", Query.Direction.DESCENDING)

        if (!statusFilter.isNullOrEmpty()) {
            query = query.whereEqualTo("status", statusFilter)
        }

        query.addSnapshotListener { snapshot, error ->
            binding.progressBar.visibility = View.GONE

            if (error != null) {
                binding.tvEmpty.text = "Gagal memuat data"
                binding.tvEmpty.visibility = View.VISIBLE
                return@addSnapshotListener
            }

            val transactions = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
            } ?: emptyList()

            adapter.submitList(transactions)

            if (transactions.isEmpty()) {
                binding.tvEmpty.text = if (statusFilter != null) {
                    "Tidak ada transaksi dengan status ini"
                } else {
                    "Belum ada transaksi"
                }
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}