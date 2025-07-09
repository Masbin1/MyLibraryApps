# 📱 Cara Melihat Notifikasi dari ivNotification di Fragment Home

## 🔔 **Fitur yang Sudah Tersedia:**

### **1. Notification Icon dengan Badge**
- **Lokasi**: Di pojok kanan atas Fragment Home (ivNotification)
- **Fitur**: 
  - Badge merah menunjukkan jumlah notifikasi yang belum dibaca
  - Badge hilang jika tidak ada notifikasi baru
  - Badge menampilkan "99+" jika lebih dari 99 notifikasi

### **2. Notification Popup**
- **Cara Akses**: Klik pada ivNotification di Header Fragment Home
- **Fitur**:
  - Popup muncul di bawah icon notification
  - Menampilkan daftar notifikasi terbaru
  - Dapat scroll jika banyak notifikasi
  - Tombol "Tandai Semua Dibaca"

### **3. Notification Types**
- **📚 Return Reminder**: Notifikasi pengingat pengembalian buku
- **⚠️ Overdue**: Notifikasi buku yang sudah terlambat
- **ℹ️ General**: Notifikasi umum lainnya

## 📋 **Cara Menggunakan:**

### **Step 1: Lihat Badge Notifikasi**
1. Buka Fragment Home
2. Perhatikan icon notifikasi di pojok kanan atas
3. Jika ada badge merah, artinya ada notifikasi baru
4. Angka di badge menunjukkan jumlah notifikasi belum dibaca

### **Step 2: Buka Popup Notifikasi**
1. Klik pada icon notifikasi (ivNotification)
2. Popup akan muncul di bawah icon
3. Scroll untuk melihat semua notifikasi
4. Klik "Tandai Semua Dibaca" untuk mark all as read

### **Step 3: Interaksi dengan Notifikasi**
1. **Klik Notifikasi Individual**:
   - Notifikasi akan di-mark sebagai read
   - Popup akan tertutup
   - Akan menampilkan pesan/navigasi sesuai type notifikasi

2. **Mark All as Read**:
   - Klik "Tandai Semua Dibaca" di header popup
   - Semua notifikasi akan di-mark sebagai read
   - Badge akan hilang

## 🔧 **Kode Implementation:**

### **A. Notification Badge**
```kotlin
// Badge otomatis update ketika ada notifikasi baru
notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
    updateNotificationBadge(count)
}
```

### **B. Popup Notification**
```kotlin
// Klik icon notification untuk show popup
binding.ivNotification.setOnClickListener {
    showNotificationPopup()
}
```

### **C. Handle Notification Click**
```kotlin
// Handle when user clicks on individual notification
private fun handleNotificationClick(notification: Notification) {
    // Mark as read
    notificationViewModel.markAsRead(notification.id)
    
    // Close popup
    notificationPopup?.dismiss()
    
    // Show message or navigate
    when (notification.type) {
        "return_reminder", "overdue" -> {
            // Show book info
            Snackbar.make(binding.root, "Buku: ${notification.relatedItemTitle}", Snackbar.LENGTH_LONG).show()
        }
        else -> {
            // Show notification message
            Snackbar.make(binding.root, notification.message, Snackbar.LENGTH_LONG).show()
        }
    }
}
```

## 📊 **Notification Data Flow:**

```
1. User Transaction Created → Database
2. Notification Scheduler → Check Due Dates
3. Create Notification → Save to notifications collection
4. HomeFragment → Load notifications for current user
5. Badge Updated → Show unread count
6. User Clicks Icon → Show popup with notifications
7. User Clicks Notification → Mark as read & handle action
```

## 🧪 **Testing Notifikasi:**

### **A. Automatic Testing**
1. Buat transaksi peminjaman buku
2. Set return date yang dekat
3. Tunggu system check (atau trigger manual)
4. Notifikasi akan muncul di popup

### **B. Manual Testing (Debug Mode)**
1. Masuk ke Account tab
2. Klik "Test Notification" 
3. Pilih create test transaction
4. Kembali ke Home
5. Klik ivNotification untuk lihat hasil

## 📱 **UI Components:**

### **Files Terkait:**
- **HomeFragment.kt**: Main logic untuk notification popup
- **popup_notifications.xml**: Layout popup notifikasi
- **NotificationAdapter.kt**: Adapter untuk RecyclerView notifications
- **NotificationViewModel.kt**: ViewModel untuk manage notifications data
- **notification_badge.xml**: Drawable untuk badge
- **dimens.xml**: Dimension values untuk popup

### **Layout Elements:**
- **ivNotification**: Icon notifikasi di header
- **notification_badge**: Badge merah untuk unread count
- **popup_notifications**: Popup container
- **rvNotifications**: RecyclerView untuk list notifikasi
- **tvMarkAllRead**: Button untuk mark all as read

## 🔍 **Troubleshooting:**

### **Problem: Badge tidak muncul**
- Pastikan ada notifikasi unread di database
- Cek apakah notificationViewModel.loadNotifications() dipanggil
- Cek Firestore rules untuk notifications collection

### **Problem: Popup tidak muncul**
- Cek apakah popup_notifications.xml ada
- Cek apakah dimension values sudah defined
- Pastikan adapter sudah di-setup dengan benar

### **Problem: Notifikasi tidak update**
- Cek Firebase connection
- Cek apakah Observer lifecycle sudah benar
- Pastikan user authentication working

## 🎯 **Next Steps:**

1. **Add Navigation**: Navigate ke transaction detail saat klik notifikasi
2. **Add Filters**: Filter notifikasi berdasarkan type atau date
3. **Add Sorting**: Sort berdasarkan timestamp atau priority
4. **Add Actions**: Action buttons untuk quick actions (mark as read, dismiss, etc.)
5. **Add Sound**: Notification sound untuk new notifications