package com.example.mylibraryapps.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentHomeBinding
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.ui.book.BookAdapter
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var bookAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupFilterButtons()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(emptyList()) { selectedBook ->
            val bundle = Bundle().apply {
                putParcelable("book", selectedBook)
            }
            findNavController().navigate(R.id.bookDetailFragment, bundle)
        }

        binding.rvBooks.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
        }
    }

    private fun setupObservers() {
        homeViewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.updateBooks(books)
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        homeViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                homeViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupFilterButtons() {
        binding.btnSemua.setOnClickListener {
            homeViewModel.filterBooksByGenre("Semua")
        }
        binding.btnSastra.setOnClickListener {
            homeViewModel.filterBooksByGenre("Sastra")
        }
        binding.btnSejarah.setOnClickListener {
            homeViewModel.filterBooksByGenre("Sejarah")
        }
        binding.btnFiksi.setOnClickListener {
            homeViewModel.filterBooksByGenre("Fiksi")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}