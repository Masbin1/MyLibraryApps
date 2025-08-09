package com.example.mylibraryapps.ui.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mylibraryapps.R
import com.example.mylibraryapps.utils.NotificationTestHelper
import com.example.mylibraryapps.service.NotificationForegroundService
import com.example.mylibraryapps.utils.AlarmScheduler
import kotlinx.coroutines.launch

class NotificationTestActivity : AppCompatActivity() {
    
    private lateinit var testHelper: NotificationTestHelper
    private var testTransactionId: String? = null
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_test)
        
        testHelper = NotificationTestHelper(this)
        
        // Check and request notification permission for Android 13+
        checkNotificationPermission()
        
        setupButtons()
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "✅ Notification permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ Notification permission denied. Notifications won't work.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setupButtons() {
        findViewById<Button>(R.id.btnTestImmediate).setOnClickListener {
            testHelper.testImmediateNotificationCheck()
            Toast.makeText(this, "Immediate notification check started", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnTestSystemNotification).setOnClickListener {
            testHelper.testWhatsAppStyleNotifications()
            Toast.makeText(this, "🚀 WhatsApp-style notifications sent! Check notification panel", Toast.LENGTH_LONG).show()
        }
        
        findViewById<Button>(R.id.btnCreate3DayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(3)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (3 days)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreate2DayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(2)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (2 days)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreate1DayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(1)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (1 day)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreateTodayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(0)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (today)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreateOverdueTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(-1)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (overdue)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnShowActiveTransactions).setOnClickListener {
            lifecycleScope.launch {
                testHelper.showActiveTransactions()
                Toast.makeText(this@NotificationTestActivity, "Check logcat for active transactions", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnClearNotificationRecords).setOnClickListener {
            lifecycleScope.launch {
                testHelper.clearNotificationSentRecords()
                testHelper.clearNotificationsFromFirebase()
                Toast.makeText(this@NotificationTestActivity, "All notification records cleared from Firebase", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnShowNotificationRecords).setOnClickListener {
            lifecycleScope.launch {
                testHelper.showNotificationSentRecords()
                testHelper.checkNotificationsInFirestore()
                Toast.makeText(this@NotificationTestActivity, "Check logcat for all notification records", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnUpdateFCMToken).setOnClickListener {
            lifecycleScope.launch {
                testHelper.updateFCMToken()
                Toast.makeText(this@NotificationTestActivity, "FCM token updated", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnDeleteTestTransaction).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId?.let { id ->
                    testHelper.deleteTestTransaction(id)
                    testTransactionId = null
                    Toast.makeText(this@NotificationTestActivity, "Test transaction deleted", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this@NotificationTestActivity, "No test transaction to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        findViewById<Button>(R.id.btnStartBackgroundService).setOnClickListener {
            NotificationForegroundService.startService(this)
            Toast.makeText(this, "Background service started", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnStopBackgroundService).setOnClickListener {
            NotificationForegroundService.stopService(this)
            Toast.makeText(this, "Background service stopped", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnScheduleAlarm).setOnClickListener {
            val alarmScheduler = AlarmScheduler(this)
            alarmScheduler.scheduleNotificationAlarm()
            Toast.makeText(this, "Alarm scheduled", Toast.LENGTH_SHORT).show()
        }
        
        // New buttons for Firebase Functions testing
        findViewById<Button>(R.id.btnTestFirebaseFunctions).setOnClickListener {
            lifecycleScope.launch {
                testHelper.testFirebaseFunctionsManualTrigger()
                Toast.makeText(this@NotificationTestActivity, "Firebase Functions triggered - check logcat", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCheckFirestoreNotifications).setOnClickListener {
            lifecycleScope.launch {
                testHelper.checkNotificationsInFirestore()
                Toast.makeText(this@NotificationTestActivity, "Check logcat for Firestore notifications", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreateOverdueTransaction).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransactionForFirebaseFunctions(10) // 10 days ago = 3 days overdue
                Toast.makeText(this@NotificationTestActivity, "Overdue transaction created", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreate3DayReminderTransaction).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransactionForFirebaseFunctions(4) // 4 days ago = 3 days remaining
                Toast.makeText(this@NotificationTestActivity, "3-day reminder transaction created", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreate2DayReminderTransaction).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransactionForFirebaseFunctions(5) // 5 days ago = 2 days remaining
                Toast.makeText(this@NotificationTestActivity, "2-day reminder transaction created", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreate1DayReminderTransaction).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransactionForFirebaseFunctions(6) // 6 days ago = 1 day remaining
                Toast.makeText(this@NotificationTestActivity, "1-day reminder transaction created", Toast.LENGTH_SHORT).show()
            }
        }
    }
}