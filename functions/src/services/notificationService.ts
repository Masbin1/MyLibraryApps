import * as admin from 'firebase-admin';

interface Transaction {
  id: string;
  nameUser: string;
  title: string;
  author: string;
  borrowDate: string;
  returnDate: string;
  status: string;
  userId: string;
  bookId: string;
}

interface User {
  id: string;
  fcmToken?: string;
  name: string;
  email: string;
}

const LOAN_PERIOD_DAYS = 7; // 7 days loan period
const WARNING_DAYS_BEFORE = [3, 2, 1]; // Send warnings 3, 2, and 1 days before due

export async function checkOverdueBooks(): Promise<void> {
  const db = admin.firestore();
  const messaging = admin.messaging();
  
  console.log('🔍 Starting checkOverdueBooks function...');
  console.log(`📅 Current date: ${new Date().toISOString()}`);
  console.log(`📅 Current date (Jakarta): ${new Date().toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' })}`);
  
  try {
    // Get all active transactions (books that are currently borrowed)
    console.log('📋 Fetching active transactions...');
    const transactionsSnapshot = await db
      .collection('transactions')
      .where('status', '==', 'sedang dipinjam')
      .get();
    
    if (transactionsSnapshot.empty) {
      console.log('❌ No active transactions found with status "sedang dipinjam"');
      
      // Debug: Check if there are any transactions at all
      const allTransactions = await db.collection('transactions').limit(5).get();
      console.log(`🔍 Total transactions in database: ${allTransactions.size}`);
      if (!allTransactions.empty) {
        console.log('📋 Sample transaction statuses:');
        allTransactions.docs.forEach(doc => {
          const data = doc.data();
          console.log(`  - ${doc.id}: status="${data.status}", title="${data.title}"`);
        });
      }
      return;
    }
    
    console.log(`✅ Found ${transactionsSnapshot.size} active transactions`);
    
    const currentDate = new Date();
    const notifications: Array<{
      token: string;
      notification: admin.messaging.Notification;
      data: { [key: string]: string };
    }> = [];
    
    // Process each transaction
    for (const doc of transactionsSnapshot.docs) {
      const transaction = { id: doc.id, ...doc.data() } as Transaction;
      
      console.log(`\n📚 Processing transaction: ${transaction.id}`);
      console.log(`📖 Book: "${transaction.title}" by ${transaction.author}`);
      console.log(`👤 User: ${transaction.nameUser} (${transaction.userId})`);
      console.log(`📅 Borrow Date: ${transaction.borrowDate}`);
      console.log(`📅 Return Date: ${transaction.returnDate || 'Not set'}`);
      console.log(`📊 Status: ${transaction.status}`);
      
      try {
        // Parse borrow date - handle multiple formats
        let borrowDate: Date;
        
        if (!transaction.borrowDate) {
          console.log(`❌ No borrow date found for transaction ${transaction.id}`);
          continue;
        }
        
        // Try different date formats
        if (transaction.borrowDate.includes('/')) {
          // Format: dd/MM/yyyy or MM/dd/yyyy
          const parts = transaction.borrowDate.split('/');
          if (parts.length !== 3) {
            console.log(`❌ Invalid date format: ${transaction.borrowDate}`);
            continue;
          }
          
          // Assume dd/MM/yyyy format first
          const [day, month, year] = parts;
          borrowDate = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
          
          // Validate date
          if (isNaN(borrowDate.getTime())) {
            console.log(`❌ Invalid date: ${transaction.borrowDate}`);
            continue;
          }
        } else if (transaction.borrowDate.includes('-')) {
          // Format: yyyy-MM-dd
          borrowDate = new Date(transaction.borrowDate);
        } else {
          console.log(`❌ Unsupported date format: ${transaction.borrowDate}`);
          continue;
        }
        
        console.log(`📅 Parsed borrow date: ${borrowDate.toISOString()}`);
        console.log(`📅 Parsed borrow date (Jakarta): ${borrowDate.toLocaleString('id-ID', { timeZone: 'Asia/Jakarta' })}`);
        
        // Calculate days since borrow
        const timeDiff = currentDate.getTime() - borrowDate.getTime();
        const daysSinceBorrow = Math.floor(timeDiff / (1000 * 3600 * 24));
        
        // Calculate days remaining until due
        const daysRemaining = LOAN_PERIOD_DAYS - daysSinceBorrow;
        
        console.log(`⏰ Days since borrow: ${daysSinceBorrow}`);
        console.log(`⏰ Days remaining: ${daysRemaining}`);
        console.log(`📋 Loan period: ${LOAN_PERIOD_DAYS} days`);
        
        if (daysSinceBorrow < 0) {
          console.log(`⚠️ Warning: Negative days since borrow. Check date format!`);
        }
        
        // Get user's FCM token
        console.log(`👤 Fetching user data for: ${transaction.userId}`);
        const userDoc = await db.collection('users').doc(transaction.userId).get();
        if (!userDoc.exists) {
          console.log(`❌ User ${transaction.userId} not found in users collection`);
          continue;
        }
        
        const user = { id: userDoc.id, ...userDoc.data() } as User;
        console.log(`👤 User found: ${user.name} (${user.email})`);
        console.log(`🔑 FCM Token: ${user.fcmToken ? 'Present' : 'Missing'}`);
        
        if (!user.fcmToken) {
          console.log(`❌ No FCM token for user ${user.name}. User needs to update token.`);
          continue;
        }
        
        let notificationData: {
          notification: admin.messaging.Notification;
          data: { [key: string]: string };
        } | null = null;
        
        // Determine notification type based on days remaining
        if (daysRemaining < 0) {
          // Book is overdue
          const daysOverdue = Math.abs(daysRemaining);
          console.log(`🚨 OVERDUE: Book is ${daysOverdue} days overdue`);
          
          notificationData = {
            notification: {
              title: '📚 Buku Terlambat!',
              body: `Buku "${transaction.title}" sudah terlambat ${daysOverdue} hari. Segera kembalikan!`
            },
            data: {
              type: 'overdue',
              bookTitle: transaction.title,
              author: transaction.author,
              daysOverdue: daysOverdue.toString(),
              transactionId: transaction.id,
              userId: transaction.userId
            }
          };
          
          // Also save notification to Firestore for in-app display
          await saveNotificationToFirestore(db, transaction.userId, {
            title: '📚 Buku Terlambat!',
            message: `Buku "${transaction.title}" sudah terlambat ${daysOverdue} hari. Segera kembalikan!`,
            type: 'overdue',
            transactionId: transaction.id,
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          });
          
        } else if (WARNING_DAYS_BEFORE.includes(daysRemaining)) {
          // Send warning notification
          console.log(`⚠️ REMINDER: ${daysRemaining} days remaining`);
          
          let warningMessage = '';
          if (daysRemaining === 3) {
            warningMessage = `Buku "${transaction.title}" harus dikembalikan dalam 3 hari lagi.`;
          } else if (daysRemaining === 2) {
            warningMessage = `Buku "${transaction.title}" harus dikembalikan dalam 2 hari lagi.`;
          } else if (daysRemaining === 1) {
            warningMessage = `Buku "${transaction.title}" harus dikembalikan besok!`;
          }
          
          notificationData = {
            notification: {
              title: '📚 Reminder Pengembalian',
              body: warningMessage
            },
            data: {
              type: 'return_reminder',
              bookTitle: transaction.title,
              author: transaction.author,
              daysRemaining: daysRemaining.toString(),
              transactionId: transaction.id,
              userId: transaction.userId
            }
          };
          
          // Save notification to Firestore
          await saveNotificationToFirestore(db, transaction.userId, {
            title: '📚 Reminder Pengembalian',
            message: warningMessage,
            type: 'return_reminder',
            transactionId: transaction.id,
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
          });
        } else {
          console.log(`✅ No notification needed. Days remaining: ${daysRemaining}`);
        }
        
        if (notificationData) {
          console.log(`📤 Adding notification to queue for ${user.name}`);
          notifications.push({
            token: user.fcmToken,
            ...notificationData
          });
        }
        
      } catch (error) {
        console.error(`Error processing transaction ${transaction.id}:`, error);
      }
    }
    
    // Send all notifications
    console.log(`\n📤 NOTIFICATION SENDING SUMMARY:`);
    console.log(`📊 Total notifications to send: ${notifications.length}`);
    
    if (notifications.length > 0) {
      console.log(`🚀 Sending ${notifications.length} notifications...`);
      
      let successCount = 0;
      let failureCount = 0;
      
      for (const notif of notifications) {
        try {
          console.log(`\n📱 Sending notification:`);
          console.log(`  📝 Title: ${notif.notification.title}`);
          console.log(`  💬 Body: ${notif.notification.body}`);
          console.log(`  📚 Book: ${notif.data.bookTitle}`);
          console.log(`  🔑 Token: ${notif.token.substring(0, 20)}...`);
          
          const result = await messaging.send({
            token: notif.token,
            notification: notif.notification,
            data: notif.data,
            android: {
              priority: 'high',
              notification: {
                channelId: 'library_notifications',
                priority: 'high',
                defaultSound: true,
                defaultVibrateTimings: true
              }
            }
          });
          
          console.log(`✅ Notification sent successfully for "${notif.data.bookTitle}"`);
          console.log(`📋 Message ID: ${result}`);
          successCount++;
          
        } catch (error) {
          console.error(`❌ Failed to send notification for "${notif.data.bookTitle}":`, error);
          failureCount++;
          
          // If token is invalid, remove it from user document
          if (error instanceof Error && 'code' in error) {
            console.log(`🔍 Error code: ${error.code}`);
            
            if (error.code === 'messaging/registration-token-not-registered' || 
                error.code === 'messaging/invalid-registration-token') {
              try {
                await db.collection('users').doc(notif.data.userId).update({
                  fcmToken: admin.firestore.FieldValue.delete()
                });
                console.log(`🧹 Removed invalid FCM token for user ${notif.data.userId}`);
              } catch (updateError) {
                console.error(`❌ Failed to remove invalid token:`, updateError);
              }
            }
          }
        }
      }
      
      console.log(`\n📊 FINAL RESULTS:`);
      console.log(`✅ Successful: ${successCount}`);
      console.log(`❌ Failed: ${failureCount}`);
      console.log(`📊 Total: ${notifications.length}`);
      
    } else {
      console.log('❌ No notifications to send');
      console.log('💡 Possible reasons:');
      console.log('  - No transactions match notification criteria');
      console.log('  - Users have no FCM tokens');
      console.log('  - All notifications already sent today');
    }
    
  } catch (error) {
    console.error('Error in checkOverdueBooks:', error);
    throw error;
  }
}

