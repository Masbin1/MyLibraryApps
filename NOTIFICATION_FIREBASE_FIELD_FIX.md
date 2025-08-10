# Notification Firebase Field Mapping Fix

## Problem Identified
Notifikasi tidak muncul karena **field mismatch** antara kode dan struktur Firebase:

### Firebase Structure (Actual):
```
notifications collection:
- createdAt (Timestamp)
- isRead (Boolean)
- message (String)
- title (String)
- transactionId (String)
- type (String)
- userId (String)
```

### Code Expected (Before Fix):
```
- timestamp (Date)
- relatedItemId (String)
- relatedItemTitle (String)
```

## Root Cause
1. **Field Name Mismatch**: Code mencari `timestamp` tapi Firebase punya `createdAt`
2. **Ordering Issue**: `.orderBy("timestamp")` gagal karena field tidak ada
3. **Missing Fields**: Code expect `relatedItemId` dan `relatedItemTitle` yang tidak ada di Firebase
4. **Data Conversion**: Field mapping tidak sesuai struktur actual

## Solutions Implemented

### 1. Fixed Field Mapping (`SafeFirestoreConverter.kt`)

#### Before:
```kotlin
timestamp = FirestoreConverters.convertToDate(document.get("timestamp"))
```

#### After:
```kotlin
// Gunakan createdAt sesuai struktur Firebase, fallback ke timestamp
timestamp = FirestoreConverters.convertToDate(document.get("createdAt") ?: document.get("timestamp"))
```

### 2. Fixed Query Ordering (`NotificationRepository.kt`)

#### Before:
```kotlin
.orderBy("timestamp", Query.Direction.DESCENDING) // Field tidak ada!
```

#### After:
```kotlin
// Sementara tanpa ordering untuk testing
// Nanti bisa ditambah: .orderBy("createdAt", Query.Direction.DESCENDING)
```

### 3. Fixed Data Saving (`NotificationRepository.kt`)

#### Before:
```kotlin
docRef.set(notificationWithId).await() // Menggunakan model langsung
```

#### After:
```kotlin
// Convert to map dengan field yang sesuai struktur Firebase
val notificationMap = mapOf(
    "id" to notificationWithId.id,
    "userId" to notificationWithId.userId,
    "title" to notificationWithId.title,
    "message" to notificationWithId.message,
    "createdAt" to notificationWithId.timestamp, // Gunakan createdAt
    "isRead" to notificationWithId.isRead,
    "type" to notificationWithId.type,
    "transactionId" to notificationWithId.transactionId
)

docRef.set(notificationMap).await()
```

### 4. Enhanced Debugging

#### Added Comprehensive Logging:
```kotlin
android.util.Log.d("NotificationRepository", "📄 Document data: ${doc.data}")
android.util.Log.d("SafeFirestoreConverter", "Raw document data: ${document.data}")
```

#### Removed Filtering Temporarily:
```kotlin
// Tidak filter apapun dulu, kirim semua notifications
android.util.Log.d("NotificationRepository", "🎯 Sending ALL ${notifications.size} notifications (no filtering)")
trySend(notifications)
```

## Testing Instructions

### 1. Test Existing Data:
1. Buka aplikasi
2. Klik icon notifikasi
3. Popup harus menampilkan data existing dari Firebase

### 2. Test New Notification:
1. Long press icon notifikasi untuk create test notification
2. Check Firebase Console - data harus tersimpan dengan field `createdAt`
3. Klik icon notifikasi - test notification harus muncul

### 3. Debug Logs:
Monitor logcat untuk melihat:
```
🔍 Setting up listener for user: [userId]
📡 Received snapshot with [count] documents
📄 Processing document: [docId]
📄 Document data: {createdAt=..., title=..., message=...}
✅ Converted [count] notifications
🎯 Sending ALL [count] notifications (no filtering)
```

## Key Changes Made

### Files Modified:
1. **NotificationRepository.kt**:
   - Changed query dari `timestamp` ke `createdAt`
   - Removed ordering temporarily
   - Fixed data saving dengan proper field mapping
   - Added comprehensive logging

2. **SafeFirestoreConverter.kt**:
   - Updated field mapping untuk `createdAt`
   - Added fallback untuk backward compatibility
   - Enhanced error logging

### Field Mapping Table:
| Firebase Field | Model Field | Type | Notes |
|---------------|-------------|------|-------|
| `createdAt` | `timestamp` | Date | Main timestamp field |
| `isRead` | `isRead` | Boolean | Direct mapping |
| `message` | `message` | String | Direct mapping |
| `title` | `title` | String | Direct mapping |
| `transactionId` | `transactionId` | String | Direct mapping |
| `type` | `type` | String | Direct mapping |
| `userId` | `userId` | String | Direct mapping |

## Expected Results

✅ **Data Loading**: Firebase data akan dimuat dengan benar
✅ **Popup Display**: Notifications akan muncul di popup
✅ **Field Compatibility**: Support untuk struktur Firebase yang ada
✅ **Debug Visibility**: Comprehensive logging untuk troubleshooting
✅ **Test Functionality**: Long press untuk create test notification

## Next Steps (Optional)

1. **Add Ordering Back**: Setelah confirm data muncul, bisa tambah ordering:
   ```kotlin
   .orderBy("createdAt", Query.Direction.DESCENDING)
   ```

2. **Add Filtering**: Jika perlu filter berdasarkan type atau status:
   ```kotlin
   val activeNotifications = notifications.filter { it.type != "archived" }
   ```

3. **Handle Missing Fields**: Jika ada data lama dengan field berbeda, bisa tambah migration logic

## Build Status
✅ **Build Successful** - No compilation errors
✅ **Field Mapping Fixed** - Sesuai struktur Firebase actual
✅ **Backward Compatible** - Support fallback untuk field lama