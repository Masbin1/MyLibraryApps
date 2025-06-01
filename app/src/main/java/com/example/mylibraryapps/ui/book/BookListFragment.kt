package com.example.mylibraryapps.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.FragmentBookListBinding
import com.example.mylibraryapps.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BookListFragment : Fragment() {

    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookListAdapter: BookListAdapter
    private val db: FirebaseFirestore = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter with empty list
        bookListAdapter = BookListAdapter(emptyList()) { selectedBook ->
            val bundle = Bundle().apply {
                putParcelable("book", selectedBook)
            }
            findNavController().navigate(R.id.bookDetailFragment, bundle)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookListAdapter
        }

        // Load books from Firestore
        loadBooksFromFirestore()
    }

    private fun loadBooksFromFirestore() {
        binding.progressBar.visibility = View.VISIBLE

        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                val books = mutableListOf<Book>()
                for (document in result) {
                    val book = document.toObject(Book::class.java).copy(id = document.id)
                    books.add(book)
                }
                bookListAdapter = BookListAdapter(books) { selectedBook ->
                    val bundle = Bundle().apply {
                        putParcelable("book", selectedBook)
                    }
                    findNavController().navigate(R.id.bookDetailFragment, bundle)
                }
                binding.recyclerView.adapter = bookListAdapter
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                // Handle error
                binding.progressBar.visibility = View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}