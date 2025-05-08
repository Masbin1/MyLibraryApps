package com.example.mylibraryapps.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentHomeBinding
import com.example.mylibraryapps.model.Book
import com.example.mylibraryapps.ui.book.BookAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookAdapter: BookAdapter
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Set greeting
        binding.tvGreeting.text = "Halo, Anggota"

        // Dummy book data
        val allBooks = homeViewModel.getAllBooks()
        setupRecyclerView(allBooks)

        // Filter button
        binding.btnSemua.setOnClickListener {
            setupRecyclerView(allBooks)
        }
        binding.btnSastra.setOnClickListener {
            setupRecyclerView(allBooks.filter { it.title.contains("Sastra", true) })
        }
        binding.btnSejarah.setOnClickListener {
            setupRecyclerView(allBooks.filter { it.title.contains("Sejarah", true) })
        }
        binding.btnFiksi.setOnClickListener {
            setupRecyclerView(allBooks.filter { it.title.contains("Fiksi", true) })
        }

        return view
    }

    private fun setupRecyclerView(books: List<Book>) {
        bookAdapter = BookAdapter(books) { selectedBook ->
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
