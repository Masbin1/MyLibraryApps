"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.checkOverdueBooks = void 0;
const admin = require("firebase-admin");
const LOAN_PERIOD_DAYS = 7; // 7 days loan period
const WARNING_DAYS_BEFORE = [3, 2, 1]; // Send warnings 3, 2, and 1 days before due
async function checkOverdueBooks() {
    const db = admin.firestore();
    const messaging = admin.messaging();
    console.log('Fetching active transactions...');
    try {
        // Get all active transactions (books that are currently borrowed)
        const transactionsSnapshot = await db
            .collection('transactions')
            .where('status', '==', 'sedang dipinjam')
            .get();
        if (transactionsSnapshot.empty) {
            console.log('No active transactions found');
            return;
        }
        console.log(`Found ${transactionsSnapshot.size} active transactions`);
        const currentDate = new Date();
        const notifications = [];
        // Process each transaction
        for (const doc of transactionsSnapshot.docs) {
            const transaction = Object.assign({ id: doc.id }, doc.data());
            try {
                // Parse borrow date (format: dd/MM/yyyy)
                const [day, month, year] = transaction.borrowDate.split('/');
                const borrowDate = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
                // Calculate days since borrow
                const timeDiff = currentDate.getTime() - borrowDate.getTime();
                const daysSinceBorrow = Math.floor(timeDiff / (1000 * 3600 * 24));
                // Calculate days remaining until due
                const daysRemaining = LOAN_PERIOD_DAYS - daysSinceBorrow;
                console.log(`Transaction ${transaction.id}: ${transaction.title} by ${transaction.nameUser}`);
                console.log(`Days since borrow: ${daysSinceBorrow}, Days remaining: ${daysRemaining}`);
                // Get user's FCM token
                const userDoc = await db.collection('users').doc(transaction.userId).get();
                if (!userDoc.exists) {
                    console.log(`User ${transaction.userId} not found`);
                    continue;
                }
                const user = Object.assign({ id: userDoc.id }, userDoc.data());
                if (!user.fcmToken) {
                    console.log(`No FCM token for user ${user.name}`);
                    continue;
                }
                let notificationData = null;
                if (daysRemaining < 0) {
                    // Book is overdue
                    const daysOverdue = Math.abs(daysRemaining);
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
                }
                else if (WARNING_DAYS_BEFORE.includes(daysRemaining)) {
                    // Send warning notification
                    let warningMessage = '';
                    if (daysRemaining === 3) {
                        warningMessage = `Buku "${transaction.title}" harus dikembalikan dalam 3 hari lagi.`;
                    }
                    else if (daysRemaining === 2) {
                        warningMessage = `Buku "${transaction.title}" harus dikembalikan dalam 2 hari lagi.`;
                    }
                    else if (daysRemaining === 1) {
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
                }
                if (notificationData) {
                    notifications.push(Object.assign({ token: user.fcmToken }, notificationData));
                }
            }
            catch (error) {
                console.error(`Error processing transaction ${transaction.id}:`, error);
            }
        }
        // Send all notifications
        if (notifications.length > 0) {
            console.log(`Sending ${notifications.length} notifications...`);
            for (const notif of notifications) {
                try {
                    await messaging.send({
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
                    console.log(`Notification sent successfully for ${notif.data.bookTitle}`);
                }
                catch (error) {
                    console.error(`Failed to send notification for ${notif.data.bookTitle}:`, error);
                    // If token is invalid, remove it from user document
                    if (error instanceof Error && 'code' in error && error.code === 'messaging/registration-token-not-registered') {
                        try {
                            await db.collection('users').doc(notif.data.userId).update({
                                fcmToken: admin.firestore.FieldValue.delete()
                            });
                            console.log(`Removed invalid FCM token for user ${notif.data.userId}`);
                        }
                        catch (updateError) {
                            console.error(`Failed to remove invalid token:`, updateError);
                        }
                    }
                }
            }
        }
        else {
            console.log('No notifications to send');
        }
    }
    catch (error) {
        console.error('Error in checkOverdueBooks:', error);
        throw error;
    }
}
exports.checkOverdueBooks = checkOverdueBooks;
async function saveNotificationToFirestore(db, userId, notificationData) {
    try {
        await db.collection('notifications').add(Object.assign({ userId }, notificationData));
        console.log(`Notification saved to Firestore for user ${userId}`);
    }
    catch (error) {
        console.error(`Failed to save notification to Firestore:`, error);
    }
}
//# sourceMappingURL=notificationService.js.map