async function saveNotificationToFirestore(
  db: admin.firestore.Firestore,
  userId: string,
  notificationData: any
): Promise<void> {
  try {
    await db.collection('notifications').add({
      userId,
      ...notificationData
    });
    console.log(`💾 Notification saved to Firestore for user ${userId}`);
  } catch (error) {
    console.error(`❌ Failed to save notification to Firestore:`, error);
  }
}

// Debug function to check transaction data
export async function debugTransactionData(): Promise<void> {
  const db = admin.firestore();
  
  console.log('🔍 DEBUG: Checking transaction data...');
  
  try {
    const snapshot = await db.collection('transactions').limit(10).get();
    
    console.log(`📊 Found ${snapshot.size} transactions`);
    
    snapshot.docs.forEach((doc, index) => {
      const data = doc.data();
      console.log(`\n📋 Transaction #${index + 1}:`);
      console.log(`  🆔 ID: ${doc.id}`);
      console.log(`  📚 Title: ${data.title}`);
      console.log(`  👤 User: ${data.nameUser} (${data.userId})`);
      console.log(`  📊 Status: "${data.status}"`);
      console.log(`  📅 Borrow Date: "${data.borrowDate}"`);
      console.log(`  📅 Return Date: "${data.returnDate}"`);
      console.log(`  📖 Author: ${data.author}`);
      
      // Check date format
      if (data.borrowDate) {
        if (data.borrowDate.includes('/')) {
          console.log(`  ✅ Date format: dd/MM/yyyy (supported)`);
        } else if (data.borrowDate.includes('-')) {
          console.log(`  ✅ Date format: yyyy-MM-dd (supported)`);
        } else {
          console.log(`  ❌ Date format: Unknown (${data.borrowDate})`);
        }
      } else {
        console.log(`  ❌ No borrow date found`);
      }
    });
    
  } catch (error) {
    console.error('❌ Error in debugTransactionData:', error);
  }
}