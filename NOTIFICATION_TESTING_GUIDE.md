# 🧪 Panduan Testing Sistem Notifikasi Firebase Functions

## 🔍 Mengapa Notifikasi Tidak Muncul?

### Masalah Umum:

1. **❌ Format Data Salah**
   - Status transaksi harus `"sedang dipinjam"` (bukan `"Dipinjam"`)
   - Format tanggal harus `"dd/MM/yyyy"` (bukan `"yyyy-MM-dd"`)

2. **❌ FCM Token Tidak Valid**
   - Token belum tersimpan di Firestore
   - Token expired atau invalid

3. **❌ Firebase Functions Belum Di-deploy**
   - Functions belum di-deploy ke Firebase
   - URL functions salah

4. **❌ Permission Notifikasi**
   - User belum memberikan permission notifikasi
   - App dalam mode Do Not Disturb

## 🚀 Langkah Testing yang Benar

### 1. Deploy Firebase Functions Dulu
```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

### 2. Update FCM Token
1. Buka app → Login
2. Buka NotificationTestActivity
3. Klik "Update FCM Token"
4. Cek di Firebase Console > Firestore > users > [user_id] → pastikan ada field `fcmToken`

### 3. Buat Test Transaction
1. Klik "Create Overdue Transaction" (untuk test buku terlambat)
2. Atau klik "Create 3-Day Reminder Transaction" (untuk test reminder)
3. Cek logcat untuk melihat detail transaksi yang dibuat

### 4. Trigger Firebase Functions
1. Klik "Test Firebase Functions"
2. Atau buka URL manual trigger di browser:
   ```
   https://asia-southeast2-[PROJECT-ID].cloudfunctions.net/manualBookReminderCheck
   ```

### 5. Cek Hasil
1. Klik "Check Firestore Notifications" untuk melihat data notifikasi
2. Cek notification bar Android
3. Lihat logcat untuk debug info

## 📱 Testing Scenarios

### Scenario 1: Test Buku Terlambat
```
1. Create Overdue Transaction (10 hari lalu)
2. Update FCM Token
3. Test Firebase Functions
4. Expected: Notifikasi "Buku Terlambat" muncul
```

### Scenario 2: Test Reminder 3 Hari
```
1. Create 3-Day Reminder Transaction (4 hari lalu)
2. Update FCM Token  
3. Test Firebase Functions
4. Expected: Notifikasi "Reminder 3 hari" muncul
```

### Scenario 3: Test Reminder 2 Hari
```
1. Create 2-Day Reminder Transaction (5 hari lalu)
2. Update FCM Token
3. Test Firebase Functions
4. Expected: Notifikasi "Reminder 2 hari" muncul
```

### Scenario 4: Test Reminder 1 Hari
```
1. Create 1-Day Reminder Transaction (6 hari lalu)
2. Update FCM Token
3. Test Firebase Functions
4. Expected: Notifikasi "Reminder 1 hari" muncul
```

## 🔧 Troubleshooting

### Problem: FCM Token Tidak Tersimpan
**Solution:**
1. Pastikan user sudah login
2. Cek permission notifikasi di Settings Android
3. Restart app setelah memberikan permission

### Problem: Firebase Functions Error
**Solution:**
1. Cek Firebase Console > Functions > Logs
2. Pastikan Firestore rules mengizinkan read/write
3. Cek format data di Firestore

### Problem: Notifikasi Tidak Muncul di Android
**Solution:**
1. Cek notification permission
2. Cek Do Not Disturb mode
3. Cek battery optimization settings
4. Test dengan local notification dulu

### Problem: Data Tidak Sesuai
**Solution:**
1. Cek format tanggal: `dd/MM/yyyy`
2. Cek status transaksi: `"sedang dipinjam"`
3. Cek userId sesuai dengan user yang login

## 📊 Monitoring dan Debug

### Logcat Tags untuk Monitoring:
```
NotificationTestHelper - Test helper logs
FCMService - Firebase messaging logs
MainActivity - FCM token updates
LoginActivity - FCM token updates
```

### Firebase Console Monitoring:
1. **Functions > Logs** - Execution logs
2. **Cloud Messaging > Reports** - Delivery reports
3. **Firestore > Data** - Check collections:
   - `transactions` - Test transactions
   - `notifications` - Generated notifications
   - `users` - FCM tokens

### Firestore Collections yang Dibuat:

#### `notifications` Collection:
```javascript
{
  id: "auto_generated",
  userId: "user_id",
  title: "📚 Buku Terlambat!",
  message: "Buku 'Test Book' sudah terlambat 3 hari...",
  type: "overdue", // atau "return_reminder"
  transactionId: "transaction_id",
  isRead: false,
  createdAt: Timestamp
}
```

## 🎯 Expected Results

### Untuk Buku Terlambat (> 7 hari):
- ✅ Push notification dengan title "📚 Buku Terlambat!"
- ✅ Data tersimpan di collection `notifications`
- ✅ Type: `"overdue"`

### Untuk Reminder (3, 2, 1 hari sebelum):
- ✅ Push notification dengan title "📚 Reminder Pengembalian"
- ✅ Data tersimpan di collection `notifications`
- ✅ Type: `"return_reminder"`

### Untuk Buku Normal (> 3 hari tersisa):
- ❌ Tidak ada notifikasi (sesuai logic)

## 🔄 Reset Testing Environment

### Clear Test Data:
1. Delete test transactions dari Firestore
2. Clear notifications collection
3. Clear notification_sent records
4. Reset FCM token

### Commands:
```javascript
// Di Firebase Console > Firestore
// Delete documents in collections:
- transactions (where title contains "Test Book")
- notifications (where userId = current_user)
- notification_sent (if exists)
```

## 📝 Testing Checklist

### Pre-Testing:
- [ ] Firebase Functions deployed
- [ ] User logged in
- [ ] FCM token updated
- [ ] Notification permission granted

### During Testing:
- [ ] Test transaction created
- [ ] Firebase Functions triggered
- [ ] Check logcat for errors
- [ ] Check Firebase Console logs

### Post-Testing:
- [ ] Notification received in Android
- [ ] Data saved in Firestore
- [ ] Clean up test data

## 🚨 Common Mistakes

1. **Menggunakan status "Dipinjam" instead of "sedang dipinjam"**
2. **Format tanggal salah (yyyy-MM-dd instead of dd/MM/yyyy)**
3. **FCM token tidak di-update setelah login**
4. **Firebase Functions belum di-deploy**
5. **Testing dengan data lama yang tidak sesuai format**

## 💡 Tips

1. **Selalu cek logcat** untuk debug info
2. **Test satu scenario per waktu** untuk isolasi masalah
3. **Gunakan Firebase Console** untuk monitoring
4. **Clear test data** setelah testing
5. **Test di device fisik** untuk hasil terbaik

---

**Remember**: Firebase Functions cron job berjalan setiap hari jam 9 pagi WIB. Untuk testing, gunakan manual trigger!