# 🔍 Debugging Notifications Guide

## 🚨 Problem: Notifikasi Tidak Muncul Padahal Ada Buku Terlambat

### **Step 1: Deploy Updated Functions**
```bash
cd functions
npm run build
firebase deploy --only functions
```

### **Step 2: Debug Transaction Data**
1. **Buka Notification Test Activity**
2. **Klik "🔍 Debug Transactions & Check Notifications"**
3. **Check Logcat** untuk melihat:
   - Format tanggal transaksi
   - Status transaksi
   - Data user dan FCM token

### **Step 3: Check Firebase Console Logs**
1. **Buka Firebase Console**
2. **Go to Functions → Logs**
3. **Look for detailed logs** dari `debugTransactions` dan `manualBookReminderCheck`

## 🔍 Common Issues & Solutions

### **Issue 1: Format Tanggal Salah**
**Symptoms:**
```
❌ Invalid date format: 2024-01-23T10:30:00Z
❌ Unsupported date format: 1706012200000
```

**Solution:**
- Functions expects: `dd/MM/yyyy` (e.g., "23/01/2024")
- Or: `yyyy-MM-dd` (e.g., "2024-01-23")

**Fix in Android App:**
```kotlin
// When creating transaction, use correct format
val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
val borrowDate = dateFormat.format(Date())
```

### **Issue 2: Status Transaksi Salah**
**Symptoms:**
```
❌ No active transactions found with status "sedang dipinjam"
📋 Sample transaction statuses:
  - abc123: status="dipinjam", title="Test Book"
```

**Solution:**
- Functions looks for: `"sedang dipinjam"`
- Make sure transaction status is exactly: `"sedang dipinjam"`

### **Issue 3: FCM Token Missing**
**Symptoms:**
```
❌ No FCM token for user John Doe. User needs to update token.
```

**Solution:**
1. **Update FCM Token** di app:
   - Klik "🔄 Update FCM Token"
2. **Check Firestore** users collection:
   - Field `fcmToken` harus ada dan valid

### **Issue 4: Calculation Error**
**Symptoms:**
```
⚠️ Warning: Negative days since borrow. Check date format!
⏰ Days since borrow: -5
⏰ Days remaining: 12
```

**Solution:**
- Check date parsing logic
- Ensure current date vs borrow date calculation is correct

## 📋 Expected Debug Output

### **Successful Transaction Processing:**
```
🔍 Starting checkOverdueBooks function...
📅 Current date: 2024-01-23T10:30:00.000Z
📅 Current date (Jakarta): 23/01/2024 17:30:00

✅ Found 3 active transactions

📚 Processing transaction: abc123
📖 Book: "Android Development" by John Smith
👤 User: Jane Doe (user123)
📅 Borrow Date: 15/01/2024
📅 Return Date: 22/01/2024
📊 Status: sedang dipinjam

📅 Parsed borrow date: 2024-01-15T00:00:00.000Z
⏰ Days since borrow: 8
⏰ Days remaining: -1
🚨 OVERDUE: Book is 1 days overdue

👤 User found: Jane Doe (jane@example.com)
🔑 FCM Token: Present
📤 Adding notification to queue for Jane Doe

📤 NOTIFICATION SENDING SUMMARY:
📊 Total notifications to send: 1

📱 Sending notification:
  📝 Title: 📚 Buku Terlambat!
  💬 Body: Buku "Android Development" sudah terlambat 1 hari. Segera kembalikan!
  📚 Book: Android Development
  🔑 Token: eGxR7_abc123...

✅ Notification sent successfully for "Android Development"
📋 Message ID: projects/myproject/messages/0:1706012200000000%abc123

📊 FINAL RESULTS:
✅ Successful: 1
❌ Failed: 0
📊 Total: 1
```

## 🛠️ Debugging Steps

### **Step 1: Check Transaction Data**
```bash
# Call debug function
curl "https://asia-southeast2-PROJECT_ID.cloudfunctions.net/debugTransactions"
```

**Expected Output:**
```
🔍 DEBUG: Checking transaction data...
📊 Found 5 transactions

📋 Transaction #1:
  🆔 ID: abc123
  📚 Title: Android Development
  👤 User: Jane Doe (user123)
  📊 Status: "sedang dipinjam"
  📅 Borrow Date: "15/01/2024"
  📅 Return Date: "22/01/2024"
  📖 Author: John Smith
  ✅ Date format: dd/MM/yyyy (supported)
```

### **Step 2: Test Manual Trigger**
```bash
# Call manual trigger
curl "https://asia-southeast2-PROJECT_ID.cloudfunctions.net/manualBookReminderCheck"
```

### **Step 3: Check FCM Service**
**Android Logcat:**
```
FCMService: From: /topics/...
FCMService: Message data payload: {type=overdue, bookTitle=Android Development, daysOverdue=1}
FCMService: Message Notification Body: Buku "Android Development" sudah terlambat 1 hari...
```

## 🎯 Testing Scenarios

### **Create Test Data with Correct Format:**
```kotlin
// In NotificationTestHelper
suspend fun createTestTransactionWithCorrectFormat() {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Create overdue transaction (10 days ago)
    val borrowCalendar = Calendar.getInstance()
    borrowCalendar.add(Calendar.DAY_OF_YEAR, -10)
    val borrowDate = dateFormat.format(borrowCalendar.time)
    
    val transaction = mapOf(
        "userId" to currentUser.uid,
        "nameUser" to currentUser.displayName,
        "title" to "Test Overdue Book",
        "author" to "Test Author",
        "borrowDate" to borrowDate, // Format: dd/MM/yyyy
        "status" to "sedang dipinjam", // Exact status
        // ... other fields
    )
}
```

## ✅ Checklist for Working Notifications

- [ ] **Functions Deployed**: `firebase deploy --only functions`
- [ ] **Correct Date Format**: `dd/MM/yyyy` or `yyyy-MM-dd`
- [ ] **Correct Status**: `"sedang dipinjam"`
- [ ] **FCM Token Present**: User has valid FCM token
- [ ] **Notification Permission**: Android permission granted
- [ ] **Internet Connection**: Device connected to internet
- [ ] **Firebase Project**: Correct project ID in functions URL

## 🚀 Quick Fix Commands

```bash
# 1. Redeploy functions
cd functions && firebase deploy --only functions

# 2. Test debug function
curl "https://asia-southeast2-PROJECT_ID.cloudfunctions.net/debugTransactions"

# 3. Test manual trigger
curl "https://asia-southeast2-PROJECT_ID.cloudfunctions.net/manualBookReminderCheck"

# 4. Check function logs
firebase functions:log
```

## 📱 Android App Testing

1. **Create Test Transaction**: Klik "⚠️ Create Overdue Transaction (Firebase)"
2. **Update FCM Token**: Klik "🔄 Update FCM Token"  
3. **Debug Data**: Klik "🔍 Debug Transactions & Check Notifications"
4. **Trigger Functions**: Klik "🔥 Test Firebase Functions Manual Trigger"
5. **Check Results**: Swipe down notification panel

---

**Follow these steps and your notifications should work! 🚀📱**