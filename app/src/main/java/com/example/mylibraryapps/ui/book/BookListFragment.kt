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
import com.example.mylibraryapps.ui.book.BookDetailFragment
import com.example.mylibraryapps.ui.book.BookListAdapter

class BookListFragment : Fragment() {

    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookListAdapter: BookListAdapter
    private lateinit var books: List<Book>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inisialisasi data dummy
        books = getDummyBooks()

        // Set adapter dan layout manager
        bookListAdapter = BookListAdapter(books) { selectedBook ->
            val bundle = Bundle().apply {
                putParcelable("book", selectedBook)
            }
            findNavController().navigate(R.id.bookDetailFragment, bundle)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookListAdapter
        }
    }

    private fun getDummyBooks(): List<Book> {
        return listOf(
            Book(
                title = "Laskar Pelangi",
                author = "Andrea Hirata",
                year = "2005",
                publisher = "Bentang Pustaka",
                type = "Novel",
                description = "Kisah anak-anak sekolah di Belitung.",
                coverResId = R.drawable.ic_dashboard_black_24dp
            ),
            Book(
                title = "Negeri 5 Menara",
                author = "Ahmad Fuadi",
                year = "2009",
                publisher = "Gramedia Pustaka Utama",
                type = "Novel",
                description = "Perjuangan 6 santri meraih impian.",
                coverResId = R.drawable.ic_dashboard_black_24dp
            )
            // Tambahkan buku lain jika perlu
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
