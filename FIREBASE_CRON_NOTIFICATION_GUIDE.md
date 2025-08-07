# Firebase Cron Notification System Guide

## Overview
Sistem notifikasi berbasis Firebase Functions yang menggunakan cron job untuk mengecek transaksi buku yang belum dikembalikan dan mengirimkan push notification ke user yang meminjam.

## Fitur
- ✅ Cron job yang berjalan setiap hari jam 9 pagi (WIB)
- ✅ Mengecek buku yang terlambat dikembalikan
- ✅ Mengirim peringatan 3, 2, dan 1 hari sebelum jatuh tempo
- ✅ Push notification via Firebase Cloud Messaging (FCM)
- ✅ Menyimpan notifikasi di Firestore untuk in-app display
- ✅ Automatic FCM token management

## Arsitektur

### Firebase Functions
- **dailyBookReminderCheck**: Scheduled function yang berjalan setiap hari jam 9 pagi
- **manualBookReminderCheck**: HTTP function untuk testing manual

### Android App
- **FCM Token Management**: Otomatis update token saat login dan app start
- **Push Notification Handling**: Menangani notifikasi dari Firebase
- **In-App Notifications**: Menampilkan notifikasi dalam aplikasi

## Setup dan Deployment

### 1. Install Firebase CLI
```bash
npm install -g firebase-tools
firebase login
```

### 2. Deploy Functions
```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

### 3. Verify Deployment
Setelah deploy, Anda akan mendapat URL seperti:
- Scheduled function: `dailyBookReminderCheck` (otomatis berjalan)
- Manual trigger: `https://asia-southeast2-[PROJECT-ID].cloudfunctions.net/manualBookReminderCheck`

## Testing

### 1. Manual Testing
Untuk test manual, buka URL function di browser atau gunakan curl:
```bash
curl https://asia-southeast2-[PROJECT-ID].cloudfunctions.net/manualBookReminderCheck
```

### 2. Test dengan Data Dummy
1. Buat transaksi dengan status "sedang dipinjam"
2. Set borrowDate beberapa hari yang lalu
3. Jalankan manual trigger
4. Cek apakah notifikasi diterima

### 3. Monitoring
- Firebase Console > Functions > Logs
- Firebase Console > Cloud Messaging > Reports

## Konfigurasi

### Periode Peminjaman
Di file `functions/src/services/notificationService.ts`:
```typescript
const LOAN_PERIOD_DAYS = 7; // 7 hari periode peminjaman
const WARNING_DAYS_BEFORE = [3, 2, 1]; // Peringatan 3, 2, 1 hari sebelum
```

### Jadwal Cron
Di file `functions/src/index.ts`:
```typescript
.schedule('0 9 * * *') // Setiap hari jam 9 pagi
.timeZone('Asia/Jakarta')
```

## Data Structure

### Transaction (Firestore)
```javascript
{
  id: "transaction_id",
  nameUser: "Nama User",
  title: "Judul Buku",
  author: "Penulis",
  borrowDate: "dd/MM/yyyy",
  returnDate: "dd/MM/yyyy",
  status: "sedang dipinjam",
  userId: "user_id",
  bookId: "book_id"
}
```

### User (Firestore)
```javascript
{
  id: "user_id",
  name: "Nama User",
  email: "email@example.com",
  fcmToken: "fcm_token_string" // Otomatis diupdate
}
```

### Notification (Firestore)
```javascript
{
  id: "notification_id",
  userId: "user_id",
  title: "Judul Notifikasi",
  message: "Pesan notifikasi",
  type: "overdue" | "return_reminder" | "general",
  transactionId: "transaction_id",
  isRead: false,
  createdAt: Timestamp
}
```

## Troubleshooting

### 1. FCM Token Tidak Tersimpan
- Pastikan user sudah login
- Cek permission notifikasi di Android
- Lihat log di Android Studio

### 2. Notifikasi Tidak Diterima
- Cek FCM token di Firestore
- Pastikan app tidak di-kill oleh sistem
- Cek Firebase Console > Cloud Messaging

### 3. Function Error
- Lihat logs di Firebase Console
- Pastikan Firestore rules mengizinkan read/write
- Cek format data di Firestore

### 4. Timezone Issues
- Function menggunakan timezone Asia/Jakarta
- Pastikan borrowDate format dd/MM/yyyy

## Security Rules

Pastikan Firestore rules mengizinkan:
```javascript
// Allow functions to read/write notifications
match /notifications/{document} {
  allow read, write: if request.auth != null;
}

// Allow functions to read transactions
match /transactions/{document} {
  allow read: if request.auth != null;
}

// Allow functions to update user FCM tokens
match /users/{userId} {
  allow update: if request.auth != null && request.auth.uid == userId;
}
```

## Monitoring dan Analytics

### Metrics yang Dipantau
- Jumlah notifikasi terkirim per hari
- Success rate pengiriman FCM
- Jumlah buku terlambat
- Response time function

### Logs
- Function execution logs
- FCM delivery reports
- Error tracking

## Maintenance

### Regular Tasks
1. Monitor function performance
2. Update dependencies secara berkala
3. Cleanup old notifications (optional)
4. Review dan optimize query performance

### Scaling Considerations
- Function timeout: 540 detik (default)
- Concurrent executions: 1000 (default)
- Memory: 256MB (default)

Untuk load yang lebih besar, pertimbangkan:
- Batch processing
- Pagination untuk query besar
- Increase function resources