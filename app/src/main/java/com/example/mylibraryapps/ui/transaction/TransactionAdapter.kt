package com.example.mylibraryapps.ui.transaction

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.ItemTransactionBinding
import com.example.mylibraryapps.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            try {
                binding.tvTitle.text = transaction.title.ifBlank { "Judul Tidak Tersedia" }
                binding.tvAuthor.text = "Penulis: ${transaction.author.ifBlank { "Tidak Diketahui" }}"

                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val borrowDate = parseDateSafe(transaction.borrowDate, inputFormat, dateFormat)
                val returnDate = parseDateSafe(transaction.returnDate, inputFormat, dateFormat)

                binding.tvDate.text = "$borrowDate - $returnDate"

                val statusText = when (transaction.status) {
                    "menunggu konfirmasi pinjam" -> "Menunggu"
                    "sedang dipinjam" -> "Dipinjam"
                    "menunggu konfirmasi pengembalian" -> "Pengembalian"
                    "sudah dikembalikan" -> "Selesai"
                    else -> transaction.status.ifBlank { "Unknown" }
                }

                val statusBg = when (transaction.status) {
                    "menunggu konfirmasi pinjam" -> R.drawable.bg_status_pending
                    "sedang dipinjam" -> R.drawable.bg_status_borrowed
                    "menunggu konfirmasi pengembalian" -> R.drawable.bg_status_returning
                    "sudah dikembalikan" -> R.drawable.bg_status_completed
                    else -> R.drawable.bg_status_pending
                }

                binding.tvStatus.text = statusText
                binding.tvStatus.setBackgroundResource(statusBg)

                if (transaction.coverUrl.isNotBlank()) {
                    Glide.with(binding.root.context)
                        .load(transaction.coverUrl)
                        .placeholder(R.drawable.ic_book_cover_placeholder)
                        .error(R.drawable.ic_book_cover_placeholder)
                        .into(binding.ivCover)
                } else {
                    binding.ivCover.setImageResource(R.drawable.ic_book_cover_placeholder)
                }

                binding.root.setOnClickListener {
                    onClick(transaction)
                }

            } catch (e: Exception) {
                Log.e("TransactionAdapter", "Error binding transaction data", e)
            }
        }

        private fun parseDateSafe(
            date: String,
            inputFormat: SimpleDateFormat,
            outputFormat: SimpleDateFormat
        ): String {
            return try {
                inputFormat.parse(date)?.let { outputFormat.format(it) } ?: date
            } catch (e: Exception) {
                date
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
