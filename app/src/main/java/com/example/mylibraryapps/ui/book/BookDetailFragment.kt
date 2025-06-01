package com.example.mylibraryapps.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
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

        arguments?.let {
            book = it.getParcelable("book") ?: return@let

            binding.tvBookTitle.text = book.title
            binding.tvAuthor.text = book.author
            binding.tvPublisher.text = book.publisher
            binding.tvPurchaseDate.text = book.purchaseDate
            binding.tvSpecifications.text = book.specifications
            binding.tvMaterial.text = book.material
            binding.tvQuantity.text = book.quantity.toString()
            binding.tvGenre.text = book.genre

            if (book.coverUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(book.coverUrl)
                    .into(binding.ivCover)
            }

            binding.btnBack.setOnClickListener {
                requireActivity().onBackPressed()
            }

            binding.btnPinjam.setOnClickListener {
                // Handle book borrowing
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}