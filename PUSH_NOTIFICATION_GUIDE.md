# 🚀 Panduan Mengaktifkan Push Notifikasi

## 📋 **Cara Mengaktifkan Push Notifikasi**

### **Method 1: Testing dengan NotificationTestActivity**

#### **Step 1: Buka Test Activity**
1. Buka app dalam **debug mode**
2. Masuk ke tab **"Akun"**
3. Klik tombol **"Test Notification"**
4. Akan terbuka **NotificationTestActivity**

#### **Step 2: Buat Test Transaction**
```kotlin
// Pilih salah satu dari tombol berikut:
- "Create 3 Day Test"    // Buat transaksi jatuh tempo 3 hari
- "Create 2 Day Test"    // Buat transaksi jatuh tempo 2 hari  
- "Create 1 Day Test"    // Buat transaksi jatuh tempo besok
- "Create Today Test"    // Buat transaksi jatuh tempo hari ini
- "Create Overdue Test"  // Buat transaksi yang sudah terlambat
```

#### **Step 3: Trigger Notification Check**
1. Klik **"Test Immediate"** untuk cek notifikasi segera
2. Sistem akan:
   - Cek semua transaksi aktif
   - Buat notifikasi sesuai jadwal
   - Trigger push notification

### **Method 2: Otomatis dengan Real Transaction**

#### **Step 1: Buat Transaksi Normal**
1. Pinjam buku melalui app
2. Set return date yang dekat (misal: 1-3 hari dari sekarang)
3. Sistem akan otomatis cek setiap hari

#### **Step 2: Aktifkan Background Worker**
```kotlin
// Sistem sudah otomatis menggunakan WorkManager
// Tapi bisa dipicu manual untuk testing
val immediateWork = OneTimeWorkRequestBuilder<NotificationWorker>().build()
WorkManager.getInstance(context).enqueue(immediateWork)
```

## 🔧 **Cara Kerja Push Notifikasi**

### **1. Sistem Automatic Check**
```kotlin
// NotificationWorker berjalan berkala
// Cek semua transaksi aktif
// Bandingkan dengan return date
// Buat notifikasi jika perlu
```

### **2. Timing Notifikasi**
- **3 hari sebelum**: "Buku harus dikembalikan dalam 3 hari"
- **2 hari sebelum**: "Buku harus dikembalikan dalam 2 hari"
- **1 hari sebelum**: "Buku harus dikembalikan besok"
- **Hari ini**: "Buku harus dikembalikan hari ini"
- **Setelah jatuh tempo**: "Buku sudah terlambat X hari"

### **3. Duplikasi Protection**
```kotlin
// Sistem cek apakah notifikasi sudah pernah dikirim
// Tidak akan kirim notifikasi yang sama berulang kali
// Menggunakan collection "notification_sent" untuk tracking
```

## 📱 **Dimana Notifikasi Muncul**

### **A. System Notification (Android)**
- **Lokasi**: Notification bar Android
- **Triggered**: LocalNotificationHelper
- **Fitur**: Sound, vibration, persistent

### **B. In-App Notification**
- **Lokasi**: Icon notifikasi di HomeFragment
- **Badge**: Menunjukkan jumlah unread
- **Popup**: Daftar notifikasi saat klik icon

### **C. Notification Tab**
- **Lokasi**: Bottom navigation "Notifikasi"
- **Content**: Semua notifikasi user
- **Actions**: Mark as read, delete, dll

## 🧪 **Step-by-Step Testing**

### **Scenario 1: Test Immediate Notification**
```bash
1. Buka NotificationTestActivity
2. Klik "Create 1 Day Test"
3. Toast: "Test transaction created (1 day)"
4. Klik "Test Immediate"
5. Toast: "Immediate notification check started"
6. Kembali ke Home
7. Cek icon notifikasi (ada badge?)
8. Klik icon notification
9. Lihat popup notifikasi
```

### **Scenario 2: Test Overdue Notification**
```bash
1. Buka NotificationTestActivity
2. Klik "Create Overdue Test"
3. Toast: "Test transaction created (overdue)"
4. Klik "Test Immediate"
5. Kembali ke Home
6. Cek notification popup
7. Harus ada notifikasi "Buku Terlambat"
```

