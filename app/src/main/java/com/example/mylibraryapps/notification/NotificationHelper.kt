package com.example.mylibraryapps.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mylibraryapps.MainActivity
import com.example.mylibraryapps.R

class NotificationHelper(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "library_notifications"
        const val CHANNEL_NAME = "Library Notifications"
        const val CHANNEL_DESCRIPTION = "Notifications for library book returns"
        
        const val OVERDUE_NOTIFICATION_ID = 1001
        const val WARNING_NOTIFICATION_ID = 1002
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showOverdueNotification(bookTitle: String, userName: String, daysOverdue: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_book_24)
            .setContentTitle("Buku Terlambat Dikembalikan!")
            .setContentText("$userName terlambat mengembalikan \"$bookTitle\" selama $daysOverdue hari")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$userName terlambat mengembalikan buku \"$bookTitle\" selama $daysOverdue hari. Segera hubungi peminjam untuk mengembalikan buku."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(OVERDUE_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle permission denied
        }
    }
    
    fun showWarningNotification(bookTitle: String, userName: String, daysLeft: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_book_24)
            .setContentTitle("Peringatan Pengembalian Buku")
            .setContentText("$userName harus mengembalikan \"$bookTitle\" dalam $daysLeft hari")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Reminder: $userName harus mengembalikan buku \"$bookTitle\" dalam $daysLeft hari. Segera ingatkan peminjam."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(WARNING_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle permission denied
        }
    }
    
    fun showMultipleOverdueNotification(overdueCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_book_24)
            .setContentTitle("Beberapa Buku Terlambat!")
            .setContentText("Ada $overdueCount buku yang terlambat dikembalikan")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Terdapat $overdueCount buku yang terlambat dikembalikan. Periksa daftar transaksi untuk detail lebih lanjut."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(OVERDUE_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle permission denied
        }
    }
}