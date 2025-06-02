package com.example.mylibraryapps.ui.transaction

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
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.tvTitle.text = transaction.title
            binding.tvAuthor.text = "Penulis: ${transaction.author}"

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val borrowDate = dateFormat.format(SimpleDateFormat("yyyy-MM-dd").parse(transaction.borrowDate))
            val returnDate = dateFormat.format(SimpleDateFormat("yyyy-MM-dd").parse(transaction.returnDate))
            binding.tvDate.text = "$borrowDate - $returnDate"

            binding.tvStatus.text = when (transaction.status) {
                "menunggu konfirmasi pinjam" -> "Menunggu"
                "sedang dipinjam" -> "Dipinjam"
                "menunggu konfirmasi pengembalian" -> "Pengembalian"
                "sudah dikembalikan" -> "Selesai"
                else -> transaction.status
            }

            val bgRes = when (transaction.status) {
                "menunggu konfirmasi pinjam" -> R.drawable.bg_status_pending
                "sedang dipinjam" -> R.drawable.bg_status_borrowed
                "menunggu konfirmasi pengembalian" -> R.drawable.bg_status_returning
                "sudah dikembalikan" -> R.drawable.bg_status_completed
                else -> R.drawable.bg_status_pending
            }
            binding.tvStatus.setBackgroundResource(bgRes)

            if (transaction.coverUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(transaction.coverUrl)
                    .into(binding.ivCover)
            }

            binding.root.setOnClickListener {
                onClick(transaction)
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