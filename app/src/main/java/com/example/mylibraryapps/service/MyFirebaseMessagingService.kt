package com.example.mylibraryapps.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mylibraryapps.MainActivity
import com.example.mylibraryapps.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "library_notifications"
        private const val CHANNEL_NAME = "Library Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for library book reminders"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            showNotification(
                title = notification.title ?: "Library App",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }
    
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token to server
        sendTokenToServer(token)
    }
    
    private fun sendTokenToServer(token: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userRef.update("fcmToken", token).await()
                    Log.d(TAG, "Token sent to server successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send token to server", e)
                }
            }
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"]
        val body = data["body"]
        val bookTitle = data["bookTitle"]
        val returnDate = data["returnDate"]
        val daysRemaining = data["daysRemaining"]
        
        when (type) {
            "return_reminder" -> {
                val message = when (daysRemaining?.toIntOrNull()) {
                    3 -> "Buku \"$bookTitle\" harus dikembalikan dalam 3 hari ($returnDate)"
                    2 -> "Buku \"$bookTitle\" harus dikembalikan dalam 2 hari ($returnDate)"
                    1 -> "Buku \"$bookTitle\" harus dikembalikan besok ($returnDate)"
                    0 -> "Buku \"$bookTitle\" harus dikembalikan hari ini ($returnDate)"
                    else -> body ?: "Reminder pengembalian buku"
                }
                
                showNotification(
                    title = title ?: "📚 Reminder Pengembalian",
                    body = message,
                    data = data
                )
            }
            "overdue" -> {
                val message = "⚠️ Buku \"$bookTitle\" sudah terlambat ${daysRemaining ?: 0} hari dari tanggal pengembalian ($returnDate)"
                showNotification(
                    title = title ?: "📚 Buku Terlambat",
                    body = message,
                    data = data
                )
            }
            else -> {
                showNotification(
                    title = title ?: "Library App",
                    body = body ?: "Notification",
                    data = data
                )
            }
        }
    }
    
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Pass data to MainActivity if needed
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
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
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}