### **Scenario 3: Clear and Retry**
```bash
1. Klik "Clear Notification Records"
2. Klik "Show Notification Records" (cek logcat)
3. Klik "Test Immediate" lagi
4. Notifikasi harus muncul kembali
```

## 🔍 **Debugging Push Notifikasi**

### **A. Check Logcat**
```bash
# Filter log untuk melihat notification activity
adb logcat | grep "NotificationTestHelper"
adb logcat | grep "PushNotificationHelper"
adb logcat | grep "NotificationWorker"
```

### **B. Check Database**
```kotlin
// Cek di Firestore Console:
// 1. Collection "transactions" - harus ada data dengan status "Dipinjam"
// 2. Collection "notifications" - harus ada notifikasi yang dibuat
// 3. Collection "notification_sent" - tracking notifikasi yang sudah dikirim
```

### **C. Check Active Transactions**
```bash
1. Buka NotificationTestActivity
2. Klik "Show Active Transactions"
3. Cek logcat untuk melihat transaksi aktif
4. Pastikan ada transaksi dengan return date yang dekat
```

## ⚠️ **Troubleshooting**

### **Problem: Notifikasi tidak muncul**
```bash
✅ Cek apakah ada transaksi dengan status "Dipinjam"
✅ Cek return date format (yyyy-MM-dd)
✅ Cek Firestore rules untuk collection "notifications"
✅ Cek apakah user login
✅ Cek apakah WorkManager berjalan
```

### **Problem: Notifikasi muncul tapi tidak ada di popup**
```bash
✅ Cek apakah notificationViewModel.loadNotifications() dipanggil
✅ Cek Firebase connection
✅ Cek lifecycle observer
✅ Cek adapter di RecyclerView
```

### **Problem: Badge tidak update**
```bash
✅ Cek apakah unreadCount observer aktif
✅ Cek apakah badge view di-create dengan benar
✅ Cek calculation unread count
```

## 🎯 **Quick Test Commands**

### **1. Test Full Flow**
```bash
1. Create test transaction → "Create 1 Day Test"
2. Check transactions → "Show Active Transactions"
3. Clear records → "Clear Notification Records"
4. Trigger check → "Test Immediate"
5. Check results → Go to Home, click notification icon
```

### **2. Test Multiple Scenarios**
```bash
1. Create 3 different test transactions (1 day, 2 day, overdue)
2. Clear notification records
3. Test immediate
4. Should see 3 different notifications
```

### **3. Test Mark as Read**
```bash
1. Create test notifications
2. Go to Home → Click notification icon
3. Click individual notification → Should mark as read
4. Click "Tandai Semua Dibaca" → Should clear all
```

## 📊 **Expected Results**

### **Successful Push Notification:**
- ✅ Badge muncul di icon notifikasi
- ✅ Popup menampilkan notifikasi
- ✅ Notification tab menampilkan semua notifikasi
- ✅ Mark as read berfungsi
- ✅ System notification muncul di Android bar

### **Notification Content:**
```
📚 Reminder: 1 Hari Lagi
Buku "Test Book - Return in 1 days" harus dikembalikan besok (2024-01-15)
```

```
⚠️ Buku Terlambat
Buku "Test Book - Return in -1 days" sudah terlambat 1 hari dari tanggal pengembalian (2024-01-13). Segera kembalikan!
```

## 🚀 **Production Setup**

### **For Real Push Notifications:**
1. **Firebase Cloud Messaging**: Setup FCM server key
2. **Backend Server**: Implement FCM Admin SDK
3. **Scheduled Jobs**: Setup cron jobs untuk auto-check
4. **FCM Tokens**: Proper token management
5. **User Preferences**: Allow users to control notification settings

### **Current Status:**
- ✅ **Local Notifications**: Working
- ✅ **In-App Notifications**: Working
- ✅ **Database Structure**: Ready
- ⚠️ **Push Notifications**: Need backend server
- ⚠️ **FCM Integration**: Need proper server setup