package com.example.mylibraryapps.ui.notification

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mylibraryapps.R
import com.example.mylibraryapps.model.Notification
import java.util.*

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        private val viewUnreadIndicator: View = itemView.findViewById(R.id.viewUnreadIndicator)
        private val viewUnreadBorder: View = itemView.findViewById(R.id.viewUnreadBorder)

        fun bind(notification: Notification) {
            tvTitle.text = notification.title
            tvMessage.text = notification.message
            
            // Format relative time dengan format yang lebih singkat
            val relativeTime = formatRelativeTime(notification.timestamp.time)
            tvTime.text = relativeTime
            
            // Show/hide unread indicators
            val isUnread = !notification.isRead
            viewUnreadIndicator.visibility = if (isUnread) View.VISIBLE else View.INVISIBLE
            viewUnreadBorder.visibility = if (isUnread) View.VISIBLE else View.GONE
            
            // Set click listener
            itemView.setOnClickListener {
                onNotificationClick(notification)
            }
        }
        
        private fun formatRelativeTime(timestamp: Long): String {
            val now = Calendar.getInstance().timeInMillis
            val diff = now - timestamp
            
            return when {
                diff < DateUtils.MINUTE_IN_MILLIS -> "Baru"
                diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m"
                diff < DateUtils.DAY_IN_MILLIS -> "${diff / DateUtils.HOUR_IN_MILLIS}j"
                diff < DateUtils.WEEK_IN_MILLIS -> "${diff / DateUtils.DAY_IN_MILLIS}h"
                else -> DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    now,
                    DateUtils.WEEK_IN_MILLIS
                ).toString()
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
}