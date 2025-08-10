package com.example.mylibraryapps.ui.recommendation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.ItemRecommendationBinding
import com.example.mylibraryapps.model.BookRecommendation
import com.example.mylibraryapps.model.RecommendationType

class RecommendationAdapter(
    private val onBookClick: (BookRecommendation) -> Unit
) : ListAdapter<BookRecommendation, RecommendationAdapter.RecommendationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecommendationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecommendationViewHolder(
        private val binding: ItemRecommendationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recommendation: BookRecommendation) {
            val book = recommendation.book
            
            binding.apply {
                // Book info
                tvBookTitle.text = book.title
                tvBookAuthor.text = book.author
                tvBookGenre.text = book.genre
                
                // Recommendation info
                tvRecommendationReason.text = recommendation.reason
                tvRecommendationScore.text = "${(recommendation.score * 100).toInt()}%"
                
                // Recommendation type badge
                when (recommendation.recommendationType) {
                    RecommendationType.COLLABORATIVE -> {
                        tvRecommendationType.text = "Kolaboratif"
                        tvRecommendationType.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.purple_500)
                        )
                    }
                    RecommendationType.CONTENT_BASED -> {
                        tvRecommendationType.text = "Konten"
                        tvRecommendationType.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.teal_700)
                        )
                    }
                    RecommendationType.POPULAR -> {
                        tvRecommendationType.text = "Populer"
                        tvRecommendationType.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.orange_500)
                        )
                    }
                    RecommendationType.TRENDING -> {
                        tvRecommendationType.text = "Trending"
                        tvRecommendationType.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.red_500)
                        )
                    }
                    RecommendationType.PERSONAL -> {
                        tvRecommendationType.text = "Personal"
                        tvRecommendationType.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.indigo_500)
                        )
                    }
                }
                
                // Load book cover
                if (book.coverUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(book.coverUrl)
                        .placeholder(R.drawable.placeholder_book)
                        .error(R.drawable.placeholder_book)
                        .into(ivBookCover)
                } else {
                    ivBookCover.setImageResource(R.drawable.placeholder_book)
                }
                
                // Click listener
                root.setOnClickListener {
                    onBookClick(recommendation)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookRecommendation>() {
        override fun areItemsTheSame(oldItem: BookRecommendation, newItem: BookRecommendation): Boolean {
            return oldItem.book.id == newItem.book.id
        }

        override fun areContentsTheSame(oldItem: BookRecommendation, newItem: BookRecommendation): Boolean {
            return oldItem == newItem
        }
    }
}