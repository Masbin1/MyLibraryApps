package com.example.mylibraryapps.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.RatingBar
import com.bumptech.glide.Glide
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.DialogRateBookBinding
import com.example.mylibraryapps.model.Book

class BookRatingDialog(
    private val context: Context,
    private val book: Book,
    private val onRatingSubmitted: (Float) -> Unit
) {
    private var dialog: Dialog? = null
    private lateinit var binding: DialogRateBookBinding
    
    fun show() {
        binding = DialogRateBookBinding.inflate(LayoutInflater.from(context))
        
        dialog = Dialog(context).apply {
            setContentView(binding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setCancelable(true)
        }
        
        setupDialog()
        dialog?.show()
    }
    
    private fun setupDialog() {
        binding.apply {
            // Set book information
            tvBookTitle.text = book.title
            tvBookAuthor.text = book.author
            
            // Load book cover
            if (book.coverUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(book.coverUrl)
                    .placeholder(R.drawable.placeholder_book)
                    .error(R.drawable.placeholder_book)
                    .into(ivBookCover)
            } else {
                ivBookCover.setImageResource(R.drawable.placeholder_book)
            }
            
            // Setup rating bar
            ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
                updateRatingText(rating)
                btnSubmit.isEnabled = rating > 0
            }
            
            // Setup buttons
            btnCancel.setOnClickListener {
                dialog?.dismiss()
            }
            
            btnSubmit.setOnClickListener {
                val rating = ratingBar.rating
                if (rating > 0) {
                    onRatingSubmitted(rating)
                    dialog?.dismiss()
                }
            }
        }
    }
    
    private fun updateRatingText(rating: Float) {
        val ratingText = when (rating.toInt()) {
            1 -> "Sangat Tidak Suka ⭐"
            2 -> "Tidak Suka ⭐⭐"
            3 -> "Biasa Saja ⭐⭐⭐"
            4 -> "Suka ⭐⭐⭐⭐"
            5 -> "Sangat Suka ⭐⭐⭐⭐⭐"
            else -> "Pilih rating"
        }
        binding.tvRatingText.text = ratingText
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
}