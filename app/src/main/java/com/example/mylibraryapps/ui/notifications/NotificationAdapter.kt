package com.example.mylibraryapps.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibraryapps.R
import com.example.mylibraryapps.databinding.ItemNotificationBinding
import com.example.mylibraryapps.model.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                tvNotificationTitle.text = notification.title
                tvNotificationMessage.text = notification.message
                tvNotificationTime.text = formatTime(notification.timestamp)
                
                // Set unread indicator visibility
                viewUnreadIndicator.visibility = if (notification.isRead) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
                
                // Set text style based on read status
                if (notification.isRead) {
                    tvNotificationTitle.alpha = 0.7f
                    tvNotificationMessage.alpha = 0.7f
                    tvNotificationTime.alpha = 0.7f
                } else {
                    tvNotificationTitle.alpha = 1.0f
                    tvNotificationMessage.alpha = 1.0f
                    tvNotificationTime.alpha = 1.0f
                }
                
                root.setOnClickListener {
                    onNotificationClick(notification)
                }
            }
        }
        
        private fun formatTime(timestamp: Date): String {
            val now = Date()
            val diff = now.time - timestamp.time
            
            return when {
                diff < 60 * 1000 -> "Baru saja"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} menit yang lalu"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} jam yang lalu"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} hari yang lalu"
                else -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(timestamp)
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
    override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem == newItem
    }
}