import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin
admin.initializeApp();

// Import services
import { checkOverdueBooks, debugTransactionData } from './services/notificationService';

// Scheduled function that runs daily at 7 AM Jakarta time (UTC+7)
export const dailyBookReminderCheck = functions
  .region('asia-southeast2') // Jakarta region
  .pubsub
  .schedule('0 7 * * *') // Every day at 7 AM
  .timeZone('Asia/Jakarta')
  .onRun(async (context) => {
    console.log('Starting daily book reminder check...');
    
    try {
      await checkOverdueBooks();
      console.log('Daily book reminder check completed successfully');
      return null;
    } catch (error) {
      console.error('Error in daily book reminder check:', error);
      throw error;
    }
  });

// Manual trigger function for testing
export const manualBookReminderCheck = functions
  .region('asia-southeast2')
  .https
  .onRequest(async (req, res) => {
    console.log('🔥 Manual book reminder check triggered');
    
    try {
      await checkOverdueBooks();
      res.status(200).json({ 
        success: true, 
        message: 'Book reminder check completed successfully' 
      });
    } catch (error) {
      console.error('❌ Error in manual book reminder check:', error);
      res.status(500).json({ 
        success: false, 
        error: error instanceof Error ? error.message : 'Unknown error'
      });
    }
  });

// Debug function to check transaction data and run overdue check
export const debugTransactions = functions
  .region('asia-southeast2')
  .https
  .onRequest(async (req, res) => {
    console.log('🔍 Debug transactions triggered');

    try {
      // First run debug to show current data
      await debugTransactionData();

      // Then run the overdue check to update fines and send notifications
      console.log('\n🔄 Running overdue check...');
      await checkOverdueBooks();

      res.status(200).json({
        success: true,
        message: 'Debug and overdue check completed successfully'
      });
    } catch (error) {
      console.error('❌ Error in debug transactions:', error);
      res.status(500).json({
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error'
      });
    }
  });