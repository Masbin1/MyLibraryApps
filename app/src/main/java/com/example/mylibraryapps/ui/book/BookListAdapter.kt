package com.example.mylibraryapps.ui.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mylibraryapps.databinding.ListBookBinding
import com.example.mylibraryapps.model.Book

class BookListAdapter(
    private var books: List<Book>,
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookListAdapter.BookViewHolder>() {

    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    inner class BookViewHolder(private val binding: ListBookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.tvTitle.text = book.title
            binding.tvAuthor.text = book.author

            // Load image using Glide (add dependency in build.gradle)
            if (book.coverUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(book.coverUrl)
                    .into(binding.imgCover)
            }

            binding.root.setOnClickListener {
                onItemClick(book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ListBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount() = books.size
}