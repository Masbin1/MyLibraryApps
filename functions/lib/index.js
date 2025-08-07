"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.manualBookReminderCheck = exports.dailyBookReminderCheck = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
// Initialize Firebase Admin
admin.initializeApp();
// Import services
const notificationService_1 = require("./services/notificationService");
// Scheduled function that runs daily at 9 AM Jakarta time (UTC+7)
exports.dailyBookReminderCheck = functions
    .region('asia-southeast2') // Jakarta region
    .pubsub
    .schedule('0 9 * * *') // Every day at 9 AM
    .timeZone('Asia/Jakarta')
    .onRun(async (context) => {
    console.log('Starting daily book reminder check...');
    try {
        await (0, notificationService_1.checkOverdueBooks)();
        console.log('Daily book reminder check completed successfully');
        return null;
    }
    catch (error) {
        console.error('Error in daily book reminder check:', error);
        throw error;
    }
});
// Manual trigger function for testing
exports.manualBookReminderCheck = functions
    .region('asia-southeast2')
    .https
    .onRequest(async (req, res) => {
    console.log('Manual book reminder check triggered');
    try {
        await (0, notificationService_1.checkOverdueBooks)();
        res.status(200).json({
            success: true,
            message: 'Book reminder check completed successfully'
        });
    }
    catch (error) {
        console.error('Error in manual book reminder check:', error);
        res.status(500).json({
            success: false,
            error: error instanceof Error ? error.message : 'Unknown error'
        });
    }
});
//# sourceMappingURL=index.js.map