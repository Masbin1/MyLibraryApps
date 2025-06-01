package com.example.mylibraryapps.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentHomeBinding
import com.example.mylibraryapps.ui.book.BookAdapter

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
            selectedBook?.let {
                val bundle = Bundle().apply {
                    putParcelable("book", it)
                }
                findNavController().navigate(R.id.bookDetailFragment, bundle)
            }
        }

        binding.rvBooks.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        homeViewModel.books.observe(viewLifecycleOwner) { books ->
            Log.d("HomeFragment", "Received ${books?.size ?: 0} books")
            books?.let {
                bookAdapter.updateBooks(it)
            }
        }

        homeViewModel.books.observe(viewLifecycleOwner) { books ->
            books?.let {
                bookAdapter.updateBooks(it)

            }
        }


        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }

        homeViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Using Snackbar instead of Toast for better UX
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    it,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
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