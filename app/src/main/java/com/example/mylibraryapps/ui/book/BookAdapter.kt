package com.example.mylibraryapps.ui.book

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.ItemBookBinding
import com.example.mylibraryapps.model.Book

class BookAdapter(
    private var books: List<Book>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.tvTitle.text = book.title

            // Load image using Glide
            if (book.coverUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(book.coverUrl)
                    .placeholder(R.drawable.ic_book_dummy)
                    .into(binding.ivCover)
            } else {
                binding.ivCover.setImageResource(R.drawable.ic_book_dummy)
            }

            binding.root.setOnClickListener { onItemClick(book) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount() = books.size
}

