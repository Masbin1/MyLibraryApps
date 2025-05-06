package com.example.mylibraryapps.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mylibraryapps.databinding.FragmentBookDetailBinding
import com.example.mylibraryapps.model.Book

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var book: Book

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mendapatkan data buku yang dikirim
        arguments?.let {
            book = it.getParcelable("book") ?: return@let

            // Set data ke view sesuai XML layout
            binding.tvBookTitle.text = book.title
            binding.ivCover.setImageResource(book.coverResId) // Asumsikan Book memiliki properti coverResId

            // Set listener untuk tombol kembali
            binding.btnBack.setOnClickListener {
                requireActivity().onBackPressed()
            }

            // Set listener untuk tombol pinjam
            binding.btnPinjam.setOnClickListener {
                // Handle peminjaman buku di sini
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(book: Book): BookDetailFragment {
            return BookDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("book", book)
                }
            }
        }
    }
}