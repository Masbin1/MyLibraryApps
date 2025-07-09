# 📱 Panduan Push Notifikasi MyLibraryApps

## 🔧 Setup Push Notifikasi

### 1. **Test Notifikasi (Debug Mode)**
- Buka app dalam mode debug
- Masuk ke tab "Akun"
- Klik tombol "Test Notification"
- Pilih jenis test yang diinginkan

### 2. **Notifikasi Otomatis (Production)**
System akan otomatis mengirim notifikasi berdasarkan:
- Tanggal pengembalian buku
- Status peminjaman
- Preferensi user

## 📍 Tempat Notifikasi Ditampilkan

### A. **System Notifications**
- **Lokasi**: Android notification bar
- **Triggered**: Otomatis oleh sistem
- **Fitur**: Sound, vibration, persistent

### B. **In-App Notifications**
1. **Popup Notifications**
   - Muncul sebagai popup di HomeFragment
   - Dapat di-dismiss
   - Menampilkan 10 notifikasi terbaru

2. **Notification Tab**
   - Tab khusus di bottom navigation
   - Menampilkan semua notifikasi
   - Dapat mark as read

3. **Notification Badge**
   - Badge merah di navigation bar
   - Menunjukkan jumlah unread notifications

## 🔄 Cara Kerja Notifikasi

### 1. **Jenis Notifikasi**
```
📚 Reminder: 3 Hari Lagi
📚 Reminder: 2 Hari Lagi  
📚 Reminder: Besok
📚 Reminder: Hari Ini
⚠️ Buku Terlambat (Overdue)
```

### 2. **Timing Notifikasi**
- **3 hari sebelum**: "Buku harus dikembalikan dalam 3 hari"
- **2 hari sebelum**: "Buku harus dikembalikan dalam 2 hari"  
- **1 hari sebelum**: "Buku harus dikembalikan besok"
- **Hari ini**: "Buku harus dikembalikan hari ini"
- **Setelah jatuh tempo**: "Buku sudah terlambat X hari"

### 3. **Data Notifikasi**
```kotlin
data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "", // "return_reminder", "overdue"
    val bookTitle: String = "",
    val bookId: String = "",
    val transactionId: String = "",
    val returnDate: String = "",
    val isRead: Boolean = false,
    val timestamp: Long = 0
)
```

## 🧪 Testing Push Notifikasi

### A. **Manual Testing**
1. Buka `NotificationTestActivity`
2. Gunakan tombol-tombol berikut:
   - `Test Immediate`: Cek notifikasi segera
   - `Create 3 Day Test`: Buat transaksi jatuh tempo 3 hari
   - `Create 2 Day Test`: Buat transaksi jatuh tempo 2 hari
   - `Create 1 Day Test`: Buat transaksi jatuh tempo besok
   - `Create Today Test`: Buat transaksi jatuh tempo hari ini
   - `Create Overdue Test`: Buat transaksi terlambat

### B. **Log Monitoring**
```kotlin
// Check logcat untuk debug info
Log.d("PushNotificationHelper", "Would send notification to token: $token")
Log.d("PushNotificationHelper", "Title: $title")
Log.d("PushNotificationHelper", "Body: $body")
```

## 🏗️ Implementasi untuk Production

### 1. **Backend Server Required**
- Push notifications memerlukan server backend
- Gunakan Firebase Admin SDK
- Server akan mengirim notifikasi ke FCM

### 2. **FCM Token Management**
```kotlin
// Update FCM token ke user document
db.collection("users").document(userId)
    .update("fcmToken", token)
```

### 3. **Scheduled Notifications**
```kotlin
// Cron job atau scheduled task di server
// Cek transaksi yang akan jatuh tempo
// Kirim notifikasi sesuai jadwal
```

## 🎯 Tips Debugging

1. **Cek Permissions**: Pastikan `POST_NOTIFICATIONS` granted
2. **Cek FCM Token**: Pastikan token tersimpan di user document
3. **Cek Firestore Rules**: Pastikan user bisa read/write notifications
4. **Check Logs**: Monitor logcat untuk error messages
5. **Test Mode**: Gunakan debug mode untuk testing

## 🔗 File Terkait

- `NotificationTestActivity.kt`: UI untuk testing
- `PushNotificationHelper.kt`: Logic push notifications
- `LocalNotificationHelper.kt`: Local notifications
- `NotificationHelper.kt`: In-app notification popup
- `NotificationsFragment.kt`: UI tab notifications
- `NotificationViewModel.kt`: ViewModel untuk notifications
- `popup_notifications.xml`: Layout popup notifications
- `fragment_notifications.xml`: Layout tab notifications

## 📋 Troubleshooting

### Problem: Notifikasi tidak muncul
- Cek permission `POST_NOTIFICATIONS`
- Cek FCM token di user document
- Cek Firestore rules
- Cek logcat untuk error

### Problem: Notifikasi muncul tapi tidak tepat waktu
- Cek scheduled job di server
- Cek timezone settings
- Cek date calculation logic

### Problem: In-app notifications tidak update
- Cek Firebase connection
- Cek ViewModel observer
- Cek RecyclerView adapter